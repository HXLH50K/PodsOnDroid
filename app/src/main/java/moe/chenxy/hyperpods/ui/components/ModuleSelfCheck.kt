package moe.chenxy.hyperpods.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.ModuleStatus
import moe.chenxy.hyperpods.utils.SystemEnvironment
import moe.chenxy.hyperpods.utils.SystemProfile
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import top.yukonga.miuix.kmp.basic.BasicComponent
import java.util.UUID

@Composable
internal fun ModuleSettings(profile: SystemProfile = SystemEnvironment.current, onReport: (String) -> Unit) {
    var refreshToken by remember { mutableIntStateOf(0) }
    Column {
        DashboardSection(stringResource(R.string.scope_restart_section))
        DashboardGroup(Modifier.testTag("scope_restart_section")) {
            ScopeRestartPanel(profile) { refreshToken++ }
        }
        DashboardSection(stringResource(R.string.scope_module_status))
        DashboardGroup(Modifier.testTag("module_status_section")) {
            ModuleSelfCheck(refreshToken, profile, onReport)
        }
    }
}

@Composable
fun ModuleSelfCheck(refreshToken: Int = 0, profile: SystemProfile = SystemEnvironment.current, onReport: (String) -> Unit) {
    val context = LocalContext.current
    var request by remember(refreshToken) { mutableStateOf(UUID.randomUUID().toString()) }
    var reports by remember { mutableStateOf(emptyMap<String, ModuleStatus.Report>()) }
    var waiting by remember { mutableStateOf(true) }
    val hosts = remember(profile) { profile.modulePackages }
    DisposableEffect(context, request, hosts) {
        reports = emptyMap()
        waiting = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ModuleStatus.read(context, this, intent, request)?.takeIf { it.packageName in hosts }
                    ?.let { reports = reports + (it.processName to it) }
            }
        }
        context.registerReceiver(receiver, IntentFilter(HyperPodsAction.ACTION_MODULE_STATUS), Context.RECEIVER_EXPORTED)
        hosts.forEach { host ->
            HyperPodsBroadcasts.send(context, Intent(HyperPodsAction.ACTION_MODULE_STATUS_REQUEST).putExtra("request", request), host)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(request) { delay(2500); waiting = false }
    BasicComponent(title = stringResource(R.string.module_self_check),
        summary = stringResource(R.string.module_installed, BuildConfig.VERSION_CODE, BuildConfig.BUILD_REVISION))
    val processNames = hosts + reports.keys.filter { it !in hosts }.sorted()
    val fields = processNames.map { process ->
        val report = reports[process]
        val title = stringResource(scopeHostTitle(process))
        val summary = if (report == null) stringResource(if (waiting) R.string.module_checking else R.string.module_no_report)
        else stringResource(R.string.module_loaded, report.version, report.revision) + "\n" +
            stringResource(if (report.matchesInstalled) R.string.module_matches else R.string.module_restart) + "\n" +
            stringResource(R.string.module_runtime, report.frameworkName, report.frameworkApi, report.pid) + "\n" +
            stringResource(R.string.module_build_instance, report.buildInstance) + "\n" +
            stringResource(when (report.detail) {
                "native_ok" -> R.string.module_native_ok
                "native_failed" -> R.string.module_native_failed
                "native_unknown" -> R.string.module_native_unknown
                "plugin_observed" -> R.string.module_plugin_observed
                "plugin_waiting" -> R.string.module_plugin_waiting
                "receiver_ready" -> R.string.module_receiver_ready
                "ui_host_ready" -> R.string.module_ui_host_ready
                else -> R.string.module_receiver_waiting
            })
        title to summary
    }
    fields.forEach { (title, summary) -> BasicComponent(title = title, summary = summary) }
    val reportText = fields.joinToString("\n") { (title, summary) -> "$title: $summary" }
    LaunchedEffect(reportText) { onReport(reportText) }
    BasicComponent(title = stringResource(R.string.module_check_again), onClick = { request = UUID.randomUUID().toString() })
}
