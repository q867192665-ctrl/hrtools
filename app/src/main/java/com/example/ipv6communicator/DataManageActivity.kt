package com.example.ipv6communicator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DataManageActivity : AppCompatActivity() {

    private lateinit var btnExport: MaterialButton
    private lateinit var btnSelectFile: MaterialButton
    private lateinit var btnImport: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var progressExport: ProgressBar
    private lateinit var progressImport: ProgressBar
    private lateinit var tvExportStatus: TextView
    private lateinit var tvImportStatus: TextView
    private lateinit var tvSelectedFile: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView

    private val client = OkHttpClient()
    private var token: String = ""
    private var username: String = ""
    private val baseUrl = "http://yaohu.dynv6.net:32996"

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    companion object {
        private const val PICK_JSON_FILE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_manage)

        loadUserInfo()
        initViews()
        setupClickListeners()

        val mode = intent.getStringExtra("mode") ?: ""
        if (mode == "export") {
            tvTitle.text = "数据导出"
            tvSubtitle.text = "将系统数据导出为备份文件"
        } else if (mode == "import") {
            tvTitle.text = "数据导入"
            tvSubtitle.text = "从备份文件恢复数据"
        }
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
    }

    private fun initViews() {
        btnExport = findViewById(R.id.btnExport)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        btnImport = findViewById(R.id.btnImport)
        btnBack = findViewById(R.id.btnBack)
        progressExport = findViewById(R.id.progressExport)
        progressImport = findViewById(R.id.progressImport)
        tvExportStatus = findViewById(R.id.tvExportStatus)
        tvImportStatus = findViewById(R.id.tvImportStatus)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
    }

    private fun setupClickListeners() {
        btnExport.setOnClickListener { confirmExport() }
        btnSelectFile.setOnClickListener { openFilePicker() }
        btnImport.setOnClickListener { confirmImport() }
        btnBack.setOnClickListener { finish() }
    }

    private fun confirmExport() {
        AlertDialog.Builder(this)
            .setTitle("确认导出")
            .setMessage("确定要导出所有数据库数据吗？\n导出过程可能需要一些时间。")
            .setPositiveButton("确定") { _, _ -> performExport() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performExport() {
        btnExport.isEnabled = false
        progressExport.visibility = ProgressBar.VISIBLE
        tvExportStatus.visibility = TextView.VISIBLE
        tvExportStatus.text = "正在导出数据..."

        lifecycleScope.launch {
            val result = doExport()
            btnExport.isEnabled = true
            progressExport.visibility = ProgressBar.GONE

            if (result.success) {
                tvExportStatus.text = "导出成功！文件已保存到:\n${result.filePath}"
                Toast.makeText(this@DataManageActivity, "数据导出成功", Toast.LENGTH_LONG).show()
            } else {
                tvExportStatus.text = "导出失败: ${result.error}"
                Toast.makeText(this@DataManageActivity, "导出失败: ${result.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun doExport(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/data/export")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    var errorMsg = "HTTP ${response.code}"
                    try {
                        val json = JSONObject(errorBody ?: "")
                        errorMsg = json.optString("error", errorMsg)
                    } catch (_: Exception) {}
                    return@withContext ExportResult(success = false, error = errorMsg)
                }

                val inputStream = response.body?.byteStream()
                if (inputStream == null) {
                    return@withContext ExportResult(success = false, error = "响应为空")
                }

                val contentDisposition = response.header("Content-Disposition") ?: ""
                var fileName = "full_backup.json"
                if (contentDisposition.contains("filename=")) {
                    fileName = contentDisposition.substringAfter("filename=").replace("\"", "").trim()
                }

                val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(downloadsDir, fileName)

                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                inputStream.close()

                ExportResult(success = true, filePath = outputFile.absolutePath)
            } catch (e: Exception) {
                ExportResult(success = false, error = e.message ?: "未知错误")
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json"))
        }
        startActivityForResult(intent, PICK_JSON_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri = uri
                selectedFileName = uri.lastPathSegment ?: "未知文件"
                tvSelectedFile.text = "已选择: $selectedFileName"
                btnImport.isEnabled = true
            }
        }
    }

    private fun confirmImport() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "请先选择备份文件", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("⚠️ 确认导入")
            .setMessage("确定要导入数据吗？\n\n导入操作将向数据库中插入或更新数据，此操作不可撤销！\n\n建议在导入前先执行一次数据导出备份。")
            .setPositiveButton("确定导入") { _, _ -> performImport() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performImport() {
        btnImport.isEnabled = false
        progressImport.visibility = ProgressBar.VISIBLE
        tvImportStatus.visibility = TextView.VISIBLE
        tvImportStatus.text = "正在导入数据..."

        lifecycleScope.launch {
            val result = doImport()
            btnImport.isEnabled = true
            progressImport.visibility = ProgressBar.GONE

            if (result.success) {
                tvImportStatus.text = "导入成功！\n${result.message}"
                Toast.makeText(this@DataManageActivity, "数据导入成功", Toast.LENGTH_LONG).show()
            } else {
                tvImportStatus.text = "导入失败: ${result.error}"
                Toast.makeText(this@DataManageActivity, "导入失败: ${result.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun doImport(): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val uri = selectedFileUri ?: return@withContext ImportResult(success = false, error = "未选择文件")

                val tempFile = File(cacheDir, "import_backup.json")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext ImportResult(success = false, error = "无法读取文件")

                val requestBody = tempFile.asRequestBody("application/json".toMediaType())
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, requestBody)
                    .addFormDataPart("mode", "merge")
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/api/admin/data/import")
                    .header("Authorization", token)
                    .post(multipartBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                tempFile.delete()

                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        val message = json.optString("message", "导入完成")
                        return@withContext ImportResult(success = true, message = message)
                    } else {
                        return@withContext ImportResult(success = false, error = json.optString("error", "导入失败"))
                    }
                } else {
                    return@withContext ImportResult(success = false, error = "服务器无响应")
                }
            } catch (e: Exception) {
                ImportResult(success = false, error = e.message ?: "未知错误")
            }
        }
    }

    data class ExportResult(val success: Boolean, val filePath: String = "", val error: String = "")
    data class ImportResult(val success: Boolean, val message: String = "", val error: String = "")
}
