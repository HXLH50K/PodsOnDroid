package moe.chenxy.hyperpods.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.ListeningCycleCommit
import moe.chenxy.hyperpods.utils.AACPManager.Companion.ListeningMode
import top.yukonga.miuix.kmp.basic.BasicComponent
import moe.chenxy.hyperpods.ui.components.DashboardGroup as Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Composable
fun ListeningModeItem(title: String, image: Painter, tint: Color = LocalContentColor.current, bytes: MutableState<Byte>, check: Byte) {
    val checkBoxColors = CheckboxColors(
        checkedForegroundColor = MiuixTheme.colorScheme.primary,
        uncheckedForegroundColor = MiuixTheme.colorScheme.secondary,
        disabledCheckedForegroundColor = MiuixTheme.colorScheme.disabledPrimary,
        disabledUncheckedForegroundColor = MiuixTheme.colorScheme.disabledSecondary,
        checkedBackgroundColor = Color.Transparent,
        uncheckedBackgroundColor = Color.Transparent,
        disabledCheckedBackgroundColor = Color.Transparent,
        disabledUncheckedBackgroundColor = Color.Transparent
    )
    fun onClick() {
        bytes.value = if (bytes.value and check == check) {
            bytes.value and check.inv()
        } else {
            bytes.value or check
        }
    }
    BasicComponent(
        title = title,
        onClick = { onClick() },
        leftAction = {
            Icon(image, contentDescription = title, tint = tint, modifier = Modifier
                .height(24.dp)
                .padding(end = 12.dp))
        },
        rightActions = {
            Checkbox(
                checked = bytes.value and check == check,
                onCheckedChange = {
                    onClick()
                },
                colors = checkBoxColors
            )
        }
    )
}

