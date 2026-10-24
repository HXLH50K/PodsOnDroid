package moe.chenxy.hyperpods.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.GlassOptics
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
internal class GlassBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    onGloballyPositioned { backdrop.origin = it.positionInRoot() }.drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

@Composable
internal fun NavigationSurface(
    backdrop: GlassBackdrop?,
    progress: Float,
    pressure: Float,
    lightProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Boolean) -> Unit,
) {
    val context = LocalContext.current
    val hardware = LocalView.current.isHardwareAccelerated
    val enabled = backdrop != null && LocalNavigationEffects.current && hardware
    val colors = MiuixTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shader = remember(enabled) {
        if (!enabled) null else try {
            RuntimeShader(GlassOptics.shader)
        } catch (error: IllegalArgumentException) {
            Log.e("PODS-Glass", "Navigation shader could not compile; using opaque navigation", error)
            null
        }
    }
    LaunchedEffect(enabled, shader) {
        if (enabled && shader == null)
            Toast.makeText(context, R.string.appearance_glass_unavailable, Toast.LENGTH_LONG).show()
    }
    val glass = enabled && shader != null
    val materialDescription = stringResource(if (glass) R.string.appearance_glass_active else R.string.appearance_glass_solid)
    var position by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(40.dp)
    Box(modifier.onGloballyPositioned { position = it.positionInRoot() }
        .testTag("navigation_material").semantics { stateDescription = materialDescription }
        .clip(shape).background(colors.surface)) {
        if (glass) {
            val source = requireNotNull(backdrop)
            val lens = requireNotNull(shader)
            Box(Modifier.matchParentSize()
                .graphicsLayer {
                    if (size.width > 0 && size.height > 0) {
                        lens.setFloatUniform("resolution", size.width, size.height)
                        lens.setFloatUniform("density", density)
                        lens.setFloatUniform("dark", if (dark) 1f else 0f)
                        lens.setFloatUniform("selection", GlassOptics.selectionCenter(progress, size.width, density, rtl))
                        lens.setFloatUniform("press", pressure)
                        lens.setFloatUniform("lightPosition", GlassOptics.selectionCenter(lightProgress, size.width, density, rtl))
                        val refraction = RenderEffect.createRuntimeShaderEffect(lens, "backdrop")
                        val scatter = RenderEffect.createBlurEffect(GlassOptics.BLUR_DP * density,
                            GlassOptics.BLUR_DP * density, Shader.TileMode.CLAMP)
                        renderEffect = RenderEffect.createChainEffect(refraction, scatter).asComposeRenderEffect()
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                }
                .drawWithContent {
                    val offset = source.origin - position
                    translate(offset.x, offset.y) { drawLayer(source.layer) }
                })
        }
        content(glass)
        Box(Modifier.matchParentSize().border(1.dp,
            if (dark) Color.White.copy(alpha = .14f) else Color.White.copy(alpha = .55f), shape))
    }
}
