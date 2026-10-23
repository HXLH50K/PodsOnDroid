package moe.chenxy.hyperpods.hook

import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.BatteryEarbudArtwork
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.NativeBatteryIslandPayload
import moe.chenxy.hyperpods.utils.NativeBatteryIslandSchema
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.ceil

/** Observes only data objects created by our native bridge, never notification-backed content. */
internal object BatteryIslandVisibilityHook {
    private val watchers = Collections.synchronizedMap(WeakHashMap<View, Watcher>())

    fun install(framework: XposedInterface, loader: ClassLoader) {
        val type = Class.forName(
            "miui.systemui.dynamicisland.window.content.DynamicIslandContentView", false, loader)
        val data = Class.forName(NativeBatteryIslandSchema.DATA, false, loader)
        val layout = type.getMethod("updateBigIslandLayout")
        val setter = type.getMethod("setCurrentIslandData", data)
        val text = Class.forName("miuix.colorful.texteffect.TimerTextEffectView", false, loader)
        check(TextView::class.java.isAssignableFrom(text))
        text.getMethod("updateTextWithNewAppearance", CharSequence::class.java, Int::class.javaObjectType)
        type.getMethod("getCurrentIslandData").also { check(it.returnType == data) }
        for (name in listOf("getBigIslandAreaLeft", "getBigIslandAreaRight")) {
            check(ViewGroup::class.java.isAssignableFrom(type.getMethod(name).returnType))
        }
        framework.intercept(setter) { chain ->
            val view = (chain.thisObject as? View)?.takeIf(type::isInstance)
            view?.let { watchers[it]?.stop() }
            chain.proceed()
        }
        framework.intercept(layout) { chain ->
            val view = chain.thisObject as? View
            val result = chain.proceed()
            if (view != null) observe(view)
            result
        }
    }

    private fun observe(view: View) {
        val data = runCatching { HostReflection.call(view, "getCurrentIslandData") }.getOrNull()
        val payload = NativeBatteryIslandBridge.payload(data) ?: run {
            watchers[view]?.stop()
            return
        }
        if (watchers[view]?.data === data) return
        watchers[view]?.stop()
        try {
            Watcher(view, data, payload).also {
                watchers[view] = it
                it.start()
            }
        } catch (_: Exception) {
            NativeBatteryIslandBridge.viewFailed(payload.token, payload.audioEvent)
        }
    }

    fun refresh(token: String) = snapshot(token).forEach { it.dispatch { it.view.invalidate() } }
    fun release(token: String) = snapshot(token).forEach { it.dispatch { it.stop() } }
    private fun snapshot(token: String) = synchronized(watchers) { watchers.values.filter { it.token == token } }

    private class Region(val text: TextView, val icon: ImageView) {
        private val appearance = text.javaClass.getMethod("updateTextWithNewAppearance",
            CharSequence::class.java, Int::class.javaObjectType)
        val colors: ColorStateList = text.textColors
        val minimumWidth = text.minimumWidth
        val description = text.contentDescription
        val originalDrawable = icon.drawable
        var resource = 0
        var color: Int? = null

        fun paint(label: String, color: Int) {
            // TimerTextEffectView renders an appearance span; setTextColor alone is insufficient.
            appearance.invoke(text, label, color)
            text.setTextColor(color)
            this.color = color
        }

        fun restore() {
            // Our verified showHighlightColor=false title starts with the native white appearance.
            appearance.invoke(text, text.text, Color.WHITE)
            text.setTextColor(colors)
            text.minimumWidth = minimumWidth
            text.contentDescription = description
            icon.setImageDrawable(originalDrawable)
        }
    }

    private class Watcher(val view: View, val data: Any?, initial: NativeBatteryIslandPayload) :
        ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        val token = initial.token
        private val event = initial.audioEvent
        private val module = view.context.createPackageContext(BuildConfig.APPLICATION_ID, 0)
        private var observer: ViewTreeObserver? = null
        private var left: Region? = null
        private var right: Region? = null
        private var acknowledged = false
        private var stopped = false

        fun dispatch(action: () -> Unit) {
            val looper = view.handler?.looper
            if (looper == null || looper == Looper.myLooper()) action()
            else view.post { if (!stopped) action() }
        }

        fun start() {
            view.addOnAttachStateChangeListener(this)
            listen()
            view.invalidate()
        }

        private fun listen() {
            observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            observer = view.viewTreeObserver.also { it.addOnPreDrawListener(this) }
        }

        fun stop() {
            if (stopped) return
            stopped = true
            observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            view.removeOnAttachStateChangeListener(this)
            listOfNotNull(left, right).forEach { region ->
                runCatching { region.restore() }
                    .onFailure { Log.w("PODS-Battery", "Unable to restore owned native battery region", it) }
            }
            if (watchers[view] === this) watchers.remove(view)
        }

