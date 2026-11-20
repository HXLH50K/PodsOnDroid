package moe.chenxy.hyperpods.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AirPodsBase
import moe.chenxy.hyperpods.utils.CustomEq
import moe.chenxy.hyperpods.utils.CustomEqState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private data class EqBandEdits(val low: Int? = null, val mid: Int? = null, val high: Int? = null) {
    fun applyTo(value: CustomEq) = value.copy(
        low = low ?: value.low, mid = mid ?: value.mid, high = high ?: value.high,
    )

    fun withoutMatching(value: CustomEq) = EqBandEdits(
        low.takeUnless { it == value.low }, mid.takeUnless { it == value.mid }, high.takeUnless { it == value.high },
    )
}

@Composable
internal fun EqualizerSettings(
    state: CustomEqState,
    model: AirPodsBase?,
    firmware: String?,
    connected: Boolean,
    active: Boolean,
    onChange: (CustomEq, CustomEqState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = when {
        !connected || state.connectionId.isEmpty() -> R.string.eq_disconnected
        CustomEq.available(model, firmware, state.value) -> null
        model == null -> R.string.eq_firmware_unknown
        !CustomEq.supportsModel(model) -> R.string.eq_unsupported
        CustomEq.firmwareGeneration(firmware) == null -> R.string.eq_firmware_unknown
        else -> R.string.eq_firmware_required
    }
    val value = state.value
    // Keep an active drag on the same state holder across reports and write feedback.
    var edits by remember(state.connectionId, value?.mode) { mutableStateOf(EqBandEdits()) }
    LaunchedEffect(state.connectionId, value) {
        if (value != null) edits = edits.withoutMatching(value)
    }
    val modes = listOf(stringResource(R.string.eq_recommended), stringResource(R.string.eq_custom))
    DashboardGroup(modifier) {
        if (unavailable != null) {
            BasicComponent(title = stringResource(R.string.eq_mode), summary = stringResource(unavailable))
            return@DashboardGroup
        }
        SuperDropdown(
            title = stringResource(R.string.eq_mode),
            items = modes,
            selectedIndex = value?.mode?.minus(1) ?: -1,
            showValue = value != null,
            summary = when {
                state.unconfirmed -> stringResource(R.string.setting_sent)
                value == null -> stringResource(R.string.eq_unknown)
                else -> null
            },
            enabled = active,
            onSelectedIndexChange = { index ->
                val mode = index + 1
                if (mode != value?.mode) onChange(value?.copy(mode = mode) ?: CustomEq(mode), state)
            },
        )
        if (value?.mode == CustomEq.CUSTOM) {
            val bands = edits.applyTo(value)
            val neutral = CustomEq(CustomEq.CUSTOM)
            val palette = MiuixTheme.colorScheme
            DashboardDivider(Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(16.dp)) {
                EqBand(R.string.eq_low, bands.low, active) { edits = edits.copy(low = it) }
                EqBand(R.string.eq_mid, bands.mid, active) { edits = edits.copy(mid = it) }
                EqBand(R.string.eq_high, bands.high, active) { edits = edits.copy(high = it) }
                Text(stringResource(R.string.eq_neutral, CustomEq.NEUTRAL), fontSize = 12.sp, lineHeight = 17.sp,
                    color = palette.onSurfaceVariantSummary, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { edits = EqBandEdits(neutral.low, neutral.mid, neutral.high).withoutMatching(value) },
                        enabled = active && bands != neutral, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.eq_reset), fontSize = 14.sp, lineHeight = 20.sp,
                            color = if (active && bands != neutral) palette.onSurface else palette.onSurfaceVariantSummary)
                    }
                    Button(onClick = { onChange(bands, state) }, enabled = active && bands != value,
                        colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.eq_apply), fontSize = 14.sp, lineHeight = 20.sp,
                            color = if (active && bands != value) palette.onPrimary else palette.onSurfaceVariantSummary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EqBand(label: Int, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val title = stringResource(label)
    Text(stringResource(R.string.eq_band_value, title, value), fontSize = 14.sp, lineHeight = 20.sp,
        modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = value.toFloat(),
        valueRange = 0f..100f,
        enabled = enabled,
        onValueChange = { onChange(it.roundToInt().coerceIn(0, 100)) },
        modifier = Modifier.fillMaxWidth().progressSemantics(value.toFloat(), 0f..100f)
            .semantics { contentDescription = title },
    )
}
