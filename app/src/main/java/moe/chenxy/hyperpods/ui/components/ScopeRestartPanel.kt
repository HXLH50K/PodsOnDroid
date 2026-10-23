package moe.chenxy.hyperpods.ui.components

import android.app.AlertDialog
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ModuleStatus
import moe.chenxy.hyperpods.utils.ScopeRestart
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
fun ScopeRestartPanel(reports: Collection<ModuleStatus.Report>, onFinished: () -> Unit) {
    val context = LocalContext.current
    val state by InterfaceScopeRestart.state.collectAsState()
    var dialog by remember { mutableStateOf<AlertDialog?>(null) }
    val interfaceTargets = InterfaceScopeRestart.eligible(reports).map { it.processName }.toSet()
    val allTargets = InterfaceScopeRestart.eligible(reports, InterfaceScopeRestart.Mode.DEBUG_ALL)
        .map { it.processName }.toSet()
    val allRequiredPresent = allTargets.containsAll(ScopeRestart.requiredReloadHosts.map { it.processName })
    DisposableEffect(Unit) {
        onDispose { dialog?.dismiss() }
    }
    LaunchedEffect(state.operationId, state.result) {
        if (!state.running && state.result != null) onFinished()
    }
    fun confirm(mode: InterfaceScopeRestart.Mode, targets: Set<String>) {
        val full = mode == InterfaceScopeRestart.Mode.DEBUG_ALL
        dialog?.dismiss()
        dialog = AlertDialog.Builder(context)
            .setTitle(if (full) R.string.scope_restart_bluetooth_title else R.string.scope_restart_title)
            .setMessage(context.getString(if (full) R.string.scope_reload_warning else R.string.scope_restart_warning,
                targets.joinToString("\n")))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.scope_restart_confirm) { _, _ ->
                InterfaceScopeRestart.start(context, mode, targets)
            }.show()
    }
    if (BuildConfig.BUILD_TYPE == "debug") {
        BasicComponent(
            title = stringResource(R.string.scope_restart_bluetooth_title),
            summary = stringResource(if (allRequiredPresent) R.string.scope_reload_summary else R.string.scope_reload_no_hosts),
            enabled = !state.running && allRequiredPresent,
            onClick = { confirm(InterfaceScopeRestart.Mode.DEBUG_ALL, allTargets) }
        )
    }
    BasicComponent(
        title = stringResource(R.string.scope_restart_title),
        summary = stringResource(if (interfaceTargets.isEmpty()) R.string.scope_restart_no_hosts
            else R.string.scope_restart_summary),
        enabled = !state.running && interfaceTargets.isNotEmpty(),
        onClick = { confirm(InterfaceScopeRestart.Mode.INTERFACE_ONLY, interfaceTargets) }
    )
    if (state.running || state.result != null) {
        val header = state.result?.let { resultText(context, it) }
            ?: context.getString(if (state.cancelling) R.string.scope_reload_cancelling
                else R.string.scope_restart_progress, state.processName ?: context.getString(R.string.scope_reload_preflight))
        BasicComponent(
            title = stringResource(R.string.scope_reload_status_title),
            summary = (listOf(header) + state.hosts.map { hostText(context, it) }).joinToString("\n")
        )
    }
    if (state.running) {
        BasicComponent(
            title = stringResource(R.string.scope_reload_cancel),
            summary = stringResource(R.string.scope_reload_cancel_summary),
            enabled = !state.cancelling,
            onClick = { InterfaceScopeRestart.cancel() }
        )
    }
}

private fun resultText(context: Context, result: InterfaceScopeRestart.Result): String {
    val summary = when {
        result.failure == null -> context.getString(R.string.scope_restart_verified, result.completed.size)
        result.signalOutcomeUnknown -> context.getString(R.string.scope_restart_unknown_outcome, result.completed.size)
        else -> context.getString(R.string.scope_restart_failed, result.completed.size,
            result.rootFailure?.name ?: requireNotNull(result.failure).name)
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
