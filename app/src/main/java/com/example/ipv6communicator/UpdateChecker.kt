package com.example.ipv6communicator

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {

    private val client = OkHttpClient()
    private const val BASE_URL = "http://yaohu.dynv6.net:32996"
    private var lastCheckTime: Long = 0
    private const val CHECK_INTERVAL_MS = 30 * 60 * 1000L // 30分钟检查一次

    fun checkForUpdate(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < CHECK_INTERVAL_MS) return
        lastCheckTime = now

        val currentVersion = getCurrentVersion(context)
        
        val request = Request.Builder()
            .url("$BASE_URL/api/app-version/check?version=$currentVersion")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.util.Log.e("UpdateChecker", "检查更新失败", e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val json = JSONObject(body)
                            if (json.getBoolean("success")) {
                                val needUpdate = json.getBoolean("need_update")
                                
                                if (needUpdate) {
                                    val latestVersion = json.getString("latest_version")
                                    val updateNote = json.optString("update_note", "有新版本可用")
                                    val downloadUrl = json.getString("download_url")

                                    android.util.Log.d("UpdateChecker", "发现新版本: $latestVersion")

                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        showUpdateDialog(context, latestVersion, downloadUrl, updateNote)
                                    }
                                } else {
                                    val latestVersion = json.optString("latest_version", "未知")
                                    android.util.Log.d("UpdateChecker", "已是最新版本，当前: $currentVersion, 服务器: $latestVersion")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UpdateChecker", "解析更新响应失败", e)
                        }
                    }
                } else {
                    android.util.Log.e("UpdateChecker", "检查更新请求失败，HTTP状态码: ${response.code}")
                }
            }
        })
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun showUpdateDialog(context: Context, versionName: String, downloadUrl: String, updateMessage: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("发现新版本 v${versionName}")
            .setMessage(updateMessage.ifEmpty { "有新版本可用，建议更新以获得更好体验" })
            .setPositiveButton("立即下载") { _, _ ->
                val fullUrl = if (downloadUrl.startsWith("http")) downloadUrl else "$BASE_URL$downloadUrl"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            .setNegativeButton("稍后再说", null)
            .setCancelable(false)
            .create()

        if (context is android.app.Activity && !context.isFinishing && !context.isDestroyed) {
            dialog.show()
        }
    }
}
