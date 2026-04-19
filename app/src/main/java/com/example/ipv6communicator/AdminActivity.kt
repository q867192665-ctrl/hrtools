package com.example.ipv6communicator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AdminActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvSignedUsers: TextView
    private lateinit var tvUnsignedUsers: TextView
    private lateinit var spinnerYear: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var listView: ListView
    private lateinit var btnExportReport: Button
    private lateinit var btnAddUser: Button
    private lateinit var btnManageUsers: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    private val client = OkHttpClient()
    private var baseUrl = "http://[240e:b8f:bd8d:5f00:bd6a:e157:b50b:713d]:5000"
    private var token: String = ""
    private var username: String = ""
    private var selectedYear: String = ""
    private var selectedMonth: String = ""
    
    private var userList = mutableListOf<UserStatus>()
    
    private val autoLogoutHandler = Handler(Looper.getMainLooper())
    private val autoLogoutRunnable = Runnable { 
        Toast.makeText(this, "长时间未操作，已自动退出登录", Toast.LENGTH_SHORT).show()
        performLogout(true)
    }
    private var isAutoLogoutEnabled = true

    companion object {
        private const val REQUEST_CODE_RESIGNATURE = 2001
        private const val AUTO_LOGOUT_DELAY_MS = 5 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        loadUserInfo()
        initViews()
        loadUserStatus()
        loadStatistics()
        startAutoLogoutTimer()
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLogoutTimer()
    }
    
    override fun onPause() {
        super.onPause()
        stopAutoLogoutTimer()
    }
    
    override fun onResume() {
        super.onResume()
        if (isAutoLogoutEnabled) {
            startAutoLogoutTimer()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoLogoutTimer()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvSignedUsers = findViewById(R.id.tvSignedUsers)
        tvUnsignedUsers = findViewById(R.id.tvUnsignedUsers)
        spinnerYear = findViewById(R.id.spinnerYear)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        listView = findViewById(R.id.listView)
        btnExportReport = findViewById(R.id.btnExportReport)
        btnAddUser = findViewById(R.id.btnAddUser)
        btnManageUsers = findViewById(R.id.btnManageUsers)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)

        tvWelcome.text = "管理员，$username"

        setupMonthSelector()

        btnExportReport.setOnClickListener { exportReport() }
        btnAddUser.setOnClickListener { showAddUserDialog() }
        btnManageUsers.setOnClickListener { showManageUsersDialog() }
        btnLogout.setOnClickListener { logout() }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val user = userList[position]
            showUserDetails(user)
        }
    }

    private fun setupMonthSelector() {
        val currentYear = java.time.LocalDate.now().year
        val currentMonth = java.time.LocalDate.now().monthValue
        
        val years = mutableListOf<String>()
        for (year in currentYear downTo currentYear - 5) {
            years.add("${year}年")
        }
        
        val months = mutableListOf<String>()
        for (month in 1..12) {
            months.add("${month}月")
        }
        
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter
        
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        
        spinnerYear.setSelection(0)
        spinnerMonth.setSelection(currentMonth - 1)
        
        selectedYear = currentYear.toString()
        selectedMonth = currentMonth.toString()
        
        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position].replace("年", "")
                loadStatistics()
                loadUserStatus()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = months[position].replace("月", "")
                loadStatistics()
                loadUserStatus()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
        val savedAddress = sharedPref.getString("server_address", "240e:b8f:bd8d:5f00:bd6a:e157:b50b:713d")
        baseUrl = buildUrl(savedAddress ?: "")
        
        android.util.Log.d("AdminActivity", "Token: ${if (token.isNotEmpty()) "已设置" else "未设置"}")
        android.util.Log.d("AdminActivity", "BaseUrl: $baseUrl")
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

    private fun loadStatistics() {
        lifecycleScope.launch {
            val result = fetchStatistics()
            if (result.success) {
                tvTotalUsers.text = "总用户: ${result.totalUsers}"
                tvSignedUsers.text = "已签收: ${result.signedUsers}"
                tvUnsignedUsers.text = "未签收: ${result.unsignedUsers}"
            }
        }
    }

    private suspend fun fetchStatistics(): StatisticsResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/api/admin/statistics?year=$selectedYear&month=$selectedMonth"
                
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val data = json.getJSONObject("data")
                        StatisticsResult(
                            success = true,
                            totalUsers = data.getInt("total_users"),
                            signedUsers = data.getInt("signed_users"),
                            unsignedUsers = data.getInt("unsigned_users")
                        )
                    } else {
                        StatisticsResult(success = false)
                    }
                } else {
                    StatisticsResult(success = false)
                }
            } catch (e: Exception) {
                StatisticsResult(success = false)
            }
        }
    }

    private fun loadUserStatus() {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = fetchUserStatus()
            showLoading(false)
            
            if (result.success) {
                userList.clear()
                userList.addAll(result.data ?: emptyList())
                updateListView()
            } else {
                Toast.makeText(this@AdminActivity, "获取用户状态失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchUserStatus(): UserStatusResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/api/admin/status?year=$selectedYear&month=$selectedMonth"
                
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONArray("data")
                        val users = mutableListOf<UserStatus>()
                        
                        for (i in 0 until data.length()) {
                            val userJson = data.getJSONObject(i)
                            users.add(UserStatus(
                                username = userJson.getString("username"),
                                viewed = userJson.getBoolean("viewed"),
                                signed = userJson.getBoolean("signed"),
                                signaturePath = userJson.optString("signature_path"),
                                salaryDate = userJson.optString("salary_date"),
                                lastLogin = userJson.optString("last_login")
                            ))
                        }
                        
                        UserStatusResult(success = true, data = users)
                    } else {
                        UserStatusResult(success = false, error = jsonResponse.getString("error"))
                    }
                } else {
                    UserStatusResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                UserStatusResult(success = false, error = "获取失败: ${e.message}")
            }
        }
    }

    private fun updateListView() {
        val adapter = ArrayAdapter(this, R.layout.list_item_user_status, 
            userList.map { user ->
                "${user.username} - 已查看: ${if (user.viewed) "是" else "否"} - 已签名: ${if (user.signed) "是" else "否"}"
            }
        )
        listView.adapter = adapter
    }

    private fun showUserDetails(user: UserStatus) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("用户详情 - ${user.username}")
        
        val details = """
            用户名: ${user.username}
            已查看工资: ${if (user.viewed) "是" else "否"}
            已签名: ${if (user.signed) "是" else "否"}
            工资日期: ${user.salaryDate}
            最后登录: ${user.lastLogin}
        """.trimIndent()
        
        builder.setMessage(details)
        
        if (user.signed) {
            builder.setPositiveButton("查看签名") { _, _ ->
                viewUserSignature(user.username)
            }
            builder.setNeutralButton("下载签名") { _, _ ->
                downloadUserSignature(user.username)
            }
        }
        
        builder.setNegativeButton("重新签名") { _, _ ->
            requestResignature(user.username)
        }
        
        builder.show()
    }

    private fun viewUserSignature(username: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = fetchUserSignature(username)
            showLoading(false)
            
            if (result.success && result.bitmap != null) {
                showSignatureImage(result.bitmap, username)
            } else {
                Toast.makeText(this@AdminActivity, "查看签名失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchUserSignature(username: String): SignatureResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/signature/$username")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val inputStream: InputStream? = response.body?.byteStream()

                if (response.isSuccessful && inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    SignatureResult(success = true, bitmap = bitmap)
                } else {
                    SignatureResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                SignatureResult(success = false, error = "获取签名失败: ${e.message}")
            }
        }
    }

    private fun showSignatureImage(bitmap: Bitmap, username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("${username}的签名")
        
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.adjustViewBounds = true
        imageView.maxHeight = 600
        imageView.maxWidth = 600
        
        builder.setView(imageView)
        builder.setPositiveButton("下载") { _, _ ->
            downloadSignatureBitmap(bitmap, username)
        }
        builder.setNegativeButton("关闭", null)
        builder.show()
    }

    private fun downloadUserSignature(username: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = fetchUserSignature(username)
            showLoading(false)
            
            if (result.success && result.bitmap != null) {
                downloadSignatureBitmap(result.bitmap, username)
            } else {
                Toast.makeText(this@AdminActivity, "下载签名失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadSignatureBitmap(bitmap: Bitmap, username: String) {
        try {
            val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val signatureFile = File(downloadsDir, "${username}_signature.png")
            
            val outputStream = FileOutputStream(signatureFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            Toast.makeText(this, "签名已下载到: ${signatureFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestResignature(username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("要求重新签名")
        builder.setMessage("确定要要求 $username 重新签名吗？")
        
        builder.setPositiveButton("确定") { _, _ ->
            lifecycleScope.launch {
                val result = requestResign(username)
                if (result.success) {
                    Toast.makeText(this@AdminActivity, "已要求重新签名", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminActivity, "操作失败: ${result.error}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private suspend fun requestResign(username: String): SimpleResult {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject()
                jsonBody.put("target_user", username)
                
                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/resign")
                    .header("Authorization", token)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    SimpleResult(success = json.getBoolean("success"), error = json.optString("error"))
                } else {
                    SimpleResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                SimpleResult(success = false, error = e.message)
            }
        }
    }

    private fun downloadAllSignatures() {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = downloadAllSignaturesZip()
            showLoading(false)
            
            if (result.success) {
                Toast.makeText(this@AdminActivity, "所有签名已下载", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@AdminActivity, "下载失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun downloadAllSignaturesZip(): SimpleResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/signatures/download")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val inputStream: InputStream? = response.body?.byteStream()

                if (response.isSuccessful && inputStream != null) {
                    val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val zipFile = File(downloadsDir, "all_signatures.zip")
                    
                    val outputStream = FileOutputStream(zipFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    SimpleResult(success = true)
                } else {
                    SimpleResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                SimpleResult(success = false, error = e.message)
            }
        }
    }

    private fun exportCSV() {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = downloadCSV()
            showLoading(false)
            
            if (result.success) {
                Toast.makeText(this@AdminActivity, "CSV已导出", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@AdminActivity, "导出失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun downloadCSV(): SimpleResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/export-csv")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val inputStream: InputStream? = response.body?.byteStream()

                if (response.isSuccessful && inputStream != null) {
                    val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val csvFile = File(downloadsDir, "salary_export.csv")
                    
                    val outputStream = FileOutputStream(csvFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    SimpleResult(success = true)
                } else {
                    SimpleResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                SimpleResult(success = false, error = e.message)
            }
        }
    }

    private fun exportReport() {
        showLoading(true)
        
        lifecycleScope.launch {
            val months = fetchAvailableMonths()
            showLoading(false)
            
            if (months.isEmpty()) {
                Toast.makeText(this@AdminActivity, "没有可用的工资月份", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val monthArray = months.toTypedArray()
            AlertDialog.Builder(this@AdminActivity)
                .setTitle("选择月份")
                .setItems(monthArray) { _, which ->
                    val selectedMonth = months[which]
                    exportFullReport(selectedMonth)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    private suspend fun fetchAvailableMonths(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/available-months")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val monthsArray = json.getJSONArray("months")
                        val months = mutableListOf<String>()
                        for (i in 0 until monthsArray.length()) {
                            months.add(monthsArray.getString(i))
                        }
                        return@withContext months
                    }
                }
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    private fun exportFullReport(month: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = downloadFullReport(month)
            showLoading(false)
            
            if (result.success) {
                Toast.makeText(this@AdminActivity, "完整签收记录已导出", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@AdminActivity, "导出失败: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun downloadFullReport(month: String): SimpleResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/export-report?type=full&month=$month")
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val inputStream: InputStream? = response.body?.byteStream()

                if (response.isSuccessful && inputStream != null) {
                    val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val monthDisplay = month.replace("-", "年") + "月"
                    val reportFile = File(downloadsDir, "${monthDisplay}签收记录表.xlsx")
                    
                    val outputStream = FileOutputStream(reportFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    SimpleResult(success = true)
                } else {
                    val errorBody = response.body?.string()
                    val errorMsg = if (errorBody != null) {
                        try {
                            val json = JSONObject(errorBody)
                            json.getString("error")
                        } catch (e: Exception) {
                            "HTTP ${response.code}"
                        }
                    } else {
                        "HTTP ${response.code}"
                    }
                    SimpleResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                SimpleResult(success = false, error = e.message)
            }
        }
    }

    private fun showDeviceManageDialog() {
        lifecycleScope.launch {
            val result = fetchDeviceList()
            if (result != null) {
                val (devices, error) = result
                if (devices != null && devices.isNotEmpty()) {
                    val deviceItems = devices.map { device ->
                        "${device.username} - ${device.deviceId.substring(0, 8)}... (${device.deviceInfo})"
                    }.toTypedArray()
                    
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("设备绑定列表")
                        .setItems(deviceItems) { _, which ->
                            val device = devices[which]
                            showDeviceOptionsDialog(device)
                        }
                        .setNegativeButton("关闭", null)
                        .show()
                } else if (devices != null && devices.isEmpty()) {
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("设备绑定列表")
                        .setMessage("暂无设备绑定记录")
                        .setPositiveButton("确定", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("加载失败")
                        .setMessage("错误: $error")
                        .setPositiveButton("确定", null)
                        .show()
                }
            } else {
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("加载失败")
                    .setMessage("网络请求失败，请检查网络连接")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    private suspend fun fetchDeviceList(): Pair<List<DeviceInfo>?, String?>? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/devices")
                    .header("Authorization", token)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val dataArray = json.getJSONArray("data")
                        val devices = mutableListOf<DeviceInfo>()
                        for (i in 0 until dataArray.length()) {
                            val deviceObj = dataArray.getJSONObject(i)
                            devices.add(DeviceInfo(
                                username = deviceObj.getString("username"),
                                deviceId = deviceObj.getString("device_id"),
                                deviceInfo = deviceObj.optString("device_info", "未知设备"),
                                boundAt = deviceObj.optString("bound_at", "")
                            ))
                        }
                        Pair(devices, null)
                    } else {
                        Pair(null, json.optString("error", "未知错误"))
                    }
                } else {
                    Pair(null, "HTTP ${response.code}: ${response.message}")
                }
            } catch (e: Exception) {
                Pair(null, e.message)
            }
        }
    }
    
    private fun showDeviceOptionsDialog(device: DeviceInfo) {
        val options = arrayOf("解绑设备", "查看详情")
        
        AlertDialog.Builder(this)
            .setTitle("设备管理: ${device.username}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmUnbindDevice(device.username)
                    1 -> showDeviceDetails(device)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun confirmUnbindDevice(username: String) {
        AlertDialog.Builder(this)
            .setTitle("确认解绑")
            .setMessage("确定要解绑用户 \"$username\" 的设备吗？")
            .setPositiveButton("解绑") { _, _ ->
                unbindDevice(username)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun unbindDevice(username: String) {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/devices/$username/unbind")
                    .header("Authorization", token)
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(this@AdminActivity, "设备解绑成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminActivity, json.optString("error", "解绑失败"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "解绑失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeviceDetails(device: DeviceInfo) {
        val message = """
            用户: ${device.username}
            设备ID: ${device.deviceId}
            设备信息: ${device.deviceInfo}
            绑定时间: ${device.boundAt}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("设备详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun logout() {
        performLogout(false)
    }
    
    private fun performLogout(isAuto: Boolean) {
        stopAutoLogoutTimer()
        isAutoLogoutEnabled = false
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/logout")
                    .header("Authorization", token)
                    .post("".toRequestBody())
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) {
            }
        }
        
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun startAutoLogoutTimer() {
        stopAutoLogoutTimer()
        autoLogoutHandler.postDelayed(autoLogoutRunnable, AUTO_LOGOUT_DELAY_MS)
    }
    
    private fun resetAutoLogoutTimer() {
        if (isAutoLogoutEnabled) {
            startAutoLogoutTimer()
        }
    }
    
    private fun stopAutoLogoutTimer() {
        autoLogoutHandler.removeCallbacks(autoLogoutRunnable)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnExportReport.isEnabled = !show
    }
    
    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.etRole)
        
        val roles = arrayOf("普通用户", "管理员")
        val roleValues = arrayOf("user", "admin")
        spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        
        AlertDialog.Builder(this)
            .setTitle("添加用户")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString()
                val role = roleValues[spinnerRole.selectedItemPosition]
                
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addUser(username, password, role)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun addUser(username: String, password: String, role: String) {
        lifecycleScope.launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                    put("role", role)
                }
                
                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/add")
                    .header("Authorization", token)
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(this@AdminActivity, "用户添加成功", Toast.LENGTH_SHORT).show()
                        loadUserStatus()
                        loadStatistics()
                    } else {
                        Toast.makeText(this@AdminActivity, "添加失败: ${json.optString("error", "未知错误")}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminActivity, "添加失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showManageUsersDialog() {
        lifecycleScope.launch {
            val result = fetchUserList()
            if (result != null) {
                val (users, error) = result
                if (users != null && users.isNotEmpty()) {
                    val userNames = users.map { it.first }.toTypedArray()
                    
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("选择用户管理")
                        .setItems(userNames) { _, which ->
                            showUserOptionsDialog(users[which].first, users[which].second)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else if (users != null && users.isEmpty()) {
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("用户列表")
                        .setMessage("暂无用户数据")
                        .setPositiveButton("确定", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("加载失败")
                        .setMessage("错误: $error")
                        .setPositiveButton("确定", null)
                        .show()
                }
            } else {
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("加载失败")
                    .setMessage("网络请求失败，请检查网络连接")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    private suspend fun fetchUserList(): Pair<List<Pair<String, String>>?, String?>? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AdminActivity", "fetchUserList: baseUrl=$baseUrl, token=${if (token.isNotEmpty()) "已设置" else "未设置"}")
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users")
                    .header("Authorization", token)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                android.util.Log.d("AdminActivity", "fetchUserList response: code=${response.code}, body=$responseBody")
                
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val dataArray = json.getJSONArray("data")
                        val users = mutableListOf<Pair<String, String>>()
                        for (i in 0 until dataArray.length()) {
                            val userObj = dataArray.getJSONObject(i)
                            val name = userObj.getString("姓名")
                            val role = userObj.optString("role", "user")
                            users.add(Pair(name, role))
                        }
                        Pair(users, null)
                    } else {
                        Pair(null, json.optString("error", "未知错误"))
                    }
                } else {
                    Pair(null, "HTTP ${response.code}: ${response.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminActivity", "fetchUserList error", e)
                Pair(null, e.message)
            }
        }
    }
    
    private fun showUserOptionsDialog(username: String, role: String) {
        val options = arrayOf("修改密码", "删除用户")
        
        AlertDialog.Builder(this)
            .setTitle("管理用户: $username (${if (role == "admin") "管理员" else "普通用户"})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showChangePasswordDialog(username)
                    1 -> deleteUser(username)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showChangePasswordDialog(username: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        
        AlertDialog.Builder(this)
            .setTitle("修改密码: $username")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newPassword = etNewPassword.text.toString()
                if (newPassword.length < 4) {
                    Toast.makeText(this, "密码长度至少4位", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                changePassword(username, newPassword)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun changePassword(username: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("new_password", newPassword)
                }
                
                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/$username/password")
                    .header("Authorization", token)
                    .put(body)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(this@AdminActivity, "密码修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminActivity, "修改失败: ${json.optString("error", "未知错误")}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminActivity, "修改失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteUser(username: String) {
        if (username == "admin") {
            Toast.makeText(this, "不能删除admin账户", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除用户 \"$username\" 吗？此操作不可恢复！")
            .setPositiveButton("删除") { _, _ ->
                performDeleteUser(username)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performDeleteUser(username: String) {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/$username")
                    .header("Authorization", token)
                    .delete()
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(this@AdminActivity, "用户已删除", Toast.LENGTH_SHORT).show()
                        loadUserStatus()
                        loadStatistics()
                    } else {
                        Toast.makeText(this@AdminActivity, "删除失败: ${json.optString("error", "未知错误")}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminActivity, "删除失败: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class UserStatus(
    val username: String,
    val viewed: Boolean,
    val signed: Boolean,
    val signaturePath: String?,
    val salaryDate: String,
    val lastLogin: String
)

data class UserStatusResult(
    val success: Boolean,
    val data: List<UserStatus>? = null,
    val error: String? = null
)

data class DeviceInfo(
    val username: String,
    val deviceId: String,
    val deviceInfo: String,
    val boundAt: String
)

data class SignatureResult(
    val success: Boolean,
    val bitmap: Bitmap? = null,
    val error: String? = null
)

data class SimpleResult(
    val success: Boolean,
    val error: String? = null
)

data class StatisticsResult(
    val success: Boolean,
    val totalUsers: Int = 0,
    val signedUsers: Int = 0,
    val unsignedUsers: Int = 0
)
