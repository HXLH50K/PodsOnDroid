package moe.chenxy.hyperpods.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Foreground, opt-in radio observation. Never connects, pairs, or writes headphone settings. */
class LidObservationCapture(private val context: Context) {
    enum class Status { IDLE, RUNNING, COMPLETE, STOPPED, PERMISSION_REQUIRED, BLUETOOTH_OFF, UNAVAILABLE, FAILED }
    data class State(
        val status: Status = Status.IDLE,
        val elapsedMs: Long = 0,
        val scanError: Int? = null,
        val observations: LidObservationSession.Snapshot = LidObservationSession().snapshot(),
    ) {
        fun toJson(): String = JSONObject(observations.toJson()).apply {
            put("capture_status", status.name)
            put("elapsed_ms", elapsedMs)
            put("scan_error", scanError ?: JSONObject.NULL)
        }.toString(2)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var session = LidObservationSession()
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null
    private var generation = 0L
    private var startedAt = 0L
    private var lastPublishedAt = 0L
    private val timeout = Runnable { finish(Status.COMPLETE) }
    private val tick = object : Runnable {
        override fun run() {
            if (callback == null) return
            if (elapsed() >= DURATION_MS) finish(Status.COMPLETE)
            else {
                publish(Status.RUNNING)
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun hasPermissions(): Boolean = PERMISSIONS.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // Checked before scanning; revocation is handled explicitly.
    fun start() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Start observation on the main thread" }
        if (callback != null) {
            Log.w(TAG, "An observation is already running")
            return
        }
        session = LidObservationSession()
        startedAt = SystemClock.elapsedRealtime()
        if (!hasPermissions()) {
            Log.w(TAG, "Nearby-device permission is required for observation")
            publish(Status.PERMISSION_REQUIRED)
            return
        }
        try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null) {
                Log.w(TAG, "Bluetooth adapter is unavailable")
                publish(Status.UNAVAILABLE)
                return
            }
            if (!adapter.isEnabled) {
                Log.w(TAG, "Bluetooth is disabled")
                publish(Status.BLUETOOTH_OFF)
                return
            }
            val activeScanner = adapter.bluetoothLeScanner
            if (activeScanner == null) {
                Log.w(TAG, "BLE scanner is unavailable")
                publish(Status.UNAVAILABLE)
                return
            }
            val token = ++generation
            val activeCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    handler.post { if (generation == token) receive(result) }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    handler.post { if (generation == token) results.forEach(::receive) }
                }

                override fun onScanFailed(errorCode: Int) {
                    handler.post {
                        if (generation == token) {
                            Log.w(TAG, "BLE observation failed: $errorCode")
                            finish(Status.FAILED, errorCode)
                        }
                    }
                }
            }
            scanner = activeScanner
            callback = activeCallback
            activeScanner.startScan(
                listOf(ScanFilter.Builder().setManufacturerData(APPLE_MANUFACTURER, byteArrayOf()).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),
                activeCallback,
            )
            publish(Status.RUNNING)
            handler.postDelayed(timeout, DURATION_MS)
            handler.postDelayed(tick, 1000)
        } catch (error: SecurityException) {
            Log.w(TAG, "Observation permission was denied or revoked", error)
            finish(Status.PERMISSION_REQUIRED)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Bluetooth state changed while starting observation", error)
            finish(Status.FAILED)
        }
    }

    @SuppressLint("MissingPermission") // The same foreground permission lease covers device identifiers.
    private fun receive(result: ScanResult) {
        if (callback == null) return
        val at = elapsed()
        if (at >= DURATION_MS) {
            finish(Status.COMPLETE)
            return
        }
        val data = result.scanRecord?.getManufacturerSpecificData(APPLE_MANUFACTURER) ?: return
        try {
            // Addresses exist only in the in-memory session map, never its snapshot/export.
            session.observe(at, result.device.address, data, result.rssi)
            if (SystemClock.elapsedRealtime() - lastPublishedAt >= 250) publish(Status.RUNNING)
        } catch (error: SecurityException) {
            Log.w(TAG, "Observation permission was revoked", error)
            finish(Status.PERMISSION_REQUIRED)
        }
    }

    fun mark(phase: LidObservationSession.Phase) {
        if (callback == null) {
            Log.w(TAG, "Cannot mark a physical action outside an active observation")
            return
        }
        val at = elapsed()
        if (at >= DURATION_MS) {
            finish(Status.COMPLETE)
            return
        }
        if (!session.mark(at, phase)) Log.w(TAG, "Observation marker limit reached")
        publish(Status.RUNNING)
    }

    fun stop() {
        if (callback != null) finish(Status.STOPPED)
    }

    @SuppressLint("MissingPermission")
    private fun finish(status: Status, errorCode: Int? = null) {
        val activeCallback = callback
        val activeScanner = scanner
        callback = null
        scanner = null
        generation++
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(tick)
        var finalStatus = status
        if (activeCallback != null && activeScanner != null) {
            try {
                activeScanner.stopScan(activeCallback)
            } catch (error: SecurityException) {
                Log.w(TAG, "Scanner cleanup could not be confirmed after permission revocation", error)
                finalStatus = Status.FAILED
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Scanner cleanup could not be confirmed after a Bluetooth state change", error)
                finalStatus = Status.FAILED
            }
        }
        session.finish()
        publish(finalStatus, errorCode)
    }

    private fun elapsed(): Long = SystemClock.elapsedRealtime() - startedAt

    private fun publish(status: Status, errorCode: Int? = null) {
        lastPublishedAt = SystemClock.elapsedRealtime()
        mutableState.value = State(status, elapsed(), errorCode, session.snapshot())
    }

    companion object {
        const val DURATION_MS = LidObservationSession.DURATION_MS
        private const val TAG = "PODS-LidObservation"
        private const val APPLE_MANUFACTURER = 0x004c
        val PERMISSIONS = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    }
}
