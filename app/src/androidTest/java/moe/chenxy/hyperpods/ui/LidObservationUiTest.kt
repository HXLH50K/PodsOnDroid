package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.LidObservationPanel
import org.junit.Rule
import org.junit.Test

class LidObservationUiTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()

    @Test fun openingTheDiagnosticPanelDoesNotStartScanningOrRequestPermissions() {
        compose.runOnUiThread {
            compose.activity.setContent { AppTheme(1) { LidObservationPanel() } }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.lid_observation_title)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.lid_observation_idle))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.lid_observation_start))
            .performScrollTo().assertIsEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.lid_observation_stop))
            .performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.lid_observation_copy))
            .performScrollTo().assertIsNotEnabled()
    }
}
