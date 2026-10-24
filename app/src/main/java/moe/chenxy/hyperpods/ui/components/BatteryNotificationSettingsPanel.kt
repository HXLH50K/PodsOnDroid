package moe.chenxy.hyperpods.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AppPreferences
import moe.chenxy.hyperpods.utils.BatteryNotificationSettings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun rememberAppPreferences(): State<AppPreferences.Snapshot> {
    val context = LocalContext.current
    val policy = remember(context) { mutableStateOf(AppPreferences.read(context)) }
    DisposableEffect(context) {
        val listener: (AppPreferences.Snapshot) -> Unit = { policy.value = it }
        AppPreferences.addChangeListener(listener)
        policy.value = AppPreferences.read(context)
        onDispose { AppPreferences.removeChangeListener(listener) }
    }
    return policy
}

internal fun saveBatteryNotificationSettings(context: Context, settings: BatteryNotificationSettings): Boolean {
    if (!settings.valid) {
        Toast.makeText(context, R.string.battery_notifications_invalid, Toast.LENGTH_LONG).show()
        return false
    }
    val snapshot = AppPreferences.read(context).copy(batteryNotifications = settings,
        batteryNotificationsMigrationRequired = false)
    if (!AppPreferences.save(context, snapshot)) {
        Toast.makeText(context, R.string.battery_notifications_save_failed, Toast.LENGTH_LONG).show()
        return false
    }
    AppPreferences.publish(context, snapshot)
    return true
}

/** Phone-only settings; place directly in an always-visible settings list item. */
@Composable
fun BatteryNotificationSettingsPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val policy by rememberAppPreferences()
    BatteryNotificationSettingsGroup(
        settings = policy.batteryNotifications,
        migrationRequired = policy.batteryNotificationsMigrationRequired,
        onSave = { saveBatteryNotificationSettings(context, it) },
        modifier = modifier,
    )
}

@Composable
fun BatteryNotificationSettingsGroup(
    settings: BatteryNotificationSettings,
    onSave: (BatteryNotificationSettings) -> Boolean,
    modifier: Modifier = Modifier,
    migrationRequired: Boolean = false,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var low by rememberSaveable { mutableStateOf(settings.lowThreshold.toString()) }
    var medium by rememberSaveable { mutableStateOf(settings.mediumThreshold.toString()) }
    var saved by remember(settings) { mutableStateOf(settings) }
    var validationError by rememberSaveable { mutableStateOf(false) }
    var lowFocused by remember { mutableStateOf(false) }
    var mediumFocused by remember { mutableStateOf(false) }
    var migrationConfirmed by rememberSaveable { mutableStateOf(false) }
    fun saveThresholds() {
        val changed = low != saved.lowThreshold.toString() || medium != saved.mediumThreshold.toString()
        if (!changed && (!migrationRequired || migrationConfirmed)) return
        val draft = saved.withThresholds(low, medium)
        if (draft == null) {
            if (!validationError)
                Toast.makeText(context, R.string.battery_notifications_invalid, Toast.LENGTH_LONG).show()
            validationError = true
        } else if (onSave(draft)) {
            saved = draft
            validationError = false
            migrationConfirmed = true
        }
    }
    val saveOnExit by rememberUpdatedState(::saveThresholds)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) saveOnExit()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            saveOnExit()
        }
    }

    Column(modifier.fillMaxWidth()) {
        DashboardGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.battery_notifications_summary),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
                if (migrationRequired && !migrationConfirmed) Text(stringResource(R.string.battery_notifications_migration_notice),
                    color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                TextField(
                    value = low, onValueChange = { low = it; validationError = false },
                    label = stringResource(R.string.battery_notifications_low_threshold),
                    singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (lowFocused && !it.isFocused) saveThresholds()
                        lowFocused = it.isFocused
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                )
                TextField(
                    value = medium, onValueChange = { medium = it; validationError = false },
                    label = stringResource(R.string.battery_notifications_medium_threshold),
                    singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (mediumFocused && !it.isFocused) saveThresholds()
                        mediumFocused = it.isFocused
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                )
                Text(stringResource(R.string.battery_notifications_threshold_hint),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
                if (validationError) Text(stringResource(R.string.battery_notifications_invalid),
                    color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            DashboardToggle(stringResource(R.string.battery_notifications_low_reminder),
                stringResource(R.string.battery_notifications_low_reminder_summary), saved.lowReminderEnabled,
                {
                    focus.clearFocus()
                    val updated = saved.copy(lowReminderEnabled = it)
                    if (onSave(updated)) { saved = updated; migrationConfirmed = true }
                })
            DashboardDivider(Modifier.padding(horizontal = 16.dp))
            DashboardToggle(stringResource(R.string.battery_notifications_medium_reminder),
                stringResource(R.string.battery_notifications_medium_reminder_summary), saved.mediumReminderEnabled,
                {
                    focus.clearFocus()
                    val updated = saved.copy(mediumReminderEnabled = it)
                    if (onSave(updated)) { saved = updated; migrationConfirmed = true }
                })
        }
    }
}
