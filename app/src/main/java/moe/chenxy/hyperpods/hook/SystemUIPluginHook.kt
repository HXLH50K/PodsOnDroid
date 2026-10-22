package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.PodsExpandedMeasure
import moe.chenxy.hyperpods.utils.PodsFocusCardFooter
import moe.chenxy.hyperpods.utils.PodsIslandData
import moe.chenxy.hyperpods.utils.PodsIslandLayout
import moe.chenxy.hyperpods.utils.PodsIslandMiniBar
import java.util.Collections
import java.util.WeakHashMap

object SystemUIPluginHook {
    @Volatile var pluginObserved = false
        private set
    private val installedLoaders = Collections.newSetFromMap(WeakHashMap<ClassLoader, Boolean>())
    private val savedLayouts = Collections.synchronizedMap(WeakHashMap<Any, Pair<PodsIslandLayout, PodsIslandLayout>>())
    private val savedGravity = Collections.synchronizedMap(WeakHashMap<View, Int>())

    fun install(framework: XposedInterface, classLoader: ClassLoader) {
        val pluginInstance = Class.forName("com.android.systemui.shared.plugins.PluginInstance", false, classLoader)
        HostReflection.methods(pluginInstance, "loadPlugin").forEach { method ->
            framework.intercept(method) { chain ->
                val result = chain.proceed()
                val instance = chain.thisObject
                val packageName = runCatching { HostReflection.call(instance, "getPackageName") }
                    .getOrElse { HostReflection.call(instance, "getPackage") }
                if (packageName == "miui.systemui.plugin") {
                    val loader = pluginClassLoader(requireNotNull(instance))
                    val firstLoad = synchronized(installedLoaders) { installedLoaders.add(loader) }
                    pluginObserved = true
                    if (firstLoad) {
                        Log.i("Art_Chen", "Installing hooks in the loaded SystemUI plugin classloader")
                        installPlugin(framework, loader)
                    }
                }
                result
            }
        }
    }

    private fun pluginClassLoader(instance: Any): ClassLoader = runCatching {
        // HyperOS 4 creates the plugin context only when loadPlugin completes.
        val data = requireNotNull(HostReflection.field(instance, "pluginData"))
        val context = HostReflection.field(data, "context")
        HostReflection.call(context, "getClassLoader") as ClassLoader
    }.getOrElse {
        // HyperOS 3 / Android U uses the plugin factory's lazy loader.
        val factory = requireNotNull(HostReflection.field(instance, "mPluginFactory"))
        HostReflection.call(HostReflection.field(factory, "mClassLoaderFactory"), "get") as ClassLoader
    }

