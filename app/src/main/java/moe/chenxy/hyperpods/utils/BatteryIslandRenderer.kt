package moe.chenxy.hyperpods.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import com.hyperfocus.api.FocusApi
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.SystemApisUtils.notifyAsUser
import moe.chenxy.hyperpods.utils.SystemApisUtils.supportsIsland
import org.json.JSONObject

/** One native dual-region card. Only its owner decides lifetime and event eligibility. */
internal class BatteryIslandRenderer(private val context: Context) {
    data class Content(
        val title: String,
        val reason: String,
        val left: Int?,
        val right: Int?,
        val settings: BatteryNotificationSettings,
    )

    data class Presentation(
        val key: PodsNotificationKey,
        val token: String,
        val audioEvent: Long,
        val submittedAt: Long,
        var firstVisibleAt: Long? = null,
        var lastContent: Content? = null,
    ) {
        fun deadline(): Long = firstVisibleAt?.plus(BatteryIslandContract.VISIBLE_MS)
            ?: submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS
    }

    private val resources = context.createPackageContext(BuildConfig.APPLICATION_ID, 0).resources
    private val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
    private val channel = "${BuildConfig.APPLICATION_ID}.battery.v4"

    init {
        manager.createNotificationChannel(NotificationChannel(channel,
            resources.getString(R.string.battery_island_channel), NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
        })
    }

    fun post(presentation: Presentation, content: Content, first: Boolean) {
        require(content.settings.valid)
        val now = SystemClock.elapsedRealtime()
        check(presentation.deadline() > now) { "Refusing an expired battery presentation" }
        // The shorter controller watchdog cancels an unseen card. Native timeout must also cover
        // the full visible window if rendering begins near the end of that startup allowance.
        val remaining = presentation.firstVisibleAt?.let { it + BatteryIslandContract.VISIBLE_MS - now }
            ?: presentation.submittedAt + BatteryIslandContract.STARTUP_TIMEOUT_MS + BatteryIslandContract.VISIBLE_MS - now
        check(supportsIsland(context)) { "Native battery island is unavailable on this system" }
        val leftLabel = resources.getString(R.string.battery_island_left, content.left?.let { "$it%" } ?: "--")
        val rightLabel = resources.getString(R.string.battery_island_right, content.right?.let { "$it%" } ?: "--")
        val leftColor = content.settings.color(content.left)
        val rightColor = content.settings.color(content.right)
        val leftIcon = batteryIcon(content.left, leftColor)
        val rightIcon = batteryIcon(content.right, rightColor)
        val seconds = ((remaining + 999) / 1000).toInt().coerceAtLeast(1)
        fun ear(type: Int, key: String, title: String) = JSONObject()
            .put("type", type)
            .put("picInfo", JSONObject().put("type", 1).put("pic", key))
            .put("textInfo", JSONObject().put("title", title))
        val island = JSONObject()
            .put("islandProperty", 1)
            .put("islandTimeout", seconds)
            .put("maxSize", false)
            .put("bigIslandArea", JSONObject()
                .put("imageTextInfoLeft", ear(1, PodsIslandData.LEFT_ICON, leftLabel))
                .put("imageTextInfoRight", ear(2, PodsIslandData.RIGHT_ICON, rightLabel)))
            .put("smallIslandArea", JSONObject().put("picInfo", JSONObject()
                .put("type", 1).put("pic", "miui.focus.pic_mark_v2")))
        // Do not send dismissIsland=true on creation/refresh: lifetime is owned by the host controller.
        val focus = FocusApi.sendFocus(
            title = presentation.key.tag, cancel = false, isShowNotification = false, showSmallIcon = false,
            baseInfo = FocusApi.baseinfo(title = content.title, basetype = 0, content = content.reason),
            island = island, updatable = true, islandFirstFloat = first, enableFloat = true,
            reopen = "false", picInfo = leftIcon, picInfotype = 2, timeout = seconds,
            ticker = "$leftLabel | $rightLabel", picticker = leftIcon,
            addpics = Bundle().apply {
                putParcelable(PodsIslandData.LEFT_ICON, leftIcon)
                putParcelable(PodsIslandData.RIGHT_ICON, rightIcon)
            },
        )
        val wire = JSONObject(requireNotNull(focus.getString("miui.focus.param")))
        wire.getJSONObject("param_v2").apply {
            put("updatable", true)
            put("islandFirstFloat", first)
            put("enableFloat", true)
        }
        focus.putString("miui.focus.param", wire.toString())
        val extras = Bundle().apply {
            putAll(focus)
            putCharSequence("miui.targetPkg", BuildConfig.APPLICATION_ID)
            putString("miui.effect.src", "true")
            putString(BatteryIslandContract.PRESENTATION_TOKEN, presentation.token)
            putLong(BatteryIslandContract.AUDIO_EVENT, presentation.audioEvent)
            putInt(BatteryIslandContract.LEFT_COLOR, leftColor)
            putInt(BatteryIslandContract.RIGHT_COLOR, rightColor)
        }
        val dismiss = Intent(BatteryIslandContract.ACTION_DISMISSED)
            .setPackage(HyperPodsBroadcasts.XIAOMI_BLUETOOTH)
            .putExtra(BatteryIslandContract.PRESENTATION_TOKEN, presentation.token)
            .setIdentifier(presentation.token)
        val builder = Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(content.title)
            .setContentText("$leftLabel | $rightLabel")
            .setSubText(content.reason)
            .setOngoing(false)
            .setDefaults(0)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(remaining)
            .setContentIntent(PendingIntent.getActivity(context, 0,
                Intent("com.hxlh50k.pods.action.show_airpods_ui").setPackage(BuildConfig.APPLICATION_ID),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setDeleteIntent(PendingIntent.getBroadcast(context, 0, dismiss,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .addExtras(extras)
        check(presentation.deadline() > SystemClock.elapsedRealtime()) { "Battery render preparation expired" }
        manager.notifyAsUser(presentation.key.tag, presentation.key.id, builder.build(), Process.myUserHandle())
        presentation.lastContent = content
    }

    @SuppressLint("UseKtx")
    private fun batteryIcon(level: Int?, color: Int): Icon {
        val bitmap = Bitmap.createBitmap(64, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(RectF(22f, 3f, 42f, 11f), 3f, 3f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(RectF(11f, 12f, 53f, 75f), 7f, 7f, paint)
        paint.style = Paint.Style.FILL
        if (level != null) {
            val height = 51f * level.coerceIn(0, 100) / 100f
            canvas.drawRoundRect(RectF(17f, 69f - height, 47f, 69f), 3f, 3f, paint)
        }
        return Icon.createWithBitmap(bitmap)
    }
}
