package moe.chenxy.hyperpods.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.data.HyperPodsAction

/** Version comes from the code running in the host, not the newly installed APK. */
object ModuleStatus {
    val packages = listOf(HyperPodsBroadcasts.BLUETOOTH, HyperPodsBroadcasts.XIAOMI_BLUETOOTH,
        HyperPodsBroadcasts.SYSTEM_UI)
    data class Report(
        val packageName: String,
        val version: Int,
        val revision: String,
        val detail: String,
        val processName: String,
        val pid: Int,
        val uid: Int,
        val frameworkApi: Int,
        val frameworkName: String,
        val buildInstance: String,
        val callState: String,
    ) {
        val matchesInstalled: Boolean
            get() = frameworkApi >= 102 && version == BuildConfig.VERSION_CODE &&
                revision == BuildConfig.BUILD_REVISION && buildInstance == BuildConfig.BUILD_INSTANCE
    }

    fun validProcess(packageName: String, processName: String): Boolean =
        packageName in packages && (processName == packageName ||
            packageName == HyperPodsBroadcasts.XIAOMI_BLUETOOTH && processName == "$packageName:ui")

    fun validRequest(request: String?): Boolean = request != null &&
        request.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))

    fun read(context: Context, receiver: BroadcastReceiver, intent: Intent, request: String): Report? {
        if (intent.action != HyperPodsAction.ACTION_MODULE_STATUS || !validRequest(request) ||
            intent.getStringExtra("request") != request) return null
        val pkg = intent.getStringExtra("host")?.takeIf { it in packages } ?: return null
        if (receiver.sentFromPackage != pkg || !HyperPodsBroadcasts.isTrusted(context, receiver, pkg)) return null
        val revision = intent.getStringExtra("revision")?.takeIf {
            it == "local" || it.matches(Regex("[0-9a-f]{8}"))
        } ?: return null
        val version = intent.getIntExtra("version", 0).takeIf { it in 1..1_000_000 } ?: return null
        val processName = intent.getStringExtra("process_name")?.takeIf { validProcess(pkg, it) } ?: return null
        val pid = intent.getIntExtra("process_id", 0).takeIf { it > 1 } ?: return null
        val frameworkApi = intent.getIntExtra("framework_api", 0).takeIf { it in 102..1000 } ?: return null
        val frameworkName = intent.getStringExtra("framework_name")?.takeIf {
            it.matches(Regex("[A-Za-z0-9][A-Za-z0-9 ._()+-]{0,79}"))
        } ?: return null
        val buildInstance = intent.getStringExtra("build_instance")?.takeIf {
            it.matches(Regex("[A-Za-z0-9._-]{1,80}"))
        } ?: return null
        val callState = intent.getStringExtra("call_state")?.takeIf {
            it in setOf("idle", "busy", "unknown")
        } ?: return null
        if (pkg != HyperPodsBroadcasts.BLUETOOTH && callState != "unknown") return null
        val allowed = when (pkg) {
            HyperPodsBroadcasts.BLUETOOTH -> setOf("native_ok", "native_failed", "native_unknown")
            HyperPodsBroadcasts.SYSTEM_UI -> setOf("plugin_observed", "plugin_waiting")
            else -> setOf("receiver_ready", "receiver_waiting", "ui_host_ready")
        }
        val detail = intent.getStringExtra("detail")?.takeIf { it in allowed } ?: return null
        if (detail == "ui_host_ready" && processName != "$pkg:ui") return null
        return Report(pkg, version, revision, detail, processName, pid, receiver.sentFromUid,
            frameworkApi, frameworkName, buildInstance, callState)
    }
}
