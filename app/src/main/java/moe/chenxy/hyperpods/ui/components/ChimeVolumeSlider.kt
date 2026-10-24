package moe.chenxy.hyperpods.ui.components

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.ChimeVolumeState
import moe.chenxy.hyperpods.utils.PodsSettings
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun ChimeVolumeSlider(
    value: Int?,
    feedback: String?,
    enabled: Boolean,
    status: String?,
    onChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(ChimeVolumeState((value ?: PodsSettings.CHIME_VOLUME_THRESHOLD).toFloat(),
        value.takeUnless { feedback == "sent" })) }
    var submission by remember { mutableIntStateOf(0) }
    val failed = feedback in setOf("write_failed", "timeout", "invalid")
    LaunchedEffect(value, feedback) {
        if (value != null && !failed && feedback != "sent") {
            val update = state.report(value)
            state = update.state
            update.adjustedTo?.let {
                Toast.makeText(context, context.getString(R.string.chime_volume_reported, it), Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(failed, submission) {
        if (failed) state = state.failed()
    }
    val animated by animateFloatAsState(state.target,
        animationSpec = if (state.dragging) snap() else tween(320), label = "ChimeVolume")
    val displayed = if (state.dragging) state.target else animated
    val visibleStart by rememberUpdatedState(displayed)
    val range = PodsSettings.MIN_CHIME_VOLUME.toFloat()..PodsSettings.MAX_CHIME_VOLUME.toFloat()
    val sliderValue = displayed.coerceIn(range)
    val belowThreshold = displayed.roundToInt() < PodsSettings.CHIME_VOLUME_THRESHOLD
    val warning = stringResource(R.string.chime_volume_below_threshold, PodsSettings.CHIME_VOLUME_THRESHOLD)
    val warningColor = if (MiuixTheme.colorScheme.background.luminance() < .5f) Color(0xFFFF825C) else Color(0xFFD84A28)
    val colors = if (belowThreshold) SliderDefaults.sliderColors(
        foregroundColor = warningColor, disabledForegroundColor = warningColor.copy(alpha = .5f),
        backgroundColor = warningColor.copy(alpha = .16f),
    ) else SliderDefaults.sliderColors()
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.control_chime_volume), fontWeight = FontWeight.Medium)
        Text(if (value != null) "${displayed.roundToInt()}%" else stringResource(R.string.settings_reading), fontSize = 13.sp)
        Text(stringResource(R.string.chime_volume_range, PodsSettings.CHIME_VOLUME_THRESHOLD),
            fontSize = 12.sp)
        status?.let { Text(it, fontSize = 12.sp) }
        Slider(value = sliderValue, valueRange = range,
            colors = colors,
            onValueChange = { state = state.drag(it, visibleStart) },
            onValueChangeFinished = {
                state = state.finish()
                submission++
                if (state.belowThreshold) Toast.makeText(context, warning, Toast.LENGTH_LONG).show()
                onChange(requireNotNull(state.requested))
            },
            enabled = enabled || state.dragging,
            modifier = Modifier.fillMaxWidth().testTag("chime_volume_slider")
                .progressSemantics(sliderValue, range)
                .semantics { if (belowThreshold) stateDescription = warning }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        try {
                            while (awaitPointerEvent(PointerEventPass.Final).changes.any { it.pressed }) { }
                        } finally {
                            // Miuix 0.6.1 exposes drag end but not drag cancellation.
                            if (state.dragging) state = state.cancel()
                        }
                    }
                })
    }
}
