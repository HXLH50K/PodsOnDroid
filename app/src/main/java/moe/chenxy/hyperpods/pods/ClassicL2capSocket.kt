package moe.chenxy.hyperpods.pods

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.AttributionSource
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

internal object ClassicL2capSocket {
    fun create(context: Context, device: BluetoothDevice): BluetoothSocket =
        create(BluetoothSocket::class.java, context, device)

    internal fun <T> create(socketClass: Class<T>, context: Context, device: BluetoothDevice): T {
        val devicePrefix = listOf(
            BluetoothDevice::class.java,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            ParcelUuid::class.java,
        )
        val adapterPrefix = listOf(BluetoothAdapter::class.java) + devicePrefix
        val tailTypes = setOf(
            Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType, String::class.java, AttributionSource::class.java,
        )
        // Android 15/16 use device-first constructors; Android 17 adds the adapter.
        val match = listOf(adapterPrefix, devicePrefix).firstNotNullOfOrNull { prefix ->
            socketClass.declaredConstructors
                .filter { candidate ->
                    val types = candidate.parameterTypes.toList()
                    types.take(prefix.size) == prefix && types.drop(prefix.size).all { it in tailTypes }
                }
                .minByOrNull { it.parameterCount }
                ?.let { it to prefix.size }
        } ?: throw NoSuchMethodException(
            "No compatible classic L2CAP BluetoothSocket constructor: " +
                socketClass.declaredConstructors.joinToString { it.toString() }
        )
        val (constructor, prefixSize) = match
        val args = mutableListOf<Any?>()
        if (prefixSize == adapterPrefix.size) args += requireNotNull(BluetoothAdapter.getDefaultAdapter()) {
            "Bluetooth adapter unavailable for classic L2CAP socket"
        }
        args.addAll(listOf(
            device,
            3, // TYPE_L2CAP (BR/EDR), not TYPE_L2CAP_LE used by createL2capChannel().
            true,
            true,
            0x1001,
            ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
        ))
        constructor.parameterTypes.drop(prefixSize).forEach { type ->
            args += when (type) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                String::class.java -> "HyperPods"
                AttributionSource::class.java -> context.attributionSource
                else -> error("Unsupported classic L2CAP constructor parameter: $type")
            }
        }
        constructor.isAccessible = true
        Log.i("PODS-L2CAPSocket", "Using BluetoothSocket constructor: $constructor")
        return socketClass.cast(constructor.newInstance(*args.toTypedArray()))
    }
}
