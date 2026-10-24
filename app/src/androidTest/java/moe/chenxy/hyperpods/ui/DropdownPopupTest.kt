package moe.chenxy.hyperpods.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import moe.chenxy.hyperpods.R
import org.junit.Rule
import org.junit.Test

/** Production pages with synthetic device callbacks; opening/dismissing never writes headphone settings. */
class DropdownPopupTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    private fun moreControls() {
        compose.revealSetting(text(R.string.dashboard_more)).performClick()
        compose.waitForIdle()
    }

    private fun openAndDismiss(control: Int, option: Int) {
        compose.revealSetting(text(control)).performClick()
        compose.onAllNodesWithText(text(option))
            .filter(SemanticsMatcher("Placed popup row") { it.layoutInfo.isPlaced })
            .onFirst().assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithText(text(option)).assertDoesNotExist()
        compose.onNodeWithText(text(control)).assertIsDisplayed()
    }

    @Test fun pressSpeedPopupOpensAndDismissesOnTheRealSecondaryPage() {
        moreControls()
        openAndDismiss(R.string.control_press_speed, R.string.control_slower)
    }

    @Test fun durationAndMicrophonePopupsShareTheSameWorkingHost() {
        moreControls()
        openAndDismiss(R.string.control_hold_duration, R.string.control_longer)
        openAndDismiss(R.string.microphone, R.string.microphone_left)
    }

    @Test fun popupCanReopenAfterReturningThroughPageTransitions() {
        moreControls()
        openAndDismiss(R.string.control_press_speed, R.string.control_slowest)
        Espresso.pressBack()
        compose.waitForIdle()
        moreControls()
        openAndDismiss(R.string.control_press_speed, R.string.control_slower)
    }
}
