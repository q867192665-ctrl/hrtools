package com.example.ipv6communicator

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

class SalaryActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvHireDate: TextView
    private lateinit var tvSocialSecurity: TextView
    private lateinit var tvShouldAttend: TextView
    private lateinit var tvActualAttend: TextView
    private lateinit var tvServiceHours: TextView
    private lateinit var tvBaseSalary: TextView
    private lateinit var tvMeritPay: TextView
    private lateinit var tvOtherSubsidy: TextView
    private lateinit var tvBaseSalaryTotal: TextView
    private lateinit var tvPositionSalary: TextView
    private lateinit var tvNurseMerit: TextView
    private lateinit var tvTransportFee: TextView
    private lateinit var tvPhoneFee: TextView
    private lateinit var tvBonus: TextView
    private lateinit var tvHighTempFee: TextView
    private lateinit var tvPension: TextView
    private lateinit var tvMedical: TextView
    private lateinit var tvUnemployment: TextView
    private lateinit var tvHousingFund: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvOtherDeduct: TextView
    private lateinit var tvOtherDonation: TextView
    private lateinit var tvAccommodationDonation: TextView
    private lateinit var tvUtilitiesDonation: TextView
    private lateinit var tvTotalDeduct: TextView
    private lateinit var tvNetSalary: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvSignatureStatus: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var btnQuery: Button
    private lateinit var btnSubmitSignature: Button
    private lateinit var btnBackToMenu: Button
    private lateinit var btnLogout: Button

    private val client = OkHttpClient()
    private val baseUrl = "http://yaohu.dynv6.net:32996"
    
    private var token: String = ""
    private var username: String = ""
    private var isSigned = false
    private var availableMonths: List<String> = emptyList()
    private var currentQueryMonth: String = ""
    
    private val autoLogoutHandler = Handler(Looper.getMainLooper())
    private val autoLogoutRunnable = Runnable { 
        Toast.makeText(this, "长时间未操作，已自动退出登录", Toast.LENGTH_SHORT).show()
        performLogout(true)
    }
    private var isAutoLogoutEnabled = true
    companion object {
        private const val REQUEST_CODE_FULLSCREEN_SIGNATURE = 1001
        private const val AUTO_LOGOUT_DELAY_MS = 5 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salary)

        initViews()
        loadUserInfo()
        loadSalaryData()
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
        tvDepartment = findViewById(R.id.tvDepartment)
        tvPosition = findViewById(R.id.tvPosition)
        tvHireDate = findViewById(R.id.tvHireDate)
        tvSocialSecurity = findViewById(R.id.tvSocialSecurity)
        tvShouldAttend = findViewById(R.id.tvShouldAttend)
        tvActualAttend = findViewById(R.id.tvActualAttend)
        tvServiceHours = findViewById(R.id.tvServiceHours)
        tvBaseSalary = findViewById(R.id.tvBaseSalary)
        tvMeritPay = findViewById(R.id.tvMeritPay)
        tvOtherSubsidy = findViewById(R.id.tvOtherSubsidy)
        tvBaseSalaryTotal = findViewById(R.id.tvBaseSalaryTotal)
        tvPositionSalary = findViewById(R.id.tvPositionSalary)
        tvNurseMerit = findViewById(R.id.tvNurseMerit)
        tvTransportFee = findViewById(R.id.tvTransportFee)
        tvPhoneFee = findViewById(R.id.tvPhoneFee)
        tvBonus = findViewById(R.id.tvBonus)
        tvHighTempFee = findViewById(R.id.tvHighTempFee)
        tvPension = findViewById(R.id.tvPension)
        tvMedical = findViewById(R.id.tvMedical)
        tvUnemployment = findViewById(R.id.tvUnemployment)
        tvHousingFund = findViewById(R.id.tvHousingFund)
        tvTax = findViewById(R.id.tvTax)
        tvOtherDeduct = findViewById(R.id.tvOtherDeduct)
        tvOtherDonation = findViewById(R.id.tvOtherDonation)
        tvAccommodationDonation = findViewById(R.id.tvAccommodationDonation)
        tvUtilitiesDonation = findViewById(R.id.tvUtilitiesDonation)
        tvTotalDeduct = findViewById(R.id.tvTotalDeduct)
        tvNetSalary = findViewById(R.id.tvNetSalary)
        tvMonth = findViewById(R.id.tvMonth)
        tvSignatureStatus = findViewById(R.id.tvSignatureStatus)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        btnQuery = findViewById(R.id.btnQuery)
        btnSubmitSignature = findViewById(R.id.btnSubmitSignature)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        btnLogout = findViewById(R.id.btnLogout)

        btnQuery.setOnClickListener { querySalaryByMonth() }
        btnSubmitSignature.setOnClickListener { submitSignature() }
        btnBackToMenu.setOnClickListener { backToMenu() }
        btnLogout.setOnClickListener { logout() }
    }

    private fun submitSignature() {
        val intent = Intent(this, FullScreenSignatureActivity::class.java)
        intent.putExtra("current_month", currentQueryMonth)
        startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_SIGNATURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_FULLSCREEN_SIGNATURE) {
            if (resultCode == RESULT_OK && data?.getBooleanExtra("signature_success", false) == true) {
                tvSignatureStatus.text = "状态：已签收"
                tvSignatureStatus.setTextColor(getColor(R.color.success))
                btnSubmitSignature.isEnabled = false
                isSigned = true
                Toast.makeText(this, "签名提交成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "签名已取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
        
        tvWelcome.text = "欢迎，$username"
    }

    private fun loadSalaryData() {
        lifecycleScope.launch {
            loadAvailableMonths()
            val result = fetchSalaryData("")
            if (result.success && result.data != null) {
                displaySalaryData(result.data)
            }
        }
    }

    private suspend fun loadAvailableMonths() {
        withContext(Dispatchers.Main) {
            val months = mutableListOf("全部月份")
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            
            for (month in 1..12) {
                months.add(String.format("%d-%02d", currentYear, month))
            }
            
            availableMonths = months
            val adapter = ArrayAdapter(this@SalaryActivity, 
                android.R.layout.simple_spinner_item, months)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMonth.adapter = adapter
        }
    }

    private fun querySalaryByMonth() {
        val selectedPosition = spinnerMonth.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= availableMonths.size) {
            Toast.makeText(this, "请先选择月份", Toast.LENGTH_SHORT).show()
            return
        }
        val month = if (selectedPosition == 0) "" else availableMonths[selectedPosition]
        currentQueryMonth = month
        
        lifecycleScope.launch {
            val result = fetchSalaryData(month)
            if (result.success && result.data != null) {
                displaySalaryData(result.data)
                Toast.makeText(this@SalaryActivity, "查询成功", Toast.LENGTH_SHORT).show()
            } else {
                val errorMsg = result.error ?: "获取工资信息失败"
                AlertDialog.Builder(this@SalaryActivity)
                    .setTitle("提示")
                    .setMessage(errorMsg)
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }
    }

    private suspend fun fetchSalaryData(month: String): SalaryResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = if (month.isNotEmpty()) "$baseUrl/api/salary?month=$month" else "$baseUrl/api/salary"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONObject("data")
                        val salary = data.getJSONObject("salary")
                        val summary = data.optJSONObject("summary")
                        val queryCount = data.getInt("query_count")
                        val signStatus = data.getString("sign_status")
                        val resignReason = data.optString("resign_reason", "")
                        
                        SalaryResult(
                            success = true,
                            data = SalaryData(
                                salary = salary,
                                summary = summary,
                                queryCount = queryCount,
                                signStatus = signStatus,
                                resignReason = resignReason
                            )
                        )
                    } else {
                        SalaryResult(success = false, error = jsonResponse.getString("error"))
                    }
                } else {
                    SalaryResult(success = false, error = "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                SalaryResult(success = false, error = "获取失败: ${e.message}")
            }
        }
    }

    private fun displaySalaryData(data: SalaryData) {
        val salary = data.salary
        
        val month = salary.optString("月份", "")
        if (month.isNotEmpty()) {
            currentQueryMonth = month
        }
        
        tvDepartment.text = salary.optString("部门", "--")
        tvPosition.text = salary.optString("岗位", "--")
        
        val hireDate = salary.optString("入职日期", "--")
        tvHireDate.text = if (hireDate.length > 10) hireDate.substring(0, 10) else hireDate
        
        tvSocialSecurity.text = salary.optString("是否代扣社保", "--")
        
        tvShouldAttend.text = "${salary.optDouble("应出勤天数", 0.0)} 天"
        tvActualAttend.text = "${salary.optDouble("实际出勤天数", 0.0)} 天"
        tvServiceHours.text = "${salary.optInt("上门服务小时", 0)} 小时"
        
        setMoneyField(tvBaseSalary, salary.optDouble("基本工资底薪", 0.0))
        setMoneyField(tvMeritPay, salary.optDouble("基本工资其它补贴", 0.0))
        setMoneyField(tvOtherSubsidy, salary.optDouble("基本工资其它补贴", 0.0))
        setMoneyField(tvBaseSalaryTotal, salary.optDouble("基本工资合计", 0.0))
        setMoneyField(tvPositionSalary, salary.optDouble("岗位工资", 0.0))
        setMoneyField(tvNurseMerit, salary.optDouble("护理员绩效工资", 0.0))
        setMoneyField(tvTransportFee, salary.optDouble("交通费", 0.0))
        setMoneyField(tvPhoneFee, salary.optDouble("手机费", 0.0))
        setMoneyField(tvBonus, salary.optDouble("奖金", 0.0))
        setMoneyField(tvHighTempFee, salary.optDouble("高温费", 0.0))
        
        tvPension.text = formatMoney(salary.optDouble("应扣款项养老", 0.0))
        tvMedical.text = formatMoney(salary.optDouble("应扣款项医疗", 0.0))
        tvUnemployment.text = formatMoney(salary.optDouble("应扣款项失业", 0.0))
        tvHousingFund.text = formatMoney(salary.optDouble("应扣款项公积金", 0.0))
        tvTax.text = formatMoney(salary.optDouble("应扣款项应缴个税", 0.0))
        setMoneyField(tvOtherDeduct, salary.optDouble("应扣款项缺勤扣款", 0.0))
        setMoneyField(tvOtherDonation, salary.optDouble("其它扣款", 0.0))
        setMoneyField(tvAccommodationDonation, salary.optDouble("住宿扣款", 0.0))
        setMoneyField(tvUtilitiesDonation, salary.optDouble("水电扣款", 0.0))
        
        val totalDeduct = salary.optDouble("应扣款项养老", 0.0) +
                          salary.optDouble("应扣款项医疗", 0.0) +
                          salary.optDouble("应扣款项失业", 0.0) +
                          salary.optDouble("应扣款项公积金", 0.0) +
                          salary.optDouble("应扣款项应缴个税", 0.0) +
                          salary.optDouble("应扣款项缺勤扣款", 0.0) +
                          salary.optDouble("其它扣款", 0.0) +
                          salary.optDouble("住宿扣款", 0.0) +
                          salary.optDouble("水电扣款", 0.0)
        tvTotalDeduct.text = formatMoney(totalDeduct)
        
        val netSalary = salary.optDouble("实发工资", 0.0)
        tvNetSalary.text = "¥ ${String.format("%.2f", netSalary)}"
        
        tvMonth.text = "查询次数：${data.queryCount}"

        if (data.signStatus == "已签收") {
            tvSignatureStatus.text = "状态：已签收"
            tvSignatureStatus.setTextColor(getColor(R.color.success))
            btnSubmitSignature.isEnabled = false
            isSigned = true
        } else {
            tvSignatureStatus.text = "状态：未签收"
            tvSignatureStatus.setTextColor(getColor(R.color.error))
            btnSubmitSignature.isEnabled = true
            
            if (data.resignReason.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("需要重新签名")
                    .setMessage("原因：${data.resignReason}\n\n请重新签名提交。")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }
    }
    
    private fun formatMoney(value: Double): String {
        return "${String.format("%.2f", value)} 元"
    }

    private fun setMoneyField(textView: TextView, value: Double) {
        if (value == 0.0) {
            if (textView.parent is android.view.ViewGroup) {
                (textView.parent as android.view.ViewGroup).visibility = android.view.View.GONE
            }
        } else {
            textView.text = formatMoney(value)
            if (textView.parent is android.view.ViewGroup) {
                (textView.parent as android.view.ViewGroup).visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun backToMenu() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
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

    data class SalaryData(
        val salary: JSONObject,
        val summary: JSONObject?,
        val queryCount: Int,
        val signStatus: String,
        val resignReason: String = ""
    )

    data class SalaryResult(
        val success: Boolean,
        val data: SalaryData? = null,
        val error: String? = null
    )

    data class UploadResult(
        val success: Boolean,
        val error: String? = null
    )
}
