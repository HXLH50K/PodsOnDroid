package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.ScopeRestartControls
import moe.chenxy.hyperpods.ui.components.scopeHostTitle
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestartSelection
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No production coordinator, root command or system process is used by these tests. */
class ScopeRestartUiTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val state = mutableStateOf(InterfaceScopeRestart.State())
    private val hosts = ScopeRestartSelection.available(true)
    private val stored = mutableMapOf<String, Boolean>()
    private val starts = mutableListOf<Set<String>>()
    private var cancellations = 0
    private var canSave = true

    private fun label(host: ScopeRestart.Host) =
        compose.activity.getString(scopeHostTitle(host.processName)) + "\n" + host.processName

    private fun show() {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(1) {
                    ScopeRestartControls(hosts, ScopeRestartSelection.selected(hosts, stored), state.value,
                        saveSelection = { selected ->
                            if (canSave) hosts.forEach { stored[it.processName] = it.processName in selected }
                            canSave
                        },
                        start = { starts.add(it); true },
                        cancel = { cancellations++ })
                }
            }
        }
    }

    private fun open() = compose.onNodeWithText(compose.activity.getString(R.string.scope_restart_title)).performClick()

    @Test fun allScopesDefaultToCheckedAndOnlyConfirmedChoicesAreSent() {
        show()
        open()
        hosts.forEach { onView(withText(label(it))).check(matches(isChecked())) }
        onView(withText(label(ScopeRestart.Host.BLUETOOTH))).perform(click())
        onView(withText(R.string.scope_restart_confirm)).perform(click())
        assertEquals(listOf(hosts.filter { it != ScopeRestart.Host.BLUETOOTH }.map { it.processName }.toSet()), starts)
        show()
        open()
        onView(withText(label(ScopeRestart.Host.BLUETOOTH))).check(matches(not(isChecked())))
        onView(withText(label(ScopeRestart.Host.SYSTEM_UI))).check(matches(isChecked()))
    }

    @Test fun emptySelectionCannotStartAndCancelDoesNotSaveOrStart() {
        show()
        open()
        hosts.forEach { onView(withText(label(it))).perform(click()) }
        onView(withText(R.string.scope_restart_confirm)).check(matches(not(isEnabled())))
        onView(withText(android.R.string.cancel)).perform(click())
        assertTrue(starts.isEmpty())
        assertTrue(stored.isEmpty())
    }

    @Test fun saveFailureDoesNotStartARestart() {
        canSave = false
        show()
        open()
        onView(withText(R.string.scope_restart_confirm)).perform(click())
        onView(withText(R.string.scope_selection_title)).check(matches(isDisplayed()))
        assertTrue(starts.isEmpty())
    }

    @Test fun progressBlocksBackAndRetainsAnExplicitStopAction() {
        state.value = InterfaceScopeRestart.State(running = true)
        show()
        onView(withText(R.string.scope_restart_running)).check(matches(isDisplayed()))
        Espresso.pressBack()
        onView(withText(R.string.scope_restart_running)).check(matches(isDisplayed()))
        onView(withText(R.string.scope_reload_cancel)).perform(click())
        assertEquals(1, cancellations)
        compose.runOnUiThread { state.value = state.value.copy(cancelling = true) }
        compose.waitForIdle()
        onView(withText(R.string.scope_reload_cancel)).check(matches(not(isEnabled())))
        compose.runOnUiThread { state.value = state.value.copy(running = false) }
        compose.waitForIdle()
        onView(withText(R.string.scope_restart_running)).check(doesNotExist())
    }
}
