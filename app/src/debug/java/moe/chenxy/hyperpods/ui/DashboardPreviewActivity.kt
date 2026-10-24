package moe.chenxy.hyperpods.ui

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.rememberGraphicsLayer
import moe.chenxy.hyperpods.ui.components.DashboardNavigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.chenxy.hyperpods.ui.components.DashboardGroup
import moe.chenxy.hyperpods.ui.components.ScopeRestartControls
import moe.chenxy.hyperpods.ui.components.ChimeVolumeSlider
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart
import moe.chenxy.hyperpods.utils.ScopeRestartSelection
import moe.chenxy.hyperpods.pods.EarDetectionStatus
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.PodsSettings
import moe.chenxy.hyperpods.utils.AirPodsModels
import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey as Key
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Renders the production page with synthetic data, without Bluetooth or preference writes. */
class DashboardPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = intent.getIntExtra("theme", 1) == 2
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark })
        window.isNavigationBarContrastEnforced = false
        val inCase = intent.getBooleanExtra("in_case", false)
        val unknown = intent.getBooleanExtra("unknown", false)
        val caseConnected = intent.getBooleanExtra("case_connected", inCase)
        val leftBattery = intent.getIntExtra("left_battery", 92)
        val modelNumber = intent.getStringExtra("model_number") ?: "A3049"
        setContent {
            AppTheme(colorMode = intent.getIntExtra("theme", 1)) {
                AppAppearanceProvider {
                if (intent.getBooleanExtra("glass_navigation_preview", false)) {
                    GlassNavigationPreview()
                    return@AppAppearanceProvider
                }
                if (intent.getBooleanExtra("scope_restart_preview", false)) {
                    ScopeRestartPreview()
                    return@AppAppearanceProvider
                }
                if (intent.getBooleanExtra("tone_volume_preview", false)) {
                    ToneVolumePreview()
                    return@AppAppearanceProvider
                }
                var anc by remember { mutableStateOf(NoiseControlMode.NOISE_CANCELLATION) }
                var personalized by remember { mutableStateOf(intent.getBooleanExtra("personalized", false)) }
                var conversation by remember { mutableStateOf(false) }
                var detection by remember { mutableStateOf(true) }
                var headGestures by remember { mutableStateOf(false) }
                var sleepDetection by remember { mutableStateOf(false) }
                var speaker by remember { mutableStateOf(false) }
                var swipe by remember { mutableStateOf(false) }
                var single by remember { mutableStateOf(false) }
                var level by remember { mutableFloatStateOf(0.5f) }
                var microphone by remember { mutableIntStateOf(0) }
                var name by remember { mutableStateOf(if (modelNumber == "A3049") "AirPods Pro"
                    else AirPodsModels.getModelByModelNumber(modelNumber)?.name ?: "AirPods") }
                val pager = rememberPagerState(pageCount = { 2 })
                AppFrame(pager) { padding ->
                    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                        if (page == 1) SettingsPage(padding = padding, isActive = pager.currentPage == 1) else PodDetailPage(
                            padding = padding,
                            batteryParams = BatteryParams(
                                PodBatteryParams(leftBattery, isConnected = !unknown, isInCase = inCase),
                                PodBatteryParams(87, isConnected = !unknown, isInCase = inCase),
                                PodBatteryParams(73, isConnected = caseConnected)),
                            earDetectionParams = EarDetectionParams(
                                if (inCase) EarDetectionStatus.IN_CASE else EarDetectionStatus.IN_EAR,
                                if (inCase) EarDetectionStatus.IN_CASE else EarDetectionStatus.IN_EAR),
                            earDetectionEnable = detection, onEarDetectionChanged = { detection = it },
                            headGestures = headGestures, onHeadGesturesChange = { headGestures = it },
                            diagnostics = Bundle().apply {
                                putString("head_gestures", if (headGestures) "waiting" else "disabled")
                                putString("connection", "connected")
                                putBoolean("ready", !unknown)
                                putBoolean("wear", !unknown)
                                putLong("wear_report_at", System.currentTimeMillis())
                                for (key in listOf("left_battery_at", "right_battery_at", "case_battery_at"))
                                    putLong(key, System.currentTimeMillis())
                            },
                            onSettingChange = { key, value -> if (key == Key.SLEEP_DETECTION) sleepDetection = value == 1 },
                            autoSwitchToSpeaker = speaker, onAutoSwitchToSpeakerChange = { speaker = it },
                            personlizedVolume = personalized, onPersonlizedVolumeChange = { personalized = it },
                            conversationAwareness = conversation, onConversationAwarenessChange = { conversation = it },
                            adjustVolumeBySwiper = swipe, onAdjustVolumeBySwiperChange = { swipe = it },
                            adaptiveAudioLevel = level, onAdaptiveAudioLevelChange = { level = it },
                            onListeningModeChange = {}, ancMode = anc, onAncModeChange = { anc = it },
                            podsInfo = if (unknown) null else AACPManager.Companion.AirPodsInformation(
                                name, modelNumber, "Apple Inc.", "DEMO-NOT-REAL", "0", "0", "1.0.0",
                                "DEMO", "DEMO-LEFT", "DEMO-RIGHT", "DEMO"),
                            onNameChange = { name = it }, microphoneMode = microphone,
                            onMicrophoneModeChange = { microphone = it },
                            noiseCancellationSingleAirPod = single, onNoiseCancellationSingleAirPodChange = { single = it },
                            deviceName = name,
                            isActive = pager.currentPage == 0,
                            settings = if (unknown) emptyMap() else mapOf(
                                PodsSettings.NOISE_MODE to anc.ordinal + 1,
                                Key.PERSONLIZED_VOLUME to if (personalized) 1 else 2,
                                Key.CONVERSATION_AWARENESS to if (conversation) 1 else 2,
                                Key.SLEEP_DETECTION to if (sleepDetection) 1 else 2,
                                Key.ADJUST_VOLUME_BY_SWIPER to if (swipe) 1 else 2,
                                Key.SINGLE_POD_ANC to if (single) 1 else 2,
                                Key.MICROPHONE_MODE to (PodsSettings.microphoneValue(microphone) ?: 0),
                                Key.ADAPTIVE_AUDIO_LEVEL to (100 - level * 100).toInt(),
                                Key.LISTENING_MODE_BYTE to 6,
                                Key.ALLOW_OFF_OPTION to 1,
                                Key.PRESS_SPEED to 0, Key.HOLD_DURATION to 0,
                                Key.SWIPE_SPEED to 1, Key.CHIME_VOLUME to 50,
                            ))
                    }

                }
            }
        }
    }
    }

    @Composable
    private fun ScopeRestartPreview() {
        val hosts = remember { ScopeRestartSelection.available() }
        var state by remember { mutableStateOf(InterfaceScopeRestart.State()) }
        Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
            DashboardGroup {
                ScopeRestartControls(hosts, hosts.map { it.processName }.toSet(), state,
                    saveSelection = { true },
                    start = { selected ->
                        state = InterfaceScopeRestart.State(running = true, processName = selected.first(),
                            hosts = selected.map { InterfaceScopeRestart.HostResult(it, optional = false) })
                        true
                    })
            }

        }
    }

    @Composable
    private fun ToneVolumePreview() {
        var value by remember { mutableIntStateOf(60) }
        var feedback by remember { mutableStateOf("reported") }
        Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
            DashboardGroup {
                ChimeVolumeSlider(value, feedback, true, null, {
                    value = it
                    feedback = "sent"
                })
            }

        }
    }

    @Composable
    private fun GlassNavigationPreview() {
        val layer = rememberGraphicsLayer()
        val backdrop = remember(layer) { GlassBackdrop(layer) }
        var selected by remember { mutableIntStateOf(0) }
        CompositionLocalProvider(LocalNavigationEffects provides !intent.getBooleanExtra("glass_opaque", false)) {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().glassSource(backdrop)) {
                    drawRect(if (intent.getIntExtra("theme", 1) == 2) ComposeColor(0xFF142438) else ComposeColor(0xFFDBEDFF))
                    val step = 24.dp.toPx()
                    for (x in 0..(size.width / step).toInt()) for (y in 0..(size.height / step).toInt())
                        if ((x + y) % 2 == 0)
                            drawRect(ComposeColor(0xFF7799B4), Offset(x * step, y * step), Size(step, step))
                }
                DashboardNavigation(selected, { selected = it }, Modifier.align(Alignment.Center), backdrop)
            }
        }
    }
}
