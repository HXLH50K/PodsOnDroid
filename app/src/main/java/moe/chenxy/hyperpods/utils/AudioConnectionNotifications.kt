package moe.chenxy.hyperpods.utils

import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import org.json.JSONObject

/** Monotonic timestamps originate at an A2DP transition, never at an AACP or UI refresh. */
internal data class AudioConnectionEvent(val id: Long, val connectedAt: Long)

/** Shared by direct A2DP handling and late UUID discovery in the Bluetooth process. */
internal class AudioConnectionSessions {
    private data class Session(val event: AudioConnectionEvent, var claimed: Boolean = false)
    private val sessions = mutableMapOf<String, Session>()
    private val cancellationIds = mutableMapOf<String, Long>()

    @Synchronized
    fun connected(address: String, event: AudioConnectionEvent): AudioConnectionEvent =
        sessions.getOrPut(address) { Session(event) }.event

    @Synchronized
    fun claim(address: String, expected: AudioConnectionEvent? = null): AudioConnectionEvent? {
        val session = sessions[address] ?: return null
        if (session.claimed || expected != null && session.event != expected) return null
        session.claimed = true
        cancellationIds.remove(address)
        return session.event
    }

    @Synchronized
    fun disconnected(address: String) {
        sessions.remove(address)
    }

    @Synchronized
    fun current(address: String): AudioConnectionEvent? = sessions[address]?.event

    /** Controller cleanup must reuse the A2DP teardown ID, not overtake a queued reconnect. */
    @Synchronized
    fun cancellationId(address: String, explicitId: Long?, nowId: Long): Long =
        explicitId?.also { cancellationIds[address] = it } ?: cancellationIds[address] ?: nowId
}

internal data class PodsNotificationKey(val tag: String, val id: Int)

/**
 * Only a real audio event creates a presentation. Battery refreshes may update its content,
 * never its deadline. Retain consumed event IDs after expiry/cancellation to prevent replay.
 */
internal class AudioConnectionNotifications(
    private val owner: String,
    private val lifetimeMs: Long = CONNECTION_LIFETIME_MS,
) {
    data class Presentation(
        val address: String,
        val event: AudioConnectionEvent,
        val deadline: Long,
        val key: PodsNotificationKey,
    ) {
        fun remaining(now: Long): Long = (deadline - now).coerceAtLeast(0)
        fun focusTimeoutSeconds(now: Long): Int = ((remaining(now) + 999) / 1000).toInt()
    }

    data class BatteryText(val left: String = "--", val right: String = "--", val case: String = "--") {
        companion object {
            fun from(params: BatteryParams): BatteryText {
                fun text(value: PodBatteryParams?): String {
                    val level = PodsIslandData.batteryText(value)
                    return level + if (level != "--" && value?.isCharging == true) " ⚡" else ""
                }
                return BatteryText(text(params.left), text(params.right), text(params.case))
            }
        }
    }

    data class Update(val presentation: Presentation, val battery: BatteryText, val firstFloat: Boolean) {
        fun island(content: String, now: Long): JSONObject = JSONObject()
            .put("islandProperty", 1)
            .put("islandTimeout", presentation.focusTimeoutSeconds(now))
            .put("dismissIsland", true)
            .put("maxSize", false)
            .put("bigIslandArea", JSONObject().put("imageTextInfoLeft", JSONObject()
                .put("type", 1)
                .put("picInfo", JSONObject().put("type", 1).put("pic", "miui.focus.pic_mark_v2"))
                .put("textInfo", JSONObject().put("title", content))))
            .put("smallIslandArea", JSONObject().put("picInfo", JSONObject()
                .put("type", 1).put("pic", "miui.focus.pic_mark_v2")))

        fun configureFocus(focus: JSONObject) {
            // HyperFocus 1.10 ignores its islandFirstFloat argument; write the wire flags explicitly.
            focus.getJSONObject("param_v2").apply {
                put("updatable", true)
                put("islandFirstFloat", firstFloat)
                put("enableFloat", firstFloat)
            }
        }
    }

    private val consumed = mutableMapOf<String, Long>()
    private val active = mutableMapOf<String, Update>()

    init {
        require(owner.isNotBlank())
        require(lifetimeMs > 0)
    }

    private fun legacyStatusKey(address: String) = PodsNotificationKey("$owner:status:$address", 10003)
    private fun connectionKey(address: String) = PodsNotificationKey("$owner:audio:$address", 10004)

    fun connected(address: String, event: AudioConnectionEvent, now: Long): Update? {
        if (event.id <= 0 || event.connectedAt < 0 || event.connectedAt > now ||
            event.id <= (consumed[address] ?: 0)) return null
        consumed[address] = event.id
        active.remove(address)
        if (now - event.connectedAt >= lifetimeMs) return null
        return Update(Presentation(address, event, event.connectedAt + lifetimeMs, connectionKey(address)),
            BatteryText(), firstFloat = true)
            .also { active[address] = it }
    }

    fun refresh(address: String, params: BatteryParams, now: Long): Update? {
        val current = active[address] ?: return null
        if (current.presentation.remaining(now) == 0L) return null
        val battery = BatteryText.from(params)
        if (battery == current.battery) return null
        return Update(current.presentation, battery, firstFloat = false).also { active[address] = it }
    }

    fun activeKeys(now: Long): Set<PodsNotificationKey> = active.values
        .filter { it.presentation.remaining(now) > 0 }.map { it.presentation.key }.toSet()

    fun expire(presentation: Presentation, now: Long): PodsNotificationKey? {
        if (active[presentation.address]?.presentation != presentation || presentation.remaining(now) > 0) return null
        active.remove(presentation.address)
        return presentation.key
    }

    fun dismiss(presentation: Presentation): PodsNotificationKey? {
        if (active[presentation.address]?.presentation != presentation) return null
        active.remove(presentation.address)
        return presentation.key
    }

    /** Ignore an obsolete teardown delivered after a newer genuine audio connection. */
    fun cancel(address: String, throughEventId: Long): List<PodsNotificationKey> {
        if (throughEventId < (consumed[address] ?: 0)) return emptyList()
        consumed[address] = throughEventId
        active.remove(address)
        return listOf(connectionKey(address), legacyStatusKey(address))
    }

    companion object {
        fun ownsPresentation(processName: String?): Boolean =
            processName == HyperPodsBroadcasts.XIAOMI_BLUETOOTH

        const val CONNECTION_LIFETIME_MS = 5000L
        const val EXTRA_EVENT_ID = "audioConnectionEventId"
        const val EXTRA_CONNECTED_AT = "audioConnectedAtElapsed"
    }
}
