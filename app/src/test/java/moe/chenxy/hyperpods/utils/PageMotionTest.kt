package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class PageMotionTest {
    @Test fun navigationDepthDefinesForwardAndReturnDirections() {
        assertEquals(1, PageMotion.direction(0, 1))
        assertEquals(1, PageMotion.direction(1, 2))
        assertEquals(-1, PageMotion.direction(2, 1))
        assertEquals(-1, PageMotion.direction(1, 0))
        assertEquals(-1, PageMotion.direction(2, 0))
    }

    @Test fun siblingNavigationDoesNotPretendToPop() {
        assertEquals(1, PageMotion.direction(1, 1))
    }

    @Test fun pageMovementIsBoundedAndReversesSymmetrically() {
        assertEquals(48, PageMotion.enterOffset(400, 1))
        assertEquals(-48, PageMotion.enterOffset(400, -1))
        assertEquals(-16, PageMotion.exitOffset(400, 1))
        assertEquals(16, PageMotion.exitOffset(400, -1))
        assertEquals(0, PageMotion.enterOffset(0, 1))
    }

    @Test fun exitFadesBeforeTheDestinationFinishesMoving() {
        assertTrue(PageMotion.EXIT_FADE_MS in 1 until PageMotion.DURATION_MS)
        assertTrue(PageMotion.DURATION_MS in 200..300)
    }

    @Test fun invalidGeometryOrDepthIsExplicitlyRejected() {
        assertThrows(IllegalArgumentException::class.java) { PageMotion.direction(-1, 0) }
        assertThrows(IllegalArgumentException::class.java) { PageMotion.enterOffset(-1, 1) }
        assertThrows(IllegalArgumentException::class.java) { PageMotion.exitOffset(400, 0) }
    }
}
