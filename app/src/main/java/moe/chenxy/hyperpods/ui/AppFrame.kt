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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import moe.chenxy.hyperpods.ui.components.DashboardNavigation
import moe.chenxy.hyperpods.utils.FloatingDockLayout
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppFrame(pager: PagerState, content: @Composable (PaddingValues) -> Unit) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { GlassBackdrop(layer) }
    val focus = LocalFocusManager.current
    LaunchedEffect(pager.targetPage) { focus.clearFocus() }
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
        Box(Modifier.fillMaxSize().then(if (effects) Modifier.glassSource(backdrop) else Modifier)) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoView) {
                content(PaddingValues(top = top, bottom = bottom))
            }
        }
        DashboardNavigation(pager, Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding().padding(horizontal = 24.dp, vertical = FloatingDockLayout.MARGIN_DP.dp),
            backdrop)
    }
    // The custom frame replaces Scaffold, including its default popup host.
    // Keep popups outside the page transitions, blur source and IME padding.
    MiuixPopupHost()
}
