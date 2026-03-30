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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableReceiverMode()
        setContentView(R.layout.activity_main)

        val receiverNameView = findViewById<TextView>(R.id.receiverNameText)
        val receiverCodeView = findViewById<TextView>(R.id.receiverCodeText)
        val statusView = findViewById<TextView>(R.id.statusText)
        val detailView = findViewById<TextView>(R.id.detailText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)
        val videoSurface = findViewById<SurfaceView>(R.id.videoSurface)

        bindRenderSurface(videoSurface.holder)
        ensureRuntimePermissions()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReceiverService.states().collect { state ->
                    receiverNameView.text = state.receiverName
                    receiverCodeView.text = getString(R.string.receiver_code_value, state.receiverCode)
                    statusView.text = state.statusText
                    detailView.text = state.detailText
                    startButton.isEnabled = !state.running
                    stopButton.isEnabled = state.running
                    detailView.visibility = if (state.detailText.isBlank()) View.GONE else View.VISIBLE
                }
            }
        }

        startButton.setOnClickListener {
            startReceiverIfPermissionsGranted()
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
                    runCatching {
                        ReceiverNativeBridge.setVideoSurface(holder.surface)
                    }.onFailure { throwable ->
                        ReceiverService.reportRuntimeIssue(
                            throwable.message ?: "The Android video surface could not be attached on this device.",
                        )
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    runCatching {
                        ReceiverNativeBridge.setVideoSurface(holder.surface)
                    }.onFailure { throwable ->
                        ReceiverService.reportRuntimeIssue(
                            throwable.message ?: "The Android video surface could not be updated on this device.",
                        )
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    runCatching {
                        ReceiverNativeBridge.setVideoSurface(null)
                    }
                }
            },
        )
    }

    private fun ensureRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val missingPermissions = buildList {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startReceiverIfPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED
        ) {
            ensureRuntimePermissions()
            return
        }

        runCatching {
            ContextCompat.startForegroundService(this, ReceiverService.startIntent(this))
        }.onFailure { throwable ->
            ReceiverService.reportRuntimeIssue(
                throwable.message ?: "The receiver service could not be started on this device.",
            )
        }
    }
}
