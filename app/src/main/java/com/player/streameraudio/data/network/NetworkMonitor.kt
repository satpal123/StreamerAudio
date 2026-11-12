package com.player.streameraudio.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun awaitOnline(timeoutMs: Long): Boolean {
        if (isOnline()) return true
        val checks = (timeoutMs / POLL_INTERVAL_MS).toInt().coerceAtLeast(1)
        repeat(checks) {
            delay(POLL_INTERVAL_MS)
            if (isOnline()) return true
        }
        return false
    }

    private companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}
