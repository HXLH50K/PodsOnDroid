package moe.chenxy.hyperpods.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.*

class SystemIntegrationsTest {
    @Test fun unimplementedSystemsNeverTouchXiaomiServicesOrSendPresentationIpc() {
        PhoneSystem.entries.filter { it != PhoneSystem.HYPER_OS }.forEach { system ->
            val context = mock(Context::class.java)
            val device = mock(BluetoothDevice::class.java)
            val integration = SystemIntegrations(SystemProfile.forSystem(system))
            integration.audioConnected(context, device, AudioConnectionEvent(1, 1))
            integration.updateBattery(context, BatteryParams(), device)
            integration.controlGap(context, device)
            integration.cancelBattery(context, device)
            integration.headsetIcon(context, true)
            verifyNoInteractions(context, device)
        }
    }

    @Test fun theVendorAdapterRetainsTheSameGenericConnectionSessionLedger() {
        assertSame(AudioConnectionSessions.shared, MiuiStrongToastUtil.audioSessions)
    }
}
