package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class HeadphonePanelLayoutTest {
    @Test fun portraitWidthMatchesReferenceProportionWithoutFillingScreen() {
        val size = HeadphonePanelLayout.size(1080, 2280, 2.75f)
        assertEquals(.84f, size.widthDp * 2.75f / 1080, .001f)
        assertTrue(size.widthDp < 392)
        assertEquals(2280 / 2.75f - 32, size.maxHeightDp, .01f)
    }

    @Test fun largeWindowsCapWidthAndShortWindowsRemainScrollable() {
        assertEquals(392f, HeadphonePanelLayout.size(2400, 1600, 2f).widthDp, .01f)
        assertEquals(288f, HeadphonePanelLayout.size(1600, 640, 2f).maxHeightDp, .01f)
    }

    @Test fun narrowWindowNeverUsesAnOffscreenMinimumWidth() {
        assertTrue(HeadphonePanelLayout.size(400, 900, 2f).widthDp < 200)
    }

    @Test fun invalidMetricsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { HeadphonePanelLayout.size(0, 100, 1f) }
        assertThrows(IllegalArgumentException::class.java) { HeadphonePanelLayout.size(100, -1, 1f) }
        assertThrows(IllegalArgumentException::class.java) { HeadphonePanelLayout.size(100, 100, Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { HeadphonePanelLayout.size(100, 100, 0f) }
    }
}
