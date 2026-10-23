package moe.chenxy.hyperpods.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.DashboardGroup
import moe.chenxy.hyperpods.ui.components.DashboardSection
import moe.chenxy.hyperpods.ui.components.DashboardToggle
import moe.chenxy.hyperpods.utils.AppAppearance
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalAppAppearance = staticCompositionLocalOf { AppAppearance() }
private val LocalSaveAppearance = staticCompositionLocalOf<(AppAppearance) -> Unit> {
    { error("Appearance provider is not installed") }
}
internal val LocalNavigationEffects = staticCompositionLocalOf { false }

@Composable
internal fun AppAppearanceProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { context.getSharedPreferences(AppAppearance.FILE, Context.MODE_PRIVATE) }
    var appearance by remember { mutableStateOf(AppAppearance()) }
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var powerSave by remember { mutableStateOf(true) }
    var highContrast by remember { mutableStateOf(true) }
    DisposableEffect(context, preferences, lifecycle) {
        fun load() {
            try {
                appearance = AppAppearance.read(preferences)
            } catch (error: ClassCastException) {
                Log.e("PODS-Appearance", "Invalid appearance preferences; retaining readable settings", error)
                Toast.makeText(context, R.string.appearance_read_failed, Toast.LENGTH_LONG).show()
            }
        }
        fun environment() {
            powerSave = requireNotNull(context.getSystemService(PowerManager::class.java)).isPowerSaveMode
            highContrast = Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) != 0
        }
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (resumed) environment()
        }
        val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { environment() }
        }
        val contrastObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { environment() }
        }
        val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> load() }
        load()
        environment()
        lifecycle.addObserver(observer)
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        context.registerReceiver(powerReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            Context.RECEIVER_NOT_EXPORTED)
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("high_text_contrast_enabled"), false, contrastObserver)
        onDispose {
            context.contentResolver.unregisterContentObserver(contrastObserver)
            context.unregisterReceiver(powerReceiver)
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            lifecycle.removeObserver(observer)
        }
    }
    val save: (AppAppearance) -> Unit = { requested ->
        try {
            check(requested.save(preferences)) { "appearance_preferences_write_failed" }
            appearance = requested
        } catch (error: RuntimeException) {
            Log.e("PODS-Appearance", "Unable to save appearance settings", error)
            Toast.makeText(context, R.string.appearance_save_failed, Toast.LENGTH_LONG).show()
        }
    }
    CompositionLocalProvider(LocalAppAppearance provides appearance, LocalSaveAppearance provides save,
        LocalNavigationEffects provides appearance.effectsAllowed(true, resumed, powerSave, highContrast),
        content = content)
}

@Composable
internal fun NavigationSurface(state: HazeState?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val effects = state != null && LocalNavigationEffects.current
    val colors = MiuixTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val style = HazeStyle(backgroundColor = colors.background,
        tint = HazeTint(colors.surface.copy(alpha = if (dark) .65f else .72f)),
        blurRadius = 24.dp, noiseFactor = .02f,
        fallbackTint = HazeTint(colors.surface))
    val shape = RoundedCornerShape(40.dp)
    val material = if (effects) Modifier.hazeEffect(requireNotNull(state), style = style)
        else Modifier.background(colors.surface)
    Box(modifier.clip(shape).then(material).border(1.dp,
        if (dark) Color.White.copy(alpha = .22f) else Color.Black.copy(alpha = .10f), shape)) { content() }
}

@Composable
internal fun AppearanceSettings() {
    val settings = LocalAppAppearance.current
    val save = LocalSaveAppearance.current
    DashboardGroup {
        DashboardToggle(stringResource(R.string.appearance_reduced), stringResource(R.string.appearance_reduced_summary),
            settings.reducedTransparency, { save(settings.copy(reducedTransparency = it)) })
    }
    Text(stringResource(R.string.appearance_navigation_summary), fontSize = 14.sp,
        modifier = Modifier.padding(16.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    if (!LocalNavigationEffects.current) Text(stringResource(R.string.appearance_opaque_active),
        modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}
