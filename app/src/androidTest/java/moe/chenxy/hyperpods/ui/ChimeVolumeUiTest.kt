package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import moe.chenxy.hyperpods.ui.components.ChimeVolumeSlider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ChimeVolumeUiTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val value = mutableStateOf(30)
    private val feedback = mutableStateOf("reported")
    private val writes = mutableListOf<Int>()
    private fun slider() = compose.onNodeWithTag("chime_volume_slider")
    private fun level() = slider().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

    private fun show() {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(1) {
                    ChimeVolumeSlider(value.value, feedback.value, true, null, { writes.add(it) })
                }
            }
        }
    }

    @Test fun aReportDuringDragDoesNotMoveTheThumbOrSendEarly() {
        show()
        slider().performTouchInput {
            down(center)
            moveTo(Offset(width * .85f, centerY), 100)
        }
        val dragged = level()
        assertTrue(dragged > 70f)
        compose.runOnIdle { value.value = 10 }
        assertEquals(dragged, level(), .1f)
        assertTrue(writes.isEmpty())
        slider().performTouchInput { up() }
        compose.runOnIdle { assertEquals(1, writes.size) }
        assertEquals(dragged, level(), .1f)
    }

    @Test fun aRealReplyAnimatesInsteadOfJumpingToTheNewValue() {
        show()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { value.value = 80 }
        compose.mainClock.advanceTimeBy(80)
        val middle = level()
        assertTrue("Expected an in-flight animation, got $middle", middle > 30f && middle < 80f)
        compose.mainClock.advanceTimeBy(400)
        assertEquals(80f, level(), .1f)
        compose.mainClock.autoAdvance = true
    }

    @Test fun sentOnlyStateDoesNotOverwriteTheUserTarget() {
        show()
        slider().performTouchInput { swipe(center, Offset(width * .85f, centerY), 300) }
        val target = level()
        compose.runOnIdle { feedback.value = "sent"; value.value = 10 }
        assertEquals(target, level(), .1f)
        compose.runOnIdle { feedback.value = "reported" }
        compose.waitForIdle()
        assertEquals(15f, level(), .1f)
        compose.onNodeWithText("10%").assertExists()
        assertEquals(1, writes.size)
    }

    @Test fun lowerRealReadingIsShownWithoutAnAutomaticWrite() {
        value.value = 10
        show()
        compose.onNodeWithText("10%").assertExists()
        val info = slider().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(15f..100f, info.range)
        assertEquals(15f, info.current, .1f)
        assertTrue(writes.isEmpty())
    }
}
