package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserHandle
import android.util.Log
import androidx.core.content.FileProvider
import com.hyperfocus.api.FocusApi
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.PodsIslandData
import moe.chenxy.hyperpods.utils.AudioConnectionEvent
import moe.chenxy.hyperpods.utils.AudioConnectionNotifications
import moe.chenxy.hyperpods.utils.PodsNotificationKey
import moe.chenxy.hyperpods.utils.OwnedPodsNotifications
import moe.chenxy.hyperpods.utils.LowBatterySettings
import moe.chenxy.hyperpods.utils.LowBatteryReminder
import moe.chenxy.hyperpods.utils.BatteryReminderStore
import moe.chenxy.hyperpods.utils.BatteryReminderText
import moe.chenxy.hyperpods.utils.BatteryReminderBatch
import moe.chenxy.hyperpods.utils.SystemApisUtils.cancelAsUser
import moe.chenxy.hyperpods.utils.SystemApisUtils.notifyAsUser
import moe.chenxy.hyperpods.utils.SystemApisUtils.supportsIsland
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil
import org.json.JSONObject
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@SuppressLint("MissingPermission")
object MiBluetoothToastHook {
    @Volatile
    private var receiverRegistered = false
    val notificationReceiverReady: Boolean get() = receiverRegistered
    private const val CASE_MP4_BASE64 = "AAAAGGZ0eXBtcDQyAAAAAG1wNDJtcDQxAAADI21vb3YAAABsbXZoZAAAAADiuBTm4rgU5gABX5AAAAXcAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAJ1dHJhawAAAFx0a2hkAAAAB+K4FObiuBTmAAAAAQAAAAAAAAXcAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAABuAAAAbgAAAAAAJGVkdHMAAAAcZWxzdAAAAAAAAAABAAAF3AAAAAAAAQAAAAAB7W1kaWEAAAAgbWRoZAAAAADiuBTm4rgU5gAA6mAAAAPoFccAAAAAAEBoZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAH01haW5jb25jZXB0IFZpZGVvIE1lZGlhIEhhbmRsZXIAAAGFbWluZgAAABR2bWhkAAAAAQAAAAAAAAAAAAAAM2hkbHIAAAAAAAAAAGFsaXMAAAAAAAAAAAAAAABBbGlhcyBEYXRhIEhhbmRsZXIAAAAAJGRpbmYAAAAcZHJlZgAAAAAAAAABAAAADHVybCAAAAABAAABEnN0YmwAAAChc3RzZAAAAAAAAAABAAAAkWF2YzEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAbgBuAEgAAABIAAAAAAAAAAEKQVZDIENvZGluZwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAY//8AAAA7YXZjQwFNACn/4QAkJ00AKZWxz+qwEQAAAwABAAADAHjoQADk4AAAq6GL3vg7Q4ZcAQAEKO48gAAAABhzdHRzAAAAAAAAAAEAAAABAAAD6AAAABxzdHNjAAAAAAAAAAEAAAABAAAAAQAAAAEAAAAUc3RzegAAAAAAAAm8AAAAAQAAABRzdGNvAAAAAAAAAAEAACsZAAAADXNkdHAAAAAAAAAAADp1ZHRhAAAAF6lUSU0ACwAAMDA6MDA6MDA6MDAAAAAOqVRTQwACAAA2MAAAAA2pVFNaAAEAADEAABAPdXVpZL56z8uXqULonHGZlJHjr6w8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pgo8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA3LjEtYzAwMCA3OS5iMGY4YmU5LCAyMDIxLzEyLzA4LTE5OjExOjIyICAgICAgICAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgeG1sbnM6eG1wRE09Imh0dHA6Ly9ucy5hZG9iZS5jb20veG1wLzEuMC9EeW5hbWljTWVkaWEvIgogICAgeG1sbnM6c3REaW09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9EaW1lbnNpb25zIyIKICAgIHhtbG5zOnRpZmY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vdGlmZi8xLjAvIgogICAgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iCiAgICB4bWxuczpzdEV2dD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL3NUeXBlL1Jlc291cmNlRXZlbnQjIgogICAgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIgogICB4bXA6Q3JlYXRlRGF0ZT0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcERNOnZpZGVvRnJhbWVSYXRlPSI2MC4wMDAwMDAiCiAgIHhtcERNOnZpZGVvRmllbGRPcmRlcj0iUHJvZ3Jlc3NpdmUiCiAgIHhtcERNOnZpZGVvUGl4ZWxBc3BlY3RSYXRpbz0iMS8xIgogICB4bXBETTpzdGFydFRpbWVTY2FsZT0iNjAiCiAgIHhtcERNOnN0YXJ0VGltZVNhbXBsZVNpemU9IjEiCiAgIHRpZmY6T3JpZW50YXRpb249IjEiCiAgIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6Nzk5OWJkMDEtMDEwNC1hMTQzLWFkOGEtMzUzYmU4YTFjZDUzIgogICB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgZGM6Zm9ybWF0PSJILjI2NCI+CiAgIDx4bXBETTpkdXJhdGlvbgogICAgeG1wRE06dmFsdWU9IjE1MDAiCiAgICB4bXBETTpzY2FsZT0iMS85MDAwMCIvPgogICA8eG1wRE06dmlkZW9GcmFtZVNpemUKICAgIHN0RGltOnc9IjExMCIKICAgIHN0RGltOmg9IjExMCIKICAgIHN0RGltOnVuaXQ9InBpeGVsIi8+CiAgIDx4bXBETTpzdGFydFRpbWVjb2RlCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIgogICAgeG1wRE06dGltZVZhbHVlPSIwMDowMDowMDowMCIvPgogICA8eG1wRE06YWx0VGltZWNvZGUKICAgIHhtcERNOnRpbWVWYWx1ZT0iMDA6MDA6MDA6MDAiCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIi8+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InNhdmVkIgogICAgICBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjc5OTliZDAxLTAxMDQtYTE0My1hZDhhLTM1M2JlOGExY2Q1MyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgICAgIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4KICAgIDwvcmRmOlNlcT4KICAgPC94bXBNTTpIaXN0b3J5PgogIDwvcmRmOkRlc2NyaXB0aW9uPgogPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4KICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgIAo8P3hwYWNrZXQgZW5kPSJ3Ij8+AAAXv2ZyZWUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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"

