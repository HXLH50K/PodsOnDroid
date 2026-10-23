package moe.chenxy.hyperpods.utils

/** The OEM returns a lazy content proxy, not necessarily its window controller. */
internal class NativeBatteryIslandEndpoint(private val content: Any) {
    private val type = content.javaClass
    private val direct = type.name == NativeBatteryIslandSchema.CONTROLLER
    private val real = if (direct) null else {
        check(type.name == PROXY) { "native_content_unsupported: ${type.name}" }
        type.getDeclaredMethod("real").apply {
            check(returnType.name == NativeBatteryIslandSchema.CONTENT)
            isAccessible = true
        }
    }
    private val creator = if (direct) null else type.getDeclaredField("this\$0").run {
        check(this.type.name == CREATOR)
        isAccessible = true
        requireNotNull(get(content))
    }
    private val create = creator?.javaClass?.getMethod("createView")?.also {
        check(it.returnType.name == NativeBatteryIslandSchema.CONTENT)
    }

    init {
        check(Class.forName(NativeBatteryIslandSchema.CONTENT, false, type.classLoader).isInstance(content))
    }

    fun current(): Any? = (if (direct) content else real?.invoke(content))?.also {
        check(it.javaClass.name == NativeBatteryIslandSchema.CONTROLLER) {
            "native_controller_unsupported: ${it.javaClass.name}"
        }
    }

    fun resolve(): Any {
        current()?.let { return it }
        // Match the OEM add path: create only for a real presentation, never during plugin load.
        val created = requireNotNull(requireNotNull(create).invoke(creator)) { "native_window_unavailable" }
        check(created === current()) { "native_window_binding_mismatch" }
        return created
    }

    companion object {
        private const val CREATOR = "miui.systemui.dynamicisland.window.DynamicIslandWindowViewCreator"
        private const val PROXY = "$CREATOR\$dynamicIslandContentProxy\$2\$1"
    }
}
