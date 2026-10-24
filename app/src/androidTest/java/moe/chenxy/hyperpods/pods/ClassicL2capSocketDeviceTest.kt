package moe.chenxy.hyperpods.pods

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassicL2capSocketDeviceTest {
    @Test fun createsClassicControlSocketWithoutConnectingToAHeadset() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val adapter = requireNotNull(context.getSystemService(BluetoothManager::class.java)?.adapter)
        val device = adapter.getRemoteDevice("02:00:00:00:00:01")
        ClassicL2capSocket.create(context, device).use { socket ->
            assertEquals(BluetoothSocket.TYPE_L2CAP, socket.connectionType)
            assertEquals(device, socket.remoteDevice)
            assertFalse(socket.isConnected)
            fun field(name: String): Any? = BluetoothSocket::class.java.getDeclaredField(name)
                .apply { isAccessible = true }.get(socket)
            assertEquals(0x1001, field("mPort"))
            assertEquals(true, field("mAuth"))
            assertEquals(true, field("mEncrypt"))
            assertEquals("74ec2172-0bad-4d01-8f77-997b2be0722a", field("mUuid").toString())
            instrumentation.sendStatus(0, Bundle().apply {
                putString("stream", "Classic L2CAP socket created: authenticated/encrypted, PSM 0x1001, " +
                    "AirPods UUID; no connection attempted.\n")
            })
        }
    }
}
