package moe.chenxy.hyperpods.utils

import android.os.Bundle
import org.json.JSONObject
import java.lang.reflect.Method

/**
 * Contract from the locally inspected HyperOS SystemUI/plugin APKs, not the strong-toast grammar.
 * Property 0 is native device content; its data contains no StatusBarNotification.
 */
internal class NativeBatteryIslandSchema(val controller: Any) {
    private val controllerType = controller.javaClass.also {
        check(it.name == CONTROLLER) { "native_controller_unsupported: ${it.name}" }
    }
    private val loader = controllerType.classLoader
    private val dataType = Class.forName(DATA, false, loader)
    private val constructor = dataType.getConstructor()
    private val setKey = dataType.getMethod("setKey", String::class.java)
    private val setTicker = dataType.getMethod("setTickerData", String::class.java)
    private val setProperties = dataType.getMethod("setProperties", Int::class.javaObjectType)
    private val setPriority = dataType.getMethod("setPriority", Int::class.javaObjectType)
    private val setExtras = dataType.getMethod("setExtras", Bundle::class.java)
    private val getExtras = dataType.getMethod("getExtras")
    private val getKey = dataType.getMethod("getKey")
    private val getProperties = dataType.getMethod("getProperties")
    private val add = controllerType.getMethod("addDynamicIslandView", dataType, Boolean::class.javaPrimitiveType)
    private val remove = controllerType.getMethod("removeDynamicIslandView", String::class.java)
    val removal: Method = controllerType.getMethod("removeDynamicIslandView",
        String::class.java, Boolean::class.javaPrimitiveType)
    private val parse = controllerType.getMethod("getIslandTemplate", dataType)
    private val window = requireNotNull(HostReflection.method(controllerType, "getView").invoke(controller))
    private val currentUser = window.javaClass.getMethod("getCurrentUserId")
    private val notifyRemove = window.javaClass.getMethod("notifyRemoveIsland",
        String::class.java, Int::class.javaObjectType, String::class.java, Int::class.javaObjectType)
    private val islandData = controllerType.getDeclaredField("islandData").apply { isAccessible = true }
    private val pendingData = controllerType.getDeclaredField("pendingData").apply { isAccessible = true }
    private val safeguards = controllerType.getMethod("getDynamicIslandSafeguardsController").invoke(controller)
    private val cancelNativeTimeout = requireNotNull(safeguards).javaClass
        .getDeclaredMethod("cancelDelayDeleted", String::class.java).apply { isAccessible = true }

    init {
        val content = Class.forName(CONTENT, false, loader)
        check(content.isInstance(controller) && islandData.type == HashMap::class.java &&
            pendingData.type == HashMap::class.java)
        check(parse.returnType.name == "miui.systemui.dynamicisland.model.IslandTemplate" &&
            window.javaClass.name == "miui.systemui.dynamicisland.window.DynamicIslandWindowView" &&
            currentUser.returnType == Int::class.javaPrimitiveType &&
            listOf(setKey, setTicker, setProperties, setPriority, setExtras, add, remove, removal, notifyRemove)
                .all { it.returnType == Void.TYPE })
        // Verify the real update interface too, but do not invoke it for refresh: this firmware's
        // implementation can re-add a missing view and renew its native timeout.
        controllerType.getMethod("updateDynamicIslandView", dataType, Boolean::class.javaPrimitiveType)
        check(getExtras.returnType == Bundle::class.java && getKey.returnType == String::class.java)
        check(getProperties.returnType == Int::class.javaObjectType)
    }

