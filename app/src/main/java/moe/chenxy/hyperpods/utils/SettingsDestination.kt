package moe.chenxy.hyperpods.utils

internal enum class SettingsDestination {
    ROOT, NOTIFICATIONS, APPEARANCE, ABOUT, TOOLS, CONNECTION, MODULES, HEAD_TRACKING, LID_OBSERVATION;

    val parent: SettingsDestination?
        get() = when (this) {
            ROOT -> null
            NOTIFICATIONS, APPEARANCE, ABOUT, TOOLS -> ROOT
            CONNECTION, MODULES, HEAD_TRACKING, LID_OBSERVATION -> TOOLS
        }

    fun available(profile: SystemProfile): Boolean =
        this != NOTIFICATIONS || profile.supports(SystemFeature.NATIVE_BATTERY_ISLAND)

    fun children(debug: Boolean, profile: SystemProfile = SystemEnvironment.current): List<SettingsDestination> =
        entries.filter { it.parent == this && (debug || it != LID_OBSERVATION) && it.available(profile) }

    val depth: Int get() = parent?.let { it.depth + 1 } ?: 0

    fun groups(debug: Boolean, profile: SystemProfile = SystemEnvironment.current): List<List<SettingsDestination>> = when (this) {
        ROOT -> listOf(listOf(NOTIFICATIONS, APPEARANCE), listOf(TOOLS, ABOUT))
        TOOLS -> listOf(listOf(CONNECTION, MODULES), listOf(HEAD_TRACKING) +
            if (debug) listOf(LID_OBSERVATION) else emptyList())
        else -> emptyList()
    }.map { group -> group.filter { it.available(profile) } }.filter { it.isNotEmpty() }
}
