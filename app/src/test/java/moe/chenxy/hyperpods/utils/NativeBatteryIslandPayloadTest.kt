package moe.chenxy.hyperpods.utils

import android.os.Bundle
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class NativeBatteryIslandPayloadTest {
    private val token = "12345678-1234-1234-1234-123456789abc"
    private fun payload() = NativeBatteryIslandPayload(token, 7, 1000, 0, 18, 72, 20, 50, "A3439")

    @Test fun typedBoundedPayloadAllowsIndependentAndUnknownLevels() {
        assertTrue(payload().valid(1000))
        assertTrue(payload().copy(left = -1).valid(1001))
        assertTrue(payload().copy(left = -1, right = -1).valid(1001))
        assertFalse(payload().copy(left = -2).valid(1001))
        assertFalse(payload().copy(right = 101).valid(1001))
        assertFalse(payload().copy(low = 50).valid(1001))
        assertFalse(payload().copy(medium = 100).valid(1001))
        assertFalse(payload().copy(model = "x".repeat(33)).valid(1001))
        assertFalse(payload().copy(model = "file:///untrusted").valid(1001))
        assertFalse(payload().copy(token = "not-a-token").valid(1001))
        assertFalse(payload().copy(audioEvent = 0).valid(1001))
        assertFalse(payload().copy(revision = -1).valid(1001))
        assertFalse(payload().copy(revision = 10_001).valid(1001))
    }

    @Test fun futureAndExpiredPacketsAreRejectedWithoutClockOverflow() {
        assertFalse(payload().valid(999))
        assertTrue(payload().valid(15_999))
        assertFalse(payload().valid(16_000))
        assertFalse(payload().copy(submittedAt = -1).valid(1001))
        assertFalse(payload().copy(submittedAt = Long.MAX_VALUE).valid(1001))
    }

    @Test fun startupIsNotVisibilityAndUpdatesNeverExtendTheDeadline() {
        val lease = NativeBatteryIslandLease(payload())
        assertNull(lease.visibleAt)
        assertEquals(11_000L, lease.deadline)
        assertTrue(lease.update(payload().copy(revision = 1, left = 17), 2000))
        assertEquals(11_000L, lease.deadline)
        assertTrue(lease.visible(10_999))
        assertEquals(15_999L, lease.deadline)
        assertTrue(lease.update(payload().copy(revision = 2, right = 70), 11_500))
        assertFalse(lease.visible(12_000))
        assertEquals(10_999L, lease.visibleAt)
        assertEquals(15_999L, lease.deadline)
    }

    @Test fun staleForeignAndExpiredUpdatesCannotReviveOwnedContent() {
        val lease = NativeBatteryIslandLease(payload())
        assertFalse(lease.update(payload(), 2000))
        assertFalse(lease.update(payload().copy(revision = 1, audioEvent = 8), 2000))
        assertFalse(lease.update(payload().copy(revision = 1, token = token.replace('a', 'b')), 2000))
        assertFalse(lease.update(payload().copy(revision = 1, submittedAt = 1001), 2000))
        assertFalse(lease.visible(999))
        assertFalse(lease.visible(11_000))
        assertFalse(lease.update(payload().copy(revision = 1), 11_000))
        assertFalse(lease.owns(token, 8))
        assertFalse(lease.owns(null, 7))
        assertTrue(lease.owns(token, 7))
    }

    @Test fun observedExpiryIsTerminalEvenForNewerRevisions() {
        val lease = NativeBatteryIslandLease(payload())
        assertTrue(lease.visible(1200))
        assertTrue(lease.update(payload().copy(revision = 1), 6199))
        assertFalse(lease.update(payload().copy(revision = 2), 6200))
        assertEquals(6200L, lease.deadline)
    }

    @Test fun lazyWindowPreparationUsesTheOriginalStartupDeadline() {
        val lease = NativeBatteryIslandLease(payload())
        assertFalse(lease.preparing(999))
        assertTrue(lease.preparing(1000))
        assertTrue(lease.update(payload().copy(revision = 1, left = 16), 10_000))
        assertEquals(16, lease.payload.left)
        assertTrue(lease.preparing(10_999))
        assertFalse(lease.preparing(11_000))
        assertEquals(11_000L, lease.deadline)
    }

    @Test fun visibilityEndsPreparationInsteadOfReaddingTheIsland() {
        val lease = NativeBatteryIslandLease(payload())
        assertTrue(lease.visible(1200))
        assertFalse(lease.preparing(1201))
        assertEquals(6200L, lease.deadline)
    }

    @Test fun senderRequiresUidAndDeclaredContextPackageTogether() {
        val allowed = setOf(HyperPodsBroadcasts.SYSTEM_UI, BatteryIslandContract.SYSTEM_UI_PLUGIN)
        assertTrue(BatteryIslandContract.trustedIdentity(1000, HyperPodsBroadcasts.SYSTEM_UI, 1000, allowed))
        assertTrue(BatteryIslandContract.trustedIdentity(1000, BatteryIslandContract.SYSTEM_UI_PLUGIN, 1000, allowed))
        assertFalse(BatteryIslandContract.trustedIdentity(1000, "other.shared.uid.app", 1000, allowed))
        assertFalse(BatteryIslandContract.trustedIdentity(1234, HyperPodsBroadcasts.SYSTEM_UI, 1000, allowed))
        assertFalse(BatteryIslandContract.trustedIdentity(-1, HyperPodsBroadcasts.SYSTEM_UI, -1, allowed))
        assertFalse(BatteryIslandContract.trustedIdentity(1000, null, 1000, allowed))
        assertFalse(BatteryIslandContract.trustedIdentity(1000, HyperPodsBroadcasts.XIAOMI_BLUETOOTH, 1000, allowed))
    }

    @Test fun exactWireSchemaRejectsUnknownFieldsWrongTypesAndVersions() {
        val values = values()
        assertEquals(payload(), NativeBatteryIslandPayload.read(bundle(values), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values + ("miui.sbn" to "fake")), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values + (BatteryIslandContract.SCHEMA to 2)), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values + (BatteryIslandContract.LEFT to "18")), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values + (BatteryIslandContract.AUDIO_EVENT to 7)), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values + (BatteryIslandContract.MODEL to 1)), 1001))
        assertNull(NativeBatteryIslandPayload.read(bundle(values - BatteryIslandContract.LOW), 1001))
        assertNull(NativeBatteryIslandPayload.read(null, 1001))
    }

    private fun values(): Map<String, Any> = mapOf(
        BatteryIslandContract.SCHEMA to 1,
        BatteryIslandContract.PRESENTATION_TOKEN to token,
        BatteryIslandContract.AUDIO_EVENT to 7L,
        BatteryIslandContract.SUBMITTED_AT to 1000L,
        BatteryIslandContract.REVISION to 0,
        BatteryIslandContract.LEFT to 18,
        BatteryIslandContract.RIGHT to 72,
        BatteryIslandContract.LOW to 20,
        BatteryIslandContract.MEDIUM to 50,
        BatteryIslandContract.MODEL to "A3439",
    )

    @Suppress("DEPRECATION")
    private fun bundle(values: Map<String, Any>): Bundle = mock(Bundle::class.java).also { bundle ->
        `when`(bundle.keySet()).thenReturn(values.keys)
        for ((key, value) in values) {
            `when`(bundle.get(key)).thenReturn(value)
            `when`(bundle.containsKey(key)).thenReturn(true)
            when (value) {
                is Int -> `when`(bundle.getInt(key)).thenReturn(value)
                is Long -> `when`(bundle.getLong(key)).thenReturn(value)
                is String -> `when`(bundle.getString(key)).thenReturn(value)
            }
        }
    }
}
