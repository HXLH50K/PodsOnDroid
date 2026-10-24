package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.ScopeRestartControls
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestartSelection
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** No production coordinator, root command or system process is used by these tests. */
@RunWith(Parameterized::class)
class ScopeRestartUiTest(private val theme: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "theme={0}")
        fun themes() = listOf(arrayOf(1), arrayOf(2))
    }
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val state = mutableStateOf(InterfaceScopeRestart.State())
    private val hosts = ScopeRestartSelection.available()
    private val stored = mutableMapOf<String, Boolean>()
    private val starts = mutableListOf<Set<String>>()
    private var canSave = true

    private fun choice(host: ScopeRestart.Host) =
        compose.onNodeWithTag("scope_choice_${host.name}", useUnmergedTree = true)

    private fun show() {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(theme) {
                    ScopeRestartControls(hosts, ScopeRestartSelection.selected(hosts, stored), state.value,
                        saveSelection = { selected ->
                            if (canSave) hosts.forEach { stored[it.processName] = it.processName in selected }
                            canSave
                        },
                        start = { starts.add(it); true })
                }
            }
        }
    }

    private fun open() = compose.onNodeWithText(compose.activity.getString(R.string.scope_restart_title)).performClick()

    @Test fun allScopesDefaultToCheckedAndOnlyConfirmedChoicesAreSent() {
        show()
        open()
        hosts.forEach { choice(it).assertIsOn() }
        choice(ScopeRestart.Host.BLUETOOTH).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.scope_restart_confirm)).performClick()
        assertEquals(listOf(hosts.filter { it != ScopeRestart.Host.BLUETOOTH }.map { it.processName }.toSet()), starts)
        show()
        open()
        choice(ScopeRestart.Host.BLUETOOTH).assertIsOff()
        choice(ScopeRestart.Host.SYSTEM_UI).assertIsOn()
    }

    @Test fun emptySelectionCannotStartAndCancelDoesNotSaveOrStart() {
        show()
        open()
        hosts.forEach { choice(it).performClick() }
        compose.onNodeWithText(compose.activity.getString(R.string.scope_restart_confirm)).assertIsNotEnabled()
        compose.onNodeWithText(compose.activity.getString(android.R.string.cancel)).performClick()
        assertTrue(starts.isEmpty())
        assertTrue(stored.isEmpty())
    }

    @Test fun saveFailureDoesNotStartARestart() {
        canSave = false
        show()
        open()
        compose.onNodeWithText(compose.activity.getString(R.string.scope_restart_confirm)).performClick()
        compose.onNodeWithTag("scope_selection_dialog").assertIsDisplayed()
        assertTrue(starts.isEmpty())
    }

    @Test fun progressBlocksBackAndOffersNoStopOrCancelAction() {
        state.value = InterfaceScopeRestart.State(running = true)
        show()
        compose.onNodeWithTag("scope_progress_dialog").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("scope_progress_dialog").assertIsDisplayed()
        compose.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("scope_progress_dialog")),
            useUnmergedTree = true).assertCountEquals(0)
        compose.runOnUiThread { state.value = state.value.copy(running = false) }
        compose.waitForIdle()
        compose.onNodeWithTag("scope_progress_dialog").assertDoesNotExist()
    }
}
