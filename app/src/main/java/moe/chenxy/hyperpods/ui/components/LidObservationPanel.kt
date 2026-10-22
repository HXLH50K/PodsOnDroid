package moe.chenxy.hyperpods.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.LidObservationCapture
import moe.chenxy.hyperpods.utils.LidObservationSession
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
fun LidObservationPanel() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val show = remember { mutableStateOf(false) }
    val visible by rememberUpdatedState(show.value)
    val capture = remember(context) { LidObservationCapture(context.applicationContext) }
    val state by capture.state.collectAsState()
    val running = state.status == LidObservationCapture.Status.RUNNING
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (visible && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) capture.start()
    }
    LaunchedEffect(show.value) {
        if (!show.value) capture.stop()
    }
    DisposableEffect(capture, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) capture.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            capture.stop()
            lifecycle.removeObserver(observer)
        }
    }
    BasicComponent(title = stringResource(R.string.lid_observation_title),
        summary = stringResource(R.string.lid_observation_summary), onClick = { show.value = true })
    CustomSuperBottomSheet(show = show, title = stringResource(R.string.lid_observation_title),
        onDismissRequest = { capture.stop(); show.value = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            BasicComponent(title = stringResource(R.string.lid_observation_instructions),
                summary = stringResource(R.string.lid_observation_privacy))
            BasicComponent(title = stringResource(statusTitle(state.status)),
                summary = stringResource(R.string.lid_observation_counts, state.elapsedMs / 1000,
                    state.observations.accepted, state.observations.candidateCount,
                    state.observations.malformed, state.observations.dropped))
            if (state.scanError != null) BasicComponent(
                title = stringResource(R.string.lid_observation_scan_error, requireNotNull(state.scanError)))
            BasicComponent(title = stringResource(R.string.lid_observation_phase),
                summary = stringResource(when (state.observations.phase) {
                    LidObservationSession.Phase.CASE_CLOSED -> R.string.lid_observation_closed
                    LidObservationSession.Phase.CASE_OPEN -> R.string.lid_observation_open
                    LidObservationSession.Phase.UNSPECIFIED -> R.string.lid_observation_unmarked
                }))
            BasicComponent(title = stringResource(R.string.lid_observation_start), enabled = !running,
                onClick = {
                    if (capture.hasPermissions()) capture.start()
                    else permissions.launch(LidObservationCapture.PERMISSIONS)
                })
            BasicComponent(title = stringResource(R.string.lid_observation_mark_closed), enabled = running,
                onClick = { capture.mark(LidObservationSession.Phase.CASE_CLOSED) })
            BasicComponent(title = stringResource(R.string.lid_observation_mark_open), enabled = running,
                onClick = { capture.mark(LidObservationSession.Phase.CASE_OPEN) })
            BasicComponent(title = stringResource(R.string.lid_observation_stop), enabled = running,
                onClick = { capture.stop() })
            BasicComponent(title = stringResource(R.string.lid_observation_copy),
                enabled = !running && state.status != LidObservationCapture.Status.IDLE,
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    if (clipboard == null) {
                        Log.w("PODS-LidObservation", "Clipboard service is unavailable")
                        Toast.makeText(context, R.string.lid_observation_clipboard_failed, Toast.LENGTH_SHORT).show()
                    } else {
                        val report = "P.O.D.S. ${BuildConfig.VERSION_NAME} / ${BuildConfig.BUILD_REVISION}\n" +
                            state.toJson()
                        clipboard.setPrimaryClip(ClipData.newPlainText("P.O.D.S. lid observations", report))
                        Toast.makeText(context, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}

private fun statusTitle(status: LidObservationCapture.Status): Int = when (status) {
    LidObservationCapture.Status.IDLE -> R.string.lid_observation_idle
    LidObservationCapture.Status.RUNNING -> R.string.lid_observation_running
    LidObservationCapture.Status.COMPLETE -> R.string.lid_observation_complete
    LidObservationCapture.Status.STOPPED -> R.string.lid_observation_stopped
    LidObservationCapture.Status.PERMISSION_REQUIRED -> R.string.lid_observation_permission
    LidObservationCapture.Status.BLUETOOTH_OFF -> R.string.lid_observation_bluetooth_off
    LidObservationCapture.Status.UNAVAILABLE -> R.string.lid_observation_unavailable
    LidObservationCapture.Status.FAILED -> R.string.lid_observation_failed
}