@Composable
fun PressAndHoldSettingPage(
    onListeningModeChange: (Byte) -> Unit,
    titleModifier: Modifier,
    cardModifier: Modifier,
    confirmedMask: Int?,
    allowedModes: Set<Int>,
    enabled: Boolean,
    statusSummary: String? = null,
    allowUnfinishedFeatures: Boolean = false,
) {
    val context = LocalContext.current
    val allowedMask = allowedModes.fold(0) { mask, mode -> mask or (1 shl (mode - 1)) }
    val listeningModeConfigByte = rememberSaveable { mutableStateOf(0.toByte()) }
    var initialMask by rememberSaveable { mutableIntStateOf(0) }
    var invalidSelection by rememberSaveable { mutableStateOf(false) }

    val showBottomSheet = rememberSaveable { mutableStateOf(false) }
    fun saveAndDismiss() {
        val selected = listeningModeConfigByte.value.toInt() and 0xff
        when (ListeningCycleCommit.evaluate(initialMask, selected, allowedMask, enabled, showBottomSheet.value)) {
            ListeningCycleCommit.Action.CLOSE -> showBottomSheet.value = false
            ListeningCycleCommit.Action.SUBMIT -> {
                // Close first: a second dismissal callback must not submit again.
                showBottomSheet.value = false
                initialMask = selected
                onListeningModeChange(selected.toByte())
            }
            ListeningCycleCommit.Action.INVALID -> {
                invalidSelection = true
                Toast.makeText(context, R.string.long_press_select_two, Toast.LENGTH_SHORT).show()
            }
            ListeningCycleCommit.Action.UNAVAILABLE -> {
                showBottomSheet.value = false
                Toast.makeText(context, R.string.long_press_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SmallTitle(stringResource(R.string.long_press_shared), modifier = titleModifier.padding(top = 12.dp))
    Card(
        modifier = cardModifier
    ) {
        BasicComponent(stringResource(R.string.set_noise_mode),
            summary = statusSummary ?: if (confirmedMask == null && enabled)
                stringResource(R.string.setting_select_to_apply) else null,
            rightActions = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (!enabled) R.string.long_press_waiting
                        else if (confirmedMask == null) R.string.long_press_unread else R.string.set_noise_mode),
                        maxLines = 1, softWrap = false, fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    Icon(MiuixIcons.Basic.ArrowRight, null)
                }
            },
            enabled = enabled, onClick = {
                listeningModeConfigByte.value = ((confirmedMask ?: 0) and allowedMask).toByte()
                initialMask = listeningModeConfigByte.value.toInt() and 0xff
                invalidSelection = false
                showBottomSheet.value = true
            }
        )
    }


    CustomSuperBottomSheet(
        show = showBottomSheet,
        backgroundColor = MiuixTheme.colorScheme.background,
        onDismissRequest = ::saveAndDismiss,
        title = stringResource(R.string.long_press_shared)
    ) {
        Text(stringResource(R.string.long_press_autosave), modifier = Modifier.padding(16.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
        if (invalidSelection && !ListeningCycleCommit.valid(listeningModeConfigByte.value.toInt() and 0xff, allowedMask))
            Text(stringResource(R.string.long_press_select_two), modifier = Modifier.padding(horizontal = 16.dp),
                color = MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
        val checkBoxColors = CheckboxColors(
            checkedForegroundColor = MiuixTheme.colorScheme.primary,
            uncheckedForegroundColor = MiuixTheme.colorScheme.secondary,
            disabledCheckedForegroundColor = MiuixTheme.colorScheme.disabledPrimary,
            disabledUncheckedForegroundColor = MiuixTheme.colorScheme.disabledSecondary,
            checkedBackgroundColor = Color.Transparent,
            uncheckedBackgroundColor = Color.Transparent,
            disabledCheckedBackgroundColor = Color.Transparent,
            disabledUncheckedBackgroundColor = Color.Transparent
        )

        Card {
            BasicComponent(
                title = stringResource(R.string.set_noise_mode),
                rightActions = {
                    Checkbox(
                        checked = true,
                        onCheckedChange = {},
                        colors = checkBoxColors
                    )
                }
            )
            BasicComponent(
                title = stringResource(R.string.voice_assist_unsupported),
                enabled = enabled && allowUnfinishedFeatures,
                onClick = { Toast.makeText(context, R.string.voice_assist_unavailable, Toast.LENGTH_LONG).show() },
                rightActions = {
                    Checkbox(
                        checked = false,
                        enabled = enabled && allowUnfinishedFeatures,
                        onCheckedChange = { Toast.makeText(context, R.string.voice_assist_unavailable, Toast.LENGTH_LONG).show() },
                        colors = checkBoxColors
                    )
                }
            )
        }

        SmallTitle(text = stringResource(R.string.set_noise_mode), insideMargin = PaddingValues(16.dp, 8.dp))
            Card(
                modifier = Modifier.padding(0.dp)
            ) {
                if (1 in allowedModes) ListeningModeItem(
                    check = ListeningMode.OFF.value,
                    title = stringResource(R.string.off),
                    image = painterResource(id = R.drawable.dashboard_mode_off),
                    bytes = listeningModeConfigByte
                )
                if (2 in allowedModes) ListeningModeItem(
                    check = ListeningMode.NC.value,
                    title = stringResource(R.string.noise_cancellation_title),
                    image = painterResource(id = R.drawable.dashboard_mode_anc),
                    bytes = listeningModeConfigByte
                )
                if (3 in allowedModes) ListeningModeItem(
                    check = ListeningMode.TRANSPARENCY.value,
                    title = stringResource(R.string.transparency_title),
                    image = painterResource(id = R.drawable.dashboard_mode_transparency),
                    bytes = listeningModeConfigByte
                )
                if (4 in allowedModes) ListeningModeItem(
                    check = ListeningMode.ADAPTIVE.value,
                    title = stringResource(R.string.adaptive_title),
                    image = painterResource(id = R.drawable.dashboard_mode_adaptive),
                    bytes = listeningModeConfigByte
                )
            }
        Text(stringResource(R.string.long_press_shared_summary), modifier = Modifier.padding(16.dp), color = MiuixTheme.colorScheme.secondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
@Preview
fun PressAndHoldSettingPagePreview() {
    PressAndHoldSettingPage(
        titleModifier = Modifier,
        cardModifier = Modifier,
        onListeningModeChange = {},
        confirmedMask = 6,
        allowedModes = setOf(1, 2, 3, 4),
        enabled = true,
    )
}
