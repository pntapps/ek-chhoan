package com.pntapps.ekchhoan.system

import kotlinx.coroutines.flow.StateFlow

/** What the internet bar shows. See prompt §4.6 for the derivation table. */
sealed interface InternetState {
    /** Default network is Wi-Fi and validated. */
    data object WifiOk : InternetState
    /** Default network is cellular and validated. [wifiEnabled] picks the subtitle variant. */
    data class MobileOk(val wifiEnabled: Boolean) : InternetState
    /** Wi-Fi connected but not validated (or captive portal) and no validated cellular. */
    data object WifiNoInternet : InternetState
    /** No validated network at all. */
    data object Offline : InternetState
}

interface InternetMonitor {
    val state: StateFlow<InternetState>
    /** Re-reads the current network state immediately (called on ON_RESUME). */
    fun refresh()
}

enum class TorchState { On, Off, Unavailable }

interface TorchController {
    val state: StateFlow<TorchState>
    /** Toggles the torch. Returns false when the camera is busy and the torch could not be switched. */
    fun toggle(): Boolean
}

/** Result of trying to open something outside the app. */
enum class LaunchResult { Ok, NotInstalled, NoPhoneNumber, Failed }

interface Speaker {
    /** Speaks [text] when readout is enabled; returns immediately. */
    fun speak(text: String)
    /** True when no voice for the app's language is installed and English is used instead (Settings shows a hint). */
    val voiceMissing: StateFlow<Boolean>
    fun setEnabled(enabled: Boolean)
    fun shutdown()
}
