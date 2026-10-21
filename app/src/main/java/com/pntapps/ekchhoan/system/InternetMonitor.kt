package com.pntapps.ekchhoan.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

/** Changes must be stable this long before the bar updates, to avoid flicker. */
private const val DEBOUNCE_MS = 2_000L

/**
 * Pure §4.6 derivation. "default*" describe the system default network (all false when there is none);
 * [wifiConnected] is true when any Wi-Fi network is associated, even if it is not the default.
 */
internal fun deriveInternetState(
    defaultIsWifi: Boolean,
    defaultIsCellular: Boolean,
    defaultValidated: Boolean,
    defaultCaptivePortal: Boolean,
    wifiConnected: Boolean,
    wifiEnabled: Boolean,
): InternetState {
    val internetWorks = defaultValidated && !defaultCaptivePortal
    return when {
        internetWorks && defaultIsCellular && !defaultIsWifi -> InternetState.MobileOk(wifiEnabled)
        // Wi-Fi, or another validated transport such as Ethernet: the internet works, show the Wi-Fi variant.
        internetWorks -> InternetState.WifiOk
        defaultIsWifi || wifiConnected -> InternetState.WifiNoInternet
        else -> InternetState.Offline
    }
}

/**
 * Watches connectivity with callbacks only (no network traffic, no INTERNET permission).
 * Lives for the whole process in [com.pntapps.ekchhoan.AppContainer], so callbacks are never unregistered.
 * All callbacks run on the main thread.
 */
class AndroidInternetMonitor(context: Context) : InternetMonitor {
    private val app = context.applicationContext
    private val connectivity: ConnectivityManager? = app.getSystemService(ConnectivityManager::class.java)
    private val wifiManager: WifiManager? = app.getSystemService(WifiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    /** Capabilities of the default network as last reported by the callback; null when there is none. */
    @Volatile private var defaultCaps: NetworkCapabilities? = null
    private val wifiNetworks: MutableSet<Network> = Collections.synchronizedSet(HashSet())

    private val _state = MutableStateFlow(compute())
    override val state: StateFlow<InternetState> = _state.asStateFlow()

    private val publish = Runnable { _state.value = compute() }

    private val defaultCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = schedule()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            defaultCaps = caps
            schedule()
        }
        override fun onLost(network: Network) {
            defaultCaps = null
            schedule()
        }
    }

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wifiNetworks.add(network)
            schedule()
        }
        override fun onLost(network: Network) {
            wifiNetworks.remove(network)
            schedule()
        }
    }

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = schedule()
    }

    init {
        connectivity?.let { cm ->
            // Some OEM builds throw SecurityException / TooManyRequestsException here; the bar then relies on refresh().
            runCatching { cm.registerDefaultNetworkCallback(defaultCallback, handler) }
            runCatching {
                val wifiRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                cm.registerNetworkCallback(wifiRequest, wifiCallback, handler)
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                app,
                wifiStateReceiver,
                IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    override fun refresh() {
        handler.removeCallbacks(publish)
        _state.value = compute()
    }

    private fun schedule() {
        handler.removeCallbacks(publish)
        handler.postDelayed(publish, DEBOUNCE_MS)
    }

    private fun compute(): InternetState {
        // The live query wins; the callback copy covers devices where activeNetwork is hidden from us.
        val caps = runCatching { connectivity?.let { it.getNetworkCapabilities(it.activeNetwork) } }.getOrNull()
            ?: defaultCaps
        val defaultIsWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return deriveInternetState(
            defaultIsWifi = defaultIsWifi,
            defaultIsCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
            defaultValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            defaultCaptivePortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
            wifiConnected = defaultIsWifi || wifiNetworks.isNotEmpty(),
            wifiEnabled = runCatching { wifiManager?.isWifiEnabled }.getOrNull() == true,
        )
    }
}
