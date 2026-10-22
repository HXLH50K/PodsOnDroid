package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class OwnedPodsNotificationsTest {
    private val owner = "com.hxlh50k.pods.debug"
    private val policy = OwnedPodsNotifications(owner)
    private val legacy = PodsNotificationKey("BTHeadsetTEST_DEVICE", 10003)

    @Test fun namespacedAlpha2AudioAndStatusCanBeRetiredWithoutGuessingBluetoothTags() {
        assertTrue(policy.canRetire(PodsNotificationKey("$owner:audio:TEST_DEVICE", 10004)))
        assertTrue(policy.canRetire(PodsNotificationKey("$owner:status:TEST_DEVICE", 10003)))
        assertFalse(policy.canRetire(PodsNotificationKey("$owner:audio:TEST_DEVICE", 10003)))
        assertFalse(policy.canRetire(PodsNotificationKey("$owner:status:", 10003)))
        assertFalse(policy.canRetire(PodsNotificationKey("$owner.other:audio:TEST_DEVICE", 10004)))
    }

    @Test fun legacyAlpha1EntriesRequireExactTargetPackageOrOwnedPendingIntent() {
        assertFalse(policy.canRetire(legacy))
        assertTrue(policy.canRetire(legacy, targetPackage = owner))
        assertTrue(policy.canRetire(legacy, pendingIntentPackages = setOf(owner)))
        val connection = PodsNotificationKey("HEADSET_CONNECT_NOTIFICATION", 1001)
        assertFalse(policy.canRetire(connection))
        assertTrue(policy.canRetire(connection, targetPackage = owner))
    }

    @Test fun concurrentlyEnabledHyperPodsAndXiaomiEntriesAreNeverOurCleanupTargets() {
        for (other in listOf("moe.chenxy.hyperpods", "moe.chenxy.hyperpods.debug",
            "com.xiaomi.bluetooth", "com.hxlh50k.pods", "other.app")) {
            assertFalse(policy.canRetire(legacy, targetPackage = other))
            assertFalse(policy.canRetire(legacy, pendingIntentPackages = setOf(other)))
            assertFalse(policy.canRetire(PodsNotificationKey("$other:audio:TEST_DEVICE", 10004)))
        }
        assertFalse(policy.canRetire(legacy, "moe.chenxy.hyperpods", setOf(owner)))
        assertFalse(policy.canRetire(PodsNotificationKey("$owner:audio:TEST_DEVICE", 10004),
            "com.xiaomi.bluetooth"))
    }

    @Test fun ownershipEvidenceDoesNotAuthorizeCancellingArbitraryHostOrAppNotices() {
        assertFalse(policy.canRetire(PodsNotificationKey("XiaomiHeadset", 10003), owner))
        assertFalse(policy.canRetire(PodsNotificationKey("BTHeadsetTEST_DEVICE", 999), owner))
        assertFalse(policy.canRetire(PodsNotificationKey("$owner:unrelated:TEST_DEVICE", 10004), owner))
    }

    @Test fun routineCleanupPreservesCurrentPresentationAndRetiresOnlySafeStaleEntries() {
        val sessions = AudioConnectionNotifications(owner)
        val current = sessions.connected("CURRENT", AudioConnectionEvent(100, 1000), 1000)!!
        val oldStatus = PodsNotificationKey("$owner:status:OLD", 10003)
        val oldAudio = PodsNotificationKey("$owner:audio:OLD", 10004)
        val hyperPods = PodsNotificationKey("moe.chenxy.hyperpods:audio:OLD", 10004)
        val entries = mutableSetOf(current.presentation.key, oldStatus, oldAudio, legacy, hyperPods)
        val active = sessions.activeKeys(2000)
        entries.removeAll { it !in active && policy.canRetire(it) }
        assertEquals(setOf(current.presentation.key, legacy, hyperPods), entries)
        entries.removeAll { it !in sessions.activeKeys(6000) && policy.canRetire(it) }
        assertEquals(setOf(legacy, hyperPods), entries)
    }
}
