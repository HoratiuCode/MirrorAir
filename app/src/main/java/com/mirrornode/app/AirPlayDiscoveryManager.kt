package com.mirrornode.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.NetworkInterface
import java.util.Locale

class AirPlayDiscoveryManager(
    context: Context,
) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private var airPlayRegistration: NsdManager.RegistrationListener? = null
    private var raopRegistration: NsdManager.RegistrationListener? = null

    fun register(
        config: ReceiverConfig,
        onRegistered: () -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        unregister()

        var registeredCount = 0
        var failed = false

        fun handleRegistered() {
            if (failed) {
                return
            }
            registeredCount += 1
            if (registeredCount == 2) {
                onRegistered()
            }
        }

        fun handleFailure(serviceLabel: String, errorCode: Int) {
            if (failed) {
                return
            }
            failed = true
            unregister()
            onFailure("$serviceLabel registration failed (error $errorCode).")
        }

        val deviceId = deviceIdentifier()
        val profile = advertisementProfile(config.receiverMode)
        val airPlayInfo = NsdServiceInfo().apply {
            serviceName = profile.serviceName(config.receiverName)
            serviceType = "_airplay._tcp."
            port = config.airPlayPort
            setAttribute("deviceid", deviceId)
            setAttribute("features", profile.features)
            setAttribute("flags", AIRPLAY_FLAGS)
            setAttribute("model", profile.model)
            setAttribute("pk", AIRPLAY_PUBLIC_KEY)
            setAttribute("pi", AIRPLAY_PAIRING_ID)
            setAttribute("protovers", "1.1")
            setAttribute("srcvers", AIRPLAY_SOURCE_VERSION)
            setAttribute("vv", AIRPLAY_PROTOCOL_VERSION)
        }

        val raopInfo = NsdServiceInfo().apply {
            serviceName = "$deviceId@${profile.serviceName(config.receiverName)}"
            serviceType = "_raop._tcp."
            port = config.raopPort
            setAttribute("ch", "2")
            setAttribute("cn", "0,1,2,3")
            setAttribute("da", "true")
            setAttribute("et", "0,3,5")
            setAttribute("ft", profile.features)
            setAttribute("am", profile.model)
            setAttribute("md", "0,1,2")
            setAttribute("pw", "false")
            setAttribute("rhd", "5.6.0.0")
            setAttribute("sf", AIRPLAY_FLAGS)
            setAttribute("sm", "false")
            setAttribute("sr", "44100")
            setAttribute("ss", "16")
            setAttribute("sv", "false")
            setAttribute("tp", "UDP")
            setAttribute("txtvers", "1")
            setAttribute("vn", "65537")
            setAttribute("vs", AIRPLAY_SOURCE_VERSION)
            setAttribute("vv", AIRPLAY_PROTOCOL_VERSION)
            setAttribute("pk", AIRPLAY_PUBLIC_KEY)
        }

        airPlayRegistration = registrationListener(
            serviceLabel = "AirPlay",
            onRegistered = ::handleRegistered,
            onFailure = ::handleFailure,
        )
        raopRegistration = registrationListener(
            serviceLabel = "RAOP",
            onRegistered = ::handleRegistered,
            onFailure = ::handleFailure,
        )
        nsdManager.registerService(airPlayInfo, NsdManager.PROTOCOL_DNS_SD, airPlayRegistration)
        nsdManager.registerService(raopInfo, NsdManager.PROTOCOL_DNS_SD, raopRegistration)
    }

    fun unregister() {
        try {
            airPlayRegistration?.let(nsdManager::unregisterService)
        } catch (_: IllegalArgumentException) {
        }
        try {
            raopRegistration?.let(nsdManager::unregisterService)
        } catch (_: IllegalArgumentException) {
        }
        airPlayRegistration = null
        raopRegistration = null
    }

    private fun registrationListener(
        serviceLabel: String,
        onRegistered: () -> Unit,
        onFailure: (String, Int) -> Unit,
    ): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = onRegistered()
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onFailure(serviceLabel, errorCode)
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
    }

    private fun deviceIdentifier(): String {
        val fallback = "02:00:00:00:00:00"
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return fallback
        var networkAddress: String? = null
        while (interfaces.hasMoreElements() && networkAddress == null) {
            val iface = interfaces.nextElement()
            val address = iface.hardwareAddress ?: continue
            if (address.isEmpty()) {
                continue
            }
            networkAddress = address.joinToString(":") { byte ->
                String.format(Locale.US, "%02X", byte)
            }
        }

        return networkAddress ?: fallback
    }

    private fun advertisementProfile(mode: ReceiverMode): AdvertisementProfile {
        return when (mode) {
            ReceiverMode.AIRPLAY -> AdvertisementProfile(
                model = "AppleTV2,1",
                features = "0x5A7FFEE6",
                serviceSuffix = "",
            )
            ReceiverMode.TV -> AdvertisementProfile(
                model = "AppleTV3,2",
                features = "0x5A7FFFF7,0x1E",
                serviceSuffix = " TV",
            )
        }
    }

    companion object {
        private const val AIRPLAY_FLAGS = "0x4"
        private const val AIRPLAY_PROTOCOL_VERSION = "2"
        private const val AIRPLAY_SOURCE_VERSION = "220.68"
        private const val AIRPLAY_PUBLIC_KEY =
            "b07727d6f6cd6e08b58ede525ec3cdeaa252ad9f683feb212ef8a205246554e7"
        private const val AIRPLAY_PAIRING_ID = "2e388006-13ba-4041-9a67-25dd4a43d536"
    }

    private data class AdvertisementProfile(
        val model: String,
        val features: String,
        val serviceSuffix: String,
    ) {
        fun serviceName(baseName: String): String = baseName + serviceSuffix
    }
}
