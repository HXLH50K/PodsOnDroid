package moe.chenxy.hyperpods.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.PageMotion
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PageScaffold(
    title: String,
    padding: PaddingValues,
    onBack: (() -> Unit)? = null,
    active: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxSize().padding(top = padding.calculateTopPadding())
            .semantics { paneTitle = title }) {
            val toolbar = Modifier.fillMaxWidth().testTag("page_toolbar")
            if (onBack == null) SmallTopAppBar(title = title, modifier = toolbar, defaultWindowInsetsPadding = false)
            else SmallTopAppBar(title = title, modifier = toolbar, defaultWindowInsetsPadding = false,
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = active, modifier = Modifier.size(48.dp).testTag("page_up")) {
                        Icon(MiuixIcons.Useful.Back, stringResource(R.string.dashboard_back),
                            modifier = Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onBackground)
                    }
                })
            Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                content(PaddingValues(bottom = padding.calculateBottomPadding()))
            }
        }
    }
}

@Composable
internal fun <T : Any> PageTransitionHost(
    target: T,
    depth: (T) -> Int,
    active: Boolean,
    content: @Composable (T, Boolean) -> Unit,
) {
    val saved = rememberSaveableStateHolder()
    val focus = LocalFocusManager.current
    LaunchedEffect(target, active) { if (active) focus.clearFocus() }
    AnimatedContent(targetState = target, contentKey = { it },
        modifier = Modifier.fillMaxSize().clipToBounds(),
        transitionSpec = {
            val direction = PageMotion.direction(depth(initialState), depth(targetState))
            ((fadeIn(tween(PageMotion.DURATION_MS)) +
                slideInHorizontally(tween(PageMotion.DURATION_MS, easing = FastOutSlowInEasing)) {
                    PageMotion.enterOffset(it, direction)
                }) togetherWith (fadeOut(tween(PageMotion.EXIT_FADE_MS)) +
                slideOutHorizontally(tween(PageMotion.DURATION_MS, easing = FastOutSlowInEasing)) {
                    PageMotion.exitOffset(it, direction)
                })).using(null)
        }, label = "PageNavigation") { page ->
        val current = active && page == target
        val interaction = if (current) Modifier else Modifier.clearAndSetSemantics {}.pointerInput(Unit) {
            // An outgoing page may still be drawn, but must not issue another command.
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
        Box(Modifier.fillMaxSize().focusProperties { canFocus = current }.then(interaction)) {
            saved.SaveableStateProvider(page) { content(page, current) }
        }
    }
}
