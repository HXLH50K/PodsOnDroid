package moe.chenxy.hyperpods.utils

import org.junit.Assert.*
import org.junit.Test

class NativeBatteryIslandQueueTest {
    private data class Data(var key: String)

    @Test fun pendingCancellationRemovesOnlyTheExactOwnedObject() {
        val owned = Data("pods")
        val foreign = Data("charging")
        val pending = hashMapOf(owned to false, foreign to true)
        assertTrue(NativeBatteryIslandSchema.removePendingIdentity(pending, owned))
        assertEquals(mapOf(foreign to true), pending)
        assertFalse(NativeBatteryIslandSchema.removePendingIdentity(pending, owned))
    }

    @Test fun equalButForeignDataIsNeverRemoved() {
        val owned = Data("pods")
        val foreign = Data("pods")
        val pending = hashMapOf(foreign to false)
        assertFalse(NativeBatteryIslandSchema.removePendingIdentity(pending, owned))
        assertSame(foreign, pending.keys.single())
    }

    @Test fun NativeMutationsCannotStrandTheOwnedPendingEntry() {
        val owned = Data("original")
        val pending = hashMapOf(owned to false)
        owned.key = "updated"
        assertTrue(NativeBatteryIslandSchema.removePendingIdentity(pending, owned))
        assertTrue(pending.isEmpty())
    }
}
