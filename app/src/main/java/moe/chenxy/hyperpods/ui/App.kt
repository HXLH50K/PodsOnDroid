package moe.chenxy.hyperpods.ui

import androidx.compose.runtime.Composable


@Composable
fun App() {
    AppTheme {
        AppAppearanceProvider { MainUI() }
    }
}