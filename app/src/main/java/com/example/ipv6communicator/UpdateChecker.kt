package com.example.ipv6communicator

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {

    private val client = OkHttpClient()
    private const val BASE_URL = "http://yaohu.dynv6.net:32996"

    fun checkForUpdate(context: androidx.appcompat.app.AppCompatActivity) {
        context.lifecycleScope.launch {
            val result = checkVersion()
            withContext(Dispatchers.Main) {
                if (result != null && result.needUpdate) {
                    showUpdateDialog(context, result)
                }
            }
        }
    }

    private suspend fun checkVersion(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/app/version")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        if (json.getBoolean("success")) {
                            UpdateInfo(
                                versionCode = json.getInt("version_code"),
                                versionName = json.getString("version_name"),
                                downloadUrl = json.getString("download_url"),
                                updateMessage = json.getString("update_message")
                            )
                        } else null
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun showUpdateDialog(context: androidx.appcompat.app.AppCompatActivity, info: UpdateInfo) {
        AlertDialog.Builder(context)
            .setTitle("发现新版本 ${info.versionName}")
            .setMessage(info.updateMessage)
            .setPositiveButton("立即下载") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                context.startActivity(intent)
            }
            .setNegativeButton("稍后再说", null)
            .setCancelable(false)
            .show()
    }

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val updateMessage: String,
        val needUpdate: Boolean = true
    )
}
