package moe.chenxy.hyperpods.utils

import android.content.Context
import android.content.Intent
import android.os.SystemClock

/** IPC client only. There is intentionally no notification publication or fallback. */
internal class BatteryIslandRenderer(private val context: Context) {
    data class Content(
        val title: String,
        val reason: String,
        val left: Int?,
        val right: Int?,
        val settings: BatteryNotificationSettings,
        val modelNumber: String? = null,
    )

    data class Presentation(
        val token: String,
        val audioEvent: Long,
        val submittedAt: Long,
        var firstVisibleAt: Long? = null,
        var lastContent: Content? = null,
        var revision: Int = -1,
    ) {
        fun deadline(): Long = firstVisibleAt?.plus(BatteryIslandContract.VISIBLE_MS)
            ?: submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS
    }

    fun post(presentation: Presentation, content: Content, first: Boolean) {
        val now = SystemClock.elapsedRealtime()
        check(presentation.deadline() > now) { "Refusing an expired battery presentation" }
        check(first == (presentation.revision == -1)) { "Invalid battery presentation sequence" }
        val payload = NativeBatteryIslandPayload(
            presentation.token, presentation.audioEvent, presentation.submittedAt, presentation.revision + 1,
            content.left ?: -1, content.right ?: -1, content.settings.lowThreshold,
            content.settings.mediumThreshold, content.modelNumber,
        )
        require(payload.valid(now)) { "Invalid native battery payload" }
        HyperPodsBroadcasts.send(context, payload.intent(BatteryIslandContract.ACTION_PRESENT),
            HyperPodsBroadcasts.SYSTEM_UI)
        presentation.revision = payload.revision
        presentation.lastContent = content
    }

    fun cancel(presentation: Presentation) {
        HyperPodsBroadcasts.send(context, Intent(BatteryIslandContract.ACTION_CANCEL)
            .putExtra(BatteryIslandContract.SCHEMA, BatteryIslandContract.VERSION)
            .putExtra(BatteryIslandContract.PRESENTATION_TOKEN, presentation.token)
            .putExtra(BatteryIslandContract.AUDIO_EVENT, presentation.audioEvent),
            HyperPodsBroadcasts.SYSTEM_UI)
    }
}