    private fun installPlugin(framework: XposedInterface, loader: ClassLoader) {
        installHook("dual-ear battery visibility") { BatteryIslandVisibilityHook.install(framework, loader) }
        installHook("SystemUI device card") { DeviceCardHook.install(framework, loader) }
        optionalHook("focus footer", loader,
            "miui.systemui.notification.focus.moduleV3.ModuleTextButtonViewHolder") { type ->
            HostReflection.methods(type, "bind", 2).forEach { method ->
                framework.intercept(method) { chain ->
                    runCatching {
                        val root = HostReflection.call(chain.thisObject, "getView") as? View
                        PodsFocusCardFooter.restore(focusPart(root, "line"))
                    }.onFailure { Log.w("Art_Chen", "Unable to restore PodsOnDroid focus footer", it) }
                    val result = chain.proceed()
                    runCatching {
                        if (!isHyperPods(chain.args[1] as? StatusBarNotification)) return@runCatching
                        val root = HostReflection.call(chain.thisObject, "getView") as? View
                        // Preserve stock margins, font metrics and height; hide decoration only.
                        PodsFocusCardFooter.apply(focusPart(root, "line"))
                    }.onFailure { Log.w("Art_Chen", "Unable to hide PodsOnDroid focus divider", it) }
                    result
                }
            }
        }
        optionalHook("island mini bar", loader,
            "miui.systemui.dynamicisland.window.content.DynamicIslandBaseContentView") { type ->
            HostReflection.methods(type, "updateMiniBar", 1).forEach { method ->
                framework.intercept(method) { chain ->
                    val result = chain.proceed()
                    runCatching {
                        val owner = chain.args[0] ?: chain.thisObject
                        val data = HostReflection.call(owner, "getCurrentIslandData") ?: return@runCatching
                        val bar = HostReflection.call(chain.thisObject, "getMiniBar") as? View
                        PodsIslandMiniBar.hide(bar, isHyperPods(notification(data)))
                    }.onFailure { Log.w("Art_Chen", "Unable to hide PodsOnDroid mini-window hint", it) }
                    result
                }
            }
        }
        optionalHook("expanded island measurement", loader,
            "miui.systemui.dynamicisland.window.DynamicIslandWindowView") { type ->
            HostReflection.methods(type, "measureExpandedViewHeight", 1).forEach { method ->
                framework.intercept(method) { chain ->
                    var prepared: View? = null
                    runCatching {
                        val data = HostReflection.call(chain.args[0], "getCurrentIslandData") ?: return@runCatching
                        if (!isHyperPods(notification(data))) return@runCatching
                        prepared = HostReflection.call(data, "getView") as? View ?: return@runCatching
                        // Let the host measure both views after releasing only our old fixed height.
                        PodsExpandedMeasure.prepare(prepared, isHyperPods = true)
                    }.onFailure { Log.w("Art_Chen", "Unable to prepare PodsOnDroid expanded measurement", it) }
                    try {
                        chain.proceed()
                    } finally {
                        runCatching { PodsExpandedMeasure.finish(prepared) }
                            .onFailure { Log.w("Art_Chen", "Unable to restore PodsOnDroid expanded measurement", it) }
                    }
                }
            }
        }
        optionalHook("island width", loader,
            "miui.systemui.dynamicisland.window.content.DynamicIslandContentView") { type ->
            HostReflection.methods(type, "calculateBigIslandWidth", 0).forEach { method ->
                framework.intercept(method) { chain ->
                    runCatching { restoreIslandLayout(requireNotNull(chain.thisObject)) }
                        .onFailure { Log.w("Art_Chen", "Unable to restore PodsOnDroid island dimensions", it) }
                    chain.proceed()
                }
            }
        }
        optionalHook("island coexistence layout", loader,
            "miui.systemui.dynamicisland.window.content.DynamicIslandContentView") { type ->
            HostReflection.methods(type, "updateBigIslandLayout", 0).forEach { method ->
                framework.intercept(method) { chain ->
                    runCatching {
                        val view = chain.thisObject as? View ?: return@runCatching
                        restoreIslandLayout(view)
                        val data = HostReflection.call(view, "getCurrentIslandData") ?: return@runCatching
                        if (!isHyperPods(notification(data)) || HostReflection.call(view, "hasSmallIsland") != true ||
                            HostReflection.call(view, "getHelper")?.javaClass?.simpleName !=
                            "DynamicIslandContentViewPhoneHelper") return@runCatching
                        val original = readIslandLayout(view)
                        val left = HostReflection.call(view, "getBigIslandAreaLeft") as? View ?: return@runCatching
                        val right = HostReflection.call(view, "getBigIslandAreaRight") as? View ?: return@runCatching
                        val adjusted = original.symmetric(view.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                            maxOf(left.measuredWidth, right.measuredWidth)) ?: return@runCatching
                        savedLayouts[view] = original to adjusted
                        writeIslandLayout(view, adjusted)
                    }.onFailure { Log.w("Art_Chen", "Unable to adjust PodsOnDroid coexistence dimensions", it) }
                    chain.proceed()
                }
            }
        }
        // OS4 reserves half a small island beside the camera. Anchor our two modules inward only.
        for ((holder, side) in listOf("IslandImageTextViewHolder" to "Left", "IslandImageTextView2Holder" to "Right")) {
            optionalHook("island $side content", loader, "miui.systemui.dynamicisland.module.$holder") { type ->
                HostReflection.methods(type, "bind", 2).forEach { method ->
                    framework.intercept(method) { chain ->
                        val result = chain.proceed()
                        runCatching {
                            val view = HostReflection.call(chain.thisObject, "getView") as? View ?: return@runCatching
                            val params = view.layoutParams as? FrameLayout.LayoutParams ?: return@runCatching
                            val big = HostReflection.call(chain.args[0], "getBigIslandArea")
                            val area = big?.let { HostReflection.call(it, "getImageTextInfo$side") }
                            val pic = area?.let { HostReflection.call(it, "getPicInfo") }
                            val icon = pic?.let { HostReflection.call(it, "getPic") as? String }
                            val desired = PodsIslandData.contentGravity(icon, params.gravity)
                            val gravity = if (desired != null) {
                                if (savedGravity[view] == null) savedGravity[view] = params.gravity
                                desired
                            } else {
                                savedGravity.remove(view) ?: return@runCatching
                            }
                            if (params.gravity != gravity) {
                                params.gravity = gravity
                                view.layoutParams = params
                            }
                        }.onFailure { Log.w("Art_Chen", "Unable to align PodsOnDroid island content", it) }
                        result
                    }
                }
            }
        }
    }

    private fun optionalHook(feature: String, loader: ClassLoader, name: String, install: (Class<*>) -> Unit) {
        val type = try {
            Class.forName(name, false, loader)
        } catch (_: ClassNotFoundException) {
            return // These templates do not exist on every supported HyperOS release.
        }
        installHook(feature) { install(type) }
    }

    @Suppress("DEPRECATION")
    private fun notification(data: Any): StatusBarNotification? =
        (HostReflection.call(data, "getExtras") as? Bundle)?.getParcelable("miui.sbn")

    private fun isHyperPods(sbn: StatusBarNotification?): Boolean =
        sbn?.packageName == HyperPodsBroadcasts.XIAOMI_BLUETOOTH &&
            sbn.notification.extras.getCharSequence("miui.targetPkg")?.toString() == BuildConfig.APPLICATION_ID

    @SuppressLint("DiscouragedApi")
    private fun focusPart(root: View?, name: String): View? {
        root ?: return null
        val id = root.resources.getIdentifier(name, "id", "miui.systemui.plugin")
        return if (id == 0) null else root.findViewById(id)
    }

    private fun readIslandLayout(view: Any): PodsIslandLayout {
        fun get(name: String) = HostReflection.call(view, "getBigIsland${name}HasSmallIsland") as Int
        return PodsIslandLayout(get("LeftWidth"), get("RightWidth"), get("ViewWidth"), get("X"))
    }

    private fun writeIslandLayout(view: Any, layout: PodsIslandLayout) {
        for ((name, value) in listOf("LeftWidth" to layout.left, "RightWidth" to layout.right,
            "ViewWidth" to layout.width, "X" to layout.x))
            HostReflection.call(view, "setBigIsland${name}HasSmallIsland", value)
    }

    private fun restoreIslandLayout(view: Any) {
        val saved = savedLayouts.remove(view) ?: return
        // Do not overwrite a newer host measurement.
        if (readIslandLayout(view) == saved.second) writeIslandLayout(view, saved.first)
    }
}
