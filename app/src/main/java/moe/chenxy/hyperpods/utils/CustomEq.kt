package moe.chenxy.hyperpods.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Native AACP format from HyperPods/LibrePods (GPL-3.0); values are not dB.
@Parcelize
data class CustomEq(val mode: Int, val low: Int = NEUTRAL, val mid: Int = NEUTRAL, val high: Int = NEUTRAL) : Parcelable {
    init {
        require(mode in RECOMMENDED..CUSTOM && low in 0..100 && mid in 0..100 && high in 0..100)
    }

    fun payload(): ByteArray =
        byteArrayOf(OPCODE, 0, 5, 0, 1, mode.toByte(), low.toByte(), mid.toByte(), high.toByte())

    companion object {
        const val OPCODE: Byte = 0x63
        const val RECOMMENDED = 1
        const val CUSTOM = 2
        const val NEUTRAL = 50
        private val reportPrefix = byteArrayOf(4, 0, 4, 0, OPCODE, 0, 5, 0, 1)
        private val firmwareBuild = Regex("^(\\d+)[A-Za-z]\\d+[A-Za-z]?$")

        fun fromReport(packet: ByteArray): CustomEq? {
            if (packet.size != 13 || reportPrefix.indices.any { packet[it] != reportPrefix[it] }) return null
            val mode = packet[9].toInt() and 0xff
            val low = packet[10].toInt() and 0xff
            val mid = packet[11].toInt() and 0xff
            val high = packet[12].toInt() and 0xff
            if (mode !in RECOMMENDED..CUSTOM || low !in 0..100 || mid !in 0..100 || high !in 0..100) return null
            return CustomEq(mode, low, mid, high)
        }

        fun supportsModel(model: AirPodsBase?): Boolean = when (model) {
            is AirPods4, is AirPods4ANC, is AirPods5, is AirPods5WCC,
            is AirPodsPro2Lightning, is AirPodsPro2USBC, is AirPodsPro3 -> true
            else -> false
        }

        fun firmwareGeneration(firmware: String?): Int? {
            val build = firmware.orEmpty().trim()
            // AACP numeric build identifiers encode the generation in the first digit.
            if (build.isNotEmpty() && build.all { it in '0'..'9' }) return build.first().digitToInt()
            return firmwareBuild.matchEntire(build)?.groupValues?.get(1)?.toIntOrNull()
        }

        fun available(model: AirPodsBase?, firmware: String?, knownEq: CustomEq?): Boolean =
            knownEq != null || supportsModel(model) && (firmwareGeneration(firmware) ?: 0) >= 9
    }
}

@Parcelize
data class CustomEqState(
    val value: CustomEq? = null,
    val unconfirmed: Boolean = false,
    val revision: Long = 0,
    val connectionId: String = "",
) : Parcelable {
    companion object {
        const val EXTRA = "custom_eq_state"
        const val EXTRA_VALUE = "custom_eq"
        const val EXTRA_CONNECTION = "custom_eq_connection"
        const val EXTRA_REVISION = "custom_eq_revision"
    }
}
