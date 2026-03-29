package com.mirrornode.app

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

    external fun stopReceiver()
}
