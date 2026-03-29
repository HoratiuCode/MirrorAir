package com.mirrornode.app

import android.view.Surface

object ReceiverNativeBridge {
    @Volatile
    private var loadAttempted = false

    @Volatile
    private var loaded = false

    @Volatile
    private var loadError: String? = null

    @Synchronized
    private fun ensureLoaded(): Boolean {
        if (loadAttempted) {
            return loaded
        }

        loadAttempted = true
        return runCatching {
            System.loadLibrary("mirrornode_receiver")
            loaded = true
            true
        }.getOrElse { throwable ->
            loadError = throwable.message ?: throwable.javaClass.simpleName
            loaded = false
            false
        }
    }

    fun isAvailable(): Boolean = ensureLoaded()

    fun lastErrorOrNull(): String? {
        ensureLoaded()
        return loadError
    }

    fun startReceiver(
        receiverName: String,
        configPath: String,
        airPlayPort: Int,
        raopPort: Int,
    ): Boolean {
        check(ensureLoaded()) { loadError ?: "Native receiver library could not be loaded." }
        return nativeStartReceiver(receiverName, configPath, airPlayPort, raopPort)
    }

    fun setVideoSurface(surface: Surface?) {
        if (ensureLoaded()) {
            nativeSetVideoSurface(surface)
        }
    }

    fun stopReceiver() {
        if (ensureLoaded()) {
            nativeStopReceiver()
        }
    }

    private external fun nativeStartReceiver(
        receiverName: String,
        configPath: String,
        airPlayPort: Int,
        raopPort: Int,
    ): Boolean

    private external fun nativeSetVideoSurface(surface: Surface?)

    private external fun nativeStopReceiver()
}
