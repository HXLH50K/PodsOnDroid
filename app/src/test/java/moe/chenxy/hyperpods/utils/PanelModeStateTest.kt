package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class PanelModeStateTest {
    @Test fun selectionDoesNotInventReport() {
        val state = PanelModeState().report(1).select(4)
        assertEquals(1, state.reported)
        assertEquals(4, state.requested)
    }

    @Test fun oldReportDoesNotConfirmNewSelection() {
        assertEquals(4, PanelModeState().select(4).report(1).requested)
    }

    @Test fun matchingReportConfirmsSelection() {
        val state = PanelModeState().select(4).report(4)
        assertNull(state.requested)
        assertEquals(4, state.reported)
    }

    @Test fun failureSurvivesCachedReport() {
        val state = PanelModeState().report(1).select(4).fail().report(1)
        assertTrue(state.failed)
        assertNull(state.requested)
        assertEquals(1, state.reported)
    }

    @Test fun retryClearsFailure() {
        assertFalse(PanelModeState().fail().select(2).failed)
    }

    @Test fun unknownIsNeverOff() {
        assertNull(PanelModeState().report(1).report(0).reported)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSelectionRejected() { PanelModeState().select(5) }
}
