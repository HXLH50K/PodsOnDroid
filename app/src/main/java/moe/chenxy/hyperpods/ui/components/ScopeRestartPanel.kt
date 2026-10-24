package moe.chenxy.hyperpods.ui.components

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestartSelection
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
fun ScopeRestartPanel(onFinished: () -> Unit) {
    val context = LocalContext.current
    val state by InterfaceScopeRestart.state.collectAsState()
    val hosts = remember { ScopeRestartSelection.available(BuildConfig.BUILD_TYPE == "debug") }
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
                InterfaceScopeRestart.Mode.DEBUG_SELECTED else InterfaceScopeRestart.Mode.INTERFACE_ONLY
            InterfaceScopeRestart.start(context, mode, selected)
        },
        cancel = InterfaceScopeRestart::cancel)
}

@Composable
internal fun ScopeRestartControls(
    hosts: List<ScopeRestart.Host>,
    initialSelection: Set<String>,
    state: InterfaceScopeRestart.State,
    saveSelection: (Set<String>) -> Boolean,
    start: (Set<String>) -> Boolean,
    cancel: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var selectionDialog by remember { mutableStateOf<AlertDialog?>(null) }
    var progressDialog by remember { mutableStateOf<AlertDialog?>(null) }
    var selected by remember { mutableStateOf(initialSelection) }
    DisposableEffect(Unit) { onDispose { selectionDialog?.dismiss() } }
    DisposableEffect(state.running) {
        if (state.running) {
            selectionDialog?.dismiss()
            progressDialog = AlertDialog.Builder(context)
                .setTitle(R.string.scope_restart_running)
                .setMessage(R.string.scope_restart_do_not_operate)
                .setCancelable(false)
                .setNegativeButton(R.string.scope_reload_cancel, null)
                .show().also { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { cancel() }
                }
        }
        onDispose { progressDialog?.dismiss(); progressDialog = null }
    }
    val header = state.result?.let { resultText(context, it) }
        ?: context.getString(if (state.cancelling) R.string.scope_reload_cancelling
            else R.string.scope_restart_progress, state.processName ?: context.getString(R.string.scope_reload_preflight))
    val details = (listOf(header) + state.hosts.map { hostText(context, it) }).joinToString("\n")
    SideEffect {
        progressDialog?.setMessage(context.getString(R.string.scope_restart_do_not_operate) + "\n\n" + details)
        progressDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.cancelling
    }
    BasicComponent(
        title = stringResource(R.string.scope_restart_title),
        summary = stringResource(R.string.scope_restart_summary),
        enabled = !state.running,
        onClick = {
            val draft = selected.toMutableSet()
            val labels = hosts.map { context.getString(scopeHostTitle(it.processName)) + "\n" + it.processName }
            val warning = TextView(context).apply {
                text = context.getString(R.string.scope_selection_warning)
                val inset = (20 * density).toInt()
                setPadding(inset, inset / 2, inset, 0)
            }
            val warningPanel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(ScrollView(context).apply { addView(warning) },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        (160 * density).toInt()))
            }
            selectionDialog = AlertDialog.Builder(context)
                .setTitle(R.string.scope_selection_title)
                .setMultiChoiceItems(labels.toTypedArray(), hosts.map { it.processName in draft }.toBooleanArray()) { dialog, index, checked ->
                    val name = hosts[index].processName
                    if (checked) draft.add(name) else draft.remove(name)
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = draft.isNotEmpty()
                }
                .setView(warningPanel)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.scope_restart_confirm, null)
                .show().also { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        isEnabled = draft.isNotEmpty()
                        setOnClickListener {
                            val confirmed = draft.toSet()
                            if (!saveSelection(confirmed)) {
                                Log.e("PODS-ScopeSelection", "Could not persist scope choices")
                                Toast.makeText(context, R.string.scope_selection_save_failed, Toast.LENGTH_LONG).show()
                            } else {
                                selected = confirmed
                                if (start(confirmed)) dialog.dismiss()
                                else Toast.makeText(context, R.string.scope_restart_start_failed, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        }
    )
    if (state.result != null) {
        BasicComponent(
            title = stringResource(R.string.scope_reload_status_title),
            summary = details,
        )
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
