package moe.chenxy.hyperpods.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Synthetic parser input, not a headset capture or proof of hardware compatibility.
 * Envelope and field offsets follow LibrePods 89356c0 Packets.kt and HeadOrientation.kt. */
internal fun syntheticHeadTrackingPacket(horizontal: Int = 0, vertical: Int = 0,
    o2: Int = 0, o3: Int = 0, size: Int = 70, report: Int = 0x44): ByteArray {
    val bytes = (byteArrayOf(4, 0, 4, 0, 0x17, 0, 0, 0, 0x10, 0, report.toByte(), 0) + ByteArray(58)).copyOf(size)
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
        putShort(45, o2.toShort())
        putShort(47, o3.toShort())
        putShort(51, horizontal.toShort())
        putShort(53, vertical.toShort())
    }
    return bytes
}
