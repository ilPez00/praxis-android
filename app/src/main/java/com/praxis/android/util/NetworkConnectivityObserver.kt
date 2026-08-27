package com.praxis.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live connectivity state. Registers a default-network callback so the flow
 * actually updates when the network comes and goes — the WebScreen offline
 * panel and its auto-reload depend on emissions, and a construction-time
 * snapshot would freeze the panel forever.
 */
class NetworkConnectivityObserver(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isConnected = MutableStateFlow(checkCurrentConnection())
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _isConnected.value = true }
        override fun onLost(network: Network) { _isConnected.value = checkCurrentConnection() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isConnected.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    init {
        // registerDefaultNetworkCallback is API 24+; minSdk is 24.
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    /** Unregister the callback — call from onDispose to avoid leaks. */
    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }

    fun checkCurrentConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
