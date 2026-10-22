package moe.chenxy.hyperpods.utils

import moe.chenxy.hyperpods.pods.BatteryComponent
import moe.chenxy.hyperpods.pods.BatteryStatus
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import org.junit.Assert.*
import org.junit.Test

class AudioConnectionNotificationsTest {
    private val owner = "com.hxlh50k.pods.debug"
    private val address = "test-device"
    private val policy = AudioConnectionNotifications(owner)
    private val event = AudioConnectionEvent(100, 1000)

    @Test fun onlyTheExactXiaomiMainProcessOwnsPresentationAndScheduling() {
        assertTrue(AudioConnectionNotifications.ownsPresentation("com.xiaomi.bluetooth"))
        for (process in listOf(null, "", "com.xiaomi.bluetooth:ui", "com.xiaomi.bluetooth:remote",
            "com.xiaomi.bluetooth.other", "com.android.bluetooth", "com.hxlh50k.pods.debug")) {
            assertFalse(AudioConnectionNotifications.ownsPresentation(process))
        }
    }

    @Test fun oneHundredFiftyStatusUpdatesCannotExtendOrReopenTheConnectionDeadline() {
        val presentation = policy.connected(address, event, 1000)!!
        repeat(150) { update ->
            assertEquals("$owner:status:$address", policy.statusKey(address).tag)
            assertEquals(6000L, presentation.deadline)
            assertNull(policy.connected(address, event, 1000L + update * 40))
        }
        assertNull(policy.expire(presentation, 5999))
        assertEquals(presentation.key, policy.expire(presentation, 6000))
        assertNull(policy.expire(presentation, 6001))
        assertNull(policy.connected(address, event, 7000))
    }

    @Test fun batterySettingsComponentOnlyAndProcessRefreshHaveOnlyAStatusKey() {
        // All status payloads share this path; battery validity is not a connection signal.
        repeat(150) {
            assertEquals(PodsNotificationKey("$owner:status:$address", 10003), policy.statusKey(address))
        }
        val restarted = AudioConnectionNotifications(owner)
        assertEquals(policy.statusKey(address), restarted.statusKey(address))
        assertNotEquals(policy.statusKey(address), policy.connected(address, event, 1000)!!.key)
    }

    @Test fun lateDiscoveryUsesOriginalDeadlineAndExpiredOrFutureEventsCannotPresent() {
        val late = policy.connected(address, event, 4500)!!
        assertEquals(1500L, late.remaining(4500))
        assertEquals(2, late.focusTimeoutSeconds(4500))
        assertEquals(1, late.focusTimeoutSeconds(5999))
        assertEquals(0, late.focusTimeoutSeconds(6000))
        assertNull(policy.connected("expired", event, 6000))
        assertNull(policy.connected("future", event, 999))
        assertNull(policy.connected("invalid", AudioConnectionEvent(0, 1000), 1000))
    }

    @Test fun disconnectCancelsPendingAndDuplicateDeliveryButReconnectGetsANewDeadline() {
        val first = policy.connected(address, event, 1000)!!
        policy.cancel(address, 200)
        assertNull(policy.connected(address, event, 1001))
        assertNull(policy.expire(first, 6000))
        val second = policy.connected(address, AudioConnectionEvent(300, 2000), 2000)!!
        assertEquals(7000L, second.deadline)
        assertTrue(policy.cancel(address, 200).isEmpty())
        assertNull(policy.expire(first, 9000))
        assertEquals(second.key, policy.expire(second, 7000))
    }

    @Test fun cancellationBeforeDelayedConnectionDeliverySuppressesThePendingPresentation() {
        policy.cancel(address, 200)
        assertNull(policy.connected(address, event, 1001))
    }

    @Test fun dismissalDoesNotRearmAConsumedConnection() {
        val presentation = policy.connected(address, event, 1000)!!
        assertEquals(presentation.key, policy.dismiss(presentation))
        assertNull(policy.dismiss(presentation))
        assertNull(policy.connected(address, event, 1100))
        assertNull(policy.expire(presentation, 6000))
    }

