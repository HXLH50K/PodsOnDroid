package moe.chenxy.hyperpods.ui.components

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.ChimeVolumeState
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
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
    var state by remember { mutableStateOf(ChimeVolumeState((value ?: 0).toFloat(),
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
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.control_chime_volume), fontWeight = FontWeight.Medium)
        Text(if (value != null) "${displayed.roundToInt()}%" else stringResource(R.string.settings_reading), fontSize = 13.sp)
        status?.let { Text(it, fontSize = 12.sp) }
        Slider(value = displayed, valueRange = 0f..100f,
            onValueChange = { state = state.drag(it) },
            onValueChangeFinished = {
                state = state.finish()
                submission++
                onChange(requireNotNull(state.requested))
            },
            enabled = enabled || state.dragging,
            modifier = Modifier.fillMaxWidth().testTag("chime_volume_slider").progressSemantics(displayed, 0f..100f))
    }
}
