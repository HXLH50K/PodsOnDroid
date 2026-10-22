package moe.chenxy.hyperpods.utils

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Collects evidence, not lid events or authenticated headphone identities. */
class LidObservationSession {
    enum class Phase { UNSPECIFIED, CASE_CLOSED, CASE_OPEN }
    enum class Outcome { RECORDED, OTHER_FORMAT, MALFORMED, LIMIT_REACHED, FINISHED }

    data class Marker(val elapsedMs: Long, val phase: Phase)
    data class Frame(
        val candidate: Int,
        val phase: Phase,
        val modelBytes: String,
        val payloadLength: Int,
        val statusByte: Int,
        val earBatteryByte: Int,
        val caseBatteryByte: Int,
        val lidByte: Int,
        val colorByte: Int,
        val connectionByte: Int,
        val firstMs: Long,
        val lastMs: Long,
        val repeats: Int,
        val minRssi: Int?,
        val maxRssi: Int?,
    )

    data class Snapshot(
        val frames: List<Frame>,
        val markers: List<Marker>,
        val candidateCount: Int,
        val accepted: Int,
        val otherFormats: Int,
        val malformed: Int,
        val dropped: Int,
        val phase: Phase,
        val finished: Boolean,
    ) {
        fun toJson(): String = JSONObject().apply {
            put("schema", "pods.lid-observation.v1")
            put("identity", "unassociated-session-candidates")
            put("finished", finished)
            put("candidates", candidateCount)
            put("accepted", accepted)
            put("other_formats", otherFormats)
            put("malformed", malformed)
            put("dropped", dropped)
            put("markers", JSONArray().apply {
                markers.forEach { marker ->
                    put(JSONObject().put("elapsed_ms", marker.elapsedMs).put("phase", marker.phase.name))
                }
            })
            put("frames", JSONArray().apply {
                frames.forEach { frame ->
                    put(JSONObject().apply {
                        put("candidate", frame.candidate)
                        put("phase", frame.phase.name)
                        put("model_bytes", frame.modelBytes)
                        put("payload_length", frame.payloadLength)
                        put("status_byte", frame.statusByte)
                        put("ear_battery_byte", frame.earBatteryByte)
                        put("case_battery_byte", frame.caseBatteryByte)
                        put("lid_byte", frame.lidByte)
                        put("color_byte", frame.colorByte)
                        put("connection_byte", frame.connectionByte)
                        put("first_ms", frame.firstMs)
                        put("last_ms", frame.lastMs)
                        put("repeats", frame.repeats)
                        put("rssi_min", frame.minRssi ?: JSONObject.NULL)
                        put("rssi_max", frame.maxRssi ?: JSONObject.NULL)
                    })
                }
            })
        }.toString(2)
    }

    private val identities = mutableMapOf<String, Int>()
    private val lastFrames = mutableMapOf<Int, Int>()
    private val frames = mutableListOf<Frame>()
    private val markers = mutableListOf<Marker>()
    private var candidateCount = 0
    private var accepted = 0
    private var otherFormats = 0
    private var malformed = 0
    private var dropped = 0
    private var phase = Phase.UNSPECIFIED
    private var finished = false

    fun mark(elapsedMs: Long, value: Phase): Boolean {
        require(elapsedMs >= 0) { "Elapsed observation time cannot be negative" }
        if (finished) return false
        if (elapsedMs >= DURATION_MS) {
            finish()
            return false
        }
        if (markers.size >= MAX_MARKERS) {
            dropped++
            return false
        }
        phase = value
        markers.add(Marker(elapsedMs, value))
        return true
    }

    fun observe(elapsedMs: Long, peer: String, data: ByteArray, rssi: Int): Outcome {
        require(elapsedMs >= 0) { "Elapsed observation time cannot be negative" }
        if (finished) return Outcome.FINISHED
        if (elapsedMs >= DURATION_MS) {
            finish()
            return Outcome.FINISHED
        }
        if (data.isEmpty() || (data[0].toInt() and 0xff) != 0x07) {
            otherFormats++
            return Outcome.OTHER_FORMAT
        }
        if (data.size !in MIN_PAYLOAD_BYTES..MAX_PAYLOAD_BYTES || (data[1].toInt() and 0xff) != data.size - 2 ||
            peer.isEmpty()) {
            malformed++
            return Outcome.MALFORMED
        }
        val candidate = identities[peer] ?: run {
            if (identities.size >= MAX_CANDIDATES) {
                dropped++
                return Outcome.LIMIT_REACHED
            }
            (++candidateCount).also { identities[peer] = it }
        }
        val model = String.format(Locale.ROOT, "%02X%02X", data[3].toInt() and 0xff, data[4].toInt() and 0xff)
        val status = data[5].toInt() and 0xff
        val earBattery = data[6].toInt() and 0xff
        val caseBattery = data[7].toInt() and 0xff
        // Preserve the clear header only; never retain or export the opaque/encrypted tail.
        val lid = data[8].toInt() and 0xff
        val color = data[9].toInt() and 0xff
        val connection = data[10].toInt() and 0xff
        val signal = rssi.takeIf { it in -127..20 }
        val index = lastFrames[candidate]
        val previous = index?.let(frames::get)
        if (index != null && previous != null && previous.phase == phase && previous.modelBytes == model &&
            previous.payloadLength == data.size && previous.statusByte == status &&
            previous.earBatteryByte == earBattery && previous.caseBatteryByte == caseBattery &&
            previous.lidByte == lid && previous.colorByte == color && previous.connectionByte == connection) {
            frames[index] = previous.copy(lastMs = elapsedMs, repeats = previous.repeats + 1,
                minRssi = listOfNotNull(previous.minRssi, signal).minOrNull(),
                maxRssi = listOfNotNull(previous.maxRssi, signal).maxOrNull())
        } else {
            if (frames.size >= MAX_FRAMES) {
                dropped++
                return Outcome.LIMIT_REACHED
            }
            lastFrames[candidate] = frames.size
            frames.add(Frame(candidate, phase, model, data.size, status, earBattery, caseBattery, lid, color, connection,
                elapsedMs, elapsedMs, 1, signal, signal))
        }
        accepted++
        return Outcome.RECORDED
    }

    fun finish() {
        finished = true
        identities.clear()
        lastFrames.clear()
    }

    fun snapshot(): Snapshot = Snapshot(frames.toList(), markers.toList(), candidateCount,
        accepted, otherFormats, malformed, dropped, phase, finished)

    companion object {
        const val DURATION_MS = 30_000L
        const val MAX_CANDIDATES = 8
        const val MAX_FRAMES = 128
        const val MAX_MARKERS = 16
        const val MIN_PAYLOAD_BYTES = 27
        const val MAX_PAYLOAD_BYTES = 64
    }
}
