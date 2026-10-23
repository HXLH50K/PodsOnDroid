package moe.chenxy.hyperpods.utils

object BatteryIslandContract {
    const val ACTION_PRESENT = "com.hxlh50k.pods.action.battery_island_present"
    const val ACTION_CANCEL = "com.hxlh50k.pods.action.battery_island_cancel"
    const val ACTION_FAILED = "com.hxlh50k.pods.action.battery_island_failed"
    const val ACTION_VISIBLE = "com.hxlh50k.pods.action.battery_island_visible"
    const val ACTION_DISMISSED = "com.hxlh50k.pods.action.battery_island_dismissed"
    const val ACTION_CONTROL_GAP = "com.hxlh50k.pods.action.battery_control_gap"
    const val PRESENTATION_TOKEN = "pods.battery.presentation"
    const val AUDIO_EVENT = "pods.battery.audio_event"
    const val VISIBLE_AT = "pods.battery.visible_at"
    const val KIND = "pods.battery.kind"
    const val LEFT_COLOR = "pods.battery.left_color"
    const val RIGHT_COLOR = "pods.battery.right_color"
    const val SETTINGS = "batteryNotificationSettings"
    const val CONTROL_GENERATION = "batteryControlGeneration"
    const val OBSERVED_AT = "batteryObservedAtElapsed"
    const val SCHEMA = "pods.battery.schema"
    const val SUBMITTED_AT = "pods.battery.submitted_at"
    const val REVISION = "pods.battery.revision"
    const val LEFT = "pods.battery.left"
    const val RIGHT = "pods.battery.right"
    const val LOW = "pods.battery.low"
    const val MEDIUM = "pods.battery.medium"
    const val MODEL = "pods.battery.model"
    const val FAILURE = "pods.battery.failure"
    const val NATIVE_KEY = "com.hxlh50k.pods:battery:native:v1"
    const val VERSION = 1
    const val SYSTEM_UI_PLUGIN = "miui.systemui.plugin"
    const val VISIBLE_MS = 5000L
    const val STARTUP_TIMEOUT_MS = 10_000L

    fun validToken(value: String?): Boolean = value != null &&
        value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))

    fun trustedIdentity(uid: Int, sender: String?, expectedUid: Int, allowedPackages: Set<String>): Boolean =
        uid >= 0 && expectedUid >= 0 && uid == expectedUid && sender in allowedPackages
}
