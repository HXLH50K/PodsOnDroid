package moe.chenxy.hyperpods.ui.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestartSelection
import moe.chenxy.hyperpods.utils.SystemEnvironment
import moe.chenxy.hyperpods.utils.SystemProfile
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScopeRestartPanel(profile: SystemProfile = SystemEnvironment.current, onFinished: () -> Unit) {
    val context = LocalContext.current
    val state by InterfaceScopeRestart.state.collectAsState()
    val hosts = remember(profile) { ScopeRestartSelection.available(profile) }
    val initialSelection = remember(context, hosts) {
        try {
            ScopeRestartSelection.read(context, hosts)
        } catch (error: IllegalArgumentException) {
            Log.e("PODS-ScopeSelection", "Invalid saved scope choices", error)
            Toast.makeText(context, R.string.scope_selection_read_failed, Toast.LENGTH_LONG).show()
            emptySet()
        }
    }
    LaunchedEffect(state.operationId, state.result) {
        if (!state.running && state.result != null) onFinished()
    }
    ScopeRestartControls(hosts, initialSelection, state,
        saveSelection = { ScopeRestartSelection.save(context, hosts, it) },
        start = { selected ->
            val mode = if (hosts.any { it.processName in selected && it.affectsControlIpc })
                InterfaceScopeRestart.Mode.SELECTED else InterfaceScopeRestart.Mode.INTERFACE_ONLY
            InterfaceScopeRestart.start(context, mode, selected)
        })
}

