package moe.chenxy.hyperpods.hook

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.util.Log
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.ModuleStatus
import moe.chenxy.hyperpods.utils.data.HyperPodsAction

/** Independent of a headset session: About can check even when no headset is connected. */
class ModuleStatusHook(
    private val framework: XposedInterface,
    private val host: String,
    private val processName: String
) {
    private var registered = false

    fun install() {
        framework.intercept(HostReflection.method(Application::class.java, "attach", Context::class.java)) { chain ->
            val result = chain.proceed()
            register(chain.thisObject as Application)
            result
        }
    }

    @Synchronized
    private fun register(context: Context) {
        if (registered || context.packageName != host) return
        runCatching {
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != HyperPodsAction.ACTION_MODULE_STATUS_REQUEST ||
                        !HyperPodsBroadcasts.isTrusted(context, this, BuildConfig.APPLICATION_ID)) return
                    val request = intent.getStringExtra("request")
                    if (!ModuleStatus.validRequest(request)) return
                    val detail = when (host) {
                        HyperPodsBroadcasts.BLUETOOTH -> runCatching {
                            if (HeadsetStateDispatcher.nativeGetHookResult()) "native_ok" else "native_failed"
                        }.getOrDefault("native_unknown")
                        HyperPodsBroadcasts.SYSTEM_UI -> if (SystemUIPluginHook.pluginObserved) "plugin_observed" else "plugin_waiting"
                        else -> if (MiBluetoothToastHook.notificationReceiverReady) "receiver_ready" else "receiver_waiting"
                    }
                    HyperPodsBroadcasts.send(context, Intent(HyperPodsAction.ACTION_MODULE_STATUS).apply {
                        putExtra("request", request)
                        putExtra("host", host)
                        putExtra("version", BuildConfig.VERSION_CODE)
                        putExtra("revision", BuildConfig.BUILD_REVISION)
                        putExtra("detail", detail)
                        putExtra("call_state", HostCallState.read(context, host))
                        putExtra("framework_api", framework.apiVersion)
                        putExtra("framework_name", framework.frameworkName)
                        putExtra("process_name", processName)
                        putExtra("process_id", Process.myPid())
                        putExtra("build_instance", BuildConfig.BUILD_INSTANCE)
                    })
                }
            }, IntentFilter(HyperPodsAction.ACTION_MODULE_STATUS_REQUEST), Context.RECEIVER_EXPORTED)
            registered = true
        }.onFailure { Log.w("Art_Chen", "Unable to register module self-check receiver", it) }
    }
}
