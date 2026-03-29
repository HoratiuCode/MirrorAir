package com.mirrornode.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val receiverNameView = findViewById<TextView>(R.id.receiverNameText)
        val statusView = findViewById<TextView>(R.id.statusText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        ensureNotificationPermission()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReceiverService.states().collect { state ->
                    receiverNameView.text = state.receiverName
                    statusView.text = state.statusText
                    startButton.isEnabled = !state.running
                    stopButton.isEnabled = state.running
                }
            }
        }

        startButton.setOnClickListener {
            ContextCompat.startForegroundService(this, ReceiverService.startIntent(this))
        }

        stopButton.setOnClickListener {
            startService(ReceiverService.stopIntent(this))
        }

        ContextCompat.startForegroundService(this, ReceiverService.startIntent(this))
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
    }

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 2001
    }
}
