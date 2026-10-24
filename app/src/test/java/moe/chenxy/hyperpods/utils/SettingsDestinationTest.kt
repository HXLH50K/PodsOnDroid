package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class SettingsDestinationTest {
    private val hyperOS = SystemProfile.forSystem(PhoneSystem.HYPER_OS)

    @Test fun groupedMenusPreserveEveryAvailableDestinationExactlyOnce() {
        for (debug in listOf(false, true)) for (menu in listOf(SettingsDestination.ROOT, SettingsDestination.TOOLS)) {
            val grouped = menu.groups(debug, hyperOS).flatten()
            assertEquals(menu.children(debug, hyperOS).toSet(), grouped.toSet())
            assertEquals(grouped.size, grouped.toSet().size)
        }
        assertEquals(listOf(SettingsDestination.NOTIFICATIONS, SettingsDestination.APPEARANCE),
            SettingsDestination.ROOT.groups(false, hyperOS).first())
        assertEquals(listOf(SettingsDestination.TOOLS, SettingsDestination.ABOUT),
            SettingsDestination.ROOT.groups(false, hyperOS).last())
    }

    @Test fun depthTracksTheActualHierarchyNotEnumDeclarationOrder() {
        assertEquals(0, SettingsDestination.ROOT.depth)
        assertEquals(1, SettingsDestination.ABOUT.depth)
        assertEquals(1, SettingsDestination.TOOLS.depth)
        assertEquals(2, SettingsDestination.CONNECTION.depth)
        assertEquals(2, SettingsDestination.MODULES.depth)
    }

    @Test fun rootContainsOnlyAppLevelCategories() {
        assertEquals(listOf(SettingsDestination.NOTIFICATIONS, SettingsDestination.APPEARANCE,
            SettingsDestination.ABOUT, SettingsDestination.TOOLS), SettingsDestination.ROOT.children(true, hyperOS))
    }

    @Test fun diagnosticActionsAreAtLeastOneMenuBelowSettingsRoot() {
        for (tool in listOf(SettingsDestination.CONNECTION, SettingsDestination.MODULES,
            SettingsDestination.HEAD_TRACKING, SettingsDestination.LID_OBSERVATION)) {
            assertEquals(SettingsDestination.TOOLS, tool.parent)
            assertFalse(tool in SettingsDestination.ROOT.children(true, hyperOS))
        }
    }

    @Test fun releaseBuildDoesNotExposeDebugOnlyLidCapture() {
        assertFalse(SettingsDestination.LID_OBSERVATION in SettingsDestination.TOOLS.children(false, hyperOS))
        assertTrue(SettingsDestination.MODULES in SettingsDestination.TOOLS.children(false, hyperOS))
    }

    @Test fun unimplementedSystemsHideOnlyTheUnimplementedBatteryCategory() {
        PhoneSystem.entries.filter { it != PhoneSystem.HYPER_OS }.forEach { system ->
            val profile = SystemProfile.forSystem(system)
            assertFalse(SettingsDestination.NOTIFICATIONS.available(profile))
            assertEquals(listOf(SettingsDestination.APPEARANCE), SettingsDestination.ROOT.groups(false, profile).first())
            for (debug in listOf(false, true)) for (menu in listOf(SettingsDestination.ROOT, SettingsDestination.TOOLS))
                assertEquals(menu.children(debug, profile).toSet(), menu.groups(debug, profile).flatten().toSet())
            assertTrue(SettingsDestination.MODULES in SettingsDestination.TOOLS.children(false, profile))
        }
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