@Composable
internal fun ScopeRestartControls(
    hosts: List<ScopeRestart.Host>,
    initialSelection: Set<String>,
    state: InterfaceScopeRestart.State,
    saveSelection: (Set<String>) -> Boolean,
    start: (Set<String>) -> Boolean,
) {
    val context = LocalContext.current
    var showSelection by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(initialSelection.toList()) }
    var draft by rememberSaveable { mutableStateOf(initialSelection.toList()) }
    val maxScrollHeight = LocalConfiguration.current.screenHeightDp.dp * .55f
    LaunchedEffect(state.running) {
        if (state.running) showSelection = false
    }
    val header = state.result?.let { resultText(context, it) }
        ?: context.getString(R.string.scope_restart_progress,
            state.processName ?: context.getString(R.string.scope_reload_preflight))
    val details = (listOf(header) + state.hosts.map { hostText(context, it) }).joinToString("\n")
    BasicComponent(
        title = stringResource(R.string.scope_restart_title),
        summary = stringResource(R.string.scope_restart_summary),
        enabled = !state.running,
        onClick = {
            draft = selected
            showSelection = true
        }
    )
    if (showSelection && !state.running) ScopeRestartDialog(
        title = stringResource(R.string.scope_selection_title),
        tag = "scope_selection_dialog",
        onDismiss = { showSelection = false },
    ) {
        Column(Modifier.heightIn(max = maxScrollHeight).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.scope_selection_warning), fontSize = 14.sp, lineHeight = 21.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            hosts.forEach { host ->
                val checked = host.processName in draft
                fun toggle() {
                    draft = if (checked) draft - host.processName else draft + host.processName
                }
                BasicComponent(title = stringResource(scopeHostTitle(host.processName)),
                    summary = host.processName, onClick = ::toggle,
                    rightActions = { Checkbox(checked = checked, onCheckedChange = { toggle() },
                        modifier = Modifier.testTag("scope_choice_${host.name}")) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(text = stringResource(android.R.string.cancel), modifier = Modifier.weight(1f),
                onClick = { showSelection = false })
            TextButton(text = stringResource(R.string.scope_restart_confirm), modifier = Modifier.weight(1f),
                enabled = draft.isNotEmpty(), onClick = {
                    val confirmed = draft.toSet()
                    if (!saveSelection(confirmed)) {
                        Log.e("PODS-ScopeSelection", "Could not persist scope choices")
                        Toast.makeText(context, R.string.scope_selection_save_failed, Toast.LENGTH_LONG).show()
                    } else {
                        selected = draft
                        if (start(confirmed)) showSelection = false
                        else Toast.makeText(context, R.string.scope_restart_start_failed, Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
    if (state.running) ScopeRestartDialog(
        title = stringResource(R.string.scope_restart_running),
        tag = "scope_progress_dialog",
        onDismiss = null,
    ) {
        CircularProgressIndicator(progress = null, modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp))
        Text(stringResource(R.string.scope_restart_do_not_operate), fontSize = 14.sp, lineHeight = 21.sp)
        Text(details, fontSize = 13.sp, lineHeight = 19.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.heightIn(max = maxScrollHeight).verticalScroll(rememberScrollState()))
    }
    if (state.result != null) {
        BasicComponent(
            title = stringResource(R.string.scope_reload_status_title),
            summary = details,
        )
    }
}

@Composable
private fun ScopeRestartDialog(
    title: String,
    tag: String,
    onDismiss: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null)) {
        DashboardGroup(Modifier.widthIn(max = 420.dp).testTag(tag)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, fontSize = MiuixTheme.textStyles.title4.fontSize, fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics { heading() })
                content()
            }
        }
    }
}
internal fun scopeHostTitle(processName: String): Int = when (processName) {
    ScopeRestart.Host.BLUETOOTH.processName -> R.string.module_bluetooth
    ScopeRestart.Host.SYSTEM_UI.processName -> R.string.module_system_ui
    ScopeRestart.Host.XIAOMI_UI.processName -> R.string.module_xiaomi_bluetooth_ui
    else -> R.string.module_xiaomi_bluetooth
}

private fun resultText(context: Context, result: InterfaceScopeRestart.Result): String {
    val reason = when (result.failure) {
        InterfaceScopeRestart.Failure.ACTIVE_CALL, InterfaceScopeRestart.Failure.CALL_STATE_UNKNOWN,
        InterfaceScopeRestart.Failure.AUDIO_STATE_UNAVAILABLE -> R.string.scope_failure_call
        InterfaceScopeRestart.Failure.MODULE_OUTDATED -> R.string.scope_failure_old_module
        InterfaceScopeRestart.Failure.OPERATION_CANCELLED -> R.string.scope_failure_cancelled
        InterfaceScopeRestart.Failure.ROOT_COMMAND_FAILED, InterfaceScopeRestart.Failure.PERMISSION_DENIED -> R.string.scope_failure_root
        InterfaceScopeRestart.Failure.RELOAD_TIMEOUT, InterfaceScopeRestart.Failure.PROCESS_DID_NOT_EXIT,
        InterfaceScopeRestart.Failure.OPERATION_TIMED_OUT -> R.string.scope_failure_timeout
        else -> R.string.scope_failure_report
    }
    val summary = when {
        result.failure == null -> context.getString(R.string.scope_restart_verified, result.completed.size)
        result.signalOutcomeUnknown -> context.getString(R.string.scope_restart_unknown_outcome, result.completed.size)
        else -> context.getString(R.string.scope_restart_failed, result.completed.size,
            context.getString(reason))
    }
    return if (result.pending.isEmpty()) summary else summary + "\n" +
        context.getString(R.string.scope_reload_pending, result.pending.joinToString(", "))
}

private fun hostText(context: Context, host: InterfaceScopeRestart.HostResult): String {
    val state = when (host.state) {
        InterfaceScopeRestart.HostState.PENDING -> R.string.scope_host_pending
        InterfaceScopeRestart.HostState.PREFLIGHTED -> R.string.scope_host_preflighted
        InterfaceScopeRestart.HostState.SIGNAL_PENDING -> R.string.scope_host_signalling
        InterfaceScopeRestart.HostState.SIGNAL_ACKNOWLEDGED -> R.string.scope_host_acknowledged
        InterfaceScopeRestart.HostState.VERIFIED -> R.string.scope_host_verified
        InterfaceScopeRestart.HostState.INDIRECTLY_VERIFIED -> R.string.scope_host_indirect
        InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH -> R.string.scope_host_lazy
        InterfaceScopeRestart.HostState.OLD_PROCESS_PRESENT -> R.string.scope_host_old_process
        InterfaceScopeRestart.HostState.OLD_MODULE -> R.string.scope_host_old_module
        InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT -> R.string.scope_host_no_report
        InterfaceScopeRestart.HostState.ORIGINAL_PROCESS_ABSENT -> R.string.scope_host_absent
        InterfaceScopeRestart.HostState.FAILED -> R.string.scope_host_failed
        InterfaceScopeRestart.HostState.OUTCOME_UNKNOWN -> R.string.scope_host_unknown
    }
    val pids = host.originalPid?.let { old -> " ($old → ${host.replacementPid ?: "?"})" }.orEmpty()
    return "${host.processName}$pids: ${context.getString(state)}"
}
