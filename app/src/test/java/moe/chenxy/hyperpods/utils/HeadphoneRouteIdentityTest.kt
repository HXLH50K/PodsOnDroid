package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class HeadphoneRouteIdentityTest {
    @Test fun verifiedSystemProviderMayCarryAddressInItsRouteId() {
        assertEquals("02:00:00:00:00:01", HeadphoneRouteIdentity.systemAddress(
            "com.android.server.media/.SystemMediaRoute2Provider:02:00:00:00:00:01", true))
        assertEquals("02:ab:cd:00:00:01", HeadphoneRouteIdentity.systemAddress("02:ab:cd:00:00:01", true))
    }

    @Test fun thirdPartyMalformedOrDeviceIndependentRoutesDoNotAcquireAnIdentity() {
        assertNull(HeadphoneRouteIdentity.systemAddress("provider:02:00:00:00:00:01", false))
        assertNull(HeadphoneRouteIdentity.systemAddress("provider:NOT_A_MAC_ADDRESS", true))
        assertNull(HeadphoneRouteIdentity.systemAddress("providerX02:00:00:00:00:01", true))
        assertNull(HeadphoneRouteIdentity.systemAddress("bluetooth_a2dp_audio_route", true))
        assertNull(HeadphoneRouteIdentity.systemAddress("DEVICE_ROUTE", true))
    }
}
