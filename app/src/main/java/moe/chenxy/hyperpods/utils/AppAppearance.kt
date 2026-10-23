package moe.chenxy.hyperpods.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences

/** Local UI preferences only; never included in headset policy or host IPC. */
internal data class AppAppearance(
    val reducedTransparency: Boolean = false,
) {
    fun effectsAllowed(visible: Boolean, resumed: Boolean, powerSave: Boolean, highContrast: Boolean): Boolean =
        !reducedTransparency && visible && resumed && !powerSave && !highContrast

    @SuppressLint("UseKtx") // Retain commit's result so persistence failures are reported.
    fun save(preferences: SharedPreferences): Boolean = preferences.edit()
        .putBoolean(REDUCED, reducedTransparency)
        .commit()

    companion object {
        const val FILE = "pods_app_appearance"
        const val REDUCED = "reduced_transparency"

        fun read(preferences: SharedPreferences) = AppAppearance(
            preferences.getBoolean(REDUCED, false),
        )
    }
}
