package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ScopeRestartSelectionTest {
    private val hyperOS = SystemProfile.forSystem(PhoneSystem.HYPER_OS)

    @Test fun newScopesDefaultToSelectedAndSavedChoicesArePreserved() {
        val hosts = ScopeRestartSelection.available(hyperOS)
        assertEquals(hosts.map { it.processName }.toSet(), ScopeRestartSelection.selected(hosts, emptyMap<String, Boolean>()))
        val saved = mapOf(ScopeRestart.Host.BLUETOOTH.processName to false, ScopeRestart.Host.SYSTEM_UI.processName to true)
        val selected = ScopeRestartSelection.selected(hosts, saved)
        assertFalse(ScopeRestart.Host.BLUETOOTH.processName in selected)
        assertTrue(ScopeRestart.Host.SYSTEM_UI.processName in selected)
        assertTrue(ScopeRestart.Host.XIAOMI_UI.processName in selected)
    }

    @Test fun everyBuildOffersAllRegisteredScopesInReloadOrder() {
        assertEquals(ScopeRestart.reloadOrder, ScopeRestartSelection.available(hyperOS))
        assertEquals(ScopeRestart.Host.entries.toSet(), ScopeRestartSelection.available(hyperOS).toSet())
        assertTrue(ScopeRestartSelection.available(hyperOS).any { it.affectsControlIpc })
    }

    @Test fun reservedSystemsOfferOnlyTheSharedBluetoothHost() {
        PhoneSystem.entries.filter { it != PhoneSystem.HYPER_OS }.forEach {
            assertEquals(listOf(ScopeRestart.Host.BLUETOOTH),
                ScopeRestartSelection.available(SystemProfile.forSystem(it)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStoredValuesAreRejected() {
        ScopeRestartSelection.selected(ScopeRestartSelection.available(hyperOS),
            mapOf(ScopeRestart.Host.SYSTEM_UI.processName to "yes"))
    }
}
