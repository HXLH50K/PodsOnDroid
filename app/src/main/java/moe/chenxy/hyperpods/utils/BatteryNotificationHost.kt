package moe.chenxy.hyperpods.utils

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.BatteryComponent
import moe.chenxy.hyperpods.utils.BatteryNotificationSession.Category
import moe.chenxy.hyperpods.utils.BatteryNotificationSession.Reason
import moe.chenxy.hyperpods.utils.SystemApisUtils.cancelAsUser
import moe.chenxy.hyperpods.utils.data.BatteryParams
import java.util.UUID

/** Serial notification arbiter in Xiaomi Bluetooth's main process; it owns no audio sockets. */
@SuppressLint("MissingPermission")
internal class BatteryNotificationHost(private val context: Context) {
    private class Session(
        val device: BluetoothDevice,
        val event: AudioConnectionEvent,
        val reducer: BatteryNotificationSession,
        var connectionPending: Boolean,
    ) {
        var controlGeneration: Long? = null
        var lastObservation = -1L
        var modelNumber: String? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private val resources = context.createPackageContext(BuildConfig.APPLICATION_ID, 0).resources
    private val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
    private val renderer = BatteryIslandRenderer(context)
    private val continuity = context.getSharedPreferences("pods_battery_audio_generation", Context.MODE_PRIVATE)
    private val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    private var lastEvent = if (bootCount >= 0 && continuity.getInt("boot", -2) == bootCount)
        continuity.getLong("event", 0L) else 0L
    private var session: Session? = null
    private var active: BatteryIslandRenderer.Presentation? = null
    private var reasonText = ""
    private var preparation: Runnable? = null
    private var expiry: Runnable? = null
    private var batch: Runnable? = null
    private var refresh: Runnable? = null
    private val pendingReasons = linkedSetOf<Reason>()

    fun register() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Register the battery owner on the main thread" }
        if (bootCount < 0) Log.w(TAG, "Boot identity unavailable; connection continuity cannot be persisted")
        retireOwned()
        val filter = IntentFilter(CONNECTED).apply {
            addAction(UPDATE)
            addAction(CANCEL)
            addAction(LEGACY_TOAST)
            addAction(BatteryIslandContract.ACTION_VISIBLE)
            addAction(BatteryIslandContract.ACTION_DISMISSED)
            addAction(BatteryIslandContract.ACTION_FAILED)
            addAction(BatteryIslandContract.ACTION_CONTROL_GAP)
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(receiveContext: Context, intent: Intent) {
                val nativeReply = intent.action in setOf(BatteryIslandContract.ACTION_VISIBLE,
                    BatteryIslandContract.ACTION_DISMISSED, BatteryIslandContract.ACTION_FAILED)
                if (nativeReply) {
                    val uid = runCatching {
                        context.packageManager.getPackageUid(HyperPodsBroadcasts.SYSTEM_UI, 0)
                    }.getOrDefault(-1)
                    if (!BatteryIslandContract.trustedIdentity(sentFromUid, sentFromPackage, uid,
                            setOf(HyperPodsBroadcasts.SYSTEM_UI, BatteryIslandContract.SYSTEM_UI_PLUGIN)) ||
                        intent.`package` != HyperPodsBroadcasts.XIAOMI_BLUETOOTH ||
                        intent.getIntExtra(BatteryIslandContract.SCHEMA, -1) != BatteryIslandContract.VERSION) return
                } else if (!HyperPodsBroadcasts.isTrusted(context, this, HyperPodsBroadcasts.BLUETOOTH)) return
                try {
                    when (intent.action) {
                        CONNECTED -> connect(intent)
                        UPDATE -> update(intent)
                        CANCEL -> disconnect(intent)
                        LEGACY_TOAST -> retireOwned()
                        BatteryIslandContract.ACTION_CONTROL_GAP -> controlGap(intent)
                        BatteryIslandContract.ACTION_VISIBLE -> visible(intent)
                        BatteryIslandContract.ACTION_DISMISSED, BatteryIslandContract.ACTION_FAILED -> {
                            if (intent.getStringExtra(BatteryIslandContract.PRESENTATION_TOKEN) == active?.token &&
                                intent.getLongExtra(BatteryIslandContract.AUDIO_EVENT, -1) == active?.audioEvent) {
                                finishPresentation(if (intent.action == BatteryIslandContract.ACTION_FAILED)
                                    "native_failed" else "native_dismissed")
                            }
                        }
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Battery event failed; no legacy notification fallback", error)
                }
            }
        }, filter, null, handler, Context.RECEIVER_EXPORTED)
    }

