package moe.chenxy.hyperpods.utils

internal object FloatingDockLayout {
    const val HEIGHT_DP = 72
    const val MARGIN_DP = 12

    fun scrollDistance(offset: Float, itemSize: Float, containerSize: Float, obscuredBottom: Float): Float {
        require(listOf(offset, itemSize, containerSize, obscuredBottom).all { it.isFinite() })
        require(itemSize >= 0 && containerSize >= 0 && obscuredBottom >= 0)
        val usable = (containerSize - obscuredBottom).coerceAtLeast(0f)
        return when {
            offset < 0 -> offset
            offset + itemSize <= usable -> 0f
            itemSize >= usable -> offset
            else -> offset + itemSize - usable
        }
    }
}
