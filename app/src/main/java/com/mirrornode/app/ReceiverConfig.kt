package com.mirrornode.app

import android.content.Context
import java.io.File
import java.util.Properties

data class ReceiverConfig(
    val receiverName: String,
    val airPlayPort: Int,
    val raopPort: Int,
) {
    companion object {
        private const val CONFIG_FILE_NAME = "receiver.properties"
        private const val DEFAULT_NAME = "MirrorNode"
        private const val DEFAULT_AIRPLAY_PORT = 7000
        private const val DEFAULT_RAOP_PORT = 5000

        fun load(context: Context): ReceiverConfig {
            val configFile = ensureConfigFile(context)
            val properties = Properties().apply {
                configFile.inputStream().use(::load)
            }

            return ReceiverConfig(
                receiverName = properties.getProperty("receiver.name", DEFAULT_NAME),
                airPlayPort = properties.getProperty("airplay.port", DEFAULT_AIRPLAY_PORT.toString()).toInt(),
                raopPort = properties.getProperty("raop.port", DEFAULT_RAOP_PORT.toString()).toInt(),
            )
        }

        fun configFile(context: Context): File = File(context.filesDir, CONFIG_FILE_NAME)

        private fun ensureConfigFile(context: Context): File {
            val destination = configFile(context)
            if (!destination.exists()) {
                context.assets.open(CONFIG_FILE_NAME).use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            return destination
        }
    }
}