    private fun connect(intent: Intent) {
        val device = intent.getParcelableExtra("device", BluetoothDevice::class.java) ?: return
        val event = readEvent(intent)
        val now = SystemClock.elapsedRealtime()
        if (event.id <= 0 || event.connectedAt !in 0..now || event.id <= lastEvent) return
        lastEvent = event.id
        if (bootCount >= 0) continuity.edit().putInt("boot", bootCount).putLong("event", lastEvent).apply()
        endSession("new_audio_session")
        val timely = now - event.connectedAt < INITIAL_DEADLINE_MS
        session = Session(device, event, BatteryNotificationSession(suppressAll = !timely), timely)
        if (timely) {
            schedulePreparation()
            Log.i(TAG, "Audio connection received; waiting for useful ear battery data")
        } else Log.w(TAG, "Late audio event suppressed; waiting for the next genuine connection")
    }

    private fun update(intent: Intent) {
        val device = intent.getParcelableExtra("device", BluetoothDevice::class.java) ?: return
        val params = intent.getParcelableExtra("batteryParams", BatteryParams::class.java) ?: return
        val settings = intent.getParcelableExtra(BatteryIslandContract.SETTINGS, BatteryNotificationSettings::class.java)
        if (settings == null || !settings.valid) {
            Log.w(TAG, "Rejecting missing or invalid battery notification configuration")
            return
        }
        val event = readEvent(intent)
        var current = session
        if (current == null) {
            // A snapshot is not evidence of a newly observed audio connection.
            current = Session(device, event, BatteryNotificationSession(settings, suppressAll = true), false)
            session = current
            Log.i(TAG, "Resumed connected state without event continuity; reminders suppressed")
        }
        if (current.device != device || current.event.id != event.id) return
        val observedAt = intent.getLongExtra(BatteryIslandContract.OBSERVED_AT, -1)
        if (observedAt < 0 || observedAt <= current.lastObservation) return
        current.lastObservation = observedAt
        intent.getStringExtra("modelNumber")?.let { model ->
            if (PodsIslandData.modelForIcons(model) != null) current.modelNumber = model
        }
        if (settings != current.reducer.snapshot().settings) {
            cancelBatch()
            check(current.reducer.settingsChanged(settings))
        }
        val generation = intent.getLongExtra(BatteryIslandContract.CONTROL_GENERATION, -1)
        if (current.controlGeneration != null && current.controlGeneration != generation) {
            cancelBatch()
            current.reducer.controlGap()
        }
        current.controlGeneration = generation
        val reasons = current.reducer.observe(params, intent.getIntArrayExtra("freshComponents") ?: intArrayOf())
        if (current.connectionPending) attemptConnection()
        queueRefresh()
        if (reasons.isNotEmpty()) {
            pendingReasons.addAll(reasons)
            if (batch == null) {
                val expected = current
                val work = Runnable {
                    batch = null
                    val merged = pendingReasons.toSet()
                    pendingReasons.clear()
                    if (session !== expected || merged.isEmpty()) return@Runnable
                    val eligible = merged.filter { reason ->
                        val ear = if (reason.component == BatteryComponent.LEFT) expected.reducer.left else expected.reducer.right
                        val config = expected.reducer.snapshot().settings
                        val threshold = if (reason.category == Category.LOW) config.lowThreshold else config.mediumThreshold
                        ear.level != null && ear.level <= threshold && ear.charging == false
                    }.toSet()
                    if (eligible.isNotEmpty()) thresholdReport(eligible)
                }
                batch = work
                if (!handler.postDelayed(work, BATCH_MS)) {
                    cancelBatch()
                    Log.w(TAG, "Unable to enqueue threshold report; event remains consumed")
                }
            }
        }
        retireOwned()
    }

    private fun thresholdReport(reasons: Set<Reason>) {
        val current = session ?: return
        val ears = reasons.map { it.component }.toSet()
        val side = resources.getString(when {
            ears.size > 1 -> R.string.battery_island_both_ears
            BatteryComponent.LEFT in ears -> R.string.battery_island_left_ear
            else -> R.string.battery_island_right_ear
        })
        val level = resources.getString(if (reasons.any { it.category == Category.LOW })
            R.string.battery_island_low else R.string.battery_island_medium)
        val reason = "$side · $level"
        if (active != null && SystemClock.elapsedRealtime() >= requireNotNull(active).deadline()) {
            finishPresentation("expired_before_threshold")
        }
        if (active != null) {
            reasonText = reason
            queueRefresh()
        } else {
            current.connectionPending = false
            clearPreparation()
            show(reason)
        }
    }

