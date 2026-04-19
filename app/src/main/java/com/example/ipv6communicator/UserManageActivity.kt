package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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

class UserManageActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnBack: Button
    private lateinit var btnLogout: Button
    private lateinit var btnAddUser: Button
    private lateinit var btnRefresh: Button
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    
    private val client = OkHttpClient()
    private var token: String = ""
    private var username: String = ""
    private var baseUrl: String = ""
    private var userList: JSONArray = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_manage)

        loadUserInfo()
        initViews()
        loadUserList()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        btnAddUser = findViewById(R.id.btnAddUser)
        btnRefresh = findViewById(R.id.btnRefresh)
        etSearch = findViewById(R.id.etSearch)
        progressBar = findViewById(R.id.progressBar)

        btnBack.setOnClickListener { goBack() }
        btnLogout.setOnClickListener { logout() }
        btnAddUser.setOnClickListener { showAddUserDialog() }
        btnRefresh.setOnClickListener { loadUserList() }
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

    private fun loadUserList() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users")
                    .header("Authorization", token)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        userList = jsonResponse.getJSONArray("data")
                        displayUserList(userList)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误，请检查网络连接和服务器状态"
                Toast.makeText(this@UserManageActivity, "加载失败: $errorMsg", Toast.LENGTH_LONG).show()
                android.util.Log.e("UserManageActivity", "loadUserList error", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayUserList(users: JSONArray) {
        val container = findViewById<android.widget.LinearLayout>(R.id.userListContainer)
        container.removeAllViews()
        
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            val name = user.getString("姓名")
            val phone = user.optString("手机号", "-")
            val department = user.optString("部门", "-")
            val role = user.optString("role", "user")
            val lastLogin = user.optString("last_login_time", "从未登录")
            
            val itemView = layoutInflater.inflate(R.layout.item_user_manage, null)
            
            itemView.findViewById<TextView>(R.id.tvName).text = name
            itemView.findViewById<TextView>(R.id.tvPhone).text = phone
            itemView.findViewById<TextView>(R.id.tvDepartment).text = department
            itemView.findViewById<TextView>(R.id.tvRole).text = if (role == "admin") "管理员" else "普通用户"
            itemView.findViewById<TextView>(R.id.tvLastLogin).text = lastLogin
            
            itemView.findViewById<Button>(R.id.btnEditInfo).setOnClickListener {
                showEditInfoDialog(name, phone, department)
            }
            
            itemView.findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
                showChangePasswordDialog(name)
            }
            
            val btnDelete = itemView.findViewById<Button>(R.id.btnDelete)
            if (name == "admin") {
                btnDelete.visibility = View.GONE
            } else {
                btnDelete.setOnClickListener {
                    showDeleteConfirmDialog(name)
                }
            }
            
            container.addView(itemView)
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etRole = dialogView.findViewById<android.widget.Spinner>(R.id.etRole)
        
        AlertDialog.Builder(this)
            .setTitle("添加用户")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val name = etUsername.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val password = etPassword.text.toString()
                val role = if (etRole.selectedItemPosition == 0) "user" else "admin"
                
                if (name.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addUser(name, phone, password, role)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addUser(name: String, phone: String, password: String, role: String) {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                    .put("username", name)
                    .put("phone", phone)
                    .put("password", password)
                    .put("role", role)
                    .toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/add")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@UserManageActivity, "添加成功", Toast.LENGTH_SHORT).show()
                        loadUserList()
                    } else {
                        Toast.makeText(this@UserManageActivity, "添加失败: ${jsonResponse.getString("error")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManageActivity, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditInfoDialog(name: String, phone: String, department: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etDepartment = dialogView.findViewById<EditText>(R.id.etDepartment)
        
        etPhone.setText(phone)
        etDepartment.setText(department)
        
        AlertDialog.Builder(this)
            .setTitle("修改信息 - $name")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                updateUserInfo(name, etPhone.text.toString().trim(), etDepartment.text.toString().trim())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateUserInfo(name: String, phone: String, department: String) {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                    .put("username", name)
                    .put("phone", phone)
                    .put("department", department)
                    .toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/profile")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@UserManageActivity, "修改成功", Toast.LENGTH_SHORT).show()
                        loadUserList()
                    } else {
                        Toast.makeText(this@UserManageActivity, "修改失败: ${jsonResponse.getString("error")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManageActivity, "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog(name: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password_admin, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        
        AlertDialog.Builder(this)
            .setTitle("修改密码 - $name")
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                val password = etPassword.text.toString()
                if (password.isEmpty()) {
                    Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                changePassword(name, password)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun changePassword(name: String, password: String) {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                    .put("username", name)
                    .put("new_password", password)
                    .toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/password")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@UserManageActivity, "密码修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@UserManageActivity, "修改失败: ${jsonResponse.getString("error")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManageActivity, "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除用户 \"$name\" 吗？此操作不可恢复！")
            .setPositiveButton("删除") { _, _ ->
                deleteUser(name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteUser(name: String) {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                    .put("username", name)
                    .toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/api/admin/users/delete")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(this@UserManageActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        loadUserList()
                    } else {
                        Toast.makeText(this@UserManageActivity, "删除失败: ${jsonResponse.getString("error")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManageActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goBack() {
        finish()
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
