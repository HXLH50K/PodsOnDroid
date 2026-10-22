package moe.chenxy.hyperpods.utils.miuiStrongToast

import DeviceNotificationBundle
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.toColorInt
import kotlinx.serialization.json.Json
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.AudioConnectionEvent
import moe.chenxy.hyperpods.utils.AudioConnectionNotifications
import moe.chenxy.hyperpods.utils.AudioConnectionSessions
import moe.chenxy.hyperpods.utils.BatteryIslandContract
import moe.chenxy.hyperpods.utils.BatteryNotificationSettings
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.LowBatterySettings
import moe.chenxy.hyperpods.utils.SystemApisUtils.isHyperOS
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.IconParams
import moe.chenxy.hyperpods.utils.data.Left
import moe.chenxy.hyperpods.utils.data.Right
import moe.chenxy.hyperpods.utils.data.StrongToastBean
import moe.chenxy.hyperpods.utils.data.TextParams

@SuppressLint("WrongConstant")
object MiuiStrongToastUtil {
    internal val audioSessions = AudioConnectionSessions()
    private val colorRed = "#FFFF3B30".toColorInt()

    fun showStringToast(context: Context, text: String?, colorType: Int) {
        if (!isHyperOS) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
            return
        }
        try {
            val textParams = TextParams(text, if (colorType == 1) Color.parseColor("#4CAF50") else Color.parseColor("#E53935"))
            val left = Left(textParams = textParams)
            val iconParams = IconParams(Category.DRAWABLE, FileType.SVG, "ic_launcher", 1)
            val right = Right(iconParams = iconParams)
            val strongToastBean = StrongToastBean(left, right)
            val jsonStr = Json.encodeToString(StrongToastBean.serializer(), strongToastBean)
            val bundle = DeviceNotificationBundle.Builder()
                .setPackageName(BuildConfig.APPLICATION_ID)
                .setStrongToastCategory(StrongToastCategory.TEXT_BITMAP_INTENT)
                .setTarget(null)
                .setParam(jsonStr)
                .onCreate()
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
        } catch (e: Exception) {
            Log.e("Art_Chen", "Failed to show HyperOS String Toast", e)
        }
    }

    /** A distinct, silent warning with a five-second maximum, never a persistent status entry. */
    fun showLowBatteryToast(context: Context, text: String): Boolean {
        if (!AudioConnectionNotifications.ownsPresentation(Application.getProcessName())) {
            Log.e("Art_Chen", "Refusing low battery presentation outside Xiaomi Bluetooth main process")
            return false
        }
        if (isHyperOS) {
            try {
                val bean = StrongToastBean(Left(textParams = TextParams(text, colorRed)),
                    Right(iconParams = IconParams(Category.DRAWABLE, FileType.SVG, "earphone", 1)))
                val bundle = DeviceNotificationBundle.Builder()
                    .setPackageName(HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
                    .setStrongToastCategory(StrongToastCategory.TEXT_BITMAP_INTENT)
                    .setNotifyId("${BuildConfig.APPLICATION_ID}:low_battery")
                    .setDuration(5000)
                    .setTarget(null)
                    .setParam(Json.encodeToString(StrongToastBean.serializer(), bean))
                    .onCreate()
                val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
                service.javaClass.getMethod("setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java)
                    .invoke(service, 1, "strong_toast_action", bundle)
                return true
            } catch (error: Exception) {
                Log.w("Art_Chen", "Low battery strong toast unavailable; using text toast", error)
            }
        }
        return try {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            true
        } catch (error: Exception) {
            Log.e("Art_Chen", "Unable to show low battery text toast", error)
            false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun showPodsBatteryToastByMiuiBt(context: Context?, batteryParams: BatteryParams) {
        // Legacy AACP caller: the device-addressed refresh below already carries these readings.
        // Battery/case availability alone has no authority to start an animation.
    }

    fun showPodsNotificationByMiuiBt(
        context: Context?,
        batteryParams: BatteryParams,
        device: BluetoothDevice,
        lowBatterySettings: LowBatterySettings = LowBatterySettings(),
        freshComponents: IntArray = intArrayOf(),
        modelNumber: String? = null,
        batteryNotificationSettings: BatteryNotificationSettings = BatteryNotificationSettings(),
        controlGeneration: Long = 0L,
    ) {
        if (!batteryNotificationSettings.valid) {
            Log.e("PODS-Battery", "Rejecting invalid notification thresholds before IPC")
            return
        }
        val intent = Intent("com.hxlh50k.pods.action.updatepodsnotification")
        intent.putExtra("batteryParams", batteryParams)
        intent.putExtra("device", device)
        intent.putExtra("lowBatterySettings", lowBatterySettings)
        intent.putExtra("freshComponents", freshComponents)
        intent.putExtra("modelNumber", modelNumber)
        intent.putExtra(BatteryIslandContract.SETTINGS, batteryNotificationSettings)
        intent.putExtra(BatteryIslandContract.CONTROL_GENERATION, controlGeneration)
        intent.putExtra(BatteryIslandContract.OBSERVED_AT, SystemClock.elapsedRealtimeNanos())
        audioSessions.current(device.address)?.let { event ->
            intent.putExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, event.id)
            intent.putExtra(AudioConnectionNotifications.EXTRA_CONNECTED_AT, event.connectedAt)
        }
        intent.`package` = HyperPodsBroadcasts.XIAOMI_BLUETOOTH
        HyperPodsBroadcasts.send(context, intent, HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
    }

    fun notifyBatteryControlGap(context: Context?, device: BluetoothDevice) {
        HyperPodsBroadcasts.send(context, Intent(BatteryIslandContract.ACTION_CONTROL_GAP)
            .putExtra("device", device)
            .putExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, audioSessions.current(device.address)?.id ?: 0L),
            HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
    }

    fun cancelPodsNotificationByMiuiBt(
        context: Context,
        device: BluetoothDevice,
        throughEventId: Long? = null,
    ) {
        val intent = Intent("com.hxlh50k.pods.action.cancelpodsnotification")
        intent.putExtra("device", device)
        intent.putExtra(AudioConnectionNotifications.EXTRA_EVENT_ID,
            audioSessions.cancellationId(device.address, throughEventId, SystemClock.elapsedRealtimeNanos()))
        intent.`package` = HyperPodsBroadcasts.XIAOMI_BLUETOOTH
        HyperPodsBroadcasts.send(context, intent, HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
    }

    /** Only the A2DP dispatcher may supply a claimed event; AACP refreshes cannot create a presentation. */
    internal fun showPodConnectingByMiuiBt(context: Context, device: BluetoothDevice, event: AudioConnectionEvent) {
        val intent = Intent("com.hxlh50k.pods.action.podconnecting")
        intent.putExtra("device", device)
        intent.putExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, event.id)
        intent.putExtra(AudioConnectionNotifications.EXTRA_CONNECTED_AT, event.connectedAt)
        intent.`package` = HyperPodsBroadcasts.XIAOMI_BLUETOOTH
        HyperPodsBroadcasts.send(context, intent, HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
    }

    object Category {
        const val RAW = "raw"
        const val DRAWABLE = "drawable"
        const val FILE = "file"
        const val MIPMAP = "mipmap"
    }

    object FileType {
        const val MP4 = "mp4"
        const val PNG = "png"
        const val SVG = "svg"
    }

    object StrongToastCategory {
        const val VIDEO_TEXT = "video_text"
        const val VIDEO_BITMAP_INTENT = "video_bitmap_intent"
        const val TEXT_BITMAP = "text_bitmap"
        const val TEXT_BITMAP_INTENT = "text_bitmap_intent"
        const val VIDEO_TEXT_TEXT_VIDEO = "video_text_text_video"
    }
}
