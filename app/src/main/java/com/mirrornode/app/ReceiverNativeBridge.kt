package com.mirrornode.app

import android.view.Surface

object ReceiverNativeBridge {
    init {
        System.loadLibrary("mirrornode_receiver")
    }

    external fun startReceiver(
        receiverName: String,
        configPath: String,
        airPlayPort: Int,
        raopPort: Int,
    ): Boolean

    external fun setVideoSurface(surface: Surface?)

    external fun stopReceiver()
}
