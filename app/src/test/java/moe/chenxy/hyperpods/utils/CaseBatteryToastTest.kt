package moe.chenxy.hyperpods.utils

import android.content.Context
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class CaseBatteryToastTest {
    @Test fun legacyInitialCaseAndBatteryRequestsCannotBroadcastOrStartAnAnimation() {
        val context = mock(Context::class.java)
        repeat(150) {
            MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(context, BatteryParams(
                left = PodBatteryParams(it % 101, isConnected = true, isInCase = true),
                case = PodBatteryParams(it % 101, isConnected = true)))
        }
        verifyNoInteractions(context)
    }

    @Test fun absentLegacyContextOrBatteryDoesNotProduceFallbackStatus() {
        MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(null, BatteryParams())
    }

    @Test fun lowBatteryWarningsAreRejectedOutsideTheMainPresentationOwner() {
        val context = mock(Context::class.java)
        // The Android JVM stub reports no process name.
        assertFalse(MiuiStrongToastUtil.showLowBatteryToast(context, "Left: 10%"))
        verifyNoInteractions(context)
    }
}
