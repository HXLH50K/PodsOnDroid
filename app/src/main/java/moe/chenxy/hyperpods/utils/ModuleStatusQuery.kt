package moe.chenxy.hyperpods.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import java.util.UUID

object ModuleStatusQuery {
    suspend fun collect(context: Context, timeoutMs: Long = 2500,
        profile: SystemProfile = SystemEnvironment.current): Map<String, ModuleStatus.Report> =
        withContext(Dispatchers.Main.immediate) {
            require(timeoutMs in 100..10_000) { "Module report timeout must be bounded" }
            val application = context.applicationContext
            val request = UUID.randomUUID().toString()
            val reports = mutableMapOf<String, ModuleStatus.Report>()
            val hosts = profile.modulePackages
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    ModuleStatus.read(context, this, intent, request)?.takeIf { it.packageName in hosts }
                        ?.let { reports[it.processName] = it }
                }
            }
            application.registerReceiver(receiver, IntentFilter(HyperPodsAction.ACTION_MODULE_STATUS),
                Context.RECEIVER_EXPORTED)
            try {
                hosts.forEach { host ->
                    HyperPodsBroadcasts.send(application,
                        Intent(HyperPodsAction.ACTION_MODULE_STATUS_REQUEST).putExtra("request", request), host)
                }
                delay(timeoutMs)
                reports.toMap()
            } finally {
                application.unregisterReceiver(receiver)
            }
        }
}
