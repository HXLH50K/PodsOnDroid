package moe.chenxy.hyperpods.utils

import kotlin.math.min

internal object HeadphonePanelLayout {
    data class Size(val widthDp: Float, val maxHeightDp: Float)

    fun size(widthPx: Int, heightPx: Int, density: Float): Size {
        require(widthPx > 0 && heightPx > 0 && density.isFinite() && density > 0)
        val width = widthPx / density
        val height = heightPx / density
        return Size(min(392f, width * .84f), (height - 32f).coerceAtLeast(1f))
    }
}
