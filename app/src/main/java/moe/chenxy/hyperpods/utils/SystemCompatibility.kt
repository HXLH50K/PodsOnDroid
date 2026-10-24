package moe.chenxy.hyperpods.utils

import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class PhoneSystem { HYPER_OS, XIAOMI, VIVO, OPPO, SAMSUNG, AOSP, OTHER }

enum class SystemFeature { NATIVE_BATTERY_ISLAND, SYSTEM_HEADPHONE_CARD, HEADSET_STATUS_ICON }

enum class SystemHook { BLUETOOTH_CONTROL, HYPER_OS_BLUETOOTH, HYPER_OS_SYSTEM_UI }

/** These are implemented adapters, not a claim that every ROM version has passed acceptance. */
class SystemProfile private constructor(
    val system: PhoneSystem,
    private val features: Set<SystemFeature>,
    private val hooks: Map<String, SystemHook>,
) {
    fun supports(feature: SystemFeature): Boolean = feature in features
    fun hookFor(packageName: String): SystemHook? = hooks[packageName]
    val modulePackages: List<String> get() = hooks.keys.toList()
    val restartProcesses: Set<String>
        get() = (modulePackages + if (hooks[HyperPodsBroadcasts.XIAOMI_BLUETOOTH] == SystemHook.HYPER_OS_BLUETOOTH)
            listOf("${HyperPodsBroadcasts.XIAOMI_BLUETOOTH}:ui") else emptyList()).toSet()

    companion object {
        fun forSystem(system: PhoneSystem): SystemProfile = when (system) {
            PhoneSystem.HYPER_OS -> SystemProfile(system, setOf(
                SystemFeature.NATIVE_BATTERY_ISLAND, SystemFeature.SYSTEM_HEADPHONE_CARD,
                SystemFeature.HEADSET_STATUS_ICON,
            ), linkedMapOf(
                HyperPodsBroadcasts.BLUETOOTH to SystemHook.BLUETOOTH_CONTROL,
                HyperPodsBroadcasts.XIAOMI_BLUETOOTH to SystemHook.HYPER_OS_BLUETOOTH,
                HyperPodsBroadcasts.SYSTEM_UI to SystemHook.HYPER_OS_SYSTEM_UI,
            ))
            else -> SystemProfile(system, emptySet(), mapOf(
                HyperPodsBroadcasts.BLUETOOTH to SystemHook.BLUETOOTH_CONTROL,
            ))
        }

        fun detect(manufacturer: String, brand: String, hyperOSVersion: String): SystemProfile {
            if (hyperOSVersion.trim().toIntOrNull()?.let { it > 0 } == true) return forSystem(PhoneSystem.HYPER_OS)
            val identities = setOf(manufacturer.trim().lowercase(Locale.ROOT), brand.trim().lowercase(Locale.ROOT))
            val system = when {
                identities.any { it in setOf("xiaomi", "redmi", "poco") } -> PhoneSystem.XIAOMI
                identities.any { it in setOf("vivo", "iqoo") } -> PhoneSystem.VIVO
                identities.any { it in setOf("oppo", "oneplus", "realme") } -> PhoneSystem.OPPO
                "samsung" in identities -> PhoneSystem.SAMSUNG
                identities.any { it in setOf("google", "aosp", "android", "generic") } -> PhoneSystem.AOSP
                else -> PhoneSystem.OTHER
            }
            return forSystem(system)
        }
    }
}

internal object SystemEnvironment {
    val hyperOSVersion: String by lazy { readProperty("ro.mi.os.version.code") }
    val current: SystemProfile by lazy {
        SystemProfile.detect(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty(), hyperOSVersion)
    }

    fun readProperty(name: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/getprop", name))
            try {
                if (!process.waitFor(1, TimeUnit.SECONDS) || process.exitValue() != 0) {
                    Log.w("PODS-Compatibility", "Unable to read ROM property $name; OEM features remain unavailable")
                    ""
                } else process.inputStream.bufferedReader().use { it.readLine().orEmpty() }
            } finally {
                process.destroy()
            }
        } catch (error: IOException) {
            Log.w("PODS-Compatibility", "ROM property $name is unavailable", error)
            ""
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w("PODS-Compatibility", "ROM property query interrupted", error)
            ""
        }
    }
}
