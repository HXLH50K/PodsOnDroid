package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import moe.chenxy.hyperpods.ui.components.ChimeVolumeSlider
import moe.chenxy.hyperpods.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ChimeVolumeUiTest(private val theme: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "theme={0}")
        fun themes() = listOf(arrayOf(1), arrayOf(2))
    }
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val value = mutableStateOf(30)
    private val feedback = mutableStateOf("reported")
    private val writes = mutableListOf<Int>()
    private fun slider() = compose.onNodeWithTag("chime_volume_slider")
    private fun level() = slider().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

    private fun show() {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(theme) {
                    ChimeVolumeSlider(value.value, feedback.value, true, null, { writes.add(it) })
                }
            }
        }
    }

    @Test fun volumeHelpUsesTheRecommendedRangeCopy() {
        show()
        compose.onNodeWithText(compose.activity.getString(R.string.chime_volume_range, 15, 100)).assertIsDisplayed()
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
        assertEquals(10f, level(), .1f)
        compose.onNodeWithText("10%").assertExists()
        assertEquals(1, writes.size)
    }

    @Test fun lowerRealReadingIsShownWithoutAnAutomaticWrite() {
        value.value = 10
        show()
        compose.onNodeWithText("10%").assertExists()
        val info = slider().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0f..100f, info.range)
        assertEquals(10f, info.current, .1f)
        assertTrue(writes.isEmpty())
    }

    @Test fun firstGestureStopsAtFifteenAndSecondGestureTurnsOrangeAndSubmitsLow() {
        value.value = 60
        show()
        slider().performTouchInput {
            down(Offset(width * .6f, centerY))
            moveTo(Offset(width * .05f, centerY), 300)
        }
        assertEquals(15f, level(), .1f)
        assertTrue(writes.isEmpty())
        slider().performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(15), writes) }
        slider().performTouchInput { swipe(Offset(width * .15f, centerY), Offset(width * .1f, centerY), 300) }
        compose.runOnIdle {
            assertEquals(2, writes.size)
            assertTrue(writes.last() in 0..14)
        }
        slider().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            compose.activity.getString(R.string.chime_volume_below_threshold, 15)))
        val pixels = slider().captureToImage().toPixelMap()
        val expectedColor = if (theme == 2) Color(0xFFFF825C) else Color(0xFFD84A28)
        assertTrue("Low tone volume must have an orange-red track",
            (0 until pixels.width).any { x -> (0 until pixels.height).any { y -> pixels[x, y] == expectedColor } })
    }

    @Test fun cancelledGestureDoesNotWriteOrUnlockTheThreshold() {
        value.value = 60
        show()
        slider().performTouchInput {
            down(Offset(width * .6f, centerY))
            moveTo(Offset(width * .05f, centerY), 300)
            cancel()
        }
        compose.waitForIdle()
        assertEquals(60f, level(), .1f)
        assertTrue(writes.isEmpty())
    }
}
