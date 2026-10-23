package moe.chenxy.hyperpods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import moe.chenxy.hyperpods.ui.AppTheme
import moe.chenxy.hyperpods.ui.components.*
import moe.chenxy.hyperpods.utils.*
import moe.chenxy.hyperpods.utils.data.*
import java.util.function.Consumer

class HeadphonePanelActivity : ComponentActivity() {
    private var blurListener: Consumer<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val density = resources.displayMetrics.density
        val size = HeadphonePanelLayout.size(
            metrics.bounds.width() - insets.left - insets.right,
            metrics.bounds.height() - insets.top - insets.bottom, density)
        window.setGravity(Gravity.CENTER)
        window.setLayout((size.widthDp * density).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.decorView.clipToOutline = true
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val listener = Consumer<Boolean> { enabled -> applyGlass(enabled) }
        blurListener = listener
        windowManager.addCrossWindowBlurEnabledListener(mainExecutor, listener)
        applyGlass(windowManager.isCrossWindowBlurEnabled)
        setContent { AppTheme(colorMode = 2) { Panel(this, size.maxHeightDp) } }
    }

    private fun applyGlass(enabled: Boolean) {
        val density = resources.displayMetrics.density
        window.setBackgroundDrawable(GradientDrawable().apply {
            cornerRadius = 40 * density
            setColor(if (enabled) Color.argb(105, 112, 112, 120) else Color.rgb(66, 66, 72))
            setStroke(density.toInt().coerceAtLeast(1), Color.argb(66, 255, 255, 255))
        })
        window.setBackgroundBlurRadius(if (enabled) (72 * density).toInt() else 0)
        window.attributes = window.attributes.apply {
            blurBehindRadius = if (enabled) (24 * density).toInt() else 0
            dimAmount = if (enabled) .22f else .48f
        }
        Log.i("PODS-Panel", if (enabled) "Native window blur enabled" else
            "Cross-window blur unavailable; using readable opaque panel")
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        blurListener?.let(windowManager::removeCrossWindowBlurEnabledListener)
        blurListener = null
        super.onDestroy()
    }
}

@Composable
private fun Panel(activity: HeadphonePanelActivity, maxHeightDp: Float) {
    var info by remember { mutableStateOf<AACPManager.Companion.AirPodsInformation?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var battery by remember { mutableStateOf(BatteryParams()) }
    var diagnostics by remember { mutableStateOf<Bundle?>(null) }
    var values by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var hostPending by remember { mutableStateOf(false) }
    var modeState by remember { mutableStateOf(PanelModeState()) }
    var request by remember { mutableIntStateOf(0) }
    DisposableEffect(activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!HyperPodsBroadcasts.isTrusted(context, this, HyperPodsBroadcasts.BLUETOOTH)) return
                when (intent.action) {
                    HyperPodsAction.ACTION_PODS_CONNECTED -> {
                        info = intent.getParcelableExtra("device_info", AACPManager.Companion.AirPodsInformation::class.java)
                        name = intent.getStringExtra("device_name")
                    }
                    HyperPodsAction.ACTION_PODS_DISCONNECTED -> activity.finish()
                    HyperPodsAction.ACTION_PODS_BATTERY_CHANGED ->
                        intent.getParcelableExtra("status", BatteryParams::class.java)?.let { battery = it }
                    HyperPodsAction.ACTION_PODS_DIAGNOSTICS -> {
                        diagnostics = intent.getBundleExtra("diagnostics")
                        modeState = modeState.report(diagnostics?.getInt("noise_mode_reported", 0) ?: 0)
                    }
                    HyperPodsAction.ACTION_PODS_SETTINGS_STATE -> {
                        val bundle = intent.getBundleExtra("settings") ?: return
                        values = bundle.keySet().filter { it in PodsSettings.identifiers }.associateWith { bundle.getInt(it) }
                        val waiting = intent.getStringArrayListExtra("pending").orEmpty() +
                            intent.getStringArrayListExtra("unconfirmed").orEmpty()
                        hostPending = PodsSettings.NOISE_MODE in waiting
                    }
                    HyperPodsAction.ACTION_PODS_SETTING_RESULT -> {
                        if (intent.getStringExtra("key") == PodsSettings.NOISE_MODE &&
                            !intent.getBooleanExtra("pending", false)) {
                            if (!intent.getBooleanExtra("success", false)) modeState = modeState.fail()
                        }
                    }
                }
            }
        }
        activity.registerReceiver(receiver, IntentFilter().apply {
            listOf(HyperPodsAction.ACTION_PODS_CONNECTED, HyperPodsAction.ACTION_PODS_DISCONNECTED,
                HyperPodsAction.ACTION_PODS_BATTERY_CHANGED,
                HyperPodsAction.ACTION_PODS_DIAGNOSTICS, HyperPodsAction.ACTION_PODS_SETTINGS_STATE,
                HyperPodsAction.ACTION_PODS_SETTING_RESULT).forEach(::addAction)
        }, Context.RECEIVER_EXPORTED)
        HyperPodsBroadcasts.send(activity, Intent(HyperPodsAction.ACTION_PODS_UI_INIT), HyperPodsBroadcasts.BLUETOOTH)
        onDispose { activity.unregisterReceiver(receiver) }
    }
    LaunchedEffect(request) {
        if (request > 0) {
            delay(6500)
            if (modeState.requested != null) modeState = modeState.fail()
        }
    }
    val model = info?.modelNumber?.let(AirPodsModels::getModelByModelNumber)
    HeadphonePanelContent(
        activity = activity,
        name = info?.name?.takeIf { it.isNotBlank() } ?: name,
        battery = battery,
        model = model,
        diagnostics = diagnostics,
        modeState = modeState,
        hostPending = hostPending,
        allowedModes = if (PodsSettings.supports(PodsSettings.NOISE_MODE, model))
            PodsSettings.allowedNoiseModes(model, values) else emptySet(),
        maxHeightDp = maxHeightDp,
        connected = info != null,
        onMode = { mode ->
            modeState = modeState.select(mode)
            request++
            HyperPodsBroadcasts.send(activity, Intent(HyperPodsAction.ACTION_ANC_SELECT)
                .putExtra("status", mode), HyperPodsBroadcasts.BLUETOOTH)
        },
        onMore = {
            activity.startActivity(Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            activity.finish()
        },
        onClose = activity::finish,
    )
}
