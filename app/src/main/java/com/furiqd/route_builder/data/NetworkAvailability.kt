package com.furiqd.route_builder.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import java.io.Closeable

interface NetworkAvailability {
    fun isAvailable(): Boolean
    fun observe(listener: (Boolean) -> Unit): Closeable
}

class AndroidNetworkAvailability(context: Context) : NetworkAvailability {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun observe(listener: (Boolean) -> Unit): Closeable {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                listener(isAvailable())
            }

            override fun onLost(network: Network) {
                listener(isAvailable())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                listener(
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        listener(isAvailable())
        return Closeable { connectivityManager.unregisterNetworkCallback(callback) }
    }
}

object AlwaysAvailableNetwork : NetworkAvailability {
    override fun isAvailable(): Boolean = true
    override fun observe(listener: (Boolean) -> Unit): Closeable {
        listener(true)
        return Closeable {}
    }
}
