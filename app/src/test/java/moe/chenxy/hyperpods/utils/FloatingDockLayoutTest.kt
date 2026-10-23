package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class FloatingDockLayoutTest {
    @Test fun fieldHiddenByDockMovesIntoTheActuallyUsableViewport() {
        assertEquals(40f, FloatingDockLayout.scrollDistance(420f, 100f, 600f, 120f), .001f)
        assertEquals(1f, FloatingDockLayout.scrollDistance(400f, 81f, 600f, 120f), .001f)
    }

    @Test fun alreadyVisibleFieldsDoNotJump() {
        assertEquals(0f, FloatingDockLayout.scrollDistance(400f, 80f, 600f, 120f), .001f)
        assertEquals(0f, FloatingDockLayout.scrollDistance(200f, 48f, 600f, 120f), .001f)
    }

    @Test fun tallFieldsAndTopClippingAlignTheirStart() {
        assertEquals(-24f, FloatingDockLayout.scrollDistance(-24f, 48f, 600f, 120f), .001f)
        assertEquals(60f, FloatingDockLayout.scrollDistance(60f, 600f, 600f, 120f), .001f)
    }

    @Test fun smallWindowsDoNotProduceNegativeViewportMath() {
        assertEquals(12f, FloatingDockLayout.scrollDistance(12f, 48f, 80f, 120f), .001f)
    }

    @Test fun invalidGeometryIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FloatingDockLayout.scrollDistance(Float.NaN, 40f, 600f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { FloatingDockLayout.scrollDistance(0f, -1f, 600f, 100f) }
    }
}
