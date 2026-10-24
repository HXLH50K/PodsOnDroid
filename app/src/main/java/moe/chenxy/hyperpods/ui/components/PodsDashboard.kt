package moe.chenxy.hyperpods.ui.components

import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.NavigationSurface
import moe.chenxy.hyperpods.ui.GlassBackdrop
import moe.chenxy.hyperpods.ui.LocalNavigationEffects
import moe.chenxy.hyperpods.utils.FloatingDockLayout
import moe.chenxy.hyperpods.pods.EarDetectionStatus
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.utils.AirPodsBase
import moe.chenxy.hyperpods.utils.StatusFreshness
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MiuixTheme.colorScheme.dividerLine))
}

@Composable
fun DashboardGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), cornerRadius = 28.dp, content = content)
}

@Composable
fun DashboardHero(ready: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dark = MiuixTheme.colorScheme.background.luminance() < .5f
    val color = if (!ready) MiuixTheme.colorScheme.surface else
        if (dark) Color(0xFF1C3933) else Color(0xFFE3F0EC)
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(color).padding(12.dp),
        content = content)
}

@Composable
fun DashboardSection(title: String) {
    Text(title, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardBattery(params: BatteryParams, ears: EarDetectionParams, model: AirPodsBase?, diagnostics: Bundle? = null,
    showUnknownCase: Boolean = false) {
    val showCase = showUnknownCase || params.case?.let { it.isConnected && it.battery in 0..100 } == true
    Box(Modifier.fillMaxWidth().height(148.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(model?.budsRes ?: R.drawable.dashboard_headphones), null,
            Modifier.size(if (model == null) 64.dp else 140.dp),
            colorFilter = if (model == null) ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantSummary) else null)
    }
    FlowRow(Modifier.fillMaxWidth().padding(bottom = 4.dp),
        maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.3f) 2 else 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BatteryColumn(params.left, ears.left, model?.leftBudsRes ?: R.drawable.dashboard_headphones,
            stringResource(R.string.dashboard_left), Modifier.weight(1f), diagnostics, "left_battery_at", model == null)
        BatteryColumn(params.right, ears.right, model?.rightBudsRes ?: R.drawable.dashboard_headphones,
            stringResource(R.string.dashboard_right), Modifier.weight(1f), diagnostics, "right_battery_at", model == null)
        if (showCase) BatteryColumn(params.case, null, model?.caseRes ?: R.drawable.airpods_pro_case_notification,
            stringResource(R.string.dashboard_case), Modifier.weight(1f), diagnostics, "case_battery_at", model == null)
    }
}

@Composable
private fun BatteryColumn(params: PodBatteryParams?, ear: Byte?, image: Int, title: String, modifier: Modifier,
    diagnostics: Bundle?, timeKey: String, generic: Boolean) {
    val available = params?.isConnected == true && params.battery in 0..100
    Column(modifier.clip(RoundedCornerShape(20.dp))
        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = .055f)).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(image), null, modifier = Modifier.size(18.dp),
                colorFilter = if (generic) ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantSummary) else null)
            Text(title, fontSize = 12.sp, lineHeight = 16.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBattery(available, params?.isCharging == true)
            Text(if (available) "${params!!.battery}%" else "—",
                fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
        }
        val wear = StatusFreshness.wear(diagnostics?.getBoolean("wear") == true,
            diagnostics?.getLong("wear_report_at") ?: 0, diagnostics?.getString("connection"))
        val status = when {
            params?.isConnected != true -> R.string.dashboard_unavailable
            params.isCharging -> R.string.dashboard_charging
            ear == null -> R.string.dashboard_connected
            wear == StatusFreshness.State.UNKNOWN -> R.string.settings_reading
            ear == EarDetectionStatus.IN_EAR -> R.string.dashboard_wearing
            ear == EarDetectionStatus.IN_CASE || params.isInCase -> R.string.dashboard_in_case
            ear.toInt() !in 0..3 -> R.string.settings_reading
            else -> R.string.dashboard_out_of_ear
        }
        Text(if (ear != null && wear == StatusFreshness.State.RETAINED && params?.isConnected == true)
            stringResource(R.string.freshness_last_wear, stringResource(status)) else stringResource(status), fontSize = 12.sp, lineHeight = 16.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 5.dp))
        val at = diagnostics?.getLong(timeKey) ?: 0
        val freshness = StatusFreshness.battery(params, at, diagnostics?.getString("connection"))
        // Exact report times remain in diagnostics. Only stale data needs a daily-use warning.
        if (freshness == StatusFreshness.State.RETAINED) Text(stringResource(R.string.freshness_retained,
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(at))),
            fontSize = 10.sp, lineHeight = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 3.dp))
    }
}

/** A2DP can be connected while the independent control channel is still unavailable. */
fun dashboardConnectionStatus(diagnostics: Bundle?): Int = when {
    diagnostics == null -> R.string.diagnostics_audio_no_control_report
    diagnostics.getString("connection") == "failed" -> R.string.diagnostics_audio_control_failed
    diagnostics.getBoolean("ready") -> R.string.dashboard_connected
    diagnostics.getString("connection") == "disconnected" -> R.string.diagnostics_no_session
    else -> R.string.diagnostics_audio_syncing
}

@Composable
private fun IconBattery(available: Boolean, charging: Boolean) {
    top.yukonga.miuix.kmp.basic.Icon(painterResource(R.drawable.dashboard_battery),
        contentDescription = if (charging) stringResource(R.string.dashboard_charging) else null,
        tint = if (available) Color(0xFF147F66) else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.size(20.dp).rotate(90f))
}

