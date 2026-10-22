package moe.chenxy.hyperpods.utils

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

    /** Controller cleanup must reuse the A2DP teardown ID, not overtake a queued reconnect. */
    @Synchronized
    fun cancellationId(address: String, explicitId: Long?, nowId: Long): Long =
        explicitId?.also { cancellationIds[address] = it } ?: cancellationIds[address] ?: nowId
}

internal data class PodsNotificationKey(val tag: String, val id: Int)

/**
 * The Xiaomi process posts one fixed-deadline connection presentation and a separate silent
 * status entry. Retain consumed event IDs after expiry/cancellation so refreshes cannot rearm it.
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

    private val consumed = mutableMapOf<String, Long>()
    private val active = mutableMapOf<String, Presentation>()

    init {
        require(owner.isNotBlank())
        require(lifetimeMs > 0)
    }

    fun statusKey(address: String) = PodsNotificationKey("$owner:status:$address", 10003)
    private fun connectionKey(address: String) = PodsNotificationKey("$owner:audio:$address", 10004)

    fun connected(address: String, event: AudioConnectionEvent, now: Long): Presentation? {
        if (event.id <= 0 || event.connectedAt < 0 || event.connectedAt > now ||
            event.id <= (consumed[address] ?: 0)) return null
        consumed[address] = event.id
        active.remove(address)
        if (now - event.connectedAt >= lifetimeMs) return null
        return Presentation(address, event, event.connectedAt + lifetimeMs, connectionKey(address))
            .also { active[address] = it }
    }

    fun expire(presentation: Presentation, now: Long): PodsNotificationKey? {
        if (active[presentation.address] != presentation || presentation.remaining(now) > 0) return null
        active.remove(presentation.address)
        return presentation.key
    }

    fun dismiss(presentation: Presentation): PodsNotificationKey? {
        if (active[presentation.address] != presentation) return null
        active.remove(presentation.address)
        return presentation.key
    }

    /** Ignore an obsolete teardown delivered after a newer genuine audio connection. */
    fun cancel(address: String, throughEventId: Long): List<PodsNotificationKey> {
        if (throughEventId < (consumed[address] ?: 0)) return emptyList()
        consumed[address] = throughEventId
        active.remove(address)
        return listOf(connectionKey(address), statusKey(address))
    }

    companion object {
        fun ownsPresentation(processName: String?): Boolean =
            processName == HyperPodsBroadcasts.XIAOMI_BLUETOOTH

        const val CONNECTION_LIFETIME_MS = 5000L
        const val EXTRA_EVENT_ID = "audioConnectionEventId"
        const val EXTRA_CONNECTED_AT = "audioConnectedAtElapsed"
    }
}