    @Test fun cancellationTargetsOnlyExactOwnedNotificationKeys() {
        policy.connected(address, event, 1000)
        val owned = listOf(
            PodsNotificationKey("$owner:audio:$address", 10004),
            PodsNotificationKey("$owner:status:$address", 10003),
        )
        assertEquals(owned, policy.cancel(address, 200))
        val unrelated = setOf(
            PodsNotificationKey("HEADSET_CONNECT_NOTIFICATION", 1001),
            PodsNotificationKey("BTHeadset$address", 10003),
            PodsNotificationKey("other.app:audio:$address", 10004),
            policy.statusKey("other-device"),
        )
        val visibleNotifications = (owned + unrelated).toMutableSet()
        policy.cancel(address, 201).forEach(visibleNotifications::remove)
        assertEquals(unrelated, visibleNotifications)
        assertTrue(policy.cancel("other-device", 202).all { it.tag.endsWith(":other-device") })
    }

    @Test fun lowBatteryWarningAfterExpiryDoesNotReopenTheConnection() {
        val presentation = policy.connected(address, event, 1000)!!
        assertEquals(presentation.key, policy.expire(presentation, 6000))
        val params = BatteryParams(left = PodBatteryParams(19, false, true, BatteryStatus.NOT_CHARGING))
        val decisions = LowBatteryReminder.evaluate(params, intArrayOf(BatteryComponent.LEFT), LowBatterySettings()) { 0 }
        val batch = BatteryReminderBatch()
        assertTrue(batch.update(params, LowBatterySettings(), decisions.filter { it.alert }.map { it.component }))
        repeat(150) {
            policy.statusKey(address)
            batch.update(params, LowBatterySettings(), emptyList())
        }
        assertEquals(listOf(BatteryComponent.LEFT), batch.take()!!.second)
        assertNull(batch.take())
        assertNull(policy.connected(address, event, 6500))
    }

    @Test fun directAndLateUuidConnectionDiscoveryClaimTheSameAudioEventOnlyOnce() {
        val sessions = AudioConnectionSessions()
        assertEquals(event, sessions.connected(address, event))
        assertEquals(event, sessions.connected(address, AudioConnectionEvent(101, 1001)))
        assertEquals(event, sessions.claim(address, event))
        assertNull(sessions.claim(address))
        assertNull(sessions.claim(address, event))
    }

    @Test fun aacpRecoveryAndStatusUpdatesCannotRearmTheAudioSession() {
        val sessions = AudioConnectionSessions()
        sessions.connected(address, event)
        val presentation = policy.connected(address, sessions.claim(address)!!, 1000)!!
        repeat(150) {
            policy.statusKey(address)
            assertNull(sessions.claim(address))
        }
        assertEquals(presentation.key, policy.expire(presentation, 6000))
        assertNull(sessions.claim(address))
        sessions.disconnected(address)
        val next = AudioConnectionEvent(200, 8000)
        sessions.connected(address, next)
        assertEquals(next, sessions.claim(address))
        assertNotNull(policy.connected(address, next, 8000))
    }

    @Test fun disconnectInvalidatesPendingDiscoveryAndOldQueuedServiceCallbacks() {
        val sessions = AudioConnectionSessions()
        sessions.connected(address, event)
        sessions.disconnected(address)
        assertNull(sessions.claim(address))
        val next = AudioConnectionEvent(200, 2000)
        sessions.connected(address, next)
        assertNull(sessions.claim(address, event))
        assertEquals(next, sessions.claim(address, next))
    }

    @Test fun delayedControllerCleanupCannotOvertakeAnAlreadyQueuedGenuineReconnect() {
        val sessions = AudioConnectionSessions()
        sessions.connected(address, event)
        policy.connected(address, sessions.claim(address)!!, 1000)
        sessions.disconnected(address)
        val next = AudioConnectionEvent(300, 2000)
        sessions.connected(address, next)
        policy.cancel(address, sessions.cancellationId(address, 200, 400))
        // L2CAP cleanup executes after the newer A2DP callback but before its handler work.
        policy.cancel(address, sessions.cancellationId(address, null, 500))
        assertNotNull(policy.connected(address, sessions.claim(address, next)!!, 2000))
        assertEquals(600L, sessions.cancellationId(address, null, 600))
    }
}
