package moe.chenxy.hyperpods.utils

import android.content.Intent
import android.os.Bundle

/** Only bounded primitives cross the host/SystemUI boundary; native objects stay in SystemUI. */
internal data class NativeBatteryIslandPayload(
    val token: String,
    val audioEvent: Long,
    val submittedAt: Long,
    val revision: Int,
    val left: Int,
    val right: Int,
    val low: Int,
    val medium: Int,
    val model: String?,
) {
    val settings get() = BatteryNotificationSettings(low, medium)
    val leftLevel get() = left.takeIf { it >= 0 }
    val rightLevel get() = right.takeIf { it >= 0 }

    fun valid(now: Long): Boolean = BatteryIslandContract.validToken(token) && audioEvent > 0 &&
        submittedAt in 0..now && now - submittedAt <
        BatteryIslandContract.STARTUP_TIMEOUT_MS + BatteryIslandContract.VISIBLE_MS &&
        revision in 0..10_000 && left in -1..100 && right in -1..100 && settings.valid &&
        (model == null || model.matches(Regex("[A-Za-z0-9,._-]{1,32}")))

    fun intent(action: String): Intent = Intent(action).apply {
        putExtra(BatteryIslandContract.SCHEMA, BatteryIslandContract.VERSION)
        putExtra(BatteryIslandContract.PRESENTATION_TOKEN, token)
        putExtra(BatteryIslandContract.AUDIO_EVENT, audioEvent)
        putExtra(BatteryIslandContract.SUBMITTED_AT, submittedAt)
        putExtra(BatteryIslandContract.REVISION, revision)
        putExtra(BatteryIslandContract.LEFT, left)
        putExtra(BatteryIslandContract.RIGHT, right)
        putExtra(BatteryIslandContract.LOW, low)
        putExtra(BatteryIslandContract.MEDIUM, medium)
        model?.let { putExtra(BatteryIslandContract.MODEL, it) }
    }

    companion object {
        private val required = mapOf(
            BatteryIslandContract.SCHEMA to Int::class.javaObjectType,
            BatteryIslandContract.PRESENTATION_TOKEN to String::class.java,
            BatteryIslandContract.AUDIO_EVENT to Long::class.javaObjectType,
            BatteryIslandContract.SUBMITTED_AT to Long::class.javaObjectType,
            BatteryIslandContract.REVISION to Int::class.javaObjectType,
            BatteryIslandContract.LEFT to Int::class.javaObjectType,
            BatteryIslandContract.RIGHT to Int::class.javaObjectType,
            BatteryIslandContract.LOW to Int::class.javaObjectType,
            BatteryIslandContract.MEDIUM to Int::class.javaObjectType,
        )

        @Suppress("DEPRECATION")
        fun read(extras: Bundle?, now: Long): NativeBatteryIslandPayload? = runCatching {
            extras ?: return null
            if (!extras.keySet().all { it in required || it == BatteryIslandContract.MODEL } ||
                required.any { (key, type) -> extras.get(key)?.javaClass != type } ||
                extras.getInt(BatteryIslandContract.SCHEMA) != BatteryIslandContract.VERSION ||
                (extras.containsKey(BatteryIslandContract.MODEL) &&
                    extras.get(BatteryIslandContract.MODEL) !is String)) return null
            NativeBatteryIslandPayload(
                requireNotNull(extras.getString(BatteryIslandContract.PRESENTATION_TOKEN)),
                extras.getLong(BatteryIslandContract.AUDIO_EVENT),
                extras.getLong(BatteryIslandContract.SUBMITTED_AT),
                extras.getInt(BatteryIslandContract.REVISION),
                extras.getInt(BatteryIslandContract.LEFT), extras.getInt(BatteryIslandContract.RIGHT),
                extras.getInt(BatteryIslandContract.LOW), extras.getInt(BatteryIslandContract.MEDIUM),
                extras.getString(BatteryIslandContract.MODEL),
            ).takeIf { it.valid(now) }
        }.getOrNull()
    }
}

/** Submission and observed visibility are deliberately different clocks. */
internal class NativeBatteryIslandLease(initial: NativeBatteryIslandPayload) {
    @Volatile var payload = initial
        private set
    @Volatile var visibleAt: Long? = null
        private set
    val deadline get() = visibleAt?.plus(BatteryIslandContract.VISIBLE_MS)
        ?: payload.submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS

    fun owns(token: String?, event: Long) = payload.token == token && payload.audioEvent == event

    fun preparing(now: Long) = visibleAt == null && now >= payload.submittedAt && now < deadline

    fun update(next: NativeBatteryIslandPayload, now: Long): Boolean {
        if (!next.valid(now) || !owns(next.token, next.audioEvent) ||
            next.submittedAt != payload.submittedAt || next.revision <= payload.revision || now >= deadline) return false
        payload = next
        return true
    }

    fun visible(now: Long): Boolean {
        if (visibleAt != null || now < payload.submittedAt || now >= deadline) return false
        visibleAt = now
        return true
    }
}
