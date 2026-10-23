package moe.chenxy.hyperpods.ui.components

import android.app.Activity
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.SeekBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AirPodsBase
import moe.chenxy.hyperpods.utils.PanelMediaVolume
import moe.chenxy.hyperpods.utils.PanelModeState
import moe.chenxy.hyperpods.utils.StatusFreshness
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import top.yukonga.miuix.kmp.basic.Text

private val PanelSecondary = Color.White.copy(alpha = .65f)
private val PanelControl = Color.Black.copy(alpha = .19f)

@Composable
internal fun HeadphonePanelContent(
    activity: Activity,
    name: String?,
    battery: BatteryParams,
    model: AirPodsBase?,
    diagnostics: Bundle?,
    modeState: PanelModeState,
    hostPending: Boolean,
    allowedModes: Set<Int>,
    maxHeightDp: Float,
    connected: Boolean,
    onMode: (Int) -> Unit,
    onMore: () -> Unit,
    onClose: () -> Unit,
) {
    val closeLabel = stringResource(R.string.panel_close)
    val reportedBatteries = listOf(battery.left, battery.right, battery.case)
    val batteryKeys = listOf("left_battery_at", "right_battery_at", "case_battery_at")
    val stale = reportedBatteries.indices.any { index ->
        StatusFreshness.battery(reportedBatteries[index], diagnostics?.getLong(batteryKeys[index]) ?: 0,
            diagnostics?.getString("connection")) == StatusFreshness.State.RETAINED
    }
    Column(Modifier.fillMaxWidth().heightIn(max = maxHeightDp.dp).clip(RoundedCornerShape(40.dp))
        .semantics { dismiss { onClose(); true } }
        .verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(name ?: stringResource(R.string.app_name), color = Color.White, fontSize = 22.sp,
                lineHeight = 28.sp, fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() })
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onClose)
                .semantics { contentDescription = closeLabel }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.panel_close), null, Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(PanelSecondary))
            }
        }
        PanelBatteryRow(battery, model)
        if (stale) Text(stringResource(R.string.panel_battery_retained), fontSize = 12.sp,
            color = PanelSecondary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        Box(Modifier.fillMaxWidth().height(172.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(model?.budsRes ?: R.drawable.dashboard_headphones), null,
                Modifier.size(if (model == null) 64.dp else 156.dp))
        }
        if (diagnostics?.getBoolean("ready") != true) Text(
            stringResource(dashboardConnectionStatus(diagnostics)), fontSize = 13.sp,
            color = PanelSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        if (allowedModes.isNotEmpty()) {
            PanelNoise(modeState, allowedModes,
                diagnostics?.getBoolean("ready") == true && !hostPending && modeState.requested == null, onMode)
            val status = when {
                modeState.failed -> R.string.panel_failed
                modeState.requested != null || hostPending -> R.string.panel_sending
                modeState.reported == null -> R.string.settings_reading
                else -> null
            }
            status?.let { Text(stringResource(it), color = PanelSecondary, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) }
            Spacer(Modifier.height(8.dp))
        }
        PanelVolume(activity, connected)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(24.dp))
            .background(PanelControl).clickable(role = Role.Button, onClick = onMore)
            .padding(14.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.panel_more), color = Color.White, fontSize = 18.sp,
                fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelBatteryRow(battery: BatteryParams, model: AirPodsBase?) {
    FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelBattery(battery.left, model?.leftBudsRes ?: R.drawable.pods_earbud_left, R.string.dashboard_left)
        PanelBattery(battery.right, model?.rightBudsRes ?: R.drawable.pods_earbud_right, R.string.dashboard_right)
        PanelBattery(battery.case, model?.caseRes ?: R.drawable.airpods_pro_case_notification, R.string.dashboard_case)
    }
}

@Composable
private fun PanelBattery(value: PodBatteryParams?, image: Int, label: Int) {
    val known = value?.isConnected == true && value.battery in 0..100
    val percent = if (known) stringResource(R.string.panel_battery_percent, value!!.battery)
        else stringResource(R.string.panel_battery_unknown)
    val charging = known && value?.isCharging == true
    val description = stringResource(R.string.panel_battery_description, stringResource(label), percent) +
        if (charging) ", ${stringResource(R.string.dashboard_charging)}" else ""
    Row(Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), null, Modifier.size(18.dp))
        Text(percent, color = PanelSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 3.dp))
        if (charging) Image(painterResource(R.drawable.panel_charging), null, Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(Color(0xFF86E983)))
    }
}

