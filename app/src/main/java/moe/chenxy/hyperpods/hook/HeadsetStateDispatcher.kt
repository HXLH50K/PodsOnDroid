package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.L2CAPController
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.AudioConnectionEvent
import moe.chenxy.hyperpods.utils.SystemApisUtils.setIconVisibility
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showPodConnectingByMiuiBt
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.audioSessions

object HeadsetStateDispatcher {
    private val airPodsUUIDs = hashSetOf(
        ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
        ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
    )
    private val uuidDiscovery = HeadsetUuidDiscovery(::isPods,
        { service, device ->
            HostReflection.call(service, "getConnectionState", device) == BluetoothProfile.STATE_CONNECTED &&
                runCatching { nativeGetHookResult() }.getOrDefault(false)
        },
        { service, device ->
            audioSessions.claim(device.address)?.let { event ->
                runCatching { showPodConnectingByMiuiBt(service, device, event) }
                    .onFailure { Log.e("Art_Chen", "Unable to announce audio connection", it) }
                L2CAPController.connectPod(service, device)
            }
        })

    @SuppressLint("PrivateApi")
    private fun getBooleanProp(prop: String, def: Boolean): Boolean {
        return HostReflection.callStatic(Class.forName("android.os.SystemProperties"), "getBoolean", prop, def) as Boolean
    }

    external fun nativeGetHookResult(): Boolean

    @SuppressLint("MissingPermission")
    fun install(framework: XposedInterface, classLoader: ClassLoader) {
        // The framework's native-init ABI is separate from the Java API version.
        runCatching { System.loadLibrary("hyperpods_hook") }
            .onFailure { Log.e("Art_Chen", "Native Bluetooth hook unavailable", it) }

        val service = Class.forName("com.android.bluetooth.a2dp.A2dpService", false, classLoader)
        HostReflection.methods(service, "handleConnectionStateChanged", 3).forEach { method ->
            framework.intercept(method) { chain ->
                val result = chain.proceed()
                val currState = chain.args[2] as Int
                val fromState = chain.args[1] as Int
                val device = chain.args[0] as BluetoothDevice?
                val context = chain.thisObject as ContextWrapper
                val handler = HostReflection.field(context, "mHandler") as Handler
                if (device == null || currState == fromState) return@intercept result
                val eventId = SystemClock.elapsedRealtimeNanos()
                val connectedEvent = if (currState == BluetoothProfile.STATE_CONNECTED) {
                    audioSessions.connected(device.address, AudioConnectionEvent(eventId, SystemClock.elapsedRealtime()))
                } else null
                if (currState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Invalidate queued direct/UUID callbacks before the service handler runs.
                    audioSessions.disconnected(device.address)
                }
                handler.post {
                    try {
                        Log.d("Art_Chen", "A2DP Connection State: $currState, isAirPod ${isPods(device)}")
                        if (currState == BluetoothProfile.STATE_DISCONNECTED) {
                            uuidDiscovery.disconnected(context, device)
                            runCatching { cancelPodsNotificationByMiuiBt(context, device, eventId) }
                                .onFailure { Log.e("Art_Chen", "Unable to cancel audio notification", it) }
                            // UUIDs may be absent again during teardown.
                            L2CAPController.disconnectedPod(context, device)
                        }
                        if (!isPods(device)) {
                            // UUID discovery can finish after the A2DP event.
                            if (currState == BluetoothProfile.STATE_CONNECTED && device.uuids == null) {
                                uuidDiscovery.discover(context, device)
                            }
                            return@post
                        }
                        val statusBarManager = context.getSystemService("statusbar") as StatusBarManager
                        if (currState == BluetoothHeadset.STATE_CONNECTED) {
                            statusBarManager.setIconVisibility("wireless_headset", true)
                            val hookRes = runCatching { nativeGetHookResult() }.getOrDefault(false)
                            if (!hookRes) {
                                Toast.makeText(
                                    context,
                                    context.packageManager.getResourcesForApplication(BuildConfig.APPLICATION_ID)
                                        .getString(R.string.module_hook_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                                return@post
                            }
                            audioSessions.claim(device.address, connectedEvent)?.let { event ->
                                runCatching { showPodConnectingByMiuiBt(context, device, event) }
                                    .onFailure { Log.e("Art_Chen", "Unable to announce audio connection", it) }
                                L2CAPController.connectPod(context, device)
                            }
                        } else if (currState == BluetoothHeadset.STATE_DISCONNECTING || currState == BluetoothHeadset.STATE_DISCONNECTED) {
                            statusBarManager.setIconVisibility("wireless_headset", false)
                        }
                    } catch (error: Exception) {
                        Log.e("Art_Chen", "Unable to handle headset connection state", error)
                    }
                }
                result
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun isPods(device: BluetoothDevice): Boolean {
        for (uuid in device.uuids ?: return false) {
            if (airPodsUUIDs.contains(uuid)) {
                return true
            }
        }
        return false
    }

}
