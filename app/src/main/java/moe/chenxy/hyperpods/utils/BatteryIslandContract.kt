package moe.chenxy.hyperpods.utils

object BatteryIslandContract {
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
    const val VISIBLE_MS = 5000L
    const val STARTUP_TIMEOUT_MS = 10_000L

    fun validToken(value: String?): Boolean = value != null &&
        value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
}
