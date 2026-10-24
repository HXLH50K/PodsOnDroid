package moe.chenxy.hyperpods.pods

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.AttributionSource
import android.content.Context
import android.os.ParcelUuid
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.*

class ClassicL2capSocketTest {
    private val context = mock(Context::class.java)
    private val device = mock(BluetoothDevice::class.java)
    private val adapter = mock(BluetoothAdapter::class.java)
    private val uuid = mock(ParcelUuid::class.java)
    private lateinit var adapters: MockedStatic<BluetoothAdapter>
    private lateinit var uuids: MockedStatic<ParcelUuid>

    @Before fun setUp() {
        adapters = mockStatic(BluetoothAdapter::class.java)
        adapters.`when`<BluetoothAdapter> { BluetoothAdapter.getDefaultAdapter() }.thenReturn(adapter)
        uuids = mockStatic(ParcelUuid::class.java)
        uuids.`when`<ParcelUuid> { ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a") }.thenReturn(uuid)
    }

    @After fun tearDown() {
        uuids.close()
        adapters.close()
    }

    open class DeviceFirst(
        val device: BluetoothDevice, val type: Int, val auth: Boolean,
        val encrypt: Boolean, val port: Int, val uuid: ParcelUuid,
    ) {
        constructor(device: BluetoothDevice, type: Int, auth: Boolean, encrypt: Boolean,
            port: Int, uuid: ParcelUuid, mitm: Boolean, pin: Boolean) :
            this(device, type, auth, encrypt, port, uuid) {
            error("The shortest device-first constructor should be selected")
        }
    }

    class AdapterFirst(
        val adapter: BluetoothAdapter, device: BluetoothDevice, type: Int,
        auth: Boolean, encrypt: Boolean, port: Int, uuid: ParcelUuid,
    ) : DeviceFirst(device, type, auth, encrypt, port, uuid) {
        constructor(adapter: BluetoothAdapter, device: BluetoothDevice, type: Int, auth: Boolean,
            encrypt: Boolean, port: Int, uuid: ParcelUuid, mitm: Boolean, pin: Boolean) :
            this(adapter, device, type, auth, encrypt, port, uuid) {
            error("The shortest adapter-first constructor should be selected")
        }
    }

    class ExtendedAdapterFirst(
        val adapter: BluetoothAdapter, device: BluetoothDevice, type: Int, auth: Boolean,
        encrypt: Boolean, port: Int, uuid: ParcelUuid, val mitm: Boolean, val pin: Boolean,
        val flags: Int, val service: String, val hub: Long, val endpoint: Long,
        val socketType: Int, val attribution: AttributionSource,
    ) : DeviceFirst(device, type, auth, encrypt, port, uuid)

    class ExtendedDeviceFirst(
        device: BluetoothDevice, type: Int, auth: Boolean, encrypt: Boolean,
        port: Int, uuid: ParcelUuid, val mitm: Boolean, val pin: Boolean, val flags: Int,
        val service: String, val hub: Long, val endpoint: Long, val socketType: Int,
    ) : DeviceFirst(device, type, auth, encrypt, port, uuid)

    class UnsupportedTail(
        device: BluetoothDevice, type: Int, auth: Boolean, encrypt: Boolean,
        port: Int, uuid: ParcelUuid, val unknown: Any?,
    ) : DeviceFirst(device, type, auth, encrypt, port, uuid)

    class ListeningOnly(val type: Int, val auth: Boolean, val encrypt: Boolean, val port: Int, val uuid: ParcelUuid)

    private fun assertTransport(socket: DeviceFirst) {
        assertSame(device, socket.device)
        assertEquals(3, socket.type)
        assertTrue(socket.auth)
        assertTrue(socket.encrypt)
        assertEquals(0x1001, socket.port)
        assertSame(uuid, socket.uuid)
    }

    @Test fun lenovoAndroid16UsesTheDeviceFirstConvenienceConstructor() {
        val socket = ClassicL2capSocket.create(DeviceFirst::class.java, context, device)
        assertTransport(socket)
        adapters.verifyNoInteractions()
        verifyNoInteractions(context, device)
    }

    @Test fun android17KeepsTheAdapterFirstConstructorAndClassicTransport() {
        val socket = ClassicL2capSocket.create(AdapterFirst::class.java, context, device)
        assertTransport(socket)
        assertSame(adapter, socket.adapter)
    }

    @Test fun extendedAndroid17ConstructorPreservesKnownDefaultsAndAttribution() {
        val attribution = mock(AttributionSource::class.java)
        `when`(context.attributionSource).thenReturn(attribution)
        val socket = ClassicL2capSocket.create(ExtendedAdapterFirst::class.java, context, device)
        assertTransport(socket)
        assertSame(adapter, socket.adapter)
        assertFalse(socket.mitm)
        assertFalse(socket.pin)
        assertEquals(0, socket.flags)
        assertEquals("HyperPods", socket.service)
        assertEquals(0L, socket.hub)
        assertEquals(0L, socket.endpoint)
        assertEquals(0, socket.socketType)
        assertSame(attribution, socket.attribution)
    }

    @Test fun extendedDeviceFirstConstructorIsSupportedWithoutAnAdapterLookup() {
        val socket = ClassicL2capSocket.create(ExtendedDeviceFirst::class.java, context, device)
        assertTransport(socket)
        assertFalse(socket.mitm)
        assertFalse(socket.pin)
        assertEquals(0, socket.flags)
        assertEquals("HyperPods", socket.service)
        assertEquals(0L, socket.hub)
        assertEquals(0L, socket.endpoint)
        assertEquals(0, socket.socketType)
        adapters.verifyNoInteractions()
    }

    @Test fun unknownTailIsRejectedInsteadOfReceivingAnArbitraryNull() {
        val error = assertThrows(NoSuchMethodException::class.java) {
            ClassicL2capSocket.create(UnsupportedTail::class.java, context, device)
        }
        assertTrue(error.message.orEmpty().contains("No compatible classic L2CAP"))
        verifyNoInteractions(context, device)
    }

    @Test fun listeningSocketIsNeverUsedForAnOutgoingControlChannel() {
        assertThrows(NoSuchMethodException::class.java) {
            ClassicL2capSocket.create(ListeningOnly::class.java, context, device)
        }
    }

    @Test fun missingAdapterFailsExplicitlyOnTheAdapterFirstPath() {
        adapters.`when`<BluetoothAdapter> { BluetoothAdapter.getDefaultAdapter() }.thenReturn(null)
        val error = assertThrows(IllegalArgumentException::class.java) {
            ClassicL2capSocket.create(AdapterFirst::class.java, context, device)
        }
        assertEquals("Bluetooth adapter unavailable for classic L2CAP socket", error.message)
    }
}
