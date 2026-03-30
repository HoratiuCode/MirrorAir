package com.mirrornode.app

import android.content.Context
import java.io.File
import java.util.Properties
import kotlin.random.Random

data class ReceiverConfig(
    val receiverName: String,
    val receiverCode: String,
    val airPlayPort: Int,
    val raopPort: Int,
    val receiverMode: ReceiverMode,
) {
    companion object {
        private const val CONFIG_FILE_NAME = "receiver.properties"
        private const val DEFAULT_NAME = "MirrorAir"
        private const val DEFAULT_AIRPLAY_PORT = 7000
        private const val DEFAULT_RAOP_PORT = 5000
        private const val KEY_RECEIVER_MODE = "receiver.mode"

        fun load(context: Context): ReceiverConfig {
            val configFile = ensureConfigFile(context)
            val properties = Properties().apply {
                configFile.inputStream().use(::load)
            }
            val receiverCode = properties.getProperty("receiver.code")?.takeIf { it.isNotBlank() }
                ?: generateAndPersistCode(configFile, properties)
            val raopPort = properties.getProperty("raop.port", DEFAULT_RAOP_PORT.toString()).toInt()
            val configuredAirPlayPort = properties.getProperty(
                "airplay.port",
                DEFAULT_AIRPLAY_PORT.toString(),
            ).toInt()
            val airPlayPort = if (configuredAirPlayPort == raopPort + 1) {
                configuredAirPlayPort
            } else {
                raopPort + 1
            }

            return ReceiverConfig(
                receiverName = properties.getProperty("receiver.name", DEFAULT_NAME),
                receiverCode = receiverCode,
                airPlayPort = airPlayPort,
                raopPort = raopPort,
                receiverMode = ReceiverMode.fromStorageValue(properties.getProperty(KEY_RECEIVER_MODE)),
            )
        }

        fun configFile(context: Context): File = File(context.filesDir, CONFIG_FILE_NAME)

        fun updateMode(context: Context, mode: ReceiverMode) {
            val configFile = ensureConfigFile(context)
            val properties = Properties().apply {
                configFile.inputStream().use(::load)
            }
            properties.setProperty(KEY_RECEIVER_MODE, mode.storageValue)
            configFile.outputStream().use { output ->
                properties.store(output, "MirrorAir receiver settings")
            }
        }

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

        private fun generateAndPersistCode(configFile: File, properties: Properties): String {
            val code = (1000 + Random.nextInt(9000)).toString()
            properties.setProperty("receiver.code", code)
            configFile.outputStream().use { output ->
                properties.store(output, "MirrorAir receiver settings")
            }
            return code
        }
    }
}
