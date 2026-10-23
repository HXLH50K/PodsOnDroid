package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class SettingsDestinationTest {
    @Test fun rootContainsOnlyAppLevelCategories() {
        assertEquals(listOf(SettingsDestination.NOTIFICATIONS, SettingsDestination.APPEARANCE,
            SettingsDestination.ABOUT, SettingsDestination.TOOLS), SettingsDestination.ROOT.children(true))
    }

    @Test fun diagnosticActionsAreAtLeastOneMenuBelowSettingsRoot() {
        for (tool in listOf(SettingsDestination.CONNECTION, SettingsDestination.MODULES,
            SettingsDestination.HEAD_TRACKING, SettingsDestination.LID_OBSERVATION)) {
            assertEquals(SettingsDestination.TOOLS, tool.parent)
            assertFalse(tool in SettingsDestination.ROOT.children(true))
        }
    }

    @Test fun releaseBuildDoesNotExposeDebugOnlyLidCapture() {
        assertFalse(SettingsDestination.LID_OBSERVATION in SettingsDestination.TOOLS.children(false))
        assertTrue(SettingsDestination.MODULES in SettingsDestination.TOOLS.children(false))
    }

    @Test fun everyNestedPageHasABoundedPathBackToRoot() {
        for (destination in SettingsDestination.entries) {
            var current = destination
            val visited = mutableSetOf<SettingsDestination>()
            while (current.parent != null) {
                assertTrue(visited.add(current))
                current = requireNotNull(current.parent)
            }
            assertEquals(SettingsDestination.ROOT, current)
            assertTrue(visited.size <= 2)
        }
    }
}
