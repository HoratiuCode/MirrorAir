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

    fun register(config: ReceiverConfig) {
        unregister()

        val deviceId = deviceIdentifier()
        val airPlayInfo = NsdServiceInfo().apply {
            serviceName = config.receiverName
            serviceType = "_airplay._tcp."
            port = config.airPlayPort
            setAttribute("deviceid", deviceId)
            setAttribute("features", "0x5A7FFFF7,0x1E")
            setAttribute("flags", "0x4")
            setAttribute("model", "MirrorNode1,1")
            setAttribute("protovers", "1.1")
            setAttribute("srcvers", "220.68")
        }

        val raopInfo = NsdServiceInfo().apply {
            serviceName = "$deviceId@${config.receiverName}"
            serviceType = "_raop._tcp."
            port = config.raopPort
            setAttribute("ch", "2")
            setAttribute("cn", "0,1,2,3")
            setAttribute("et", "0,3,5")
            setAttribute("md", "0,1,2")
            setAttribute("pw", "false")
            setAttribute("sm", "false")
            setAttribute("sr", "44100")
            setAttribute("ss", "16")
            setAttribute("tp", "UDP")
            setAttribute("txtvers", "1")
        }

        airPlayRegistration = registrationListener()
        raopRegistration = registrationListener()
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

    private fun registrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
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
}