    private fun attemptConnection() {
        val current = session ?: return
        if (!current.connectionPending) return
        val age = SystemClock.elapsedRealtime() - current.event.connectedAt
        val left = current.reducer.left.level
        val right = current.reducer.right.level
        val useful = left != null || right != null
        if (left != null && right != null || useful && age >= INITIAL_PARTIAL_MS) {
            current.connectionPending = false
            clearPreparation()
            show(resources.getString(R.string.battery_island_connected))
        } else if (age >= INITIAL_DEADLINE_MS) {
            current.connectionPending = false
            clearPreparation()
            Log.w(TAG, "initial_battery_unavailable: no useful connection presentation")
        } else schedulePreparation()
    }

    private fun schedulePreparation() {
        val current = session ?: return
        clearPreparation()
        val now = SystemClock.elapsedRealtime()
        val age = now - current.event.connectedAt
        val due = current.event.connectedAt + if (age < INITIAL_PARTIAL_MS) INITIAL_PARTIAL_MS else INITIAL_DEADLINE_MS
        val work = Runnable {
            preparation = null
            if (session === current) attemptConnection()
        }
        preparation = work
        if (!handler.postDelayed(work, (due - now).coerceAtLeast(0))) {
            current.connectionPending = false
            preparation = null
            Log.w(TAG, "Unable to schedule connection battery readiness")
        }
    }

    private fun show(reason: String) {
        val current = session ?: return
        finishPresentation("superseded")
        val presentation = BatteryIslandRenderer.Presentation(
            UUID.randomUUID().toString(), current.event.id, SystemClock.elapsedRealtime(),
        )
        active = presentation
        reasonText = reason
        try {
            renderer.post(presentation, content(), first = true)
            scheduleExpiry(presentation)
            Log.i(TAG, "Native island IPC submitted; waiting for dual-ear pre-draw acknowledgment")
        } catch (error: Exception) {
            Log.e(TAG, "Battery presentation failed; event will not replay", error)
            finishPresentation("post_failed")
        }
    }

    private fun visible(intent: Intent) {
        val presentation = active ?: return
        if (intent.getStringExtra(BatteryIslandContract.PRESENTATION_TOKEN) != presentation.token ||
            intent.getLongExtra(BatteryIslandContract.AUDIO_EVENT, -1) != presentation.audioEvent ||
            presentation.firstVisibleAt != null) return
        val at = intent.getLongExtra(BatteryIslandContract.VISIBLE_AT, -1)
        val now = SystemClock.elapsedRealtime()
        if (at < presentation.submittedAt || at > now ||
            at > presentation.submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS) {
            Log.w(TAG, "Rejecting invalid or late native battery visibility timestamp")
            return
        }
        presentation.firstVisibleAt = at
        scheduleExpiry(presentation)
        Log.i(TAG, "Native dual-ear content visible; five-second lifetime started")
    }

    private fun scheduleExpiry(presentation: BatteryIslandRenderer.Presentation) {
        expiry?.let(handler::removeCallbacks)
        val work = Runnable {
            if (active === presentation) {
                finishPresentation(if (presentation.firstVisibleAt == null) "visibility_unconfirmed" else "visible_timeout")
            }
        }
        expiry = work
        if (!handler.postDelayed(work, (presentation.deadline() - SystemClock.elapsedRealtime()).coerceAtLeast(0))) {
            Log.w(TAG, "Unable to schedule battery expiry; cancelling immediately")
            finishPresentation("schedule_failed")
        }
    }

    private fun queueRefresh() {
        if (active == null || refresh != null) return
        val presentation = requireNotNull(active)
        val work = Runnable {
            refresh = null
            if (active !== presentation) return@Runnable
            if (SystemClock.elapsedRealtime() >= presentation.deadline()) {
                finishPresentation("expired_before_refresh")
                return@Runnable
            }
            try {
                val next = content()
                if (next != presentation.lastContent) renderer.post(presentation, next, first = false)
            } catch (error: Exception) {
                Log.e(TAG, "Battery update failed; no persistent fallback", error)
                finishPresentation("update_failed")
            }
        }
        refresh = work
        if (!handler.postDelayed(work, 100)) {
            refresh = null
            Log.w(TAG, "Unable to enqueue battery presentation refresh")
        }
    }

