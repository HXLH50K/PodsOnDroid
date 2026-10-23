package moe.chenxy.hyperpods.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences

/** Local UI preferences only; never included in headset policy or host IPC. */
internal data class AppAppearance(
    val glassEnabled: Boolean = false,
    val reducedTransparency: Boolean = false,
) {
    fun effectsAllowed(visible: Boolean, resumed: Boolean, powerSave: Boolean, highContrast: Boolean): Boolean =
        glassEnabled && !reducedTransparency && visible && resumed && !powerSave && !highContrast

    @SuppressLint("UseKtx") // Retain commit's result so persistence failures are reported.
    fun save(preferences: SharedPreferences): Boolean = preferences.edit()
        .putBoolean(GLASS, glassEnabled)
        .putBoolean(REDUCED, reducedTransparency)
        .commit()

    companion object {
        const val FILE = "pods_app_appearance"
        const val GLASS = "glass_preview"
        const val REDUCED = "reduced_transparency"

        fun read(preferences: SharedPreferences) = AppAppearance(
            preferences.getBoolean(GLASS, false),
            preferences.getBoolean(REDUCED, false),
        )
    }
}
