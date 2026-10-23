package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test
import moe.chenxy.hyperpods.utils.ListeningCycleCommit.Action

class ListeningCycleCommitTest {
    @Test fun changedValidCycleSubmitsOnNormalDismissal() {
        assertEquals(Action.SUBMIT, ListeningCycleCommit.evaluate(0, 6, 14, true, true))
        assertEquals(Action.SUBMIT, ListeningCycleCommit.evaluate(6, 14, 14, true, true))
    }

    @Test fun unchangedUnknownOrRepeatedDismissalDoesNotWrite() {
        assertEquals(Action.CLOSE, ListeningCycleCommit.evaluate(0, 0, 14, true, true))
        assertEquals(Action.CLOSE, ListeningCycleCommit.evaluate(6, 6, 14, true, true))
        assertEquals(Action.CLOSE, ListeningCycleCommit.evaluate(0, 6, 14, true, false))
    }

    @Test fun invalidSelectionsCannotBeSent() {
        for (value in listOf(0, 2, 7, 16, -1))
            assertEquals(Action.INVALID, ListeningCycleCommit.evaluate(6, value, 14, true, true))
        assertFalse(ListeningCycleCommit.valid(6, -1))
    }

    @Test fun readinessLossIsExplicitRatherThanASuccessfulSave() {
        assertEquals(Action.UNAVAILABLE, ListeningCycleCommit.evaluate(0, 6, 14, false, true))
        assertEquals(Action.CLOSE, ListeningCycleCommit.evaluate(6, 6, 14, false, true))
    }
}