    private fun content(): BatteryIslandRenderer.Content {
        val current = requireNotNull(session)
        return BatteryIslandRenderer.Content(
            PodsIslandData.displayName(current.device.alias, current.device.name), reasonText,
            current.reducer.left.level, current.reducer.right.level, current.reducer.snapshot().settings,
            current.modelNumber,
        )
    }

    private fun controlGap(intent: Intent) {
        val current = session ?: return
        if (intent.getParcelableExtra("device", BluetoothDevice::class.java) != current.device ||
            intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, -1) != current.event.id) return
        current.reducer.controlGap()
        cancelBatch()
        queueRefresh()
    }

    private fun disconnect(intent: Intent) {
        val current = session ?: return
        val id = intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, 0)
        if (intent.getParcelableExtra("device", BluetoothDevice::class.java) != current.device || id < current.event.id) return
        endSession("audio_disconnected")
        retireOwned()
    }

    private fun endSession(reason: String) {
        clearPreparation()
        cancelBatch()
        finishPresentation(reason)
        session = null
    }

    private fun clearPreparation() {
        preparation?.let(handler::removeCallbacks)
        preparation = null
    }

    private fun cancelBatch() {
        batch?.let(handler::removeCallbacks)
        batch = null
        pendingReasons.clear()
    }

    private fun finishPresentation(reason: String) {
        refresh?.let(handler::removeCallbacks)
        refresh = null
        expiry?.let(handler::removeCallbacks)
        expiry = null
        val previous = active
        active = null
        if (previous != null) {
            try { renderer.cancel(previous) }
            catch (error: Exception) { Log.e(TAG, "Native cancellation IPC failed; SystemUI watchdog remains", error) }
            Log.i(TAG, "Battery presentation ended: $reason")
        }
    }

    private fun cancel(key: PodsNotificationKey, user: UserHandle = Process.myUserHandle()) {
        try { manager.cancelAsUser(key.tag, key.id, user) }
        catch (error: Exception) { Log.e(TAG, "Unable to remove owned battery presentation", error) }
    }

    private fun retireOwned() {
        try {
            val ownership = OwnedPodsNotifications(BuildConfig.APPLICATION_ID)
            for (entry in manager.activeNotifications) {
                val key = PodsNotificationKey(entry.tag ?: continue, entry.id)
                if (!ownership.isNamespaced(key) && !ownership.isLegacyCandidate(key)) continue
                val target = entry.notification.extras.getCharSequence("miui.targetPkg")?.toString()
                val intentPackages = mutableSetOf<String>()
                if (target.isNullOrEmpty() && ownership.isLegacyCandidate(key)) {
                    entry.notification.contentIntent?.let { pending ->
                        pending.creatorPackage?.let(intentPackages::add)
                        try {
                            val original = HostReflection.call(pending, "getIntent") as? Intent
                            original?.`package`?.let(intentPackages::add)
                            original?.component?.packageName?.let(intentPackages::add)
                        } catch (error: Exception) {
                            Log.w(TAG, "Legacy notification ownership cannot be proved; leaving it untouched", error)
                        }
                    }
                }
                if (ownership.canRetire(key, target, intentPackages)) cancel(key, entry.user)
            }
        } catch (error: Exception) { Log.e(TAG, "Owned battery notification cleanup failed", error) }
    }

    private fun readEvent(intent: Intent) = AudioConnectionEvent(
        intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, 0),
        intent.getLongExtra(AudioConnectionNotifications.EXTRA_CONNECTED_AT, -1),
    )

    companion object {
        private const val TAG = "PODS-Battery"
        private const val INITIAL_PARTIAL_MS = 3000L
        private const val INITIAL_DEADLINE_MS = 10_000L
        private const val BATCH_MS = 350L
        private const val CONNECTED = "com.hxlh50k.pods.action.podconnecting"
        private const val UPDATE = "com.hxlh50k.pods.action.updatepodsnotification"
        private const val CANCEL = "com.hxlh50k.pods.action.cancelpodsnotification"
        private const val LEGACY_TOAST = "com.hxlh50k.pods.action.sendstrongtoast"
    }
}
