package moe.chenxy.hyperpods.hook

import android.graphics.Rect
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.content.Intent
import android.util.Log
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.BatteryIslandContract
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import java.util.WeakHashMap

/** Acknowledges actual pre-draw of our two battery regions, not merely notification submission. */
internal object BatteryIslandVisibilityHook {
    private val watchers = WeakHashMap<View, Watcher>()
    private val acknowledged = LinkedHashSet<String>()
    private val savedColors = WeakHashMap<TextView, ColorStateList>()

    fun install(framework: XposedInterface, loader: ClassLoader) {
        val type = Class.forName(
            "miui.systemui.dynamicisland.window.content.DynamicIslandContentView", false, loader)
        val methods = HostReflection.methods(type, "updateBigIslandLayout", 0)
        check(methods.isNotEmpty()) { "No native battery visibility lifecycle method" }
        methods.forEach { method ->
            framework.intercept(method) { chain ->
                (chain.thisObject as? View)?.let(::restoreText)
                val result = chain.proceed()
                val view = chain.thisObject as? View
                if (view != null) {
                    try { observe(view) } catch (error: Exception) {
                        Log.w("PODS-Battery", "Cannot observe native battery presentation", error)
                    }
                }
                result
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun ownNotification(view: View): StatusBarNotification? {
        val data = HostReflection.call(view, "getCurrentIslandData") ?: return null
        val notification = (HostReflection.call(data, "getExtras") as? Bundle)
            ?.getParcelable<StatusBarNotification>("miui.sbn") ?: return null
        return notification.takeIf {
            it.packageName == HyperPodsBroadcasts.XIAOMI_BLUETOOTH &&
                it.notification.extras.getCharSequence("miui.targetPkg")?.toString() == BuildConfig.APPLICATION_ID
        }
    }

    private fun observe(view: View) {
        val token = ownNotification(view)?.notification?.extras
            ?.getString(BatteryIslandContract.PRESENTATION_TOKEN)
        if (!BatteryIslandContract.validToken(token)) {
            watchers.remove(view)?.stop()
            return
        }
        if (token in acknowledged) {
            val extras = ownNotification(view)?.notification?.extras ?: return
            (HostReflection.call(view, "getBigIslandAreaLeft") as? View)?.let {
                tintText(it, extras.getInt(BatteryIslandContract.LEFT_COLOR))
            }
            (HostReflection.call(view, "getBigIslandAreaRight") as? View)?.let {
                tintText(it, extras.getInt(BatteryIslandContract.RIGHT_COLOR))
            }
            return
        }
        if (watchers[view]?.token == token) return
        watchers.remove(view)?.stop()
        Watcher(view, requireNotNull(token)).also {
            watchers[view] = it
            it.start()
        }
    }

    private class Watcher(private val view: View, val token: String) :
        ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener, Runnable {
        private val observer = view.viewTreeObserver

        fun start() {
            observer.addOnPreDrawListener(this)
            view.addOnAttachStateChangeListener(this)
            if (!view.postDelayed(this, BatteryIslandContract.STARTUP_TIMEOUT_MS)) {
                Log.w("PODS-Battery", "Unable to schedule native visibility watchdog")
                stop()
            }
        }

        fun stop() {
            if (observer.isAlive) observer.removeOnPreDrawListener(this)
            view.removeOnAttachStateChangeListener(this)
            view.removeCallbacks(this)
            if (watchers[view] === this) watchers.remove(view)
        }

        override fun run() {
            Log.w("PODS-Battery", "Native dual-ear content was not observed before watchdog")
            stop()
        }

        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) = stop()

        override fun onPreDraw(): Boolean {
            try {
                val notification = ownNotification(view) ?: run { stop(); return true }
                val extras = notification.notification.extras
                if (extras.getString(BatteryIslandContract.PRESENTATION_TOKEN) != token) {
                    stop()
                    return true
                }
                val left = HostReflection.call(view, "getBigIslandAreaLeft") as? View
                val right = HostReflection.call(view, "getBigIslandAreaRight") as? View
                if (!visibleBattery(left) || !visibleBattery(right)) return true
                tintText(requireNotNull(left), extras.getInt(BatteryIslandContract.LEFT_COLOR))
                tintText(requireNotNull(right), extras.getInt(BatteryIslandContract.RIGHT_COLOR))
                val sender = requireNotNull(view.context.applicationContext) { "SystemUI application context unavailable" }
                HyperPodsBroadcasts.send(sender, Intent(BatteryIslandContract.ACTION_VISIBLE)
                    .putExtra(BatteryIslandContract.PRESENTATION_TOKEN, token)
                    .putExtra(BatteryIslandContract.AUDIO_EVENT, extras.getLong(BatteryIslandContract.AUDIO_EVENT))
                    .putExtra(BatteryIslandContract.VISIBLE_AT, SystemClock.elapsedRealtime()),
                    HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
                acknowledged.add(token)
                while (acknowledged.size > 32) acknowledged.remove(acknowledged.first())
                stop()
            } catch (error: Exception) {
                Log.w("PODS-Battery", "Native battery visibility acknowledgment failed", error)
                stop()
            }
            return true
        }
    }

    private fun visibleBattery(view: View?): Boolean {
        if (view == null || !view.isAttachedToWindow || !view.isShown || view.alpha < 0.95f ||
            !view.getGlobalVisibleRect(Rect())) return false
        return when (view) {
            is TextView -> view.text.contains('%') || view.text.contains("--")
            is ViewGroup -> (0 until view.childCount).any { visibleBattery(view.getChildAt(it)) }
            else -> false
        }
    }

    private fun tintText(view: View, color: Int) {
        if (view is TextView && (view.text.contains('%') || view.text.contains("--"))) {
            if (!savedColors.containsKey(view)) savedColors[view] = view.textColors
            view.setTextColor(color)
        } else if (view is ViewGroup) {
            for (index in 0 until view.childCount) tintText(view.getChildAt(index), color)
        }
    }

    private fun restoreText(view: View) {
        if (view is TextView) savedColors.remove(view)?.let(view::setTextColor)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) restoreText(view.getChildAt(index))
        }
    }
}
