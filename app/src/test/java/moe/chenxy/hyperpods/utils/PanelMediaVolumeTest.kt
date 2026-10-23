package moe.chenxy.hyperpods.utils

import android.media.AudioManager
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class PanelMediaVolumeTest {
    private val audio = mock(AudioManager::class.java).apply {
        `when`(getStreamMinVolume(AudioManager.STREAM_MUSIC)).thenReturn(0)
        `when`(getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(6)
    }
    private val volume = PanelMediaVolume(audio)

    @Test fun openingAndRefreshingNeverChangesTheVolume() {
        assertEquals(6, volume.read().level)
        `when`(audio.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(9)
        assertEquals(9, volume.read().level)
        verify(audio, never()).setStreamVolume(anyInt(), anyInt(), anyInt())
    }

    @Test fun explicitChangeWritesOnlyMediaThenReadsActualState() {
        doAnswer {
            `when`(audio.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(8)
            null
        }.`when`(audio).setStreamVolume(AudioManager.STREAM_MUSIC, 8, 0)
        assertEquals(8, volume.set(8).level)
        verify(audio).setStreamVolume(AudioManager.STREAM_MUSIC, 8, 0)
    }

    @Test fun systemLimitIsNotMisreportedAsSuccessfulRequestedValue() {
        assertEquals(6, volume.set(15).level)
        verify(audio).setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0)
    }

    @Test fun fixedAndInvalidRequestsCannotWrite() {
        assertThrows(IllegalArgumentException::class.java) { volume.set(16) }
        assertThrows(IllegalArgumentException::class.java) { volume.set(-1) }
        `when`(audio.isVolumeFixed).thenReturn(true)
        assertFalse(volume.read().adjustable)
        assertThrows(IllegalStateException::class.java) { volume.set(7) }
        verify(audio, never()).setStreamVolume(anyInt(), anyInt(), anyInt())
    }

    @Test fun invalidPlatformReadingIsExplicitlyRejected() {
        `when`(audio.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(16)
        assertThrows(IllegalStateException::class.java) { volume.read() }
    }

    @Test fun noRangeIsNotAnAdjustableSlider() {
        `when`(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(0)
        `when`(audio.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(0)
        assertFalse(volume.read().adjustable)
    }
}
