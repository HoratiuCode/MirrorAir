package com.mirrornode.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReceiverService : Service() {

    private lateinit var discoveryManager: AirPlayDiscoveryManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isRunning = false
    private lateinit var currentConfig: ReceiverConfig

    override fun onCreate() {
        super.onCreate()
        discoveryManager = AirPlayDiscoveryManager(this)
        currentConfig = ReceiverConfig.load(this)
        createNotificationChannel()
        updateState(
            status = "Receiver idle",
            running = false,
            detail = "Press Start Receiver to advertise ${currentConfig.receiverName} on Wi-Fi.",
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopReceiverAndSelf()
            else -> startReceiver()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        shutdownReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startReceiver() {
        currentConfig = ReceiverConfig.load(this)
        val foregroundStarted = runCatching {
            startForeground(NOTIFICATION_ID, notification(currentConfig, "Starting receiver..."))
        }.isSuccess

        if (!foregroundStarted) {
            updateState(
                status = "Receiver could not start",
                running = false,
                detail = "Foreground service startup was blocked on this device.",
            )
            stopSelf()
            return
        }

        if (isRunning) {
            updateState(
                status = "Receiver advertised on Wi-Fi",
                running = true,
                detail = "The app is visible on the network, but Mac mirroring is not fully active in this build yet.",
            )
            return
        }

        val started = runCatching {
            acquireMulticastLock()
            ReceiverNativeBridge.startReceiver(
                receiverName = currentConfig.receiverName,
                configPath = ReceiverConfig.configFile(this).absolutePath,
                airPlayPort = currentConfig.airPlayPort,
                raopPort = currentConfig.raopPort,
            )
        }.getOrElse {
            shutdownReceiver()
            updateState(
                status = "Receiver engine failed",
                running = false,
                detail = it.message ?: "The receiver could not start.",
            )
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
            return
        }

        isRunning = started
        if (started) {
            discoveryManager.register(
                config = currentConfig,
                onRegistered = {
                    updateState(
                        status = "Receiver advertised on Wi-Fi",
                        running = true,
                        detail = "The app is advertising ${currentConfig.receiverName}, but the full Mac mirroring engine is still being integrated.",
                    )
                },
                onFailure = { reason ->
                    shutdownReceiver()
                    updateState(
                        status = "Receiver network error",
                        running = false,
                        detail = reason,
                    )
                    stopSelf()
                },
            )
            updateState(
                status = "Starting network receiver...",
                running = true,
                detail = "Advertising ${currentConfig.receiverName} on Wi-Fi. This build is discovery-only until the AirPlay engine is integrated.",
            )
        } else {
            shutdownReceiver()
            updateState(
                status = "Receiver engine failed",
                running = false,
                detail = "The native mirroring engine returned an error during startup.",
            )
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopReceiverAndSelf() {
        shutdownReceiver()
        stopSelf()
    }

    private fun shutdownReceiver() {
        if (isRunning) {
            ReceiverNativeBridge.stopReceiver()
        }
        isRunning = false
        discoveryManager.unregister()
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        multicastLock = null
        updateState(
            status = "Receiver stopped",
            running = false,
            detail = "Press Start Receiver to advertise this device again.",
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun acquireMulticastLock() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("mirrornode-multicast").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun notification(config: ReceiverConfig, status: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(config.receiverName)
            .setContentText(status)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MirrorAir Receiver",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun updateState(status: String, running: Boolean, detail: String) {
        stateFlow.value = ReceiverState(
            receiverName = currentConfig.receiverName,
            receiverCode = currentConfig.receiverCode,
            statusText = status,
            running = running,
            detailText = detail,
        )
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val manager = getSystemService(NotificationManager::class.java)
            runCatching {
                manager.notify(NOTIFICATION_ID, notification(currentConfig, status))
            }
        }
    }

    companion object {
        const val ACTION_START = "com.mirrornode.app.action.START"
        const val ACTION_STOP = "com.mirrornode.app.action.STOP"

        private const val NOTIFICATION_CHANNEL_ID = "mirrornode.receiver"
        private const val NOTIFICATION_ID = 1001

        private val stateFlow = MutableStateFlow(
            ReceiverState(
                receiverName = "MirrorAir",
                receiverCode = "0000",
                statusText = "Receiver idle",
                running = false,
                detailText = "Press Start Receiver to advertise MirrorAir on Wi-Fi.",
            ),
        )

        fun states() = stateFlow.asStateFlow()

        fun startIntent(context: Context) = Intent(context, ReceiverService::class.java).apply {
            action = ACTION_START
        }

        fun stopIntent(context: Context) = Intent(context, ReceiverService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
