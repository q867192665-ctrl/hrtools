package com.example.ipv6communicator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_IPV6_ADDRESS = "240e:b8f:bd8d:5f00:887c:2ed1:5c80:5877"
        private const val TIMEOUT_MS = 5000
    }

    private lateinit var etIPv6Address: EditText
    private lateinit var btnCheck: Button
    private lateinit var cardResult: CardView
    private lateinit var tvResultStatus: TextView
    private lateinit var tvResultDetails: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setDefaultAddress()
        setupClickListeners()
    }

    private fun initViews() {
        etIPv6Address = findViewById(R.id.etIPv6Address)
        btnCheck = findViewById(R.id.btnCheck)
        cardResult = findViewById(R.id.cardResult)
        tvResultStatus = findViewById(R.id.tvResultStatus)
        tvResultDetails = findViewById(R.id.tvResultDetails)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setDefaultAddress() {
        etIPv6Address.setText(DEFAULT_IPV6_ADDRESS)
    }

    private fun setupClickListeners() {
        btnCheck.setOnClickListener {
            checkIPv6Reachability()
        }
    }

    private fun checkIPv6Reachability() {
        val address = etIPv6Address.text.toString().trim()

        if (address.isEmpty()) {
            Toast.makeText(this, R.string.empty_address, Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidIPv6Address(address)) {
            Toast.makeText(this, R.string.invalid_ipv6, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        cardResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = performIPv6Check(address)
            showResult(result)
            showLoading(false)
        }
    }

    private fun isValidIPv6Address(address: String): Boolean {
        return try {
            val inetAddress = InetAddress.getByName(address)
            inetAddress is Inet6Address
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun performIPv6Check(address: String): ReachabilityResult {
        return withContext(Dispatchers.IO) {
            try {
                val inetAddress = InetAddress.getByName(address)
                val startTime = System.currentTimeMillis()
                val isReachable = inetAddress.isReachable(TIMEOUT_MS)
                val latency = System.currentTimeMillis() - startTime

                if (isReachable) {
                    ReachabilityResult(
                        success = true,
                        message = getString(R.string.status_reachable),
                        latency = latency
                    )
                } else {
                    ReachabilityResult(
                        success = false,
                        message = getString(R.string.status_unreachable),
                        latency = latency
                    )
                }
            } catch (e: Exception) {
                ReachabilityResult(
                    success = false,
                    message = "${getString(R.string.status_unreachable)}: ${e.message}",
                    latency = 0
                )
            }
        }
    }

    private fun showResult(result: ReachabilityResult) {
        cardResult.visibility = View.VISIBLE

        if (result.success) {
            tvResultStatus.text = result.message
            tvResultStatus.setTextColor(getColor(R.color.success))
            tvResultDetails.text = "延迟: ${result.latency}ms"
        } else {
            tvResultStatus.text = result.message
            tvResultStatus.setTextColor(getColor(R.color.error))
            tvResultDetails.text = "地址: ${etIPv6Address.text}"
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnCheck.isEnabled = !show
        btnCheck.text = if (show) getString(R.string.checking) else getString(R.string.check_button)
    }

    data class ReachabilityResult(
        val success: Boolean,
        val message: String,
        val latency: Long
    )
}
