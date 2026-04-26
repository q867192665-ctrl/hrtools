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
    private var hasCheckedThisSession = false

    fun checkForUpdate(context: Context) {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        val currentVersion = getCurrentVersion(context)
        
        val request = Request.Builder()
            .url("$BASE_URL/api/app-version/check?version=$currentVersion")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
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

                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        showUpdateDialog(context, latestVersion, downloadUrl, updateNote)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
