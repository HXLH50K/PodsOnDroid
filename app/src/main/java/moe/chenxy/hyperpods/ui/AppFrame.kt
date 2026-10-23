package moe.chenxy.hyperpods.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import moe.chenxy.hyperpods.ui.components.DashboardNavigation
import moe.chenxy.hyperpods.utils.FloatingDockLayout
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppFrame(pager: PagerState, content: @Composable (PaddingValues) -> Unit) {
    val state = remember { HazeState() }
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = navigation + (FloatingDockLayout.HEIGHT_DP + FloatingDockLayout.MARGIN_DP * 2).dp
    val obscured = with(LocalDensity.current) { bottom.toPx() }
    val bringIntoView = remember(obscured) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                FloatingDockLayout.scrollDistance(offset, size, containerSize, obscured)
        }
    }
    val effects = LocalNavigationEffects.current
    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).imePadding()) {
        Box(Modifier.fillMaxSize().then(if (effects) Modifier.hazeSource(state) else Modifier)) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoView) {
                content(PaddingValues(top = top, bottom = bottom))
            }
        }
        DashboardNavigation(pager, Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding().padding(horizontal = 24.dp, vertical = FloatingDockLayout.MARGIN_DP.dp),
            state)
    }
}
