package moe.chenxy.hyperpods.utils

import moe.chenxy.hyperpods.pods.BatteryComponent
import moe.chenxy.hyperpods.pods.BatteryStatus
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AudioConnectionNotificationsTest {
    private val owner = "com.hxlh50k.pods.debug"
    private val address = "test-device"
    private val policy = AudioConnectionNotifications(owner)
    private val event = AudioConnectionEvent(100, 1000)
    private fun battery(level: Int = 80) = BatteryParams(
        left = PodBatteryParams(level, false, true, BatteryStatus.NOT_CHARGING),
        right = PodBatteryParams(75, true, true, BatteryStatus.CHARGING),
        case = PodBatteryParams(60, false, true, BatteryStatus.NOT_CHARGING))

    @Test fun onlyTheExactXiaomiMainProcessOwnsPresentationAndScheduling() {
        assertTrue(AudioConnectionNotifications.ownsPresentation("com.xiaomi.bluetooth"))
        for (process in listOf(null, "", "com.xiaomi.bluetooth:ui", "com.xiaomi.bluetooth:remote",
            "com.xiaomi.bluetooth.other", "com.android.bluetooth", owner)) {
            assertFalse(AudioConnectionNotifications.ownsPresentation(process))
        }
    }

    @Test fun oneHundredFiftyPostExpiryUpdatesProduceNoPresentationEvenBeforeTimerRuns() {
        val first = policy.connected(address, event, 1000)!!
        repeat(150) { update ->
            assertNull(policy.refresh(address, battery(update % 101), 6000L + update))
            assertNull(policy.connected(address, event, 6000L + update))
            assertTrue(policy.activeKeys(6000L + update).isEmpty())
        }
        assertEquals(first.presentation.key, policy.expire(first.presentation, 6200))
        assertNull(policy.expire(first.presentation, 6201))
        repeat(150) { assertNull(policy.refresh(address, battery(), 7000L + it)) }
    }

    @Test fun lateBatteryUpdatesTheSameKeyWithoutReplayingOrExtendingTheOriginalDeadline() {
        val initial = policy.connected(address, event, 1000)!!
        val late = policy.refresh(address, battery(), 4500)!!
        assertTrue(initial.firstFloat)
        assertFalse(late.firstFloat)
        assertSame(initial.presentation, late.presentation)
        assertEquals(PodsNotificationKey("$owner:audio:$address", 10004), late.presentation.key)
        assertEquals(6000L, late.presentation.deadline)
        assertEquals(1500L, late.presentation.remaining(4500))
        assertEquals(AudioConnectionNotifications.BatteryText("80%", "75% ⚡", "60%"), late.battery)
        repeat(150) { assertNull(policy.refresh(address, battery(), 4501L + it)) }
        assertEquals(initial.presentation.key, policy.expire(initial.presentation, 6000))
    }

    @Test fun emittedIslandPayloadsAreBoundedAndUpdatesExplicitlyDisableReplay() {
        val initial = policy.connected(address, event, 1000)!!
        val updated = policy.refresh(address, battery(), 4900)!!
        for ((output, now) in listOf(initial to 1000L, updated to 4900L)) {
            val focus = JSONObject().put("param_v2", JSONObject())
            output.configureFocus(focus)
            val flags = focus.getJSONObject("param_v2")
            assertTrue(flags.getBoolean("updatable"))
            assertEquals(output.firstFloat, flags.getBoolean("islandFirstFloat"))
            assertEquals(output.firstFloat, flags.getBoolean("enableFloat"))
            val island = output.island("Left: 80% | Right: 75% | Case: 60%", now)
            assertTrue(island.getBoolean("dismissIsland"))
            assertTrue(island.getInt("islandTimeout") in 1..5)
            assertEquals(output.presentation.focusTimeoutSeconds(now), island.getInt("islandTimeout"))
            assertEquals("Left: 80% | Right: 75% | Case: 60%",
                island.getJSONObject("bigIslandArea").getJSONObject("imageTextInfoLeft")
                    .getJSONObject("textInfo").getString("title"))
            assertFalse(output.presentation.key.tag.contains(":status:"))
        }
        assertNull(policy.refresh(address, battery(1), 6000))
    }

    @Test fun repeatedOrNonVisualInitialDataChangesDoNotEmitRedundantUpdates() {
        policy.connected(address, event, 1000)
        val params = battery()
        assertNotNull(policy.refresh(address, params, 1001))
        repeat(150) {
            params.left!!.isInCase = it % 2 == 0
            assertNull(policy.refresh(address, params, 1002L + it))
        }
        params.left!!.battery = 81
        assertEquals("81%", policy.refresh(address, params, 1400)!!.battery.left)
    }

    @Test fun unknownReadingsStayHonestAndAvailableCaseDoesNotRequireAnEarInTheCase() {
        val initial = policy.connected(address, event, 1000)!!
        assertEquals(AudioConnectionNotifications.BatteryText(), initial.battery)
        assertNull(policy.refresh(address, BatteryParams(), 1001))
        val params = BatteryParams(
            PodBatteryParams(101, true, true),
            PodBatteryParams(50, true, false),
            PodBatteryParams(0, true, true))
        val updated = policy.refresh(address, params, 1002)!!
        assertEquals(AudioConnectionNotifications.BatteryText("--", "--", "0% ⚡"), updated.battery)
        params.case!!.battery = 55
        assertEquals("0% ⚡", updated.battery.case)
        assertEquals("55% ⚡", policy.refresh(address, params, 1003)!!.battery.case)
    }

    @Test fun batterySettingsCaseAndProcessRecoveryHaveNoAuthorityToCreateStatusPayloads() {
        repeat(150) { assertNull(policy.refresh(address, battery(it % 101), 1000L + it)) }
        val restarted = AudioConnectionNotifications(owner)
        repeat(150) { assertNull(restarted.refresh(address, battery(), 7000L + it)) }
        assertTrue(restarted.activeKeys(8000).isEmpty())
    }

    @Test fun lateDiscoveryUsesOriginalDeadlineAndExpiredOrFutureEventsCannotPresent() {
        val late = policy.connected(address, event, 4500)!!.presentation
        assertEquals(1500L, late.remaining(4500))
        assertEquals(2, late.focusTimeoutSeconds(4500))
        assertEquals(1, late.focusTimeoutSeconds(5999))
        assertEquals(0, late.focusTimeoutSeconds(6000))
        assertNull(policy.connected("expired", event, 6000))
        assertNull(policy.connected("future", event, 999))
        assertNull(policy.connected("invalid", AudioConnectionEvent(0, 1000), 1000))
    }

    @Test fun disconnectCancelsPendingAndDuplicateDeliveryButReconnectGetsANewDeadline() {
        val first = policy.connected(address, event, 1000)!!.presentation
        policy.cancel(address, 200)
        assertNull(policy.connected(address, event, 1001))
        assertNull(policy.refresh(address, battery(), 1001))
        assertNull(policy.expire(first, 6000))
        val second = policy.connected(address, AudioConnectionEvent(300, 2000), 2000)!!
        assertTrue(second.firstFloat)
        assertEquals(7000L, second.presentation.deadline)
        assertTrue(policy.cancel(address, 200).isEmpty())
        assertNull(policy.expire(first, 9000))
        assertEquals(second.presentation.key, policy.expire(second.presentation, 7000))
    }

    @Test fun cancellationBeforeDelayedConnectionDeliverySuppressesThePendingPresentation() {
        policy.cancel(address, 200)
        assertNull(policy.connected(address, event, 1001))
    }

    @Test fun dismissalDoesNotRearmAConsumedConnection() {
        val presentation = policy.connected(address, event, 1000)!!.presentation
        assertEquals(presentation.key, policy.dismiss(presentation))
        assertNull(policy.dismiss(presentation))
        assertNull(policy.refresh(address, battery(), 1100))
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
            PodsNotificationKey("$owner:status:other-device", 10003),
        )
        val visibleNotifications = (owned + unrelated).toMutableSet()
        policy.cancel(address, 201).forEach(visibleNotifications::remove)
        assertEquals(unrelated, visibleNotifications)
    }

    @Test fun configuredLowBatteryWarningAfterExpiryDoesNotCreateAStatusOrConnection() {
        val presentation = policy.connected(address, event, 1000)!!.presentation
        assertEquals(presentation.key, policy.expire(presentation, 6000))
        val params = battery(19)
        val decisions = LowBatteryReminder.evaluate(params, intArrayOf(BatteryComponent.LEFT), LowBatterySettings()) { 0 }
        val batch = BatteryReminderBatch()
        assertTrue(batch.update(params, LowBatterySettings(), decisions.filter { it.alert }.map { it.component }))
        repeat(150) {
            assertNull(policy.refresh(address, params, 6500L + it))
            batch.update(params, LowBatterySettings(), emptyList())
        }
        assertEquals(listOf(BatteryComponent.LEFT), batch.take()!!.second)
        assertNull(batch.take())
        assertNull(policy.connected(address, event, 6800))
        val disabled = LowBatterySettings(earsEnabled = false, caseEnabled = false)
        assertTrue(LowBatteryReminder.evaluate(params, intArrayOf(BatteryComponent.LEFT), disabled) { 0 }.none { it.alert })
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
        val presentation = policy.connected(address, sessions.claim(address)!!, 1000)!!.presentation
        repeat(150) {
            assertNull(policy.refresh(address, battery(), 6000L + it))
            assertNull(sessions.claim(address))
        }
        assertEquals(presentation.key, policy.expire(presentation, 6200))
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
        policy.cancel(address, sessions.cancellationId(address, null, 500))
        assertNotNull(policy.connected(address, sessions.claim(address, next)!!, 2000))
        assertEquals(600L, sessions.cancellationId(address, null, 600))
    }
}
