package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class GlassOpticsTest {
    @Test fun selectedLensFollowsTwoTabsAndMirrorsInRtl() {
        assertEquals(88f, GlassOptics.selectionCenter(0f, 340f, 1f, false), .001f)
        assertEquals(252f, GlassOptics.selectionCenter(1f, 340f, 1f, false), .001f)
        assertEquals(170f, GlassOptics.selectionCenter(.5f, 340f, 1f, false), .001f)
        for (position in listOf(0f, .2f, .7f, 1f)) {
            assertEquals(340f, GlassOptics.selectionCenter(position, 340f, 1f, false) +
                GlassOptics.selectionCenter(position, 340f, 1f, true), .001f)
        }
    }

    @Test fun lensCoordinatesScaleWithDensityAndClampInterruptedPageOffsets() {
        val center = GlassOptics.selectionCenter(.3f, 340f, 1f, false)
        assertEquals(center * 3, GlassOptics.selectionCenter(.3f, 1020f, 3f, false), .001f)
        assertEquals(88f, GlassOptics.selectionCenter(-.4f, 340f, 1f, false), .001f)
        assertEquals(252f, GlassOptics.selectionCenter(1.4f, 340f, 1f, false), .001f)
        assertTrue(GlassOptics.REFRACTION_DP < GlassOptics.EDGE_DP)
        assertTrue(GlassOptics.BLUR_DP < GlassOptics.REFRACTION_DP)
    }
}
