package moe.chenxy.hyperpods.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatteryNotificationSettings(
    val lowThreshold: Int = 20,
    val mediumThreshold: Int = 50,
    val lowReminderEnabled: Boolean = true,
    val mediumReminderEnabled: Boolean = true,
) : Parcelable {
    val valid: Boolean get() = lowThreshold in 1..98 && mediumThreshold in 2..99 && lowThreshold < mediumThreshold

    enum class Band { UNKNOWN, LOW, MEDIUM, HIGH }

    fun band(level: Int?): Band {
        require(valid) { "Battery thresholds must satisfy 1 <= low < medium <= 99" }
        return when {
            level == null || level !in 0..100 -> Band.UNKNOWN
            level < lowThreshold -> Band.LOW
            level > mediumThreshold -> Band.HIGH
            else -> Band.MEDIUM
        }
    }

    fun color(level: Int?): Int = when (band(level)) {
        Band.UNKNOWN -> 0xffb0b4bd.toInt()
        Band.LOW -> 0xffff625b.toInt()
        Band.MEDIUM -> 0xffffd45a.toInt()
        Band.HIGH -> 0xff5cdb91.toInt()
    }
}
