package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ProtocolSummaryTest {
    @Test fun motionEvidenceStopsAtTheEnvelopeBoundary() {
        val packet = ByteArray(80) { 0x7f }.apply {
            byteArrayOf(4, 0, 4, 0, 0x17, 0, 0, 0, 0x10, 0, 0x46, 0).copyInto(this)
        }
        assertEquals("04 00 04 00 17 00 00 00 10 00 46 00", ProtocolSummary.prefix(packet, 12))
        assertFalse(ProtocolSummary.prefix(packet, 12).contains("7F"))
    }

    @Test fun shortAndUnsignedValuesAreRepresentedWithoutPaddingOrExtraPayload() {
        assertEquals("", ProtocolSummary.prefix(byteArrayOf(), 12))
        assertEquals("00 FF 80", ProtocolSummary.prefix(byteArrayOf(0, -1, -128), 4))
        assertThrows(IllegalArgumentException::class.java) { ProtocolSummary.prefix(ByteArray(32), 13) }
    }

    @Test fun rejectionReasonsDoNotRelaxSensorAcceptance() {
        assertEquals("short_reply", HeadGestureDetector.packetRejection(ByteArray(60)))
        assertEquals("envelope_mismatch", HeadGestureDetector.packetRejection(ByteArray(80)))
        val unknown = syntheticHeadTrackingPacket(report = 0x46)
        assertEquals("unsupported_marker", HeadGestureDetector.packetRejection(unknown))
        assertNull(HeadGestureDetector.motion(unknown))
        assertNull(HeadGestureDetector.packetRejection(syntheticHeadTrackingPacket()))
    }
}