    var miheadset_notification_Box: Int = -1
    var miheadset_notification_LeftEar: Int = -1
    var miheadset_notification_RightEar: Int = -1
    var miheadset_notification_Disconnect: Int = -1
    var earphone_drawable: Int = -1
    var system_notification_accent_color: Int = -1
    var ic_headset_notification: Int = -1

    lateinit var mThiz: Any
    val focusApi = FocusApi

    @OptIn(ExperimentalEncodingApi::class)
    fun install(framework: XposedInterface, classLoader: ClassLoader) {
        val notifications = AudioConnectionNotifications(BuildConfig.APPLICATION_ID)
        fun getCaseMp4Uri(context: Context): Uri? {
            val file = File(context.filesDir, "my_internal_files")
            if (file.isDirectory || file.mkdirs()) {
                val file2 = File(file, "airpods_gen3_case.mp4")
                if (!file2.exists()) {
                    file2.createNewFile()
                    file2.writeBytes(Base64.decode(CASE_MP4_BASE64))
                    file2.setReadable(true)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.xiaomi.bluetooth.fileprovider",
                    file2
                )
                context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                return uri
            }
            return null
        }

        fun getResourcesUrl(context: Context, name: String) : Uri? {
            var uri: Uri? = null
            try {
                val file = File(context.filesDir, "my_internal_files")
                if (file.exists()) {
                    val str3 = "$name.mp4"
                    val file2 = File(file, str3)
                    if (file2.exists()) {
                        uri = FileProvider.getUriForFile(
                            context,
                            "com.xiaomi.bluetooth.fileprovider",
                            file2
                        )
                        context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            } catch (e: Exception) {
                Log.e("Art_Chen", "getResourcesUrl failed! ", e)
            }
            Log.d("Art_Chen", "getResourceUri uri = $uri")
            return uri

        }

        fun deleteIntent(context: Context, bluetoothDevice: BluetoothDevice,
            presentation: AudioConnectionNotifications.Presentation): PendingIntent {
            val intent = Intent("com.hxlh50k.pods.action.dismissconnection")
            intent.setPackage(HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
            intent.putExtra("device", bluetoothDevice)
            intent.putExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, presentation.event.id)
            intent.identifier = presentation.key.tag
            return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun initResources(context: Context) {
            if (miheadset_notification_Box == -1) {
                miheadset_notification_Box = context.resources.getIdentifier("miheadset_notification_Box", "string", "com.xiaomi.bluetooth")
                miheadset_notification_LeftEar = context.resources.getIdentifier("miheadset_notification_LeftEar", "string", "com.xiaomi.bluetooth")
                miheadset_notification_RightEar = context.resources.getIdentifier("miheadset_notification_RightEar", "string", "com.xiaomi.bluetooth")
                miheadset_notification_Disconnect = context.resources.getIdentifier("miheadset_notification_Disconnect", "string", "com.xiaomi.bluetooth")
                system_notification_accent_color = context.resources.getIdentifier("system_notification_accent_color", "color", "android")
                ic_headset_notification = context.resources.getIdentifier("ic_headset_notification", "drawable", "com.xiaomi.bluetooth")
                earphone_drawable = context.resources.getIdentifier("earphone", "drawable", "com.xiaomi.bluetooth")
                if (ic_headset_notification == 0) ic_headset_notification = android.R.drawable.stat_sys_data_bluetooth
                if (earphone_drawable == 0) earphone_drawable = android.R.drawable.stat_sys_data_bluetooth
                if (system_notification_accent_color == 0) system_notification_accent_color = android.R.color.holo_blue_light
            }
        }

        fun showConnectedToast(bluetoothDevice: BluetoothDevice, context: Context,
            update: AudioConnectionNotifications.Update) {
            initResources(context)
            val presentation = update.presentation
            val notificationManager = context.getSystemService("notification") as NotificationManager
            val resources = context.createPackageContext(BuildConfig.APPLICATION_ID, 0).resources
            val batteryText = resources.getString(R.string.batt_left_pod) + update.battery.left + " | " +
                resources.getString(R.string.batt_right_pod) + update.battery.right + " | " +
                resources.getString(R.string.pod_case) + update.battery.case
            val channelId = presentation.key.tag
            val deviceName = PodsIslandData.displayName(bluetoothDevice.alias, bluetoothDevice.name)
            val remaining = presentation.remaining(SystemClock.elapsedRealtime())
            if (remaining == 0L) return

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    deviceName,
                    NotificationManager.IMPORTANCE_LOW
                )
            )

            val builder = Notification.Builder(context, channelId)
                    .setSmallIcon(earphone_drawable)
                    .setContentTitle(deviceName)
                    .setContentText(batteryText)
                    .setStyle(Notification.BigTextStyle().bigText(batteryText))
                    .setContentIntent(PendingIntent.getActivity(context, 0,
                        Intent("com.hxlh50k.pods.action.show_airpods_ui").setPackage(BuildConfig.APPLICATION_ID),
                        PendingIntent.FLAG_IMMUTABLE))
                    .setDefaults(0)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setDeleteIntent(deleteIntent(context, bluetoothDevice, presentation))
                    .setTimeoutAfter(remaining)
                    .addExtras(Bundle().apply {
                        putCharSequence("miui.targetPkg", BuildConfig.APPLICATION_ID)
                    })
            if (supportsIsland(context)) {
                try {
                    val icon = Icon.createWithResource(context, earphone_drawable)
                    // HyperFocus 1.10 uses seconds; Android's notification timeout uses milliseconds.
                    val focusTimeout = presentation.focusTimeoutSeconds(SystemClock.elapsedRealtime())
                    val api = focusApi.sendFocus(
                        title = channelId,
                        cancel = false,
                        isShowNotification = false,
                        showSmallIcon = false,
                        baseInfo = focusApi.baseinfo(title = deviceName, basetype = 0, content = batteryText),
                        island = update.island(batteryText, SystemClock.elapsedRealtime()),
                        updatable = true,
                        islandFirstFloat = update.firstFloat,
                        enableFloat = update.firstFloat,
                        reopen = "false",
                        picInfo = icon,
                        picInfotype = 2,
                        timeout = focusTimeout,
                        ticker = batteryText,
                        picticker = icon
                    )
                    val focus = JSONObject(requireNotNull(api.getString("miui.focus.param")))
                    update.configureFocus(focus)
                    api.putString("miui.focus.param", focus.toString())
                    builder.addExtras(Bundle().apply {
                        putString("miui.effect.src", "true")
                        putCharSequence("miui.targetPkg", BuildConfig.APPLICATION_ID)
                        putAll(api)
                    })
                } catch (error: Exception) {
                    Log.w("Art_Chen", "Unable to create connection island; using transient notification", error)
                }
            }
            val finalRemaining = presentation.remaining(SystemClock.elapsedRealtime())
            if (finalRemaining == 0L) return
            builder.setTimeoutAfter(finalRemaining)
            notificationManager.notifyAsUser(
                presentation.key.tag,
                presentation.key.id,
                builder.build(),
                Process.myUserHandle()
            )
        }

        fun cancelNotification(key: PodsNotificationKey, context: Context,
            user: UserHandle = Process.myUserHandle()) {
            try {
                val notificationManager = context.getSystemService("notification") as NotificationManager
                notificationManager.cancelAsUser(key.tag, key.id, user)
            } catch (e: Exception) {
                Log.e("Art_Chen", "Failed to cancel Pod Notification!", e)
            }
        }

        fun retireOwnedNotifications(context: Context) {
            val ownership = OwnedPodsNotifications(BuildConfig.APPLICATION_ID)
            val preservedKeys = notifications.activeKeys(SystemClock.elapsedRealtime())
            try {
                val manager = requireNotNull(context.getSystemService(NotificationManager::class.java)) {
                    "Notification manager unavailable for owned-entry cleanup"
                }
                for (entry in manager.activeNotifications) {
                    val key = PodsNotificationKey(entry.tag ?: continue, entry.id)
                    if (key in preservedKeys ||
                        (!ownership.isNamespaced(key) && !ownership.isLegacyCandidate(key))) continue
                    val target = entry.notification.extras.getCharSequence("miui.targetPkg")?.toString()
                    val intentPackages = mutableSetOf<String>()
                    if (target.isNullOrEmpty() && ownership.isLegacyCandidate(key)) {
                        val contentIntent = entry.notification.contentIntent
                        if (contentIntent != null) {
                            contentIntent.creatorPackage?.let(intentPackages::add)
                            try {
                                val intent = HostReflection.call(contentIntent, "getIntent") as? Intent
                                intent?.`package`?.let(intentPackages::add)
                                intent?.component?.packageName?.let(intentPackages::add)
                            } catch (error: Exception) {
                                Log.w("Art_Chen", "Cannot verify legacy headset notification target; leaving it untouched", error)
                            }
                        }
                    }
                    if (ownership.canRetire(key, target, intentPackages)) cancelNotification(key, context, entry.user)
                }
            } catch (error: Exception) {
                Log.e("Art_Chen", "Unable to inspect owned headset notifications for cleanup", error)
            }
        }

        fun registerHyperPodsReceiver(context: Context) {
            if (receiverRegistered) return
            // MiuiApplication also runs in :ui; only the main process may consume presentation events.
            if (!AudioConnectionNotifications.ownsPresentation(Application.getProcessName())) {
                Log.i("Art_Chen", "Skipping notification receiver outside Xiaomi Bluetooth main process")
                return
            }
            val handler = Handler(Looper.getMainLooper())
            val presentations = mutableMapOf<String, Pair<AudioConnectionNotifications.Presentation, Runnable>>()
            val warningBatch = BatteryReminderBatch()
            var warningDevice: BluetoothDevice? = null
            var warningTask: Runnable? = null
            val warningDecisions = mutableMapOf<Int, LowBatteryReminder.Decision>()
            var warningStore: BatteryReminderStore? = null
            fun cancelWarning() {
                warningTask?.let(handler::removeCallbacks)
                warningTask = null
                warningBatch.clear()
                warningDevice = null
                warningDecisions.clear()
                warningStore = null
            }
            fun clearPresentation(address: String) {
                presentations.remove(address)?.second?.let(handler::removeCallbacks)
            }
            fun presentConnection(device: BluetoothDevice, event: AudioConnectionEvent) {
                retireOwnedNotifications(context)
                val update = notifications.connected(device.address, event, SystemClock.elapsedRealtime()) ?: return
                val presentation = update.presentation
                clearPresentation(device.address)
                val expiry = Runnable {
                    presentations.remove(device.address)
                    notifications.expire(presentation, SystemClock.elapsedRealtime())?.let {
                        cancelNotification(it, context)
                    }
                }
                presentations[device.address] = presentation to expiry
                try {
                    check(handler.postDelayed(expiry, presentation.remaining(SystemClock.elapsedRealtime()))) {
                        "Unable to schedule connection notification expiry"
                    }
                    showConnectedToast(device, context, update)
                } catch (error: Exception) {
                    clearPresentation(device.address)
                    notifications.dismiss(presentation)?.let { cancelNotification(it, context) }
                    throw error
                }
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiveContext: Context?, intent: Intent?) {
                    val sender = if (intent?.action == "com.hxlh50k.pods.action.dismissconnection")
                        HyperPodsBroadcasts.XIAOMI_BLUETOOTH else HyperPodsBroadcasts.BLUETOOTH
                    if (!HyperPodsBroadcasts.isTrusted(context, this, sender)) return
                    try {
                        when (intent?.action) {
                            "com.hxlh50k.pods.action.sendstrongtoast" -> {
                                // Ignore queued legacy case/battery animations, but retire provably owned remnants.
                                retireOwnedNotifications(context)
                            }
                            "com.hxlh50k.pods.action.updatepodsnotification" -> {
                                retireOwnedNotifications(context)
                                val params = intent.getParcelableExtra("batteryParams", BatteryParams::class.java) ?: return
                                val device = intent.getParcelableExtra("device", BluetoothDevice::class.java) ?: return
                                notifications.refresh(device.address, params, SystemClock.elapsedRealtime())?.let { update ->
                                    try {
                                        showConnectedToast(device, context, update)
                                    } catch (error: Exception) {
                                        clearPresentation(device.address)
                                        notifications.dismiss(update.presentation)?.let { cancelNotification(it, context) }
                                        Log.e("Art_Chen", "Unable to update bounded connection battery presentation", error)
                                    }
                                }
                                val settings = intent.getParcelableExtra("lowBatterySettings", LowBatterySettings::class.java)
                                if (settings == null || !settings.valid) {
                                    cancelWarning()
                                    Log.w("Art_Chen", "Ignoring low battery refresh without valid reminder settings")
                                    return
                                }
                                try {
                                    val resources = context.createPackageContext(BuildConfig.APPLICATION_ID, 0).resources
                                    val store = BatteryReminderStore(context.getSharedPreferences("hyperpods_battery_reminders", Context.MODE_PRIVATE), device.address)
                                    val decisions = LowBatteryReminder.evaluate(params, intent.getIntArrayExtra("freshComponents") ?: intArrayOf(), settings, store::state)
                                    // Recharge observations rearm immediately; alerts are consumed only on successful display.
                                    store.commit(decisions, false)
                                    if (warningDevice != device) cancelWarning()
                                    warningDevice = device
                                    warningStore = store
                                    decisions.forEach { warningDecisions[it.component] = it }
                                    val alerts = decisions.filter { it.alert }.map { it.component }
                                    val hasWarning = warningBatch.update(params, settings, alerts)
                                    // Merge split left/right/case reports arriving close together into one transient warning.
                                    if (hasWarning && warningTask == null) {
                                        warningTask = Runnable {
                                            warningTask = null
                                            try {
                                                warningBatch.take()?.let { (latest, components) ->
                                                    val text = BatteryReminderText.format(resources, latest, components)
                                                    if (text.isNotEmpty() && MiuiStrongToastUtil.showLowBatteryToast(context, text)) {
                                                        warningStore?.commit(warningDecisions.values
                                                            .filter { it.component in components && it.alert }, true)
                                                    }
                                                }
                                            } catch (error: Exception) {
                                                warningBatch.clear()
                                                Log.w("Art_Chen", "Unable to show low battery warning", error)
                                            } finally {
                                                warningDecisions.clear()
                                                warningStore = null
                                            }
                                        }.also {
                                            check(handler.postDelayed(it, 350)) { "Unable to schedule low battery warning" }
                                        }
                                    }
                                } catch (error: Exception) {
                                    Log.w("Art_Chen", "Low battery reminder unavailable; no status notification will be posted", error)
                                    cancelWarning()
                                }
                            }
                            "com.hxlh50k.pods.action.cancelpodsnotification" ->
                                intent.getParcelableExtra("device", BluetoothDevice::class.java)?.let {
                                    val keys = notifications.cancel(it.address,
                                        intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, 0))
                                    if (keys.isNotEmpty()) {
                                        clearPresentation(it.address)
                                        if (it == warningDevice) cancelWarning()
                                        keys.forEach { key -> cancelNotification(key, context) }
                                        retireOwnedNotifications(context)
                                    }
                                }
                            "com.hxlh50k.pods.action.podconnecting" ->
                                intent.getParcelableExtra("device", BluetoothDevice::class.java)?.let {
                                    presentConnection(it, AudioConnectionEvent(
                                        intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, 0),
                                        intent.getLongExtra(AudioConnectionNotifications.EXTRA_CONNECTED_AT, -1)))
                                }
                            "com.hxlh50k.pods.action.dismissconnection" ->
                                intent.getParcelableExtra("device", BluetoothDevice::class.java)?.let {
                                    val presentation = presentations[it.address]?.first ?: return
                                    if (presentation.event.id != intent.getLongExtra(AudioConnectionNotifications.EXTRA_EVENT_ID, 0)) return
                                    clearPresentation(it.address)
                                    notifications.dismiss(presentation)?.let { key -> cancelNotification(key, context) }
                                }
                        }
                    } catch (error: Exception) {
                        Log.e("Art_Chen", "Unable to handle headset notification", error)
                    }
                }
            }
            retireOwnedNotifications(context)
            val filter = IntentFilter("com.hxlh50k.pods.action.sendstrongtoast").apply {
                addAction("com.hxlh50k.pods.action.updatepodsnotification")
                addAction("com.hxlh50k.pods.action.cancelpodsnotification")
                addAction("com.hxlh50k.pods.action.podconnecting")
                addAction("com.hxlh50k.pods.action.dismissconnection")
            }
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            receiverRegistered = true
            Log.i("Art_Chen", "P.O.D.S. receiver registered (connection-only-battery-v2)")
        }

        installHook("Xiaomi Bluetooth application receiver") {
            val application = Class.forName("com.android.bluetooth.ble.MiuiApplication", false, classLoader)
            framework.intercept(HostReflection.method(application, "onCreate")) { chain ->
                val result = chain.proceed()
                registerHyperPodsReceiver(chain.thisObject as Context)
                result
            }
        }

        // HyperOS 4 has its own AirPods fast-connect pipeline. It consumes Apple
        // manufacturer advertisements, writes airpodsRepository and starts its
        // popup/island controller. Disable only that scan entry so it cannot race
        // HyperPods; L2CAP control and ordinary Xiaomi fast-connect stay intact.
        val officialScan = runCatching {
            Class.forName("b1.d", false, classLoader).also {
                check(it.getDeclaredMethod("U", ScanResult::class.java).returnType == Void.TYPE)
            }
        }.getOrNull()
        if (officialScan != null) {
            installHook("official AirPods scan interception") {
                framework.intercept(HostReflection.method(officialScan, "U", ScanResult::class.java)) {
                    if (BuildConfig.DEBUG) Log.v("Art_Chen", "Blocked official HyperOS AirPods fast-connect scan")
                    null
                }
            }
            Log.i("Art_Chen", "HyperOS AirPods scan hook registered (${BuildConfig.BUILD_REVISION})")
        } else Log.w("Art_Chen", "Official AirPods scan signature changed; interception unavailable on API ${android.os.Build.VERSION.SDK_INT}")

        installHook("Xiaomi Bluetooth notification receiver") {
            val notification = Class.forName("com.android.bluetooth.ble.app.MiuiBluetoothNotification", false, classLoader)
            notification.declaredConstructors.filter { it.parameterCount == 2 }.forEach { constructor ->
                framework.intercept(constructor) { chain ->
                    val result = chain.proceed()
                    mThiz = requireNotNull(chain.thisObject)
                    // update case video first
                    val context = HostReflection.field(mThiz, "mContext") as Context
                    registerHyperPodsReceiver(context)
                    result
                }
            }
        }
    }

}
