package moe.chenxy.hyperpods.utils

/** Explicit debug actions open a bounded, allowlisted metadata window, not a packet logger. */
class ProtocolEvidence(private val clock: () -> Long, private val emit: (String) -> Unit) {
    companion object {
        const val TAG = "PODS-ProtocolEvidence"
        const val WINDOW_MS = 10_000L
        const val MAX_RECORDS = 64
    }

    private var deadline: Long? = null
    private var sawSensorEnvelope = false
    private val records = mutableListOf<String>()

    @Synchronized fun begin(reason: String) {
        require(reason in setOf("ownership", "head", "swipe"))
        records.clear()
        sawSensorEnvelope = false
        deadline = clock() + WINDOW_MS
        append("begin reason=$reason")
    }

    @Synchronized fun packet(packet: ByteArray, sent: Boolean? = null) {
        val until = deadline ?: return
        if (clock() >= until || records.size >= MAX_RECORDS) return
        if (packet.size < 6 || !packet.take(4).toByteArray().contentEquals(byteArrayOf(4, 0, 4, 0)) ||
            packet[5] != 0.toByte()) return
        val event = when (sent) { null -> "rx"; true -> "tx_queued"; false -> "tx_failed" }
        val detail = when (packet[4].toInt() and 0xff) {
            9 -> {
                val id = packet.getOrNull(6)?.toInt()?.and(0xff) ?: return
                if (id !in setOf(6, 0x23)) return
                "control id=${if (id == 6) "06" else "23"} size=${packet.size} data=" +
                    hex(packet.drop(7).take(4))
            }
            0x17 -> {
                if (sent == null && packet.size > 60) {
                    if (sawSensorEnvelope) return
                    sawSensorEnvelope = true
                }
                // Only the observed short envelope may expose its eight-byte response body.
                val shortReply = sent == null && packet.size == 20 &&
                    packet.copyOfRange(6, 12).contentEquals(byteArrayOf(0, 0, 0x10, 0, 8, 0))
                "head size=${packet.size} data=" + hex(packet.take(if (shortReply) 20 else 12))
            }
            0x0f -> "status size=${packet.size} data=-"
            else -> return
        }
        append("$event kind=$detail")
    }

    @Synchronized fun snapshot(): List<String> = records.toList()

    @Synchronized fun clear() {
        deadline = null
        records.clear()
        sawSensorEnvelope = false
    }

    private fun append(event: String) {
        val line = "PODS_TRACE t=${clock()} event=$event"
        records.add(line)
        emit(line)
    }

    private fun hex(bytes: List<Byte>) = bytes.joinToString(" ") {
        "%02X".format(it.toInt() and 0xff)
    }.ifEmpty { "-" }
}
