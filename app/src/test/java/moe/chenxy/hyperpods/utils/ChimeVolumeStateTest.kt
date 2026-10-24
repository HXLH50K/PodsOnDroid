package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class ChimeVolumeStateTest {
    @Test fun lowerActualReportsAreRetainedWithoutAutomaticallyRequestingAValue() {
        for (value in 0..14) {
            val update = ChimeVolumeState(30f, 30).report(value)
            assertEquals(value.toFloat(), update.state.target, 0f)
            assertEquals(value, update.state.reported)
            assertNull(update.state.requested)
        }
    }

    @Test fun dragEndpointsMatchTheWritableRange() {
        val initial = ChimeVolumeState(15f, 15)
        assertEquals(0, initial.drag(0f).finish().requested)
        assertEquals(15, initial.drag(15f).finish().requested)
        assertEquals(100, initial.drag(100f).finish().requested)
        val fractional = initial.drag(87.25f).finish()
        assertEquals(87.25f, fractional.target, 0f)
        assertEquals(87, fractional.requested)
    }

    @Test fun crossingFromAboveStopsAtFifteenForTheEntireGesture() {
        var state = ChimeVolumeState(60f, 60).drag(5f)
        assertEquals(15f, state.target, 0f)
        assertFalse(state.belowThreshold)
        state = state.drag(0f).drag(40f).drag(1f)
        assertEquals(15f, state.target, 0f)
        assertEquals(15, state.finish().requested)
    }

    @Test fun releaseAtTheStopAllowsANewGestureToSubmitALowerValue() {
        val stopped = ChimeVolumeState(60f, 60).drag(5f).finish()
        val lower = stopped.drag(5f).finish()
        assertEquals(5, lower.requested)
        assertTrue(lower.belowThreshold)
        assertEquals(0, ChimeVolumeState(10f, 10).drag(0f).finish().requested)
    }

    @Test fun releasingAboveTheThresholdDoesNotUnlockTheNextGesture() {
        val above = ChimeVolumeState(60f, 60).drag(30f).finish()
        assertEquals(15f, above.drag(0f).target, 0f)
    }

    @Test fun aReportDuringTheFirstGestureCannotUnlockTheStop() {
        val state = ChimeVolumeState(60f, 60).drag(20f).report(0).state.drag(0f)
        assertEquals(15f, state.target, 0f)
        assertEquals(15, state.finish().requested)
    }

    @Test fun gateUsesTheVisibleValueWhenAReportIsAnimating() {
        assertEquals(15f, ChimeVolumeState(10f, 10).drag(5f, visibleStart = 60f).target, 0f)
        assertEquals(5f, ChimeVolumeState(60f, 60).drag(5f, visibleStart = 15f).target, 0f)
    }

    @Test fun cancelledGestureRestoresTheReportAndCannotSubmitOrUnlock() {
        val cancelled = ChimeVolumeState(60f, 60).drag(5f).cancel()
        assertEquals(60f, cancelled.target, 0f)
        assertFalse(cancelled.dragging)
        assertNull(cancelled.requested)
        assertEquals(15f, cancelled.drag(0f).target, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun dragRejectsValuesOutsideTheProtocolRange() {
        ChimeVolumeState(30f, 30).drag(-1f)
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
