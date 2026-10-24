package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class SystemCompatibilityTest {
    @Test fun hyperOSRomEvidenceSelectsTheExistingAdapterRegardlessOfHardwareBrand() {
        for ((manufacturer, brand) in listOf("Xiaomi" to "Redmi", "Other" to "Other")) {
            val profile = SystemProfile.detect(manufacturer, brand, "4")
            assertEquals(PhoneSystem.HYPER_OS, profile.system)
            assertTrue(profile.supports(SystemFeature.NATIVE_BATTERY_ISLAND))
        }
    }

    @Test fun xiaomiHardwareAloneDoesNotEnableHyperOSPrivateInterfaces() {
        val profile = SystemProfile.detect("Xiaomi", "POCO", "")
        assertEquals(PhoneSystem.XIAOMI, profile.system)
        assertFalse(profile.supports(SystemFeature.NATIVE_BATTERY_ISLAND))
        assertNull(profile.hookFor(HyperPodsBroadcasts.SYSTEM_UI))
        for (version in listOf("unknown", "0", "-1"))
            assertFalse(SystemProfile.detect("Xiaomi", "POCO", version).supports(SystemFeature.NATIVE_BATTERY_ISLAND))
    }

    @Test fun reservedVendorsAreClassifiedButDoNotAdvertiseUnimplementedIntegrations() {
        val identities = listOf(
            Triple(" vivo ", "iQOO", PhoneSystem.VIVO),
            Triple("OnePlus", "oneplus", PhoneSystem.OPPO),
            Triple("realme", "realme", PhoneSystem.OPPO),
            Triple("Samsung", "samsung", PhoneSystem.SAMSUNG),
            Triple("Google", "google", PhoneSystem.AOSP),
            Triple("", "", PhoneSystem.OTHER),
        )
        identities.forEach { (manufacturer, brand, expected) ->
            assertEquals(expected, SystemProfile.detect(manufacturer, brand, "").system)
        }
        PhoneSystem.entries.filter { it != PhoneSystem.HYPER_OS }.forEach { system ->
            val profile = SystemProfile.forSystem(system)
            SystemFeature.entries.forEach { assertFalse(profile.supports(it)) }
            assertEquals(listOf(HyperPodsBroadcasts.BLUETOOTH), profile.modulePackages)
            assertEquals(setOf(HyperPodsBroadcasts.BLUETOOTH), profile.restartProcesses)
            assertEquals(SystemHook.BLUETOOTH_CONTROL, profile.hookFor(HyperPodsBroadcasts.BLUETOOTH))
            assertNull(profile.hookFor(HyperPodsBroadcasts.XIAOMI_BLUETOOTH))
            assertNull(profile.hookFor(HyperPodsBroadcasts.SYSTEM_UI))
        }
    }

    @Test fun hyperOSKeepsTheExistingHostsFeaturesAndHookRoutes() {
        val profile = SystemProfile.forSystem(PhoneSystem.HYPER_OS)
        assertEquals(ModuleStatus.packages, profile.modulePackages)
        assertEquals(ScopeRestart.reloadOrder.map { it.processName }.toSet(), profile.restartProcesses)
        assertTrue(profile.supports(SystemFeature.SYSTEM_HEADPHONE_CARD))
        assertTrue(profile.supports(SystemFeature.HEADSET_STATUS_ICON))
        assertEquals(SystemHook.HYPER_OS_BLUETOOTH, profile.hookFor(HyperPodsBroadcasts.XIAOMI_BLUETOOTH))
        assertEquals(SystemHook.HYPER_OS_SYSTEM_UI, profile.hookFor(HyperPodsBroadcasts.SYSTEM_UI))
        assertNull(profile.hookFor("com.milink.service"))
        assertNull(profile.hookFor("com.android.systemui:unknown"))
    }

    @Test fun lenovoRetainsCoreBluetoothWithoutXiaomiIntegrations() {
        val profile = SystemProfile.detect("LENOVO", "Lenovo", "")
        assertEquals(PhoneSystem.OTHER, profile.system)
        assertEquals(SystemHook.BLUETOOTH_CONTROL, profile.hookFor(HyperPodsBroadcasts.BLUETOOTH))
        assertEquals(listOf(HyperPodsBroadcasts.BLUETOOTH), profile.modulePackages)
        SystemFeature.entries.forEach { assertFalse(profile.supports(it)) }
        assertNull(profile.hookFor(HyperPodsBroadcasts.XIAOMI_BLUETOOTH))
        assertNull(profile.hookFor(HyperPodsBroadcasts.SYSTEM_UI))
    }
}
