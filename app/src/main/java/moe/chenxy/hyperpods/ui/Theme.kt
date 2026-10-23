package moe.chenxy.hyperpods.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun AppTheme(
    colorMode: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val light = lightColorScheme(
        primary = Color(0xFF006BE6), onPrimary = Color.White,
        primaryVariant = Color(0xFF006BE6), onPrimaryVariant = Color.White,
        background = Color(0xFFF3F5F9), onBackground = Color(0xFF171D28),
        surface = Color.White, onSurface = Color(0xFF171717),
        surfaceVariant = Color(0xFFE9EEF7),
        onBackgroundVariant = Color(0xFF66717E),
        onSurfaceVariantSummary = Color(0xFF66717E),
        dividerLine = Color(0xFFECEEF1)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF6594FF), onPrimary = Color(0xFF101B31),
        primaryVariant = Color(0xFF346BDD), onPrimaryVariant = Color.White,
        background = Color.Black, surface = Color(0xFF222D3E),
        onBackgroundVariant = Color(0xFFB3BED0), onSurfaceVariantSummary = Color(0xFFB3BED0),
        surfaceVariant = Color(0xFF303E55), dividerLine = Color(0xFF3C495D)
    )
    return MiuixTheme(
        colors = when (colorMode) {
            1 -> light
            2 -> dark
            else -> if (darkTheme) dark else light
        },
        content = content
    )
}
