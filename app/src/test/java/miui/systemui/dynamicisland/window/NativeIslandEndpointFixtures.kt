package miui.systemui.dynamicisland.window

import com.android.systemui.plugins.miui.dynamicisland.DynamicIslandContent

/** Minimal independently authored shapes for testing the verified lazy endpoint contract. */
class DynamicIslandWindowViewController : DynamicIslandContent

class DynamicIslandWindowViewCreator {
    var current: DynamicIslandContent? = null
    var next: DynamicIslandContent? = DynamicIslandWindowViewController()
    var bind = true
    var creations = 0

    fun createView(): DynamicIslandContent? {
        creations++
        if (bind) current = next
        return next
    }
}

class `DynamicIslandWindowViewCreator$dynamicIslandContentProxy$2$1`(
    @JvmField val `this$0`: DynamicIslandWindowViewCreator,
) : DynamicIslandContent {
    private fun real(): DynamicIslandContent? = `this$0`.current
}
