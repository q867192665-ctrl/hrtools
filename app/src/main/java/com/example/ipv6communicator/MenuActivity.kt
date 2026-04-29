package com.example.ipv6communicator

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MenuActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: MaterialButton

    private val client = OkHttpClient()
    private var token: String = ""
    private var username: String = ""
    private var role: String = ""
    private val baseUrl = "http://yaohu.dynv6.net:32996"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        initViews()
        loadUserInfo()
        setupClickListeners()

        UpdateChecker.checkForUpdate(this)
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("salary_system", MODE_PRIVATE)
        token = sharedPref.getString("token", "") ?: ""
        username = sharedPref.getString("username", "") ?: ""
        role = sharedPref.getString("role", "") ?: ""

        tvWelcome.text = "欢迎，$username"
    }

    private fun setupClickListeners() {
        val cardSalary = findViewById<androidx.cardview.widget.CardView>(R.id.cardSalary)
        cardSalary.setOnClickListener {
            vibrate()
            goToSalary()
        }

        val cardLeave = findViewById<androidx.cardview.widget.CardView>(R.id.cardLeave)
        cardLeave.setOnClickListener {
            vibrate()
            goToLeave()
        }

        btnLogout.setOnClickListener {
            vibrate()
            logout()
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
