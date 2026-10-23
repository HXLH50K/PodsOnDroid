package moe.chenxy.hyperpods.utils

internal data class PanelModeState(
    val reported: Int? = null,
    val requested: Int? = null,
    val failed: Boolean = false
) {
    fun select(mode: Int): PanelModeState {
        require(mode in 1..4)
        return copy(requested = mode, failed = false)
    }

    fun report(mode: Int): PanelModeState =
        if (mode !in 1..4) copy(reported = null)
        else copy(reported = mode, requested = requested?.takeUnless { it == mode })

    fun fail(): PanelModeState = copy(requested = null, failed = true)
}
