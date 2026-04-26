package com.example.ipv6communicator

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LeaveActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var spinnerLeaveType: Spinner
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var etLeaveDays: EditText
    private lateinit var etReason: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnViewRecords: Button
    private lateinit var btnBackToMenu: Button
    private lateinit var btnLogout: Button

    private val client = OkHttpClient()
    private val baseUrl = "http://yaohu.dynv6.net:32996"
    
    private var token: String = ""
    private var username: String = ""
    
    private val autoLogoutHandler = Handler(Looper.getMainLooper())
    private val autoLogoutRunnable = Runnable { 
        Toast.makeText(this, "长时间未操作，已自动退出登录", Toast.LENGTH_SHORT).show()
        performLogout(true)
    }
    
    companion object {
        private const val AUTO_LOGOUT_DELAY_MS = 5 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave)

        initViews()
        loadUserInfo()
        startAutoLogoutTimer()
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLogoutTimer()
    }
    
    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        spinnerLeaveType = findViewById(R.id.spinnerLeaveType)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        etLeaveDays = findViewById(R.id.etLeaveDays)
        etReason = findViewById(R.id.etReason)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnViewRecords = findViewById(R.id.btnViewRecords)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        btnLogout = findViewById(R.id.btnLogout)

        val leaveTypes = arrayOf("事假", "其他")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, leaveTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLeaveType.adapter = adapter

        etStartDate.setOnClickListener { showDatePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }

        btnSubmit.setOnClickListener { 
            vibrate()
            submitLeave() 
        }
        btnViewRecords.setOnClickListener { 
            vibrate()
            viewRecords() 
        }
        btnBackToMenu.setOnClickListener { 
            vibrate()
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnLogout.setOnClickListener { 
            vibrate()
            performLogout(false) 
        }
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
        
        tvWelcome.text = "欢迎，$username"
    }

    private fun showDatePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                if (isStart) {
                    etStartDate.setText(date)
                } else {
                    etEndDate.setText(date)
                }
                calculateLeaveDays()
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun calculateLeaveDays() {
        val startDateStr = etStartDate.text.toString()
        val endDateStr = etEndDate.text.toString()
        
        if (startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = sdf.parse(startDateStr)
                val endDate = sdf.parse(endDateStr)
                
                if (startDate != null && endDate != null) {
                    val diff = endDate.time - startDate.time
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                    etLeaveDays.setText(days.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun submitLeave() {
        val leaveType = spinnerLeaveType.selectedItem.toString()
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()
        val leaveDaysStr = etLeaveDays.text.toString().trim()
        val reason = etReason.text.toString().trim()

        if (startDate.isEmpty()) {
            Toast.makeText(this, "请选择开始日期", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDate.isEmpty()) {
            Toast.makeText(this, "请选择结束日期", Toast.LENGTH_SHORT).show()
            return
        }

        if (leaveDaysStr.isEmpty()) {
            Toast.makeText(this, "请输入请假天数", Toast.LENGTH_SHORT).show()
            return
        }

        val leaveDays = leaveDaysStr.toDoubleOrNull()
        if (leaveDays == null || leaveDays <= 0) {
            Toast.makeText(this, "请假天数必须大于0", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("leave_type", leaveType)
                    put("start_date", startDate)
                    put("end_date", endDate)
                    put("leave_days", leaveDays)
                    put("reason", reason)
                }

                val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/api/leave/submit")
                    .addHeader("Authorization", token)
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val responseBody = withContext(Dispatchers.IO) { response.body?.string() }

                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@LeaveActivity, "请假申请提交成功", Toast.LENGTH_SHORT).show()
                        clearForm()
                    } else {
                        val error = jsonResponse.optString("error", "提交失败")
                        Toast.makeText(this@LeaveActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@LeaveActivity, "提交失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        spinnerLeaveType.setSelection(0)
        etStartDate.text.clear()
        etEndDate.text.clear()
        etLeaveDays.text.clear()
        etReason.text.clear()
    }

    private fun viewRecords() {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/leave/my-records")
                    .addHeader("Authorization", token)
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val responseBody = withContext(Dispatchers.IO) { response.body?.string() }

                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val dataArray = jsonResponse.getJSONArray("data")
                        showRecordsDialog(dataArray)
                    } else {
                        val error = jsonResponse.optString("error", "查询失败")
                        Toast.makeText(this@LeaveActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@LeaveActivity, "查询失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRecordsDialog(dataArray: org.json.JSONArray) {
        if (dataArray.length() == 0) {
            AlertDialog.Builder(this)
                .setTitle("请假记录")
                .setMessage("暂无请假记录")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        val records = StringBuilder()
        for (i in 0 until dataArray.length()) {
            val record = dataArray.getJSONObject(i)
            records.append("类型: ${record.getString("请假类型")}\n")
            records.append("日期: ${record.getString("开始日期")} 至 ${record.getString("结束日期")}\n")
            records.append("天数: ${record.getDouble("请假天数")}天\n")
            records.append("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("请假记录")
            .setMessage(records.toString())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun performLogout(isAuto: Boolean) {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/logout")
                    .addHeader("Authorization", token)
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()

                withContext(Dispatchers.IO) { client.newCall(request).execute() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                
                val intent = Intent(this@LeaveActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun startAutoLogoutTimer() {
        autoLogoutHandler.postDelayed(autoLogoutRunnable, AUTO_LOGOUT_DELAY_MS)
    }

    private fun resetAutoLogoutTimer() {
        autoLogoutHandler.removeCallbacks(autoLogoutRunnable)
        startAutoLogoutTimer()
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

    override fun onDestroy() {
        super.onDestroy()
        autoLogoutHandler.removeCallbacks(autoLogoutRunnable)
    }
}
