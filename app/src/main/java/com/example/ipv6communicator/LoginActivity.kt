package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRememberPassword: CheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private val client = OkHttpClient()
    private val baseUrl = "http://yaohu.dynv6.net:32996"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        loadSavedCredentials()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        cbRememberPassword = findViewById(R.id.cbRememberPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun generateDeviceId(): String {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val packageName = packageName
        val signature = packageManager.getPackageInfo(
            packageName,
            android.content.pm.PackageManager.GET_SIGNATURES
        ).signatures?.firstOrNull()?.toCharsString() ?: ""

        val combined = "$androidId:$packageName:$signature"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(combined.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun getDeviceInfo(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val version = android.os.Build.VERSION.RELEASE
        return "$manufacturer $model (Android $version)"
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            vibrate()
            performLogin()
        }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedCredentials() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        val savedUsername = sharedPref.getString("saved_username", "")
        val savedPassword = sharedPref.getString("saved_password", "")
        val rememberPassword = sharedPref.getBoolean("remember_password", false)

        if (rememberPassword && savedUsername != null && savedPassword != null) {
            etUsername.setText(savedUsername)
            etPassword.setText(savedPassword)
            cbRememberPassword.isChecked = true
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showError("请输入用户名和密码")
            return
        }

        showLoading(true)
        hideError()

        val deviceId = generateDeviceId()
        val deviceInfo = getDeviceInfo()

        lifecycleScope.launch {
            val result = loginUser(username, password, deviceId, deviceInfo)
            showLoading(false)

            if (result.success) {
                val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("token", result.token)
                    putString("username", username)
                    putString("role", result.role)

                    if (cbRememberPassword.isChecked) {
                        putString("saved_username", username)
                        putString("saved_password", password)
                        putBoolean("remember_password", true)
                    } else {
                        remove("saved_username")
                        remove("saved_password")
                        putBoolean("remember_password", false)
                    }

                    apply()
                }

                UpdateChecker.checkForUpdate(this@LoginActivity)

                val intent = Intent(this@LoginActivity, MenuActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                showError(result.error ?: "登录失败")
            }
        }
    }

    private suspend fun loginUser(username: String, password: String, deviceId: String, deviceInfo: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("name", username)
                    .put("password", password)
                    .put("device_id", deviceId)
                    .put("device_info", deviceInfo)
                    .toString()

                val requestBody = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/login")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        LoginResult(
                            success = true,
                            token = jsonResponse.getString("token"),
                            role = jsonResponse.getString("role")
                        )
                    } else {
                        LoginResult(
                            success = false,
                            error = jsonResponse.getString("error")
                        )
                    }
                } else if (response.code == 401 && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    LoginResult(
                        success = false,
                        error = jsonResponse.optString("error", "用户名或密码错误")
                    )
                } else if (response.code == 403 && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    LoginResult(
                        success = false,
                        error = jsonResponse.getString("error")
                    )
                } else {
                    LoginResult(
                        success = false,
                        error = "网络错误"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                LoginResult(
                    success = false,
                    error = "网络错误"
                )
            } catch (e: java.net.SocketTimeoutException) {
                LoginResult(
                    success = false,
                    error = "网络错误"
                )
            } catch (e: java.net.ConnectException) {
                LoginResult(
                    success = false,
                    error = "网络错误"
                )
            } catch (e: Exception) {
                LoginResult(
                    success = false,
                    error = "网络错误"
                )
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "登录中..." else "登录"
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = android.view.View.GONE
    }

    data class LoginResult(
        val success: Boolean,
        val token: String = "",
        val role: String = "user",
        val error: String? = null
    )
}