@Composable
private fun PanelNoise(state: PanelModeState, supported: Set<Int>, enabled: Boolean, select: (Int) -> Unit) {
    val labels = listOf(R.string.off, R.string.noise_cancellation_title, R.string.dashboard_transparency, R.string.adaptive_title)
    val icons = listOf(R.drawable.dashboard_mode_off, R.drawable.dashboard_mode_anc,
        R.drawable.dashboard_mode_transparency, R.drawable.dashboard_mode_adaptive)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PanelControl).padding(12.dp)) {
        Text(stringResource(R.string.dashboard_noise), color = PanelSecondary, fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.indices.filter { it + 1 in supported }.forEach { index ->
                val selected = state.reported == index + 1
                val foreground = if (selected) Color(0xFF414147) else Color.White
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color.White.copy(alpha = .92f) else Color.Transparent)
                    .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = { select(index + 1) })
                    .alpha(if (enabled) 1f else .45f).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(icons[index]), null, Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(foreground))
                    Text(stringResource(labels[index]), color = foreground, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun PanelVolume(activity: Activity, connected: Boolean) {
    val volume = remember(activity) { PanelMediaVolume(requireNotNull(activity.getSystemService(AudioManager::class.java))) }
    var reading by remember { mutableStateOf<PanelMediaVolume.Reading?>(null) }
    var error by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(volume) {
        try {
            while (true) {
                reading = volume.read()
                delay(500)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: IllegalStateException) {
            Log.e("PODS-Panel", "Media volume state unavailable", failure)
            reading = null
            error = R.string.panel_volume_unavailable
        } catch (failure: SecurityException) {
            Log.e("PODS-Panel", "Media volume access denied", failure)
            reading = null
            error = R.string.panel_volume_unavailable
        }
    }
    val setVolume: (Int) -> Int? = { requested ->
        if (connected) {
            try {
                val actual = volume.set(requested)
                reading = actual
                error = if (actual.level == requested) null else R.string.panel_volume_limited
            } catch (failure: RuntimeException) {
                Log.e("PODS-Panel", "Media volume request failed", failure)
                error = R.string.panel_volume_unavailable
            }
        }
        reading?.level
    }
    val currentCallback by rememberUpdatedState(setVolume)
    val volumeLabel = stringResource(R.string.panel_media_volume)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PanelControl).padding(16.dp)) {
        Text(volumeLabel, color = Color.White, fontSize = 17.sp)
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.CenterStart) {
            AndroidView(modifier = Modifier.fillMaxWidth().height(48.dp),
                factory = { context ->
                    SeekBar(context).apply {
                        val density = resources.displayMetrics.density
                        setPadding(0, 0, 0, 0)
                        thumb = null
                        splitTrack = false
                        val track = GradientDrawable().apply {
                            cornerRadius = 24 * density
                            setColor(android.graphics.Color.argb(38, 255, 255, 255))
                        }
                        val fill = GradientDrawable().apply {
                            cornerRadius = 24 * density
                            setColor(android.graphics.Color.WHITE)
                        }
                        progressDrawable = LayerDrawable(arrayOf(track,
                            ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL))).apply {
                            setId(0, android.R.id.background)
                            setId(1, android.R.id.progress)
                        }
                        minHeight = (22 * density).toInt()
                        maxHeight = (22 * density).toInt()
                        contentDescription = volumeLabel
                        var tracking = false
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onStartTrackingTouch(seekBar: SeekBar) { tracking = true }
                            override fun onStopTrackingTouch(seekBar: SeekBar) {
                                tracking = false
                                currentCallback(seekBar.progress)?.let { seekBar.progress = it }
                            }
                            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                if (fromUser && !tracking) currentCallback(progress)?.let { seekBar.progress = it }
                            }
                        })
                    }
                },
                update = { view ->
                    val value = reading
                    view.isEnabled = connected && value?.adjustable == true
                    view.alpha = if (view.isEnabled) 1f else .45f
                    view.min = value?.minimum ?: 0
                    view.max = value?.maximum?.coerceAtLeast(view.min + 1) ?: 1
                    if (!view.isPressed) view.progress = value?.level ?: 0
                    view.contentDescription = volumeLabel
                })
            Image(painterResource(R.drawable.panel_volume), null, Modifier.padding(start = 12.dp).size(18.dp),
                colorFilter = ColorFilter.tint(if ((reading?.level ?: 0) > 0) Color(0xFF55555A) else PanelSecondary))
        }
        val message = error ?: if (reading?.adjustable == false) R.string.panel_volume_fixed else null
        message?.let { Text(stringResource(it), color = PanelSecondary, fontSize = 12.sp) }
    }
}
