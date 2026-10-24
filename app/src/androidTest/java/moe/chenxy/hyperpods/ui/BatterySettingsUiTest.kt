package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.BatteryNotificationSettingsGroup
import moe.chenxy.hyperpods.utils.BatteryNotificationSettings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BatterySettingsUiTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val visible = mutableStateOf(true)
    private val writes = mutableListOf<BatteryNotificationSettings>()

    private fun show(acceptSave: Boolean = true) {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(1) {
                    if (visible.value) BatteryNotificationSettingsGroup(
                        settings = BatteryNotificationSettings(),
                        onSave = { if (acceptSave) writes.add(it); acceptSave },
                    )
                }
            }
        }
    }

    @Test fun switchesSaveImmediatelyWithoutASaveRowOrPreview() {
        show()
        compose.onNodeWithText(compose.activity.getString(R.string.battery_notifications_low_reminder)).performClick()
        compose.runOnIdle { assertFalse(writes.single().lowReminderEnabled) }
        compose.onAllNodes(hasClickAction() and hasText("Save", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("18%", substring = true)).assertCountEquals(0)
    }

    @Test fun thresholdsSaveOnBlurAndOnLeavingThePage() {
        show()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("10")
        compose.runOnIdle { assertTrue(writes.isEmpty()) }
        compose.onAllNodes(hasSetTextAction())[1].performClick()
        compose.runOnIdle { assertEquals(10, writes.last().lowThreshold) }
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("60")
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        assertEquals(60, writes.last().mediumThreshold)
        assertEquals(2, writes.size)
    }

    @Test fun backgroundingSavesTheFocusedField() {
        show()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("15")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(15, writes.single().lowThreshold)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test fun invalidThresholdsDoNotBlockAnIndependentSwitchSave() {
        show()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("90")
        compose.onAllNodes(hasSetTextAction())[1].performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.battery_notifications_invalid)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.battery_notifications_medium_reminder)).performClick()
        compose.runOnIdle {
            assertEquals(BatteryNotificationSettings(mediumReminderEnabled = false), writes.single())
        }
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("90")
    }

    @Test fun failedSwitchSaveDoesNotShowASavedValue() {
        show(acceptSave = false)
        compose.onNodeWithText(compose.activity.getString(R.string.battery_notifications_low_reminder)).performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.battery_notifications_low_reminder)) and isToggleable())
            .assertIsOn()
        compose.runOnIdle { assertTrue(writes.isEmpty()) }
    }
}
