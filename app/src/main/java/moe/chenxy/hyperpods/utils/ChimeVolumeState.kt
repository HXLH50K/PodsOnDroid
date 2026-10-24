package moe.chenxy.hyperpods.utils

import kotlin.math.roundToInt

internal data class ChimeVolumeState(
    val target: Float,
    val reported: Int? = null,
    val dragging: Boolean = false,
    val requested: Int? = null,
    val stopAtThreshold: Boolean = false,
) {
    data class Update(val state: ChimeVolumeState, val adjustedTo: Int? = null)

    fun drag(value: Float, visibleStart: Float = target): ChimeVolumeState {
        require(value.isFinite() && value in PodsSettings.MIN_CHIME_VOLUME.toFloat()..PodsSettings.MAX_CHIME_VOLUME.toFloat())
        require(visibleStart.isFinite() && visibleStart in 0f..100f)
        val stop = if (dragging) stopAtThreshold else visibleStart > PodsSettings.CHIME_VOLUME_THRESHOLD
        val target = if (stop) value.coerceAtLeast(PodsSettings.CHIME_VOLUME_THRESHOLD.toFloat()) else value
        return copy(target = target, dragging = true, stopAtThreshold = stop)
    }

    fun finish(): ChimeVolumeState {
        val value = target.roundToInt().coerceIn(PodsSettings.MIN_CHIME_VOLUME, PodsSettings.MAX_CHIME_VOLUME)
        return copy(target = target.coerceIn(PodsSettings.MIN_CHIME_VOLUME.toFloat(),
            PodsSettings.MAX_CHIME_VOLUME.toFloat()), dragging = false, requested = value, stopAtThreshold = false)
    }

    val belowThreshold: Boolean get() = target.roundToInt() < PodsSettings.CHIME_VOLUME_THRESHOLD

    fun cancel(): ChimeVolumeState =
        copy(target = reported?.toFloat() ?: target, dragging = false, requested = null, stopAtThreshold = false)

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
