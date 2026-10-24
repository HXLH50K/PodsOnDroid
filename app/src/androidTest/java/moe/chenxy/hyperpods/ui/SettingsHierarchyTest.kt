package moe.chenxy.hyperpods.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import moe.chenxy.hyperpods.R
import org.junit.Rule
import org.junit.Test

/** Exercises menu routing only; never starts a scope restart, capture or headset command. */
class SettingsHierarchyTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    private fun settingsTab() {
        compose.onNode(hasText(text(R.string.settings_title)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
    }

    @Test fun bothRootTitlesStartJustBelowTheStatusBarWithoutDuplicatedInsets() {
        fun checkHeader(title: Int) {
            val toolbar = compose.onNodeWithTag("page_toolbar").fetchSemanticsNode().boundsInRoot
            val density = compose.activity.resources.displayMetrics.density
            val statusBar = requireNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
                .getInsets(WindowInsetsCompat.Type.statusBars()).top
            val titleBounds = compose.onNode(hasText(text(title)) and hasAnyAncestor(hasTestTag("page_toolbar")))
                .fetchSemanticsNode().boundsInRoot
            assertEquals("The toolbar must apply the status-bar inset only once",
                statusBar.toFloat(), toolbar.top, 1f)
            assertTrue("Root toolbar should be compact", toolbar.height / density <= 60f)
            val gap = (titleBounds.top - statusBar) / density
            assertTrue("Title-to-status-bar gap was $gap dp", gap in 0f..24f)
        }
        checkHeader(R.string.dashboard_headphones)
        settingsTab()
        compose.waitForIdle()
        checkHeader(R.string.settings_title)
    }

    @Test fun rootOnlyShowsCategoriesAndToolsHaveTheirOwnMenu() {
        settingsTab()
        for (id in listOf(R.string.diagnostics_title, R.string.settings_modules, R.string.head_test_title))
            compose.onNodeWithText(text(id)).assertDoesNotExist()
        compose.revealSetting(text(R.string.settings_tools)).performClick()
        for (id in listOf(R.string.diagnostics_title, R.string.settings_modules, R.string.head_test_title))
            compose.revealSetting(text(id)).assertIsDisplayed()
    }

    @Test fun notificationsAreAppSettingsAndBackReturnsToSettings() {
        settingsTab()
        compose.revealSetting(text(R.string.battery_notifications_title)).performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle,
            text(R.string.battery_notifications_title))).assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.revealSetting(text(R.string.settings_tools)).assertIsDisplayed()
        compose.revealSetting(text(R.string.about_hyperpods)).assertIsDisplayed()
    }

    @Test fun projectAboutHasASeparateEntryAndSurvivesRecreation() {
        settingsTab()
        compose.revealSetting(text(R.string.about_hyperpods)).performClick()
        compose.activityRule.scenario.recreate()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle,
            text(R.string.about_hyperpods))).assertExists()
        compose.revealSetting(text(R.string.about_sources)).assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.revealSetting(text(R.string.appearance_title)).assertIsDisplayed()
    }

    @Test fun childBackButtonStaysPinnedAfterScrollingToTheLicense() {
        settingsTab()
        compose.revealSetting(text(R.string.about_hyperpods)).performClick()
        compose.waitForIdle()
        val before = compose.onNodeWithTag("page_up").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        compose.revealSetting(text(R.string.about_view_license)).assertIsDisplayed()
        val after = compose.onNodeWithTag("page_up").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, .5f)
        assertEquals(before.left, after.left, .5f)
        compose.onNodeWithTag("page_up").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("page_up").assertDoesNotExist()
        compose.revealSetting(text(R.string.settings_tools)).assertIsDisplayed()
    }

    @Test fun nestedToolbarReturnsThroughToolsBeforeTheRoot() {
        settingsTab()
        compose.revealSetting(text(R.string.settings_tools)).performClick()
        compose.revealSetting(text(R.string.diagnostics_title)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("page_up").performClick()
        compose.waitForIdle()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle,
            text(R.string.settings_tools))).assertExists()
        compose.onNodeWithTag("page_up").performClick()
        compose.waitForIdle()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle,
            text(R.string.settings_title))).assertExists()
        compose.onNodeWithTag("page_up").assertDoesNotExist()
    }
}
