package moe.chenxy.hyperpods.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
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