    fun create(payload: NativeBatteryIslandPayload): Any {
        val current = islandData.get(controller) as Map<*, *>
        val pending = pendingData.get(controller) as Map<*, *>
        check(!current.containsKey(BatteryIslandContract.NATIVE_KEY) &&
            pending.keys.none { dataType.isInstance(it) && getKey.invoke(it) == BatteryIslandContract.NATIVE_KEY }) {
            "native_key_busy"
        }
        check((current.values + pending.keys).none { dataType.isInstance(it) && getProperties.invoke(it) == 0 }) {
            "native_device_island_busy"
        }
        val data = constructor.newInstance()
        setKey.invoke(data, BatteryIslandContract.NATIVE_KEY)
        setProperties.invoke(data, 0)
        setPriority.invoke(data, 1)
        val (left, right) = BatteryEarbudArtwork.icons(payload.model)
        setExtras.invoke(data, Bundle().apply {
            putInt("miui.user.id", currentUser.invoke(window) as Int)
            putBundle("miui.focus.pics", Bundle().apply {
                putParcelable(LEFT_PICTURE, left)
                putParcelable(RIGHT_PICTURE, right)
            })
            putInt(BatteryIslandContract.SCHEMA, BatteryIslandContract.VERSION)
            putString(BatteryIslandContract.PRESENTATION_TOKEN, payload.token)
            putLong(BatteryIslandContract.AUDIO_EVENT, payload.audioEvent)
        })
        fun region(type: Int, picture: String, level: Int) = JSONObject()
            .put("type", type)
            .put("picInfo", JSONObject().put("type", 1).put("pic", picture))
            .put("textInfo", JSONObject().put("title", label(level))
                .put("showHighlightColor", false).put("turnAnim", false))
        val ticker = JSONObject()
            .put("islandProperty", 0)
            .put("islandPriority", 1)
            .put("islandTimeout", (BatteryIslandContract.STARTUP_TIMEOUT_MS +
                BatteryIslandContract.VISIBLE_MS) / 1000)
            .put("maxSize", false)
            .put("bigIslandArea", JSONObject()
                .put("imageTextInfoLeft", region(1, LEFT_PICTURE, payload.left))
                .put("imageTextInfoRight", region(2, RIGHT_PICTURE, payload.right)))
        setTicker.invoke(data, ticker.toString())
        val parsed = requireNotNull(parse.invoke(controller, data)) { "native_schema_unsupported" }
        check(HostReflection.call(parsed, "getIslandProperty") == 0 &&
            HostReflection.call(parsed, "getIslandPriority") == 1 &&
            HostReflection.call(parsed, "getIslandTimeout") == 15) { "native_schema_unsupported" }
        val area = requireNotNull(HostReflection.call(parsed, "getBigIslandArea"))
        for ((getter, type) in listOf("getImageTextInfoLeft" to 1, "getImageTextInfoRight" to 2)) {
            val region = requireNotNull(HostReflection.call(area, getter))
            check(HostReflection.call(region, "getType") == type)
            val picture = requireNotNull(HostReflection.call(region, "getPicInfo"))
            check(HostReflection.call(picture, "getType") == 1) { "native_schema_unsupported" }
            val text = requireNotNull(HostReflection.call(region, "getTextInfo"))
            check(HostReflection.call(text, "getTurnAnim") == false &&
                HostReflection.call(text, "getShowHighlightColor") == false) { "native_schema_unsupported" }
        }
        return data
    }

    fun submit(data: Any) {
        // Native add initiates the width handshake. Our lease bounds this single owned entry,
        // including pending-only cancellation before the native view has been materialized.
        add.invoke(controller, data, false)
    }

    fun removeOwned(data: Any) {
        val pending = pendingData.get(controller) as MutableMap<*, *>
        val queued = removePendingIdentity(pending, data)
        val current = (islandData.get(controller) as Map<*, *>)[BatteryIslandContract.NATIVE_KEY]
        if (current === data) {
            remove.invoke(controller, BatteryIslandContract.NATIVE_KEY)
            cancelNativeTimeout.invoke(safeguards, BatteryIslandContract.NATIVE_KEY)
        } else if (queued && current == null &&
            pending.keys.none { dataType.isInstance(it) && getKey.invoke(it) == BatteryIslandContract.NATIVE_KEY }) {
            val extras = requireNotNull(getExtras.invoke(data) as? Bundle)
            notifyRemove.invoke(window, extras.getString("miui.pkg.name"), extras.getInt("miui.user.id"),
                BatteryIslandContract.NATIVE_KEY, getProperties.invoke(data))
        }
    }

    fun owns(data: Any?): Boolean = data != null &&
        (islandData.get(controller) as Map<*, *>)[BatteryIslandContract.NATIVE_KEY] === data

    fun removedOwned(data: Any) {
        if (owns(data)) cancelNativeTimeout.invoke(safeguards, BatteryIslandContract.NATIVE_KEY)
    }

    companion object {
        internal fun removePendingIdentity(pending: MutableMap<*, *>, data: Any): Boolean {
            val entries = pending.keys.iterator()
            while (entries.hasNext()) {
                if (entries.next() === data) {
                    entries.remove()
                    return true
                }
            }
            return false
        }

        const val CONTROLLER = "miui.systemui.dynamicisland.window.DynamicIslandWindowViewController"
        const val DATA = "com.android.systemui.plugins.miui.dynamicisland.DynamicIslandData"
        const val CONTENT = "com.android.systemui.plugins.miui.dynamicisland.DynamicIslandContent"
        const val LEFT_PICTURE = "pods.native.ear.left"
        const val RIGHT_PICTURE = "pods.native.ear.right"
        fun label(level: Int) = if (level >= 0) "$level%" else "--"
    }
}
