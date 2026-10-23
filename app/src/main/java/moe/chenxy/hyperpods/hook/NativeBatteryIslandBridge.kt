package moe.chenxy.hyperpods.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.utils.BatteryIslandContract
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.NativeBatteryIslandLease
import moe.chenxy.hyperpods.utils.NativeBatteryIslandPayload
import moe.chenxy.hyperpods.utils.NativeBatteryIslandSchema
import java.util.Collections
import java.util.WeakHashMap

/** SystemUI-side transport only. Xiaomi Bluetooth remains the sole session/reducer/queue owner. */
internal object NativeBatteryIslandBridge {
    private const val TAG = "PODS-Battery"
    private const val OWNER = "com.android.systemui.statusbar.notification.DynamicIslandPluginController"
    private const val GET_CONTENT =
        "getContent\$packages__apps__MiuiSystemUI__packages__SystemUI__miuiModules__Notification__android_common__MiuiNotification"
    private val handler = Handler(Looper.getMainLooper())
    private val hookedLoaders = Collections.newSetFromMap(WeakHashMap<ClassLoader, Boolean>())
    private val failedLoaders = Collections.newSetFromMap(WeakHashMap<ClassLoader, Boolean>())
    private val retired = LinkedHashSet<String>()
    private var sender: Context? = null
    private var plugin: Any? = null
    private var schema: NativeBatteryIslandSchema? = null
    private var failure = "native_plugin_unavailable"
    private var lastSubmittedAt = -1L
    private var expiry: Runnable? = null
    @Volatile private var lease: NativeBatteryIslandLease? = null
    @Volatile private var nativeData: Any? = null

    fun install(framework: XposedInterface, loader: ClassLoader) {
        val owner = Class.forName(OWNER, false, loader)
        val pluginType = Class.forName("com.android.systemui.plugins.Plugin", false, loader)
        val lifecycle = Class.forName("com.android.systemui.plugins.PluginLifecycleManager", false, loader)
        val contextField = owner.getDeclaredField("context")
        check(contextField.type == Context::class.java)
        val getContent = owner.getMethod(GET_CONTENT)
        check(getContent.returnType.name == NativeBatteryIslandSchema.CONTENT)
        val loaded = owner.getMethod("onPluginLoaded", pluginType, Context::class.java, lifecycle)
        val unloaded = owner.getMethod("onPluginUnloaded", pluginType, lifecycle)
        framework.intercept(loaded) { chain ->
            val result = chain.proceed()
            runCatching {
                val context = contextField.get(chain.thisObject) as Context
                val content = getContent.invoke(chain.thisObject)
                val loadedPlugin = requireNotNull(chain.args[0])
                onMain { attach(framework, context, loadedPlugin, content) }
            }.onFailure { Log.e(TAG, "native_schema_unsupported: no notification fallback", it) }
            result
        }
        framework.intercept(unloaded) { chain ->
            val unloadedPlugin = chain.args[0]
            onMain {
                if (plugin === unloadedPlugin) {
                    finish("native_plugin_unloaded", failed = true)
                    plugin = null
                    schema = null
                    failure = "native_plugin_unavailable"
                }
            }
            chain.proceed()
        }
    }

    private fun attach(framework: XposedInterface, context: Context, loadedPlugin: Any, content: Any?) {
        if (plugin === loadedPlugin) return
        finish("native_plugin_replaced", failed = true)
        plugin = loadedPlugin
        schema = null
        failure = "native_schema_unsupported"
        try {
            val application = requireNotNull(context.applicationContext)
            check(application.packageName == HyperPodsBroadcasts.SYSTEM_UI)
            if (sender == null) {
                sender = application
                register(application)
            }
            val native = NativeBatteryIslandSchema(requireNotNull(content))
            val loader = requireNotNull(content.javaClass.classLoader)
            check(loader !in failedLoaders) { "native_schema_unsupported" }
            if (loader !in hookedLoaders) {
                try {
                    BatteryIslandVisibilityHook.install(framework, loader)
                    framework.intercept(native.removal) { chain ->
                        if (chain.thisObject === schema?.controller &&
                            chain.args[0] == BatteryIslandContract.NATIVE_KEY &&
                            schema?.owns(nativeData) == true) {
                            onMain { finish("native_removed", remove = false) }
                        }
                        chain.proceed()
                    }
                    hookedLoaders.add(loader)
                } catch (error: Exception) {
                    failedLoaders.add(loader)
                    throw error
                }
            }
            schema = native
            failure = ""
            Log.i(TAG, "Notification-free native device-island bridge ready; visibility still requires pre-draw")
        } catch (error: Exception) {
            Log.e(TAG, "native_schema_unsupported: no notification fallback", error)
        }
    }

