package moe.chenxy.hyperpods.ui

import android.graphics.RuntimeShader
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.DashboardNavigation
import moe.chenxy.hyperpods.utils.GlassOptics
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Hardware-rendered app-only fixture. No Bluetooth, preference or host operations. */
@RunWith(Parameterized::class)
@OptIn(ExperimentalTestApi::class)
class LiquidGlassTest(private val motionScale: Float) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "motion={0}")
        fun motionScales() = listOf(arrayOf(1f), arrayOf(0f))
    }
    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>(
        effectContext = object : MotionDurationScale { override val scaleFactor = motionScale })
    private val alternate = mutableStateOf(false)
    private val enabled = mutableStateOf(true)
    private val selection = mutableIntStateOf(0)

    private fun show(theme: Int = 1, rtl: Boolean = false) {
        compose.runOnUiThread {
            compose.activity.setContent {
                AppTheme(theme) {
                    CompositionLocalProvider(LocalNavigationEffects provides enabled.value,
                        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        val layer = rememberGraphicsLayer()
                        val backdrop = remember(layer) { GlassBackdrop(layer) }
                        Box(Modifier.fillMaxSize()) {
                            Canvas(Modifier.fillMaxSize().glassSource(backdrop)) {
                                val base = if (alternate.value) Color(0xFF1133CC) else Color(0xFFCC3311)
                                drawRect(base)
                                val step = 20.dp.toPx()
                                for (column in 0..(size.width / step).toInt())
                                    for (row in 0..(size.height / step).toInt())
                                        if ((column + row) % 2 == 0)
                                            drawRect(Color.White, Offset(column * step, row * step), Size(step, step))
                            }
                            DashboardNavigation(selection.intValue, { selection.intValue = it },
                                Modifier.align(Alignment.Center), backdrop)
                        }
                    }
                }
            }
        }
    }

    private fun snapshot() = compose.onNodeWithTag("navigation_material").captureToImage().toPixelMap()

    private fun changeDifference(): Float {
        val before = snapshot()
        compose.runOnIdle { alternate.value = !alternate.value }
        val after = snapshot()
        var delta = 0f
        var count = 0
        // Sample the lower rim, away from the unfiltered icon/label foreground.
        val y = (before.height * .9f).toInt()
        for (x in before.width / 5 until before.width * 4 / 5) {
            delta += kotlin.math.abs(before[x, y].red - after[x, y].red)
            delta += kotlin.math.abs(before[x, y].blue - after[x, y].blue)
            count += 2
        }
        return delta / count
    }

    @Test fun shaderCompilesOnTheSupportedAndroidRuntime() {
        RuntimeShader(GlassOptics.shader)
    }

    @Test fun realBackdropChangesReachTheGlassWithoutBitmapCapture() {
        show()
        compose.onNodeWithTag("navigation_material").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, compose.activity.getString(R.string.appearance_glass_active)))
        assertTrue("Live scene must contribute to refractive glass", changeDifference() > .02f)
    }

    @Test fun solidFallbackDoesNotSampleBackdropContent() {
        enabled.value = false
        show()
        compose.onNodeWithTag("navigation_material").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, compose.activity.getString(R.string.appearance_glass_solid)))
        assertTrue("Opaque fallback must ignore scene color", changeDifference() < .005f)
    }

    @Test fun darkGlassRetainsLiveContentAndTwoAccessibleTabs() {
        show(theme = 2)
        assertTrue(changeDifference() > .02f)
        val tabs = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        tabs.assertCountEquals(2)
        val density = compose.activity.resources.displayMetrics.density
        for (index in 0..1)
            assertTrue("Tab touch height must remain at least 48 dp", tabs[index].fetchSemanticsNode().boundsInRoot.height / density >= 48)
        tabs[1].performClick().assertIsSelected()
        tabs[0].performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(0, selection.intValue) }
    }

    @Test fun selectedLabelPlateMaintainsContrastOverDarkAndLightContent() {
        for (theme in listOf(1, 2)) {
            show(theme)
            for (changed in listOf(false, true)) {
                compose.runOnIdle { alternate.value = changed }
                val image = snapshot()
                val plate = image[(image.width * .12f).toInt(), image.height / 2]
                val foreground = if (theme == 1) Color(0xFF006BE6) else Color(0xFF6594FF)
                val ratio = (maxOf(plate.luminance(), foreground.luminance()) + .05f) /
                    (minOf(plate.luminance(), foreground.luminance()) + .05f)
                assertTrue("Selected text contrast was $ratio for theme $theme", ratio >= 4.5f)
            }

        }
    }

    @Test fun turningEffectsOffAndOnRetainsNavigationAndRefreshesTheBackdrop() {
        show()
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithTag("navigation_material").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, compose.activity.getString(R.string.appearance_glass_solid)))
        assertTrue(changeDifference() < .005f)
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[1]
            .performClick().assertIsSelected()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithTag("navigation_material").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, compose.activity.getString(R.string.appearance_glass_active)))
        assertTrue(changeDifference() > .02f)
    }

    @Test fun rtlKeepsTheSelectedLensBehindTheLogicalFirstTab() {
        show(rtl = true)
        val first = compose.onNode(hasText(compose.activity.getString(R.string.dashboard_headphones)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        val second = compose.onNode(hasText(compose.activity.getString(R.string.settings_title)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        assertTrue(first.fetchSemanticsNode().boundsInRoot.left > second.fetchSemanticsNode().boundsInRoot.left)
        first.assertIsSelected()
        second.performClick().assertIsSelected()
    }
}
