package moe.chenxy.hyperpods.utils

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LidObservationSessionTest {
    private fun advertisement(status: Int = 0x11, length: Int = 27): ByteArray =
        ByteArray(length).apply {
            this[0] = 7
            this[1] = (length - 2).toByte()
            this[3] = 0xfe.toByte()
            this[4] = 0xed.toByte()
            this[5] = status.toByte()
            this[6] = 0xa9.toByte()
            this[7] = 0x18
            this[8] = 0x08
            this[10] = 0x02
        }

    @Test fun repeatedPacketsAreAggregatedWithoutInventingLidEvents() {
        val session = LidObservationSession()
        repeat(100) { assertEquals(LidObservationSession.Outcome.RECORDED,
            session.observe(it.toLong(), "test-peer", advertisement(), -50 - it % 5)) }
        val snapshot = session.snapshot()
        assertEquals(100, snapshot.accepted)
        assertEquals(1, snapshot.frames.size)
        assertEquals(LidObservationSession.Phase.UNSPECIFIED, snapshot.frames.single().phase)
        assertEquals(100, snapshot.frames.single().repeats)
        assertEquals(99L, snapshot.frames.single().lastMs)
        assertTrue(snapshot.markers.isEmpty())
    }

    @Test fun onlyExplicitMarkersDescribeTheUsersPhysicalActions() {
        val session = LidObservationSession()
        session.mark(0, LidObservationSession.Phase.CASE_CLOSED)
        session.observe(1, "peer", advertisement(), -50)
        session.mark(100, LidObservationSession.Phase.CASE_OPEN)
        session.observe(101, "peer", advertisement(), -50)
        assertEquals(listOf(LidObservationSession.Phase.CASE_CLOSED, LidObservationSession.Phase.CASE_OPEN),
            session.snapshot().frames.map { it.phase })
    }

    @Test fun candidateIdentityIsSessionLocalAndDoesNotAssertOwnership() {
        val session = LidObservationSession()
        session.observe(0, "peer-a", advertisement(), -30)
        session.observe(1, "peer-b", advertisement(), -20)
        assertEquals(listOf(1, 2), session.snapshot().frames.map { it.candidate })
        assertEquals("unassociated-session-candidates", JSONObject(session.snapshot().toJson()).getString("identity"))
    }

    @Test fun exportNeverIncludesPeerAddressesOrTheOpaquePayloadTail() {
        val session = LidObservationSession()
        val data = advertisement(length = 40)
        "PRIVATE_ENCRYPTED_TAIL".toByteArray().copyInto(data, 11)
        session.observe(20, "AA:BB:CC:DD:EE:FF", data, -45)
        session.finish()
        val exported = session.snapshot().toJson()
        assertFalse(exported.contains("AA:BB:CC:DD:EE:FF"))
        assertFalse(exported.contains("PRIVATE_ENCRYPTED_TAIL"))
        assertEquals("FEED", JSONObject(exported).getJSONArray("frames").getJSONObject(0).getString("model_bytes"))
    }

    @Test fun changedClearFieldsProduceDistinctRecords() {
        val session = LidObservationSession()
        session.observe(0, "peer", advertisement(0x11), -45)
        session.observe(1, "peer", advertisement(0x21), -45)
        assertEquals(listOf(0x11, 0x21), session.snapshot().frames.map { it.statusByte })
    }

    @Test fun malformedAndOtherFormatsAreCountedButNotDecoded() {
        val session = LidObservationSession()
        session.observe(0, "peer", byteArrayOf(1, 2), -40)
        session.observe(1, "peer", byteArrayOf(7), -40)
        session.observe(2, "peer", advertisement().apply { this[1] = 0 }, -40)
        assertEquals(1, session.snapshot().otherFormats)
        assertEquals(2, session.snapshot().malformed)
        assertTrue(session.snapshot().frames.isEmpty())
    }

    @Test fun theRecorderAcceptsOnlyBoundedPayloadsAndCandidateSets() {
        val session = LidObservationSession()
        assertEquals(LidObservationSession.Outcome.MALFORMED,
            session.observe(0, "peer", advertisement(length = 65), -50))
        repeat(20) { session.observe(it.toLong(), "peer-$it", advertisement(), -50) }
        assertEquals(LidObservationSession.MAX_CANDIDATES, session.snapshot().candidateCount)
        assertEquals(12, session.snapshot().dropped)
    }

    @Test fun frameAndMarkerLimitsAreExplicit() {
        val session = LidObservationSession()
        repeat(150) { session.observe(it.toLong(), "peer", advertisement(it), -50) }
        repeat(20) { session.mark(it.toLong(), LidObservationSession.Phase.CASE_OPEN) }
        assertEquals(LidObservationSession.MAX_FRAMES, session.snapshot().frames.size)
        assertEquals(LidObservationSession.MAX_MARKERS, session.snapshot().markers.size)
        assertEquals(26, session.snapshot().dropped)
    }

    @Test fun finishedSessionsRejectLateCallbacksAndKeepAnImmutableSnapshot() {
        val session = LidObservationSession()
        session.observe(0, "peer", advertisement(), -50)
        val before = session.snapshot()
        session.finish()
        assertEquals(LidObservationSession.Outcome.FINISHED, session.observe(10, "peer", advertisement(2), -50))
        assertFalse(session.mark(10, LidObservationSession.Phase.CASE_OPEN))
        assertTrue(session.snapshot().finished)
        assertFalse(before.finished)
        assertEquals(1, before.accepted)
    }

    @Test fun unavailableRssiIsNotMisrepresentedAsZero() {
        val session = LidObservationSession()
        session.observe(0, "peer", advertisement(), 127)
        assertNull(session.snapshot().frames.single().minRssi)
        assertTrue(JSONObject(session.snapshot().toJson()).getJSONArray("frames").getJSONObject(0).isNull("rssi_min"))
    }

    @Test fun theThirtySecondBoundaryRejectsFurtherSamplesAndMarkers() {
        val session = LidObservationSession()
        assertEquals(LidObservationSession.Outcome.RECORDED,
            session.observe(29_999, "peer", advertisement(), -50))
        assertEquals(LidObservationSession.Outcome.FINISHED,
            session.observe(30_000, "peer", advertisement(2), -50))
        assertFalse(session.mark(30_001, LidObservationSession.Phase.CASE_OPEN))
        assertEquals(1, session.snapshot().accepted)
        assertTrue(session.snapshot().finished)
        val markerOnly = LidObservationSession()
        assertFalse(markerOnly.mark(30_000, LidObservationSession.Phase.CASE_OPEN))
        assertTrue(markerOnly.snapshot().finished)
    }

    @Test fun exportedCaptureFailuresCannotLookLikeSuccessfulThirtySecondRuns() {
        val session = LidObservationSession()
        session.observe(100, "peer", advertisement(), -50)
        session.finish()
        val state = LidObservationCapture.State(LidObservationCapture.Status.FAILED,
            elapsedMs = 1250, scanError = 2, observations = session.snapshot())
        val json = JSONObject(state.toJson())
        assertEquals("FAILED", json.getString("capture_status"))
        assertEquals(1250L, json.getLong("elapsed_ms"))
        assertEquals(2, json.getInt("scan_error"))
    }

    @Test fun emptyObservationsRemainExportableWithoutClaimingCompatibility() {
        val state = LidObservationCapture.State(LidObservationCapture.Status.COMPLETE, elapsedMs = 30_000)
        val json = JSONObject(state.toJson())
        assertEquals(0, json.getInt("accepted"))
        assertEquals(0, json.getJSONArray("frames").length())
        assertEquals("unassociated-session-candidates", json.getString("identity"))
    }

    @Test fun lidOnlyChangesAreNotCollapsedIntoBatteryRefreshes() {
        val session = LidObservationSession()
        session.observe(0, "peer", advertisement(), -50)
        session.observe(1, "peer", advertisement().apply { this[8] = 0 }, -50)
        assertEquals(listOf(8, 0), session.snapshot().frames.map { it.lidByte })
        val frames = JSONObject(session.snapshot().toJson()).getJSONArray("frames")
        assertEquals(8, frames.getJSONObject(0).getInt("lid_byte"))
        assertEquals(0, frames.getJSONObject(1).getInt("lid_byte"))
        assertEquals(LidObservationSession.Phase.UNSPECIFIED, session.snapshot().phase)
        assertEquals(LidObservationSession.Outcome.MALFORMED,
            session.observe(2, "peer", advertisement(length = 26), -50))
    }
}
