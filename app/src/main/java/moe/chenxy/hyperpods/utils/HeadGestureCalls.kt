package moe.chenxy.hyperpods.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Runs in the scoped Bluetooth process, using its existing telephony permissions.
 * No root commands, dialer replacement, phone numbers or call records are needed.
 * The host's lock serializes receiver/timer callbacks with its socket lifecycle.
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION") // Same accept/reject API as LibrePods; no default-dialer role takeover.
class HeadGestureCalls(private val context: Context, private val lock: Any, sharedStream: HeadTrackingStream? = null,
    private val onStateChanged: () -> Unit) {
    private val detector = HeadGestureDetector()
    private var manager: AACPManager? = null
    private val stream = sharedStream ?: HeadTrackingStream(sendStandard = { start ->
        manager?.let { it.sendDataPacket(if (start) it.createStartHeadTrackingPacket()
            else it.createStopHeadTrackingPacket()) } == true
    }) { start ->
        manager?.let { it.sendDataPacket(if (start) it.createAlternateStartHeadTrackingPacket()
            else it.createAlternateStopHeadTrackingPacket()) } == true
    }
    private var enabled = false
    private var supported = false
    private var worn = false
    private var ready = false
    private var registered = false
    private var tracking = false
    private var handled = false
    private var started = 0L
    private var lastPacket = 0L
    var packets = 0
        private set
    var rejectedPackets = 0
        private set
    var filteredSamples = 0
        private set
    var lastPacketSize = 0
        private set
    var variant = "alternate"
        private set
    private var timeout: Job? = null
    var state = "disabled"
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = synchronized(lock) {
            // This is a system-protected action. Read aggregate state afresh, not
            // per-SIM extras (or the phone number), and ignore duplicate broadcasts.
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED && registered) refresh()
        }
    }

    private fun available(): Boolean = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
        (context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.MODIFY_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) &&
        context.getSystemService(TelephonyManager::class.java) != null && context.getSystemService(TelecomManager::class.java) != null &&
        context.getSystemService(SubscriptionManager::class.java) != null

    private fun ringing(): Boolean = runCatching {
        val phone = context.getSystemService(TelephonyManager::class.java) ?: return@runCatching false
        if (phone.callState != TelephonyManager.CALL_STATE_RINGING) return@runCatching false
        // Aggregate state alone also includes some VoIP calls. Require an actual
        // ringing cellular subscription, checking both SIMs rather than the default.
        // Read only transient subscription IDs, never numbers, names or identifiers.
        val states = context.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList.orEmpty().map {
            phone.createForSubscriptionId(it.subscriptionId).callStateForSubscription
        }
        // Telecom's APIs cannot target a subscription. Do not risk ending an
        // active call or choosing between concurrent incoming cellular calls.
        states.count { it == TelephonyManager.CALL_STATE_RINGING } == 1 &&
            states.none { it == TelephonyManager.CALL_STATE_OFFHOOK }
    }.getOrDefault(false)

    private fun state(value: String) {
        val next = if (stream.stopPending) "stop_failed" else value
        if (state == next) return
        state = next
        onStateChanged()
    }

    fun update(manager: AACPManager?, enabled: Boolean, supported: Boolean, worn: Boolean, ready: Boolean = true) {
        if (this.manager !== manager) {
            stop()
            handled = false
        }
        this.manager = manager
        this.enabled = enabled
        this.supported = supported
        this.worn = worn
        this.ready = ready
        val unavailable = when {
            !enabled -> "disabled"
            !supported -> "unsupported"
            manager == null -> "disconnected"
            !ready -> "not_ready"
            !available() -> "unavailable"
            else -> null
        }
        if (unavailable != null) {
            close()
            state(unavailable)
            return
        }
        if (!registered) {
            try {
                context.registerReceiver(receiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED), Context.RECEIVER_EXPORTED)
                registered = true
            } catch (_: Exception) {
                state("unavailable")
                return
            }
        }
        refresh()
    }

    private fun refresh() {
        if (!ringing()) {
            handled = false
            stop()
            state("waiting")
            return
        }
        if (!worn) {
            stop()
            state("not_worn")
            return
        }
        if (tracking || handled) return
        if (manager == null) return
        if (!stream.acquire(HeadTrackingStream.Owner.CALL)) {
            handled = true // Do not retry on every battery/wear packet.
            state("write_failed")
            return
        }
        detector.reset()
        tracking = true
        variant = stream.variant.name.lowercase()
        packets = 0
        rejectedPackets = 0
        filteredSamples = 0
        lastPacketSize = 0
        started = SystemClock.elapsedRealtime()
        lastPacket = started
        timeout = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(1000)
                synchronized(lock) {
                    ensureActive()
                    tick(SystemClock.elapsedRealtime())
                }
            }
        }
        state("starting")
    }

    fun onPacket(packet: ByteArray) {
        if (!tracking) return
        if (!enabled || !supported || !worn || !ready || !ringing()) {
            refresh()
            return
        }
        val at = SystemClock.elapsedRealtime()
        tick(at)
        if (!tracking) return
        lastPacketSize = packet.size
        val orientation = HeadGestureDetector.motion(packet)
        if (orientation == null) {
            rejectedPackets++
            detector.reset()
            return
        }
        packets++
        lastPacket = at
        if (!HeadGestureDetector.validForGesture(orientation)) filteredSamples++
        state("tracking")
        val gesture = detector.process(orientation, at) ?: return
        handled = true // One decision at most for this ringing window.
        if (!stop()) {
            state("stop_failed")
            return
        }
        if (!ringing()) {
            state("waiting") // Discard late gestures; do not act on an answered call.
            return
        }
        val result = runCatching {
            val telecom = context.getSystemService(TelecomManager::class.java) ?: return@runCatching false
            if (gesture == HeadGestureDetector.Gesture.NOD) {
                telecom.acceptRingingCall()
                true // API is void: request sent, not proof the call was answered.
            } else telecom.endCall()
        }.getOrDefault(false)
        state(if (!result) "call_failed" else if (gesture == HeadGestureDetector.Gesture.NOD) "answer_sent" else "reject_sent")
    }

    fun tick(at: Long) {
        if (!tracking) return
        if (!ringing()) {
            refresh()
            return
        }
        when {
            at - started >= 60000 -> finish("timeout")
            packets == 0 && at - started >= 2000 &&
                stream.canRetryWithStandard(HeadTrackingStream.Owner.CALL) -> {
                detector.reset()
                val retried = stream.retryWithStandard(HeadTrackingStream.Owner.CALL)
                variant = stream.variant.name.lowercase()
                if (!retried) finish("write_failed")
                else {
                    lastPacket = at
                    onStateChanged()
                }
            }
            at - lastPacket >= 5000 -> finish(if (packets == 0 && rejectedPackets > 0) "invalid_data" else "no_data")
        }
    }

    private fun finish(reason: String) {
        handled = true
        stop()
        state(reason)
    }

    private fun stop(): Boolean {
        timeout?.cancel()
        timeout = null
        val stopped = !tracking || stream.release(HeadTrackingStream.Owner.CALL)
        tracking = false
        detector.reset()
        return stopped
    }

    fun close() {
        stop()
        if (registered) runCatching { context.unregisterReceiver(receiver) }
        registered = false
        manager = null
        handled = false
    }
}