@Composable
internal fun DashboardNavigation(pagerState: PagerState, modifier: Modifier = Modifier, backdrop: GlassBackdrop? = null) {
    val scope = rememberCoroutineScope()
    var animation by remember { mutableStateOf<Job?>(null) }
    DashboardNavigation(pagerState.targetPage, { index ->
        // Retarget from the currently rendered offset, not a delayed selected-tab
        // copy. A new tap or pager swipe can immediately take over the animation.
        animation?.cancel()
        animation = scope.launch {
            pagerState.animateScrollToPage(index, animationSpec = tween(240, easing = FastOutSlowInEasing))
        }
    }, modifier, backdrop, pagerState.currentPage + pagerState.currentPageOffsetFraction)
}

@Composable
internal fun DashboardNavigation(selected: Int, onSelect: (Int) -> Unit,
                        modifier: Modifier = Modifier, backdrop: GlassBackdrop? = null,
                        pagePosition: Float = selected.toFloat()) {
    val effects = backdrop != null && LocalNavigationEffects.current
    val firstInteraction = remember { MutableInteractionSource() }
    val secondInteraction = remember { MutableInteractionSource() }
    val firstPressed by firstInteraction.collectIsPressedAsState()
    val secondPressed by secondInteraction.collectIsPressedAsState()
    val pressure by animateFloatAsState(if (effects && (firstPressed || secondPressed)) 1f else 0f,
        spring(dampingRatio = .8f, stiffness = Spring.StiffnessMedium), label = "GlassPressure")
    val position by animateFloatAsState(pagePosition.coerceIn(0f, 1f),
        spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow), label = "GlassSelection")
    val shownPosition = if (effects) position else pagePosition.coerceIn(0f, 1f)
    val lightPosition = if (firstPressed) 0f else if (secondPressed) 1f else shownPosition
    NavigationSurface(backdrop, shownPosition, pressure.coerceIn(0f, 1f), lightPosition,
        modifier.widthIn(max = 340.dp).fillMaxWidth()
            .graphicsLayer { scaleX = 1f + pressure * .006f; scaleY = 1f - pressure * .012f }
            .shadow(12.dp, RoundedCornerShape(40.dp))) { glass ->
        BoxWithConstraints(Modifier.fillMaxWidth().height(FloatingDockLayout.HEIGHT_DP.dp).padding(6.dp)) {
        if (!glass) Box(Modifier.offset(x = (maxWidth / 2) * shownPosition)
            .width(maxWidth / 2).fillMaxHeight().clip(RoundedCornerShape(32.dp))
            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = .10f)))
        Row(Modifier.fillMaxSize().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically) {
            listOf(R.string.dashboard_headphones, R.string.settings_title).forEachIndexed { index, label ->
                val tint by animateColorAsState(
                    if (selected == index) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                    animationSpec = tween(180), label = "DashboardTabTint")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(32.dp))
                    .selectable(selected == index, role = Role.Tab, indication = null,
                    interactionSource = if (index == 0) firstInteraction else secondInteraction,
                    onClick = { onSelect(index) }).padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (index == 0) Icon(painterResource(R.drawable.dashboard_headphones), null,
                        tint = tint, modifier = Modifier.size(24.dp))
                    else Icon(painterResource(R.drawable.dashboard_settings), null, tint = tint, modifier = Modifier.size(24.dp))
                    Text(stringResource(label), color = tint, fontSize = 12.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                }
            }
        }
    }
}

@Composable
fun DashboardToggle(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
        .semantics { contentDescription = title }
        .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, lineHeight = 22.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .45f))
            Text(summary, fontSize = 12.sp, lineHeight = 16.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 4.dp))
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.switchColors(
                checkedTrackColor = MiuixTheme.colorScheme.primaryVariant,
                checkedThumbColor = Color.White))
    }
}

@Composable
fun DashboardLink(title: String, summary: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
        .heightIn(min = if (summary == null) 56.dp else 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, lineHeight = 22.sp)
            summary?.let { Text(it, fontSize = 12.sp, lineHeight = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 5.dp)) }
        }
        Icon(MiuixIcons.Basic.ArrowRight, null, modifier = Modifier.size(20.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
fun DashboardNoise(mode: NoiseControlMode?, onSelect: (NoiseControlMode) -> Unit, allowedModes: Set<Int> = setOf(1, 2, 3, 4)) {
    val modes = NoiseControlMode.entries
    val labels = listOf(R.string.off, R.string.noise_cancellation_title,
        R.string.dashboard_transparency, R.string.adaptive_title)
    val icons = listOf(R.drawable.dashboard_mode_off, R.drawable.dashboard_mode_anc,
        R.drawable.dashboard_mode_transparency, R.drawable.dashboard_mode_adaptive)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MiuixTheme.colorScheme.surface)
        .padding(6.dp).selectableGroup()) {
        modes.forEachIndexed { index, value ->
            val selected = value == mode
            val enabled = index + 1 in allowedModes
            val foreground by animateColorAsState(
                if (selected) MiuixTheme.colorScheme.onPrimaryVariant else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                animationSpec = tween(160), label = "NoiseModeForeground")
            val background by animateColorAsState(
                if (selected) MiuixTheme.colorScheme.primaryVariant else Color.Transparent,
                animationSpec = tween(160), label = "NoiseModeBackground")
            Column(Modifier.weight(1f).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(20.dp))
                .background(background)
                .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(value) })
                .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(icons[index]), null, modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(foreground))
                Text(stringResource(labels[index]), color = foreground, fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}
