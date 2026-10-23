package moe.chenxy.hyperpods.utils

import kotlin.math.roundToInt

internal object PageMotion {
    const val DURATION_MS = 260
    const val EXIT_FADE_MS = 140

    fun direction(fromDepth: Int, toDepth: Int): Int {
        require(fromDepth >= 0 && toDepth >= 0)
        return if (toDepth < fromDepth) -1 else 1
    }

    fun enterOffset(width: Int, direction: Int): Int {
        require(width >= 0 && direction in listOf(-1, 1))
        return (width * .12f).roundToInt() * direction
    }

    fun exitOffset(width: Int, direction: Int): Int = -enterOffset(width, direction) / 3
}
