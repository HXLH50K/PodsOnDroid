package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ProtocolEvidenceTest {
    private var now = 100L
    private val emitted = mutableListOf<String>()
    private val evidence = ProtocolEvidence({ now }, emitted::add)
    private fun control(id: Int = 0x23, value: Int = 3) =
        byteArrayOf(4, 0, 4, 0, 9, 0, id.toByte(), value.toByte(), 0, 0, 0)

    @Test fun onlyExplicitWindowsCaptureAndDeadlineIsExclusive() {
        evidence.packet(control())
        assertTrue(emitted.isEmpty())
        evidence.begin("swipe")
        now += 9999
        evidence.packet(control())
        now++
        evidence.packet(control())
        assertEquals(2, emitted.size)
        assertEquals("PODS_TRACE t=10099 event=rx kind=control id=23 size=11 data=03 00 00 00", emitted.last())
    }

    @Test fun recordCapAndDisconnectClearingAreExact() {
        evidence.begin("ownership")
        repeat(100) { evidence.packet(control(6, 1)) }
        assertEquals(ProtocolEvidence.MAX_RECORDS, emitted.size)
        assertEquals(ProtocolEvidence.MAX_RECORDS, evidence.snapshot().size)
        evidence.clear()
        evidence.packet(control())
        assertTrue(evidence.snapshot().isEmpty())
        assertEquals(ProtocolEvidence.MAX_RECORDS, emitted.size)
    }

    @Test fun shortResponseBodyIsLimitedToTheObservedEnvelope() {
        val reply = byteArrayOf(4, 0, 4, 0, 0x17, 0, 0, 0, 0x10, 0, 8, 0, 8, 1, 0x10, 2, 0x18, 0, 0x20, 0)
        evidence.begin("head")
        evidence.packet(reply)
        assertTrue(emitted.last().endsWith("08 01 10 02 18 00 20 00"))
        reply[10] = 9
        evidence.packet(reply)
        assertTrue(emitted.last().endsWith("data=04 00 04 00 17 00 00 00 10 00 09 00"))
        val sensor = reply.copyOf(80).apply { this[10] = 0x44; fill(0x7f, 12) }
        evidence.packet(sensor)
        val count = emitted.size
        repeat(20) { evidence.packet(sensor) }
        assertEquals(count, emitted.size)
        assertFalse(emitted.last().contains("7F"))
    }

    @Test fun sensitiveOpcodesAndOtherControlsAreExcluded() {
        evidence.begin("swipe")
        for (opcode in 0..255) {
            if (opcode in setOf(9, 0x17, 0x0f)) continue
            evidence.packet(control().apply { this[4] = opcode.toByte() })
        }
        evidence.packet(control(0x1f))
        evidence.packet(control().apply { this[0] = 1 })
        evidence.packet(control().apply { this[5] = 1 })
        assertEquals(1, emitted.size)
    }

    @Test fun queuedFailedAndReceivedValuesStayDistinctAndBounded() {
        evidence.begin("swipe")
        evidence.packet(control(value = 1), true)
        now++
        evidence.packet(control(value = 2), false)
        now++
        evidence.packet(control().copyOf(30).apply { fill(0x7f, 11) })
        assertTrue(emitted[1].contains("event=tx_queued"))
        assertTrue(emitted[2].contains("event=tx_failed"))
        assertTrue(emitted[3].contains("t=102 event=rx"))
        assertTrue(emitted[3].endsWith("data=03 00 00 00"))
    }
}
