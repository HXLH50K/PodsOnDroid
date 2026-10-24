package moe.chenxy.hyperpods.utils

import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import moe.chenxy.hyperpods.utils.SystemApisUtils.setIconVisibility
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil

/** Core transport calls this boundary; OEM classes stay behind their implemented feature gates. */
internal class SystemIntegrations(private val profile: SystemProfile) {
    private fun usesHyperOS(feature: SystemFeature): Boolean =
        profile.system == PhoneSystem.HYPER_OS && profile.supports(feature)

    fun audioConnected(context: Context, device: BluetoothDevice, event: AudioConnectionEvent) {
        if (usesHyperOS(SystemFeature.NATIVE_BATTERY_ISLAND))
            MiuiStrongToastUtil.showPodConnectingByMiuiBt(context, device, event)
    }

    fun updateBattery(
        context: Context?,
        params: BatteryParams,
        device: BluetoothDevice,
        lowBatterySettings: LowBatterySettings = LowBatterySettings(),
        freshComponents: IntArray = intArrayOf(),
        modelNumber: String? = null,
        batteryNotificationSettings: BatteryNotificationSettings = BatteryNotificationSettings(),
        controlGeneration: Long = 0L,
    ) {
        if (usesHyperOS(SystemFeature.NATIVE_BATTERY_ISLAND))
            MiuiStrongToastUtil.showPodsNotificationByMiuiBt(context, params, device, lowBatterySettings,
                freshComponents, modelNumber, batteryNotificationSettings, controlGeneration)
    }

    fun controlGap(context: Context?, device: BluetoothDevice) {
        if (usesHyperOS(SystemFeature.NATIVE_BATTERY_ISLAND))
            MiuiStrongToastUtil.notifyBatteryControlGap(context, device)
    }

    fun cancelBattery(context: Context, device: BluetoothDevice, throughEventId: Long? = null) {
        if (usesHyperOS(SystemFeature.NATIVE_BATTERY_ISLAND))
            MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt(context, device, throughEventId)
    }

    fun headsetIcon(context: Context, visible: Boolean) {
        if (usesHyperOS(SystemFeature.HEADSET_STATUS_ICON)) {
            val statusBar = requireNotNull(context.getSystemService("statusbar") as? StatusBarManager)
            statusBar.setIconVisibility("wireless_headset", visible)
        }
    }

    companion object {
        val current: SystemIntegrations by lazy { SystemIntegrations(SystemEnvironment.current) }
    }
}
