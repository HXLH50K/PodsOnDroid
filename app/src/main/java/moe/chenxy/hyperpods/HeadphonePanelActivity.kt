package moe.chenxy.hyperpods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.ui.AppTheme
import moe.chenxy.hyperpods.ui.components.*
import moe.chenxy.hyperpods.utils.*
import moe.chenxy.hyperpods.utils.data.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

class HeadphonePanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            .coerceAtMost((420 * resources.displayMetrics.density).toInt())
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        setContent { AppTheme { Panel(this) } }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }
}

@Composable
private fun Panel(activity: HeadphonePanelActivity) {
    var info by remember { mutableStateOf<AACPManager.Companion.AirPodsInformation?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var battery by remember { mutableStateOf(BatteryParams()) }
    var ears by remember { mutableStateOf(EarDetectionParams((-1).toByte(), (-1).toByte())) }
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
                    HyperPodsAction.ACTION_EAR_DETECTION_STATUS_CHANGED ->
                        intent.getParcelableExtra("status", EarDetectionParams::class.java)?.let { ears = it }
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
                HyperPodsAction.ACTION_PODS_BATTERY_CHANGED, HyperPodsAction.ACTION_EAR_DETECTION_STATUS_CHANGED,
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
    val reported = modeState.reported ?: 0
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).clip(RoundedCornerShape(28.dp))
        .background(MiuixTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(info?.name?.takeIf { it.isNotBlank() } ?: name ?: stringResource(R.string.app_name))
        Text(stringResource(dashboardConnectionStatus(diagnostics)))
        DashboardBattery(battery, ears, model, diagnostics, showUnknownCase = true)
        if (PodsSettings.supports(PodsSettings.NOISE_MODE, model)) {
            DashboardSection(stringResource(R.string.dashboard_noise))
            DashboardNoise(NoiseControlMode.entries.getOrNull(reported - 1), { mode ->
                modeState = modeState.select(mode.ordinal + 1)
                request++
                HyperPodsBroadcasts.send(activity, Intent(HyperPodsAction.ACTION_ANC_SELECT)
                    .putExtra("status", mode.ordinal + 1), HyperPodsBroadcasts.BLUETOOTH)
            }, if (diagnostics?.getBoolean("ready") == true && !hostPending && modeState.requested == null)
                PodsSettings.allowedNoiseModes(model, values) else emptySet())
            Text(stringResource(when {
                modeState.failed -> R.string.panel_failed
                modeState.requested != null || hostPending -> R.string.panel_sending
                modeState.reported != null -> R.string.panel_reported
                else -> R.string.settings_reading
            }))
        }
        DashboardLink(stringResource(R.string.panel_more)) {
            activity.startActivity(Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            activity.finish()
        }
        DashboardLink(stringResource(R.string.panel_close)) { activity.finish() }
    }
}
