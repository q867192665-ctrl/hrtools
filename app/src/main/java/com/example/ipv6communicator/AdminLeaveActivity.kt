package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AdminLeaveActivity : AppCompatActivity() {

    private lateinit var tvTotalCount: TextView
    private lateinit var tvTotalDays: TextView
    private lateinit var tvMonthDays: TextView
    private lateinit var spinnerYear: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var etSearchName: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvLeaveRecords: RecyclerView
    private lateinit var tvNoRecords: TextView
    private lateinit var btnBack: Button
    private lateinit var btnLogout: Button

    private val client = OkHttpClient()
    private var baseUrl = ""
    private var token = ""
    private var username = ""
    private var selectedYear: String = ""
    private var selectedMonth: String = ""
    
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
        setContentView(R.layout.activity_admin_leave)

        loadUserInfo()
        initViews()
        loadLeaveRecords()
        startAutoLogoutTimer()
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLogoutTimer()
    }

    private fun initViews() {
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvTotalDays = findViewById(R.id.tvTotalDays)
        tvMonthDays = findViewById(R.id.tvMonthDays)
        spinnerYear = findViewById(R.id.spinnerYear)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        etSearchName = findViewById(R.id.etSearchName)
        btnSearch = findViewById(R.id.btnSearch)
        rvLeaveRecords = findViewById(R.id.rvLeaveRecords)
        tvNoRecords = findViewById(R.id.tvNoRecords)
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)

        rvLeaveRecords.layoutManager = LinearLayoutManager(this)

        setupMonthSelector()

        btnSearch.setOnClickListener { loadLeaveRecords() }
        btnBack.setOnClickListener { 
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnLogout.setOnClickListener { performLogout(false) }
    }

    private fun setupMonthSelector() {
        val currentYear = java.time.LocalDate.now().year
        val currentMonth = java.time.LocalDate.now().monthValue
        
        val years = mutableListOf<String>()
        for (year in currentYear downTo currentYear - 5) {
            years.add("${year}年")
        }
        
        val months = mutableListOf<String>()
        months.add("全部")
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
        spinnerMonth.setSelection(0)
        
        selectedYear = currentYear.toString()
        selectedMonth = ""
        
        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position].replace("年", "")
                loadLeaveRecords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = if (position == 0) "" else months[position].replace("月", "")
                loadLeaveRecords()
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
        
        android.util.Log.d("AdminLeaveActivity", "Token: ${if (token.isNotEmpty()) "已设置" else "未设置"}")
        android.util.Log.d("AdminLeaveActivity", "BaseUrl: $baseUrl")
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

    private fun loadLeaveRecords() {
        val name = etSearchName.text.toString().trim()
        
        android.util.Log.d("AdminLeaveActivity", "loadLeaveRecords: baseUrl=$baseUrl, token=${if (token.isNotEmpty()) "已设置" else "未设置"}")
        
        lifecycleScope.launch {
            try {
                var url = "$baseUrl/api/admin/leave/records?year=$selectedYear"
                if (selectedMonth.isNotEmpty()) {
                    url += "&month=$selectedMonth"
                }
                if (name.isNotEmpty()) {
                    url += "&name=${java.net.URLEncoder.encode(name, "UTF-8")}"
                }

                android.util.Log.d("AdminLeaveActivity", "Request URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                android.util.Log.d("AdminLeaveActivity", "Response: code=${response.code}, body=$responseBody")

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val dataArray = json.getJSONArray("data")
                        val records = mutableListOf<LeaveRecord>()
                        
                        var totalCount = 0
                        var totalDays = 0.0
                        var monthDays = 0.0
                        
                        val currentMonth = java.time.LocalDate.now().monthValue
                        val currentYear = java.time.LocalDate.now().year
                        
                        val filterYear = selectedYear.toIntOrNull() ?: currentYear
                        val filterMonth = selectedMonth.toIntOrNull()

                        for (i in 0 until dataArray.length()) {
                            val recordObj = dataArray.getJSONObject(i)
                            val record = LeaveRecord(
                                name = recordObj.getString("姓名"),
                                leaveType = recordObj.getString("请假类型"),
                                startDate = recordObj.getString("开始日期"),
                                endDate = recordObj.getString("结束日期"),
                                leaveDays = recordObj.getDouble("请假天数"),
                                reason = recordObj.optString("请假原因", ""),
                                applyTime = recordObj.optString("申请时间", "")
                            )
                            records.add(record)
                            
                            totalCount++
                            totalDays += record.leaveDays
                            
                            try {
                                val startDate = java.time.LocalDate.parse(record.startDate)
                                if (filterMonth != null) {
                                    if (startDate.year == filterYear && startDate.monthValue == filterMonth) {
                                        monthDays += record.leaveDays
                                    }
                                } else {
                                    if (startDate.year == filterYear) {
                                        monthDays += record.leaveDays
                                    }
                                }
                            } catch (e: Exception) {
                            }
                        }

                        tvTotalCount.text = totalCount.toString()
                        tvTotalDays.text = String.format("%.1f", totalDays)
                        tvMonthDays.text = String.format("%.1f", monthDays)

                        if (records.isNotEmpty()) {
                            rvLeaveRecords.adapter = LeaveRecordAdapter(records)
                            rvLeaveRecords.visibility = View.VISIBLE
                            tvNoRecords.visibility = View.GONE
                        } else {
                            rvLeaveRecords.visibility = View.GONE
                            tvNoRecords.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(this@AdminLeaveActivity, 
                            "加载失败: ${json.optString("error", "未知错误")}", 
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminLeaveActivity, 
                        "加载失败: HTTP ${response.code}", 
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误，请检查网络连接和服务器状态"
                android.util.Log.e("AdminLeaveActivity", "loadLeaveRecords error", e)
                Toast.makeText(this@AdminLeaveActivity, 
                    "加载失败: $errorMsg", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startAutoLogoutTimer() {
        autoLogoutHandler.postDelayed(autoLogoutRunnable, AUTO_LOGOUT_DELAY_MS)
    }
    
    private fun resetAutoLogoutTimer() {
        stopAutoLogoutTimer()
        startAutoLogoutTimer()
    }
    
    private fun stopAutoLogoutTimer() {
        autoLogoutHandler.removeCallbacks(autoLogoutRunnable)
    }
    
    private fun performLogout(isAuto: Boolean) {
        stopAutoLogoutTimer()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/logout")
                    .header("Authorization", token)
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) {
            }
        }

        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    data class LeaveRecord(
        val name: String,
        val leaveType: String,
        val startDate: String,
        val endDate: String,
        val leaveDays: Double,
        val reason: String,
        val applyTime: String
    )

    inner class LeaveRecordAdapter(private val records: List<LeaveRecord>) : 
        RecyclerView.Adapter<LeaveRecordAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leave_record, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = records[position]
            holder.tvName.text = record.name
            holder.tvLeaveType.text = record.leaveType
            holder.tvDateRange.text = "${record.startDate} 至 ${record.endDate}"
            holder.tvLeaveDays.text = "${record.leaveDays}天"
            holder.tvReason.text = if (record.reason.isNotEmpty()) record.reason else "无"
            holder.tvApplyTime.text = record.applyTime
        }

        override fun getItemCount() = records.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvLeaveType: TextView = view.findViewById(R.id.tvLeaveType)
            val tvDateRange: TextView = view.findViewById(R.id.tvDateRange)
            val tvLeaveDays: TextView = view.findViewById(R.id.tvLeaveDays)
            val tvReason: TextView = view.findViewById(R.id.tvReason)
            val tvApplyTime: TextView = view.findViewById(R.id.tvApplyTime)
        }
    }
}
