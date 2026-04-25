package com.example.ipv6communicator

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {

    private val client = OkHttpClient()
    // 本地测试用局域网IP，正式环境改为 yaohu.dynv6.net
    private const val BASE_URL = "http://172.20.16.230:32996"
    private var hasCheckedThisSession = false

    fun checkForUpdate(context: Context) {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        val request = Request.Builder()
            .url("$BASE_URL/api/app/version")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val json = JSONObject(body)
                            if (json.getBoolean("success")) {
                                val versionCode = json.getInt("version_code")
                                val versionName = json.getString("version_name")
                                val downloadUrl = json.getString("download_url")
                                val updateMessage = json.getString("update_message")

                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    showUpdateDialog(context, versionName, downloadUrl, updateMessage)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(context: Context, versionName: String, downloadUrl: String, updateMessage: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("发现新版本 $versionName")
            .setMessage(updateMessage)
            .setPositiveButton("立即下载") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
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
