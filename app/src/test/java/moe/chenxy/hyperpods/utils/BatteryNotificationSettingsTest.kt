package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class BatteryNotificationSettingsTest {
    private val settings = BatteryNotificationSettings(lowReminderEnabled = false)

    @Test fun validThresholdEditsPreserveReminderSwitches() {
        assertEquals(settings.copy(lowThreshold = 10, mediumThreshold = 60),
            settings.withThresholds("10", "60"))
        assertEquals(settings.copy(lowThreshold = 1, mediumThreshold = 99),
            settings.withThresholds("1", "99"))
    }

    @Test fun invalidEditsNeverReplaceSavedSettings() {
        for ((low, medium) in listOf("" to "50", "x" to "50", "0" to "50",
            "20" to "100", "50" to "50", "60" to "50", "1.5" to "50",
            "9999999999999" to "50")) {
            assertNull("$low / $medium", settings.withThresholds(low, medium))
        }
        assertEquals(20, settings.lowThreshold)
        assertEquals(50, settings.mediumThreshold)
        assertFalse(settings.lowReminderEnabled)
    }
}
