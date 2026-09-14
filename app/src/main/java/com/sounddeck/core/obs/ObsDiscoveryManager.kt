package com.sounddeck.core.obs

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredObsInstance(
    val serviceName: String,
    val host: String,
    val port: Int
)

class ObsDiscoveryManager(private val context: Context) {
    private val tag = "ObsDiscoveryManager"
    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredInstances = MutableStateFlow<List<DiscoveredObsInstance>>(emptyList())
    val discoveredInstances: StateFlow<List<DiscoveredObsInstance>> = _discoveredInstances.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (_isDiscovering.value || nsdManager == null) return
        _discoveredInstances.value = emptyList()
        _isDiscovering.value = true

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(tag, "Service discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(tag, "Service lost: ${service.serviceName}")
                _discoveredInstances.value = _discoveredInstances.value.filter {
                    it.serviceName != service.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "Discovery stopped: $serviceType")
                _isDiscovering.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Stop discovery failed: $errorCode")
                stopDiscovery()
            }
        }

        try {
            // OBS Studio 28+ broadcasts `_obs-websocket._tcp.`
            nsdManager.discoverServices(
                "_obs-websocket._tcp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start discovery: ${e.message}", e)
            _isDiscovering.value = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(tag, "Resolve failed for ${service.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val host = resolved.host?.hostAddress ?: return
                val port = resolved.port
                val instance = DiscoveredObsInstance(
                    serviceName = resolved.serviceName,
                    host = host,
                    port = port
                )
                val current = _discoveredInstances.value.toMutableList()
                if (current.none { it.host == host && it.port == port }) {
                    current.add(instance)
                    _discoveredInstances.value = current
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.w(tag, "Exception resolving service: ${e.message}")
        }
    }

    fun stopDiscovery() {
        if (!_isDiscovering.value || nsdManager == null) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery: ${e.message}")
        } finally {
            discoveryListener = null
            _isDiscovering.value = false
        }
    }
}
