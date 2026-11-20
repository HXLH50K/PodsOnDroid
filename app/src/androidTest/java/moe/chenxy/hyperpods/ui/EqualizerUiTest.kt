package moe.chenxy.hyperpods.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.EqualizerSettings
import moe.chenxy.hyperpods.utils.AirPods5WCC
import moe.chenxy.hyperpods.utils.CustomEq
import moe.chenxy.hyperpods.utils.CustomEqState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EqualizerUiTest {
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val state = mutableStateOf(CustomEqState(CustomEq(CustomEq.CUSTOM), connectionId = "fixture"))
    private val changes = mutableListOf<Pair<CustomEq, CustomEqState>>()
    private fun text(id: Int) = compose.activity.getString(id)

    private fun show(firmware: String) {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(1) {
                    EqualizerSettings(state.value, AirPods5WCC(), firmware, connected = true, active = true,
                        onChange = { value, snapshot -> changes.add(value to snapshot) })
                }
            }
        }
    }

    @Test fun bandEditsStayLocalUntilAppliedAndReportsReplaceDrafts() {
        show("9441861")
        val apply = compose.onNodeWithText(text(R.string.eq_apply))
        apply.assertIsNotEnabled()
        compose.onNodeWithContentDescription(text(R.string.eq_low)).performTouchInput {
            swipe(center, Offset(width * .85f, centerY))
        }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        apply.assertIsEnabled().performClick()
        compose.runOnIdle {
            val (value, snapshot) = changes.single()
            assertEquals(state.value, snapshot)
            assertTrue(value.low > 50)
            assertEquals(50, value.mid)
            assertEquals(50, value.high)
            state.value = state.value.copy(value = CustomEq(CustomEq.RECOMMENDED), revision = 1)
        }
        apply.assertDoesNotExist()
        compose.onNodeWithContentDescription(text(R.string.eq_low)).assertDoesNotExist()
    }

    @Test fun olderFirmwareShowsAnExplanationWithoutEditingControls() {
        state.value = state.value.copy(value = null)
        show("8A358")
        compose.onNodeWithText(text(R.string.eq_firmware_required)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.eq_low)).assertDoesNotExist()
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test fun aHeadsetReportDuringDragKeepsTheEditedBandAndUpdatesUntouchedBands() {
        show("9441861")
        val low = compose.onNodeWithContentDescription(text(R.string.eq_low))
        low.performTouchInput {
            down(center)
            moveTo(Offset(width * .85f, centerY), 100)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.eq_band_value, text(R.string.eq_low), 50))
            .assertDoesNotExist()
        compose.runOnIdle {
            state.value = state.value.copy(value = CustomEq(CustomEq.CUSTOM, mid = 70), revision = 1)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.eq_band_value, text(R.string.eq_low), 50))
            .assertDoesNotExist()
        low.performTouchInput { up() }
        compose.onNodeWithText(text(R.string.eq_apply)).performClick()
        compose.runOnIdle {
            val (value, snapshot) = changes.single()
            assertTrue(value.low > 50)
            assertEquals(70, value.mid)
            assertEquals(1L, snapshot.revision)
        }
    }

    @Test fun footerUsesSmallCopyAndSeparatedTouchTargets() {
        show("9441861")
        val helpText = compose.activity.getString(R.string.eq_neutral, CustomEq.NEUTRAL)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(helpText).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertTrue(layouts.single().layoutInput.style.fontSize.value <= 13)
        val reset = compose.onNode(hasText(text(R.string.eq_reset)) and hasClickAction()).getUnclippedBoundsInRoot()
        val apply = compose.onNode(hasText(text(R.string.eq_apply)) and hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue((reset.bottom - reset.top).value >= 47 && (apply.bottom - apply.top).value >= 47)
        assertTrue(apply.left.value - reset.right.value >= 15)
    }

    @Test fun allBandsStillDragInThePagerAfterACancelledGesture() {
        compose.runOnIdle { compose.activity.intent.putExtra("equalizer_preview", true) }
        compose.activityRule.scenario.recreate()
        openEqualizer()
        val low = compose.onNodeWithContentDescription(text(R.string.eq_low))
        low.performTouchInput {
            down(center)
            moveTo(Offset(width * .8f, centerY), 100)
            cancel()
        }
        for (band in listOf(R.string.eq_low, R.string.eq_mid, R.string.eq_high)) {
            val slider = compose.onNodeWithContentDescription(text(band))
            for (fraction in listOf(.2f, .8f)) {
                slider.performTouchInput {
                    swipe(Offset(width * (1f - fraction), centerY), Offset(width * fraction, centerY))
                }
                val value = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
                assertTrue("Band ${text(band)} should follow the swipe to $fraction, but was $value",
                    if (fraction < .5f) value < 40 else value > 60)
            }
        }
        compose.onNodeWithText(text(R.string.eq_apply)).assertIsEnabled().performClick()
        compose.onNodeWithText(text(R.string.eq_apply)).assertIsNotEnabled()
    }

    @Test fun equalizerBackReturnsToMoreControls() {
        openEqualizer()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(text(R.string.eq_title)).assertIsDisplayed()
    }

    private fun openEqualizer() {
        val more = text(R.string.dashboard_more)
        val eq = text(R.string.eq_title)
        compose.revealSetting(more)
        compose.onNodeWithText(more).performClick()
        compose.revealSetting(eq)
        compose.onNodeWithText(eq).performClick()
        compose.onNodeWithText(text(R.string.eq_mode)).assertIsDisplayed()
    }
}
