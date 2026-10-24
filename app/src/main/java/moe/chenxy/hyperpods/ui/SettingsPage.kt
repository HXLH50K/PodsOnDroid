package moe.chenxy.hyperpods.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.*
import moe.chenxy.hyperpods.utils.SettingsDestination
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(padding: PaddingValues, diagnostics: Bundle? = null, isActive: Boolean = true) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.ROOT) }
    var moduleReport by rememberSaveable { mutableStateOf("") }
    val lists = SettingsDestination.entries.map { rememberLazyListState() }
    fun back() { destination = destination.parent ?: SettingsDestination.ROOT }
    BackHandler(isActive && destination.parent != null) { back() }
    PageTransitionHost(destination, { it.depth }, isActive) { page, current ->
        if (page == SettingsDestination.ABOUT) {
            AboutPage(padding, ::back, lists[page.ordinal], current)
        } else PageScaffold(stringResource(page.title), padding,
            if (page.parent == null) null else ::back, current) { bodyPadding ->
            LazyColumn(state = lists[page.ordinal], modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bodyPadding.calculateBottomPadding() + 24.dp)) {
                if (page == SettingsDestination.ROOT || page == SettingsDestination.TOOLS) {
                    if (page == SettingsDestination.TOOLS) item(key = "tools_notice") {
                        Text(stringResource(R.string.settings_tools_notice), fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                    if (page == SettingsDestination.TOOLS) item(key = "unfinished_features") {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            DashboardGroup { UnfinishedFeaturesSettings(current) }
                        }
                    }
                    page.groups(BuildConfig.BUILD_TYPE == "debug").forEachIndexed { index, group ->
                        item(key = "group_$index") {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                val heading = if (page == SettingsDestination.ROOT) {
                                    if (index == 0) R.string.settings_group_app else R.string.settings_group_support
                                } else if (index == 0) R.string.settings_group_diagnostics else R.string.settings_group_tests
                                DashboardSection(stringResource(heading))
                                DashboardGroup {
                                    group.forEachIndexed { row, child ->
                                        if (row > 0) DashboardDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                                        SettingsEntry(child, current) { destination = child }
                                    }
                                }
                            }
                        }
                    }
                } else item(key = page.name) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        when (page) {
                            SettingsDestination.NOTIFICATIONS -> BatteryNotificationSettingsPanel()
                            SettingsDestination.APPEARANCE -> AppearanceSettings()
                            SettingsDestination.CONNECTION -> DashboardGroup {
                                ConnectionDiagnostics(diagnostics, includeModuleCheck = false, knownModuleReport = moduleReport)
                            }
                            SettingsDestination.MODULES -> DashboardGroup { ModuleSelfCheck { moduleReport = it } }
                            SettingsDestination.HEAD_TRACKING -> DashboardGroup { HeadTrackingPage(current) }
                            SettingsDestination.LID_OBSERVATION -> if (BuildConfig.BUILD_TYPE == "debug")
                                DashboardGroup { LidObservationPanel(current) }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

private val SettingsDestination.title: Int
    get() = when (this) {
        SettingsDestination.ROOT -> R.string.settings_title
        SettingsDestination.NOTIFICATIONS -> R.string.battery_notifications_title
        SettingsDestination.APPEARANCE -> R.string.appearance_title
        SettingsDestination.ABOUT -> R.string.about_hyperpods
        SettingsDestination.TOOLS -> R.string.settings_tools
        SettingsDestination.CONNECTION -> R.string.diagnostics_title
        SettingsDestination.MODULES -> R.string.settings_modules
        SettingsDestination.HEAD_TRACKING -> R.string.head_test_title
        SettingsDestination.LID_OBSERVATION -> R.string.settings_lid_observation
    }

@Composable
private fun SettingsEntry(destination: SettingsDestination, enabled: Boolean, open: () -> Unit) {
    val summary = when (destination) {
        SettingsDestination.NOTIFICATIONS -> R.string.settings_notifications_summary
        SettingsDestination.APPEARANCE -> R.string.settings_appearance_summary
        SettingsDestination.ABOUT -> R.string.settings_about_summary
        SettingsDestination.TOOLS -> R.string.settings_tools_summary
        SettingsDestination.CONNECTION -> R.string.settings_connection_summary
        SettingsDestination.MODULES -> R.string.settings_modules_summary
        SettingsDestination.HEAD_TRACKING -> R.string.head_test_summary
        SettingsDestination.LID_OBSERVATION -> R.string.settings_lid_summary
        SettingsDestination.ROOT -> error("The settings root is not a child entry")
    }
    val icon = when (destination) {
        SettingsDestination.NOTIFICATIONS -> R.drawable.dashboard_battery
        SettingsDestination.HEAD_TRACKING, SettingsDestination.LID_OBSERVATION -> R.drawable.dashboard_headphones
        else -> R.drawable.dashboard_settings
    }
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = open)
            .heightIn(min = 84.dp).padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Image(painterResource(icon), null, Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.primary))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(destination.title), fontSize = 17.sp, lineHeight = 23.sp)
                Text(stringResource(summary), fontSize = 13.sp, lineHeight = 19.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Icon(MiuixIcons.Basic.ArrowRight, null, modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
}
