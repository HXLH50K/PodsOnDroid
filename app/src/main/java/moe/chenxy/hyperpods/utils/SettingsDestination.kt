package moe.chenxy.hyperpods.utils

internal enum class SettingsDestination {
    ROOT, NOTIFICATIONS, APPEARANCE, ABOUT, TOOLS, CONNECTION, MODULES, HEAD_TRACKING, LID_OBSERVATION;

    val parent: SettingsDestination?
        get() = when (this) {
            ROOT -> null
            NOTIFICATIONS, APPEARANCE, ABOUT, TOOLS -> ROOT
            CONNECTION, MODULES, HEAD_TRACKING, LID_OBSERVATION -> TOOLS
        }

    fun children(debug: Boolean): List<SettingsDestination> =
        entries.filter { it.parent == this && (debug || it != LID_OBSERVATION) }
}
