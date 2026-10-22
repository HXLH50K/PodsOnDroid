package moe.chenxy.hyperpods.hook

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telecom.TelecomManager
import android.util.Log
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts

/** A negative call check is trusted only when both host services and phone-state access are available. */
internal object HostCallState {
    @SuppressLint("MissingPermission") // Evaluated against the host UID, not the module app's permissions.
    fun read(context: Context, host: String): String {
        if (host != HyperPodsBroadcasts.BLUETOOTH) return "unknown"
        return try {
            val allowed = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == PackageManager.PERMISSION_GRANTED
            val telecom = context.getSystemService(TelecomManager::class.java)
            val audio = context.getSystemService(AudioManager::class.java)
            if (!allowed || telecom == null || audio == null) {
                Log.w("Art_Chen", "Call-state check unavailable: permission or service missing")
                "unknown"
            } else if (telecom.isInCall || audio.mode != AudioManager.MODE_NORMAL) {
                "busy"
            } else {
                "idle"
            }
        } catch (_: SecurityException) {
            Log.w("Art_Chen", "Call-state check unavailable: host access denied")
            "unknown"
        } catch (_: RuntimeException) {
            Log.w("Art_Chen", "Call-state check unavailable: host service failed")
            "unknown"
        }
    }
}