    private fun register(context: Context) {
        val filter = IntentFilter(BatteryIslandContract.ACTION_PRESENT).apply {
            addAction(BatteryIslandContract.ACTION_CANCEL)
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(receiveContext: Context, intent: Intent) {
                val uid = runCatching {
                    context.packageManager.getPackageUid(HyperPodsBroadcasts.XIAOMI_BLUETOOTH, 0)
                }.getOrDefault(-1)
                if (!BatteryIslandContract.trustedIdentity(sentFromUid, sentFromPackage, uid,
                        setOf(HyperPodsBroadcasts.XIAOMI_BLUETOOTH)) ||
                    intent.`package` != HyperPodsBroadcasts.SYSTEM_UI) return
                try {
                    when (intent.action) {
                        BatteryIslandContract.ACTION_PRESENT -> {
                            val payload = NativeBatteryIslandPayload.read(intent.extras, SystemClock.elapsedRealtime())
                            if (payload == null) Log.w(TAG, "native_payload_rejected") else present(payload)
                        }
                        BatteryIslandContract.ACTION_CANCEL -> cancel(intent)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "native_bridge_failed: no notification fallback", error)
                    finish("native_bridge_failed", failed = true)
                }
            }
        }, filter, null, handler, Context.RECEIVER_EXPORTED)
    }

    private fun present(payload: NativeBatteryIslandPayload) {
        val now = SystemClock.elapsedRealtime()
        val current = lease
        if (current?.owns(payload.token, payload.audioEvent) == true) {
            if (current.update(payload, now)) {
                // Painting existing owned regions cannot re-enter native add/update animations or
                // renew the controller's watchdog. An update without a view waits for pre-draw.
                BatteryIslandVisibilityHook.refresh(payload.token)
            }
            return
        }
        if (payload.revision != 0 || payload.token in retired || payload.submittedAt <= lastSubmittedAt ||
            now >= payload.submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS ||
            payload.left < 0 && payload.right < 0) return
        lastSubmittedAt = payload.submittedAt
        finish("superseded")
        val native = schema
        if (native == null) {
            retire(payload.token)
            send(BatteryIslandContract.ACTION_FAILED, payload, failure)
            return
        }
        lease = NativeBatteryIslandLease(payload)
        try {
            val data = native.create(payload)
            nativeData = data
            if (!scheduleExpiry()) return
            native.submit(data)
            Log.i(TAG, "Native device island submitted, not yet observed")
        } catch (error: Exception) {
            Log.e(TAG, "native_submission_failed: no notification fallback", error)
            finish("native_submission_failed", failed = true)
        }
    }

    @Suppress("DEPRECATION")
    private fun cancel(intent: Intent) {
        val extras = intent.extras ?: return
        if (extras.keySet() != setOf(BatteryIslandContract.SCHEMA,
                BatteryIslandContract.PRESENTATION_TOKEN, BatteryIslandContract.AUDIO_EVENT) ||
            extras.get(BatteryIslandContract.SCHEMA) !is Int ||
            extras.getInt(BatteryIslandContract.SCHEMA) != BatteryIslandContract.VERSION ||
            extras.get(BatteryIslandContract.AUDIO_EVENT) !is Long ||
            extras.get(BatteryIslandContract.PRESENTATION_TOKEN) !is String) return
        val token = extras.getString(BatteryIslandContract.PRESENTATION_TOKEN)
        val event = extras.getLong(BatteryIslandContract.AUDIO_EVENT)
        if (!BatteryIslandContract.validToken(token) || event <= 0) return
        retire(requireNotNull(token))
        if (lease?.owns(token, event) == true) finish("host_cancelled")
    }

    fun payload(data: Any?): NativeBatteryIslandPayload? =
        if (data != null && data === nativeData) lease?.payload else null

    fun observed(token: String, event: Long, at: Long) = onMain {
        val current = lease ?: return@onMain
        if (!current.owns(token, event) || !current.visible(at)) return@onMain
        if (!scheduleExpiry()) return@onMain
        send(BatteryIslandContract.ACTION_VISIBLE, current.payload, visibleAt = at)
        Log.i(TAG, "Native dual-ear pre-draw observed; five-second lease started")
    }

    fun detached(token: String, event: Long) = onMain {
        if (lease?.owns(token, event) == true) finish("native_view_detached")
    }

    fun viewFailed(token: String, event: Long) = onMain {
        if (lease?.owns(token, event) == true) finish("native_view_schema_unsupported", failed = true)
    }

    private fun scheduleExpiry(): Boolean {
        expiry?.let(handler::removeCallbacks)
        val current = lease ?: return false
        val work = Runnable {
            if (lease === current) finish(if (current.visibleAt == null)
                "native_visibility_unconfirmed" else "native_visible_timeout", failed = current.visibleAt == null)
        }
        expiry = work
        if (handler.postDelayed(work, (current.deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))) return true
        finish("native_watchdog_failed", failed = true)
        return false
    }

    private fun finish(reason: String, remove: Boolean = true, failed: Boolean = false) {
        expiry?.let(handler::removeCallbacks)
        expiry = null
        val old = lease ?: return
        val data = nativeData
        lease = null
        nativeData = null
        retire(old.payload.token)
        BatteryIslandVisibilityHook.release(old.payload.token)
        if (data != null) runCatching {
            if (remove) schema?.removeOwned(data) else schema?.removedOwned(data)
        }.onFailure { Log.e(TAG, "native_owned_removal_failed; native safety timeout remains", it) }
        send(if (failed) BatteryIslandContract.ACTION_FAILED else BatteryIslandContract.ACTION_DISMISSED,
            old.payload, reason)
    }

    private fun retire(token: String) {
        retired.add(token)
        while (retired.size > 32) retired.remove(retired.first())
    }

    private fun send(action: String, payload: NativeBatteryIslandPayload, reason: String? = null, visibleAt: Long? = null) {
        val intent = Intent(action)
            .putExtra(BatteryIslandContract.SCHEMA, BatteryIslandContract.VERSION)
            .putExtra(BatteryIslandContract.PRESENTATION_TOKEN, payload.token)
            .putExtra(BatteryIslandContract.AUDIO_EVENT, payload.audioEvent)
        reason?.let { intent.putExtra(BatteryIslandContract.FAILURE, it) }
        visibleAt?.let { intent.putExtra(BatteryIslandContract.VISIBLE_AT, it) }
        runCatching { HyperPodsBroadcasts.send(sender, intent, HyperPodsBroadcasts.XIAOMI_BLUETOOTH) }
            .onFailure { Log.e(TAG, "native_ack_failed; local watchdog remains", it) }
    }

    private inline fun onMain(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else check(handler.post { action() }) { "native_main_dispatch_failed" }
    }
}
