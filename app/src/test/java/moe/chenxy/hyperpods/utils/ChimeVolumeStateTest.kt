package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ChimeVolumeStateTest {
    @Test fun lowerActualReportsAreRetainedWithoutSendingAnInvalidRequest() {
        for (value in 0..14) {
            val update = ChimeVolumeState(30f, 30).report(value)
            assertEquals(value.toFloat(), update.state.target, 0f)
            assertEquals(value, update.state.reported)
            assertNull(update.state.requested)
            assertEquals(15, update.state.finish().requested)
        }
    }

    @Test fun dragEndpointsMatchTheWritableRange() {
        val initial = ChimeVolumeState(30f, 30)
        assertEquals(15, initial.drag(15f).finish().requested)
        assertEquals(100, initial.drag(100f).finish().requested)
        val fractional = initial.drag(87.25f).finish()
        assertEquals(87.25f, fractional.target, 0f)
        assertEquals(87, fractional.requested)
    }

    @Test(expected = IllegalArgumentException::class)
    fun dragCannotSubmitBelowTheUpstreamMinimum() {
        ChimeVolumeState(30f, 30).drag(14f)
    }

    @Test fun reportsCannotMoveTheThumbWhileDragging() {
        val drag = ChimeVolumeState(30f, 30).drag(85f)
        val update = drag.report(40)
        assertEquals(85f, update.state.target, 0f)
        assertTrue(update.state.dragging)
        assertNull(update.adjustedTo)
        val finished = update.state.finish()
        assertEquals(85, finished.requested)
        assertEquals(85f, finished.target, 0f)
    }

    @Test fun differentRealReplyAfterReleaseRequestsAnAnimatedCorrectionAndNotice() {
        val state = ChimeVolumeState(30f, 30).drag(85f).finish()
        val update = state.report(45)
        assertEquals(45f, update.state.target, 0f)
        assertEquals(45, update.adjustedTo)
        assertNull(update.state.requested)
        assertNull(update.state.report(45).adjustedTo)
    }

    @Test fun matchingReplyAndInitialReadingDoNotShowCorrectionNotices() {
        assertNull(ChimeVolumeState(0f).report(30).adjustedTo)
        assertNull(ChimeVolumeState(30f, 30).drag(85f).finish().report(85).adjustedTo)
    }

    @Test fun newDragTakesPriorityOverAPreviousReplyOrFailure() {
        val next = ChimeVolumeState(30f, 30).drag(85f).finish().drag(65f)
        assertEquals(65f, next.report(85).state.target, 0f)
        assertEquals(next, next.failed())
    }

    @Test fun failuresReturnToTheRealValueNotTheUnconfirmedRequest() {
        val state = ChimeVolumeState(30f, 30).drag(85f).finish().failed()
        assertEquals(30f, state.target, 0f)
        assertNull(state.requested)
    }
}
