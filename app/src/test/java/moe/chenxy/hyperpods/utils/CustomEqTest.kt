package moe.chenxy.hyperpods.utils

import android.bluetooth.BluetoothSocket
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class CustomEqTest {
    private val customReport = byteArrayOf(4, 0, 4, 0, 0x63, 0, 5, 0, 1, 2, 0, 45, 100)

    @Test fun writeMatchesNativeProtocolAndValidReportReplacesUnconfirmedValue() {
        val writes = mutableListOf<ByteArray>()
        var writable = true
        var now = 100L
        val manager = AACPManager(mock(BluetoothSocket::class.java), { writes.add(it); writable }, { now })
        val callback = mock(AACPManager.PacketCallback::class.java)
        manager.setPacketCallback(callback)

        assertTrue(manager.sendCustomEq(CustomEq(CustomEq.CUSTOM, 0, 45, 100)))
        assertArrayEquals(customReport, writes.single())
        assertTrue(manager.customEqState.unconfirmed)
        assertEquals(0L, manager.settingsReceivedAt)
        verifyNoInteractions(callback)

        val recommendedReport = customReport.clone().apply { this[9] = 1; this[10] = 30 }
        manager.receivePacket(recommendedReport)
        val reported = manager.customEqState
        assertEquals(CustomEq(CustomEq.RECOMMENDED, 30, 45, 100), reported.value)
        assertFalse(reported.unconfirmed)
        assertEquals(100L, manager.settingsReceivedAt)
        verify(callback).onCustomEqReceived()

        now = 200
        manager.receivePacket(recommendedReport)
        assertEquals("Identical reports must not invalidate an in-progress edit", reported, manager.customEqState)
        assertEquals(200L, manager.settingsReceivedAt)
        verifyNoMoreInteractions(callback)

        writable = false
        assertFalse(manager.sendCustomEq(CustomEq(CustomEq.CUSTOM)))
        assertEquals(reported.copy(revision = reported.revision + 1), manager.customEqState)
    }

    @Test fun unrecognizedVariantAndMalformedReportDoNotEstablishAvailability() {
        val manager = AACPManager(mock(BluetoothSocket::class.java), { false }, { 100L })
        listOf(
            customReport.copyOf(12), customReport.copyOf(14),
            customReport.clone().apply { this[8] = 0 },
            customReport.clone().apply { this[9] = 3 },
            customReport.clone().apply { this[10] = 0xff.toByte() },
        ).forEach(manager::receivePacket)
        assertNull(manager.customEqState.value)
        assertEquals(0L, manager.settingsReceivedAt)
    }

    @Test fun firmwareGateRequiresKnownModelAndSupportedBuildOrAReport() {
        for (build in listOf("9A348", "10A100", " 9441861 ")) {
            assertTrue(CustomEq.available(AirPods5WCC(), build, null))
            assertFalse(CustomEq.available(AirPodsPro1(), build, null))
        }
        for (build in listOf(null, "unrecognized", "1.0.0", "8A358", "8454592")) {
            assertFalse(CustomEq.available(AirPods5WCC(), build, null))
        }
        assertTrue(CustomEq.available(null, null, CustomEq.fromReport(customReport)))
    }
}
