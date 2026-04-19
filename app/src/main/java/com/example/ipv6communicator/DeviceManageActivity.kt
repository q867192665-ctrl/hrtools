package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class DeviceManageActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnBack: Button
    private lateinit var btnLogout: Button
    private lateinit var btnRefreshBindings: Button
    private lateinit var btnRefreshAttempts: Button
    private lateinit var bindingsContainer: LinearLayout
    private lateinit var attemptsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    
    private val client = OkHttpClient()
    private var token: String = ""
    private var username: String = ""
    private var baseUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_manage)

        loadUserInfo()
        initViews()
        loadDeviceBindings()
        loadFailedAttempts()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        btnRefreshBindings = findViewById(R.id.btnRefreshBindings)
        btnRefreshAttempts = findViewById(R.id.btnRefreshAttempts)
        bindingsContainer = findViewById(R.id.bindingsContainer)
        attemptsContainer = findViewById(R.id.attemptsContainer)
        progressBar = findViewById(R.id.progressBar)

        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener { logout() }
        btnRefreshBindings.setOnClickListener { loadDeviceBindings() }
        btnRefreshAttempts.setOnClickListener { loadFailedAttempts() }
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
        val savedAddress = sharedPref.getString("server_address", "")
        baseUrl = buildUrl(savedAddress ?: "")
        
        tvWelcome.text = "欢迎，$username"
    }

    private fun buildUrl(address: String): String {
        val trimmed = address.trim()
        return if (trimmed.contains(":") && !trimmed.startsWith("[")) {
            "http://[$trimmed]:5000"
        } else if (trimmed.startsWith("[")) {
            "http://$trimmed"
        } else {
            "http://$trimmed:5000"
        }
    }

    private fun loadDeviceBindings() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/device-bindings")
                    .header("Authorization", token)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val bindings = jsonResponse.getJSONArray("bindings")
                        displayBindings(bindings)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误，请检查网络连接和服务器状态"
                Toast.makeText(this@DeviceManageActivity, "加载失败: $errorMsg", Toast.LENGTH_LONG).show()
                android.util.Log.e("DeviceManageActivity", "loadDeviceBindings error", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayBindings(bindings: JSONArray) {
        bindingsContainer.removeAllViews()
        
        if (bindings.length() == 0) {
            val emptyView = TextView(this)
            emptyView.text = "暂无设备绑定记录"
            emptyView.setTextColor(getColor(R.color.gray))
            emptyView.setPadding(20, 40, 20, 40)
            emptyView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            bindingsContainer.addView(emptyView)
            return
        }
        
        for (i in 0 until bindings.length()) {
            val binding = bindings.getJSONObject(i)
            val username = binding.getString("username")
            val deviceId = binding.optString("device_id", "-")
            val deviceInfo = binding.optString("device_info", "-")
            val boundAt = binding.optString("bound_at", "-")
            val lastLogin = binding.optString("last_login_at", "-")
            
            val itemView = layoutInflater.inflate(R.layout.item_device_binding, null)
            
            itemView.findViewById<TextView>(R.id.tvUsername).text = username
            itemView.findViewById<TextView>(R.id.tvDeviceId).text = if (deviceId.length > 16) deviceId.substring(0, 16) + "..." else deviceId
            itemView.findViewById<TextView>(R.id.tvDeviceInfo).text = deviceInfo
            itemView.findViewById<TextView>(R.id.tvBoundAt).text = boundAt
            itemView.findViewById<TextView>(R.id.tvLastLogin).text = lastLogin
            
            itemView.findViewById<Button>(R.id.btnUnbind).setOnClickListener {
                showUnbindConfirmDialog(username)
            }
            
            bindingsContainer.addView(itemView)
        }
    }

    private fun loadFailedAttempts() {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/failed-attempts?limit=50")
                    .header("Authorization", token)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val attempts = jsonResponse.getJSONArray("attempts")
                        displayAttempts(attempts)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误，请检查网络连接和服务器状态"
                Toast.makeText(this@DeviceManageActivity, "加载失败: $errorMsg", Toast.LENGTH_LONG).show()
                android.util.Log.e("DeviceManageActivity", "loadFailedAttempts error", e)
            }
        }
    }

    private fun displayAttempts(attempts: JSONArray) {
        attemptsContainer.removeAllViews()
        
        if (attempts.length() == 0) {
            val emptyView = TextView(this)
            emptyView.text = "暂无异常登录记录"
            emptyView.setTextColor(getColor(R.color.gray))
            emptyView.setPadding(20, 40, 20, 40)
            emptyView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            attemptsContainer.addView(emptyView)
            return
        }
        
        for (i in 0 until attempts.length()) {
            val attempt = attempts.getJSONObject(i)
            val username = attempt.optString("username", "-")
            val deviceId = attempt.optString("device_id", "-")
            val deviceInfo = attempt.optString("device_info", "-")
            val matchedUsername = attempt.optString("matched_username", "-")
            val attemptTime = attempt.optString("attempt_time", "-")
            
            val itemView = layoutInflater.inflate(R.layout.item_failed_attempt, null)
            
            itemView.findViewById<TextView>(R.id.tvUsername).text = username
            itemView.findViewById<TextView>(R.id.tvDeviceId).text = if (deviceId.length > 16) deviceId.substring(0, 16) + "..." else deviceId
            itemView.findViewById<TextView>(R.id.tvDeviceInfo).text = deviceInfo
            itemView.findViewById<TextView>(R.id.tvMatchedUser).text = matchedUsername
            itemView.findViewById<TextView>(R.id.tvAttemptTime).text = attemptTime
            
            attemptsContainer.addView(itemView)
        }
    }

    private fun showUnbindConfirmDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("确认解绑")
            .setMessage("确定要解绑用户 \"$username\" 的设备吗？\n解绑后用户需要重新绑定设备才能登录。")
            .setPositiveButton("解绑") { _, _ ->
                unbindDevice(username)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun unbindDevice(username: String) {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                    .put("username", username)
                    .toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/unbind-device")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@DeviceManageActivity, "解绑成功", Toast.LENGTH_SHORT).show()
                        loadDeviceBindings()
                    } else {
                        Toast.makeText(this@DeviceManageActivity, "解绑失败: ${jsonResponse.getString("error")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceManageActivity, "解绑失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("token")
            remove("username")
            remove("role")
            apply()
        }
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
