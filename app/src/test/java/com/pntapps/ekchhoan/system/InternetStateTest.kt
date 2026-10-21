package com.pntapps.ekchhoan.system

import org.junit.Assert.assertEquals
import org.junit.Test

/** Every row of the §4.6 table, plus the edge cases from §7. */
class InternetStateTest {

    private fun derive(
        defaultIsWifi: Boolean = false,
        defaultIsCellular: Boolean = false,
        defaultValidated: Boolean = false,
        defaultCaptivePortal: Boolean = false,
        wifiConnected: Boolean = false,
        wifiEnabled: Boolean = false,
    ) = deriveInternetState(
        defaultIsWifi, defaultIsCellular, defaultValidated, defaultCaptivePortal, wifiConnected, wifiEnabled,
    )

    @Test fun wifiDefaultAndValidated_isWifiOk() {
        assertEquals(
            InternetState.WifiOk,
            derive(defaultIsWifi = true, defaultValidated = true, wifiConnected = true, wifiEnabled = true),
        )
    }

    @Test fun cellularDefaultValidated_wifiOff_isMobileOkWifiDisabled() {
        assertEquals(
            InternetState.MobileOk(wifiEnabled = false),
            derive(defaultIsCellular = true, defaultValidated = true),
        )
    }

    @Test fun cellularDefaultValidated_wifiOnButNotConnected_isMobileOkWifiEnabled() {
        assertEquals(
            InternetState.MobileOk(wifiEnabled = true),
            derive(defaultIsCellular = true, defaultValidated = true, wifiEnabled = true),
        )
    }

    @Test fun wifiConnectedWithoutInternet_butCellularValidated_isMobileOk() {
        assertEquals(
            InternetState.MobileOk(wifiEnabled = true),
            derive(defaultIsCellular = true, defaultValidated = true, wifiConnected = true, wifiEnabled = true),
        )
    }

    @Test fun wifiDefaultNotValidated_isWifiNoInternet() {
        assertEquals(
            InternetState.WifiNoInternet,
            derive(defaultIsWifi = true, wifiConnected = true, wifiEnabled = true),
        )
    }

    @Test fun wifiCaptivePortal_isWifiNoInternet() {
        assertEquals(
            InternetState.WifiNoInternet,
            derive(defaultIsWifi = true, defaultCaptivePortal = true, wifiConnected = true, wifiEnabled = true),
        )
    }

    @Test fun captivePortalEvenIfMarkedValidated_isWifiNoInternet() {
        assertEquals(
            InternetState.WifiNoInternet,
            derive(
                defaultIsWifi = true, defaultValidated = true, defaultCaptivePortal = true,
                wifiConnected = true, wifiEnabled = true,
            ),
        )
    }

    @Test fun wifiAssociatedButNotDefault_noOtherNetwork_isWifiNoInternet() {
        assertEquals(InternetState.WifiNoInternet, derive(wifiConnected = true, wifiEnabled = true))
    }

    @Test fun wifiOnButNotConnected_cellularNotValidated_isOffline() {
        assertEquals(InternetState.Offline, derive(defaultIsCellular = true, wifiEnabled = true))
    }

    @Test fun wifiOffAndDataOff_isOffline() {
        assertEquals(InternetState.Offline, derive())
    }

    @Test fun airplaneMode_isOffline() {
        // Airplane mode: no default network, no Wi-Fi association, Wi-Fi radio off.
        assertEquals(InternetState.Offline, derive(wifiEnabled = false))
    }

    @Test fun airplaneModeWithWifiRadioLeftOn_isOffline() {
        assertEquals(InternetState.Offline, derive(wifiEnabled = true))
    }

    @Test fun otherValidatedTransport_isWifiOk() {
        // e.g. Ethernet: neither Wi-Fi nor cellular, but the internet works.
        assertEquals(InternetState.WifiOk, derive(defaultValidated = true))
    }
}
