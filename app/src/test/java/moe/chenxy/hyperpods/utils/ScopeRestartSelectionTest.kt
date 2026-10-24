package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ScopeRestartSelectionTest {
    @Test fun newScopesDefaultToSelectedAndSavedChoicesArePreserved() {
        val hosts = ScopeRestartSelection.available()
        assertEquals(hosts.map { it.processName }.toSet(), ScopeRestartSelection.selected(hosts, emptyMap<String, Boolean>()))
        val saved = mapOf(ScopeRestart.Host.BLUETOOTH.processName to false, ScopeRestart.Host.SYSTEM_UI.processName to true)
        val selected = ScopeRestartSelection.selected(hosts, saved)
        assertFalse(ScopeRestart.Host.BLUETOOTH.processName in selected)
        assertTrue(ScopeRestart.Host.SYSTEM_UI.processName in selected)
        assertTrue(ScopeRestart.Host.XIAOMI_UI.processName in selected)
    }

    @Test fun everyBuildOffersAllRegisteredScopesInReloadOrder() {
        assertEquals(ScopeRestart.reloadOrder, ScopeRestartSelection.available())
        assertEquals(ScopeRestart.Host.entries.toSet(), ScopeRestartSelection.available().toSet())
        assertTrue(ScopeRestartSelection.available().any { it.affectsControlIpc })
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStoredValuesAreRejected() {
        ScopeRestartSelection.selected(ScopeRestartSelection.available(),
            mapOf(ScopeRestart.Host.SYSTEM_UI.processName to "yes"))
    }
}
