package moe.chenxy.hyperpods.ui.components

import android.app.AlertDialog
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ModuleStatus
import moe.chenxy.hyperpods.utils.ScopeRestart
import top.yukonga.miuix.kmp.basic.BasicComponent

@Composable
fun ScopeRestartPanel(reports: Collection<ModuleStatus.Report>, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(InterfaceScopeRestart.lastResult?.let { resultText(context, it) }) }
    var dialog by remember { mutableStateOf<AlertDialog?>(null) }
    val targets = InterfaceScopeRestart.eligible(reports).map { it.processName }.toSet()
    DisposableEffect(Unit) {
        onDispose { dialog?.dismiss() }
    }
    BasicComponent(
        title = stringResource(R.string.scope_restart_title),
        summary = status ?: stringResource(if (targets.isEmpty()) R.string.scope_restart_no_hosts
            else R.string.scope_restart_summary),
        enabled = !busy && targets.isNotEmpty(),
        onClick = {
            dialog?.dismiss()
            dialog = AlertDialog.Builder(context)
                .setTitle(R.string.scope_restart_title)
                .setMessage(context.getString(R.string.scope_restart_warning, targets.joinToString("\n")))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.scope_restart_confirm) { _, _ ->
                    busy = true
                    scope.launch {
                        try {
                            val result = InterfaceScopeRestart.run(context, targets) { process ->
                                status = context.getString(R.string.scope_restart_progress, process)
                            }
                            status = resultText(context, result)
                            onFinished()
                        } finally {
                            busy = false
                        }
                    }
                }.show()
        }
    )
    BasicComponent(title = stringResource(R.string.scope_restart_bluetooth_title),
        summary = stringResource(R.string.scope_restart_bluetooth_pending))
}

private fun resultText(context: Context, result: InterfaceScopeRestart.Result): String =
    if (result.failure == null) {
        context.getString(R.string.scope_restart_verified, result.completed.size)
    } else if (result.failure == InterfaceScopeRestart.Failure.VERIFICATION_INTERRUPTED ||
        result.rootFailure in setOf(ScopeRestart.FailureCode.TIMEOUT, ScopeRestart.FailureCode.IO_ERROR,
            ScopeRestart.FailureCode.INTERRUPTED, ScopeRestart.FailureCode.OUTPUT_LIMIT)) {
        context.getString(R.string.scope_restart_unknown_outcome, result.completed.size)
    } else {
        val code = result.rootFailure?.name ?: requireNotNull(result.failure).name
        context.getString(R.string.scope_restart_failed, result.completed.size, code)
    }
