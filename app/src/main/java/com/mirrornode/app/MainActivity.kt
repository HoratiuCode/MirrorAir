package com.mirrornode.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableReceiverMode()
        setContentView(R.layout.activity_main)

        val receiverNameView = findViewById<TextView>(R.id.receiverNameText)
        val statusView = findViewById<TextView>(R.id.statusText)
        val detailView = findViewById<TextView>(R.id.detailText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)
        val videoSurface = findViewById<SurfaceView>(R.id.videoSurface)

        ensureNotificationPermission()
        bindRenderSurface(videoSurface.holder)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReceiverService.states().collect { state ->
                    receiverNameView.text = state.receiverName
                    statusView.text = state.statusText
                    detailView.text = state.detailText
                    startButton.isEnabled = !state.running
                    stopButton.isEnabled = state.running
                    detailView.visibility = if (state.detailText.isBlank()) View.GONE else View.VISIBLE
                }
            }
        }

        startButton.setOnClickListener {
            ContextCompat.startForegroundService(this, ReceiverService.startIntent(this))
        }

        stopButton.setOnClickListener {
            startService(ReceiverService.stopIntent(this))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun enableReceiverMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
    }

    private fun hideSystemUi() {
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun bindRenderSurface(holder: SurfaceHolder) {
        holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    ReceiverNativeBridge.setVideoSurface(holder.surface)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    ReceiverNativeBridge.setVideoSurface(holder.surface)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    ReceiverNativeBridge.setVideoSurface(null)
                }
            },
        )
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
