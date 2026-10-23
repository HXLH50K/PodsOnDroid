package moe.chenxy.hyperpods.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AppPreferences
import moe.chenxy.hyperpods.utils.BatteryNotificationSettings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun rememberBatteryNotificationPreferences(): State<AppPreferences.Snapshot> {
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
    Toast.makeText(context, R.string.battery_notifications_saved, Toast.LENGTH_SHORT).show()
    return true
}

/** Phone-only settings; place directly in an always-visible settings list item. */
@Composable
fun BatteryNotificationSettingsPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val policy by rememberBatteryNotificationPreferences()
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
    var low by rememberSaveable(settings, migrationRequired) { mutableStateOf(settings.lowThreshold.toString()) }
    var medium by rememberSaveable(settings, migrationRequired) { mutableStateOf(settings.mediumThreshold.toString()) }
    var lowEnabled by rememberSaveable(settings, migrationRequired) { mutableStateOf(settings.lowReminderEnabled) }
    var mediumEnabled by rememberSaveable(settings, migrationRequired) { mutableStateOf(settings.mediumReminderEnabled) }
    var validationError by rememberSaveable(settings, migrationRequired) { mutableStateOf(false) }
    val lowValue = low.toIntOrNull()
    val mediumValue = medium.toIntOrNull()
    val draft = if (lowValue != null && mediumValue != null)
        BatteryNotificationSettings(lowValue, mediumValue, lowEnabled, mediumEnabled).takeIf { it.valid }
    else null
    val preview = draft ?: settings

    Column(modifier.fillMaxWidth()) {
        DashboardGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.battery_notifications_summary),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
                if (migrationRequired) Text(stringResource(R.string.battery_notifications_migration_notice),
                    color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                TextField(
                    value = low, onValueChange = { low = it },
                    label = stringResource(R.string.battery_notifications_low_threshold),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = medium, onValueChange = { medium = it },
                    label = stringResource(R.string.battery_notifications_medium_threshold),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(stringResource(R.string.battery_notifications_threshold_hint),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
                if (validationError) Text(stringResource(R.string.battery_notifications_invalid),
                    color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            DashboardToggle(stringResource(R.string.battery_notifications_low_reminder),
                stringResource(R.string.battery_notifications_low_reminder_summary), lowEnabled,
                { lowEnabled = it })
            DashboardDivider(Modifier.padding(horizontal = 16.dp))
            DashboardToggle(stringResource(R.string.battery_notifications_medium_reminder),
                stringResource(R.string.battery_notifications_medium_reminder_summary), mediumEnabled,
                { mediumEnabled = it })
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.battery_notifications_preview_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.battery_notifications_preview_summary),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
                if (draft == null) Text(stringResource(R.string.battery_notifications_preview_saved),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BatteryPreviewEar(stringResource(R.string.battery_notifications_left), 18, preview, Modifier.weight(1f))
                    BatteryPreviewEar(stringResource(R.string.battery_notifications_right), 72, preview, Modifier.weight(1f))
                }
            }
            DashboardDivider(Modifier.padding(horizontal = 16.dp))
            DashboardLink(stringResource(R.string.battery_notifications_save), null) {
                if (draft == null) {
                    validationError = true
                    Toast.makeText(context, R.string.battery_notifications_invalid, Toast.LENGTH_LONG).show()
                } else if (onSave(draft)) {
                    validationError = false
                }
            }
        }
    }
}

@Composable
private fun BatteryPreviewEar(side: String, level: Int, settings: BatteryNotificationSettings, modifier: Modifier) {
    val band = stringResource(when (settings.band(level)) {
        BatteryNotificationSettings.Band.LOW -> R.string.battery_notifications_band_low
        BatteryNotificationSettings.Band.MEDIUM -> R.string.battery_notifications_band_medium
        BatteryNotificationSettings.Band.HIGH -> R.string.battery_notifications_band_high
        BatteryNotificationSettings.Band.UNKNOWN -> R.string.battery_notifications_band_unknown
    })
    val description = stringResource(R.string.battery_notifications_preview_accessibility, side, level, band)
    // The shared notification palette remains legible on this dark surface in either app theme.
    Column(modifier.background(Color(0xff20232a), RoundedCornerShape(16.dp))
        .clearAndSetSemantics { contentDescription = description }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(side, color = Color.White, fontSize = 14.sp)
        Text("$level%", color = Color(settings.color(level)), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(band, color = Color.White, fontSize = 12.sp)
    }
}