        override fun onViewAttachedToWindow(v: View) = listen()
        override fun onViewDetachedFromWindow(v: View) {
            stop()
            NativeBatteryIslandBridge.detached(token, event)
        }

        override fun onPreDraw(): Boolean {
            if (stopped) return true
            try {
                val currentData = HostReflection.call(view, "getCurrentIslandData")
                val payload = NativeBatteryIslandBridge.payload(currentData)
                if (currentData !== data || payload?.token != token) {
                    stop()
                    return true
                }
                val leftRoot = HostReflection.call(view, "getBigIslandAreaLeft") as? View ?: return true
                val rightRoot = HostReflection.call(view, "getBigIslandAreaRight") as? View ?: return true
                if (left?.let { !descendant(it.text, leftRoot) || !descendant(it.icon, leftRoot) } == true) {
                    left?.restore()
                    left = null
                }
                if (right?.let { !descendant(it.text, rightRoot) || !descendant(it.icon, rightRoot) } == true) {
                    right?.restore()
                    right = null
                }
                val leftRegion = left ?: region(leftRoot)?.also { left = it } ?: return true
                val rightRegion = right ?: region(rightRoot)?.also { right = it } ?: return true
                check(leftRegion.text !== rightRegion.text && leftRegion.icon !== rightRegion.icon)
                val artwork = BatteryEarbudArtwork.resources(payload.model)
                val changed = paint(leftRegion, payload.left, payload.settings.color(payload.leftLevel),
                    artwork.left, R.string.battery_island_left) or
                    paint(rightRegion, payload.right, payload.settings.color(payload.rightLevel),
                        artwork.right, R.string.battery_island_right)
                // Layout requested by a fresh percentage must complete before acknowledging it.
                if (changed || leftRegion.text.isLayoutRequested || rightRegion.text.isLayoutRequested) return true
                if (!acknowledged && visible(view) && visible(leftRegion.text) && visible(rightRegion.text) &&
                    visible(leftRegion.icon) && visible(rightRegion.icon)) {
                    acknowledged = true
                    NativeBatteryIslandBridge.observed(token, event, SystemClock.elapsedRealtime())
                }
            } catch (_: Exception) {
                stop()
                NativeBatteryIslandBridge.viewFailed(token, event)
            }
            return true
        }

        private fun paint(region: Region, level: Int, color: Int, resource: Int, description: Int): Boolean {
            val label = NativeBatteryIslandSchema.label(level)
            var changed = false
            if (region.text.text.toString() != label || region.color != color) {
                region.paint(label, color)
                changed = true
            }
            val minimum = ceil(region.text.paint.measureText("100%")).toInt()
            if (region.text.minimumWidth < minimum) {
                region.text.minimumWidth = minimum
                changed = true
            }
            check(region.text.text.toString() == label) { "native_percentage_not_renderable" }
            region.text.contentDescription = module.getString(description, label)
            if (region.resource != resource) {
                // Load original artwork, never apply ImageView/Drawable tint or a color filter.
                check(region.icon.imageTintList == null && region.icon.colorFilter == null)
                region.icon.setImageDrawable(requireNotNull(module.getDrawable(resource)))
                region.resource = resource
                changed = true
            }
            return changed
        }
    }

    private fun region(root: View): Region? {
        val texts = ArrayList<TextView>()
        val images = ArrayList<ImageView>()
        fun visit(view: View) {
            if (view.visibility != View.VISIBLE) return
            if (view is TextView && view.text.toString().matches(Regex("(?:[0-9]{1,3}%|--)"))) {
                check(view.javaClass.name == "miuix.colorful.texteffect.TimerTextEffectView")
                texts.add(view)
            }
            if (view is ImageView && view.drawable != null) images.add(view)
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(root)
        check(texts.size <= 1 && images.size <= 1) { "native_view_schema_unsupported" }
        return if (texts.size == 1 && images.size == 1) Region(texts.single(), images.single()) else null
    }

    private fun visible(view: View): Boolean {
        if (!view.isAttachedToWindow || !view.isShown || view.windowVisibility != View.VISIBLE ||
            view.width <= 0 || view.height <= 0 || !view.getGlobalVisibleRect(Rect())) return false
        var current: View? = view
        while (current != null) {
            if (current.alpha < 0.95f || current.scaleX < 0.5f || current.scaleY < 0.5f) return false
            current = current.parent as? View
        }
        return true
    }

    private fun descendant(view: View, root: View): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === root) return true
            current = current.parent as? View
        }
        return false
    }
}
