package moe.chenxy.hyperpods.utils

import kotlin.math.roundToInt

internal data class ChimeVolumeState(
    val target: Float,
    val reported: Int? = null,
    val dragging: Boolean = false,
    val requested: Int? = null,
) {
    data class Update(val state: ChimeVolumeState, val adjustedTo: Int? = null)

    fun drag(value: Float): ChimeVolumeState {
        require(value.isFinite() && value in 0f..100f)
        return copy(target = value, dragging = true)
    }

    fun finish(): ChimeVolumeState = copy(dragging = false, requested = target.roundToInt())

    fun report(value: Int): Update {
        require(value in 0..100)
        if (dragging) return Update(copy(reported = value))
        val changed = (reported != null || requested != null) && target.roundToInt() != value
        return Update(copy(target = value.toFloat(), reported = value, requested = null),
            value.takeIf { changed })
    }

    fun failed(): ChimeVolumeState =
        if (dragging || reported == null) this
        else copy(target = reported.toFloat(), requested = null)
}
