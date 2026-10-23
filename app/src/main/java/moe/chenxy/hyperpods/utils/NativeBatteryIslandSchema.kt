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
        check(it.name == CONTROLLER) { "native_schema_unsupported" }
    }
    private val loader = controllerType.classLoader
    private val dataType = Class.forName(DATA, false, loader)
    private val constructor = dataType.getConstructor()
    private val setKey = dataType.getMethod("setKey", String::class.java)
    private val setTicker = dataType.getMethod("setTickerData", String::class.java)
    private val setProperties = dataType.getMethod("setProperties", Int::class.javaObjectType)
    private val setPriority = dataType.getMethod("setPriority", Int::class.javaObjectType)
    private val setExtras = dataType.getMethod("setExtras", Bundle::class.java)
    private val getProperties = dataType.getMethod("getProperties")
    private val add = controllerType.getMethod("addDynamicIslandView", dataType, Boolean::class.javaPrimitiveType)
    private val remove = controllerType.getMethod("removeDynamicIslandView", String::class.java)
    val removal: Method = controllerType.getMethod("removeDynamicIslandView",
        String::class.java, Boolean::class.javaPrimitiveType)
    private val maxWidth = controllerType.getMethod("getIslandMaxWidth")
    private val parse = controllerType.getMethod("getIslandTemplate", dataType)
    private val window = requireNotNull(HostReflection.method(controllerType, "getView").invoke(controller))
    private val currentUser = window.javaClass.getMethod("getCurrentUserId")
    private val islandData = controllerType.getDeclaredField("islandData").apply { isAccessible = true }
    private val pendingData = controllerType.getDeclaredField("pendingData").apply { isAccessible = true }
    private val safeguards = controllerType.getMethod("getDynamicIslandSafeguardsController").invoke(controller)
    private val cancelNativeTimeout = requireNotNull(safeguards).javaClass
        .getDeclaredMethod("cancelDelayDeleted", String::class.java).apply { isAccessible = true }

    init {
        val content = Class.forName(CONTENT, false, loader)
        check(content.isInstance(controller) && islandData.type == HashMap::class.java &&
            pendingData.type == HashMap::class.java && maxWidth.returnType == Float::class.javaPrimitiveType)
        check(parse.returnType.name == "miui.systemui.dynamicisland.model.IslandTemplate" &&
            window.javaClass.name == "miui.systemui.dynamicisland.window.DynamicIslandWindowView" &&
            currentUser.returnType == Int::class.javaPrimitiveType &&
            listOf(setKey, setTicker, setProperties, setPriority, setExtras, add, remove, removal)
                .all { it.returnType == Void.TYPE })
        // Verify the real update interface too, but do not invoke it for refresh: this firmware's
        // implementation can re-add a missing view and renew its native timeout.
        controllerType.getMethod("updateDynamicIslandView", dataType, Boolean::class.javaPrimitiveType)
        dataType.getMethod("getExtras").also { check(it.returnType == Bundle::class.java) }
        dataType.getMethod("getKey").also { check(it.returnType == String::class.java) }
        check(getProperties.returnType == Int::class.javaObjectType)
    }

    fun create(payload: NativeBatteryIslandPayload): Any {
        check((maxWidth.invoke(controller) as Float) > 0f) { "native_island_not_ready" }
        val current = islandData.get(controller) as Map<*, *>
        check(!current.containsKey(BatteryIslandContract.NATIVE_KEY)) {
            "native_key_busy"
        }
        check(current.values.none { dataType.isInstance(it) && getProperties.invoke(it) == 0 }) {
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
        // Do not enter the controller's unbounded pending-width queue.
        check((maxWidth.invoke(controller) as Float) > 0f) { "native_island_not_ready" }
        add.invoke(controller, data, false)
    }

    fun removeOwned(data: Any) {
        @Suppress("UNCHECKED_CAST")
        val pending = pendingData.get(controller) as MutableMap<Any, Any>
        pending.remove(data)
        val current = (islandData.get(controller) as Map<*, *>)[BatteryIslandContract.NATIVE_KEY]
        if (current === data) {
            remove.invoke(controller, BatteryIslandContract.NATIVE_KEY)
            cancelNativeTimeout.invoke(safeguards, BatteryIslandContract.NATIVE_KEY)
        }
    }

    fun owns(data: Any?): Boolean = data != null &&
        (islandData.get(controller) as Map<*, *>)[BatteryIslandContract.NATIVE_KEY] === data

    fun removedOwned(data: Any) {
        if (owns(data)) cancelNativeTimeout.invoke(safeguards, BatteryIslandContract.NATIVE_KEY)
    }

    companion object {
        const val CONTROLLER = "miui.systemui.dynamicisland.window.DynamicIslandWindowViewController"
        const val DATA = "com.android.systemui.plugins.miui.dynamicisland.DynamicIslandData"
        const val CONTENT = "com.android.systemui.plugins.miui.dynamicisland.DynamicIslandContent"
        const val LEFT_PICTURE = "pods.native.ear.left"
        const val RIGHT_PICTURE = "pods.native.ear.right"
        fun label(level: Int) = if (level >= 0) "$level%" else "--"
    }
}
