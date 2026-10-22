package moe.chenxy.hyperpods.hook

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telecom.TelecomManager
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class HostCallStateTest {
    private val context = mock(Context::class.java)
    private val telecom = mock(TelecomManager::class.java)
    private val audio = mock(AudioManager::class.java)

    init {
        `when`(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(context.checkSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE"))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(context.getSystemService(TelecomManager::class.java)).thenReturn(telecom)
        `when`(context.getSystemService(AudioManager::class.java)).thenReturn(audio)
        `when`(audio.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(telecom.isInCall).thenReturn(false)
    }

    private fun read() = HostCallState.read(context, HyperPodsBroadcasts.BLUETOOTH)

    @Test fun onlyBluetoothMayAssertIdle() {
        assertEquals("idle", read())
        assertEquals("unknown", HostCallState.read(context, HyperPodsBroadcasts.SYSTEM_UI))
        assertEquals("unknown", HostCallState.read(context, HyperPodsBroadcasts.XIAOMI_BLUETOOTH))
    }

    @Test fun activeCallIsBusyEvenWithNormalAudio() {
        `when`(telecom.isInCall).thenReturn(true)
        assertEquals("busy", read())
    }

    @Test fun callRingingAndCommunicationAudioAreBusy() {
        for (mode in listOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_COMMUNICATION)) {
            `when`(audio.mode).thenReturn(mode)
            assertEquals("busy", read())
        }
    }

    @Test fun missingPermissionCannotAssertIdle() {
        `when`(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        assertEquals("unknown", read())
        verify(telecom, never()).isInCall
    }

    @Test fun privilegedPhoneStatePermissionIsSufficient() {
        `when`(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(context.checkSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE"))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        assertEquals("idle", read())
    }

    @Test fun missingEitherServiceCannotAssertIdle() {
        `when`(context.getSystemService(TelecomManager::class.java)).thenReturn(null)
        assertEquals("unknown", read())
        `when`(context.getSystemService(TelecomManager::class.java)).thenReturn(telecom)
        `when`(context.getSystemService(AudioManager::class.java)).thenReturn(null)
        assertEquals("unknown", read())
    }

    @Test fun accessDeniedCannotAssertIdle() {
        `when`(telecom.isInCall).thenThrow(SecurityException("denied"))
        assertEquals("unknown", read())
    }

    @Test fun serviceFailureCannotAssertIdle() {
        `when`(audio.mode).thenThrow(IllegalStateException("unavailable"))
        assertEquals("unknown", read())
    }
}
