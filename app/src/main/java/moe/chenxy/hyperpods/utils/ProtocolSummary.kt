package moe.chenxy.hyperpods.utils

internal object ProtocolSummary {
    private const val HEX = "0123456789ABCDEF"

    fun prefix(bytes: ByteArray, limit: Int): String {
        require(limit in 1..12)
        return buildString {
            for (i in 0 until minOf(bytes.size, limit)) {
                if (i != 0) append(' ')
                val value = bytes[i].toInt() and 0xff
                append(HEX[value ushr 4])
                append(HEX[value and 15])
            }
        }
    }
}
