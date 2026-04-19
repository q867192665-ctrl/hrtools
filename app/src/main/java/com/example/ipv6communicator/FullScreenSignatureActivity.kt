package com.example.ipv6communicator

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class FullScreenSignatureActivity : AppCompatActivity() {

    private lateinit var signatureView: SignatureView
    private lateinit var btnClear: Button
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var tvInstruction: TextView

    private val client = OkHttpClient()
    private val baseUrl = "http://yaohu.dynv6.net:32996"
    private var token: String = ""
    private var username: String = ""
    private var isAdminAction = false
    private var targetUsername: String? = null
    private var currentMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_fullscreen_signature)

        initViews()
        loadUserInfo()
        setupIntentData()
    }

    private fun initViews() {
        signatureView = findViewById(R.id.signatureView)
        btnClear = findViewById(R.id.btnClear)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)
        tvInstruction = findViewById(R.id.tvInstruction)

        btnClear.setOnClickListener { clearSignature() }
        btnConfirm.setOnClickListener { submitSignature() }
        btnCancel.setOnClickListener { cancelSignature() }

        // 设置签名区域为全屏
        signatureView.setFullScreenMode(true)
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
    }

    private fun setupIntentData() {
        isAdminAction = intent.getBooleanExtra("is_admin_action", false)
        targetUsername = intent.getStringExtra("target_username")
        currentMonth = intent.getStringExtra("current_month") ?: ""
        
        if (isAdminAction && targetUsername != null) {
            tvInstruction.text = "请为 ${targetUsername} 重新签名"
        } else {
            tvInstruction.text = "请在下方区域签名"
        }
    }

    private fun clearSignature() {
        signatureView.clear()
        Toast.makeText(this, "签名已清除", Toast.LENGTH_SHORT).show()
    }

    private fun submitSignature() {
        if (!signatureView.hasSignature()) {
            Toast.makeText(this, "请先签名", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = uploadSignature()
            
            if (result.success) {
                Toast.makeText(this@FullScreenSignatureActivity, "签名提交成功", Toast.LENGTH_SHORT).show()
                
                // 返回结果
                val resultIntent = Intent()
                resultIntent.putExtra("signature_success", true)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this@FullScreenSignatureActivity, "签名提交失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadSignature(): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                val signatureBitmap = signatureView.getSignatureBitmap()
                if (signatureBitmap == null) {
                    return@withContext UploadResult(success = false, error = "没有签名数据")
                }

                val byteArrayOutputStream = ByteArrayOutputStream()
                signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val signatureBytes = byteArrayOutputStream.toByteArray()
                val base64Signature = Base64.getEncoder().encodeToString(signatureBytes)

                // 后端API期望的格式
                val json = JSONObject().apply {
                    put("signature", base64Signature)  // 纯Base64字符串，不带前缀
                    put("format", "PNG")  // 指定格式
                    put("month", currentMonth)  // 当前查询的月份
                }.toString()

                val requestBody = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/signature")
                    .header("Authorization", token)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        UploadResult(success = true)
                    } else {
                        UploadResult(success = false, error = jsonResponse.getString("error"))
                    }
                } else {
                    UploadResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                UploadResult(success = false, error = "上传失败: ${e.message}")
            }
        }
    }

    private fun cancelSignature() {
        val resultIntent = Intent()
        resultIntent.putExtra("signature_success", false)
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        cancelSignature()
    }

    data class UploadResult(
        val success: Boolean,
        val error: String? = null
    )
}