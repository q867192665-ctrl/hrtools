package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MenuActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button

    private val client = OkHttpClient()
    private var token: String = ""
    private var username: String = ""
    private val baseUrl = "http://yaohu.dynv6.net:32996"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        initViews()
        loadUserInfo()
        setupClickListeners()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""

        tvWelcome.text = "欢迎，$username"
    }

    private fun setupClickListeners() {
        val cardSalary = findViewById<androidx.cardview.widget.CardView>(R.id.cardSalary)
        cardSalary.setOnClickListener {
            goToSalary()
        }

        val cardLeave = findViewById<androidx.cardview.widget.CardView>(R.id.cardLeave)
        cardLeave.setOnClickListener {
            goToLeave()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun goToSalary() {
        val intent = Intent(this, SalaryActivity::class.java)
        startActivity(intent)
    }

    private fun goToLeave() {
        val intent = Intent(this, LeaveActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
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
}
