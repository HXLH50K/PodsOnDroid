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

    val depth: Int get() = parent?.let { it.depth + 1 } ?: 0

    fun groups(debug: Boolean): List<List<SettingsDestination>> = when (this) {
        ROOT -> listOf(listOf(NOTIFICATIONS, APPEARANCE), listOf(TOOLS, ABOUT))
        TOOLS -> listOf(listOf(CONNECTION, MODULES), listOf(HEAD_TRACKING) +
            if (debug) listOf(LID_OBSERVATION) else emptyList())
        else -> emptyList()
    }
}
