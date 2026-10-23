package moe.chenxy.hyperpods.utils

import android.media.AudioManager

internal class PanelMediaVolume(private val audio: AudioManager) {
    data class Reading(val level: Int, val minimum: Int, val maximum: Int, val adjustable: Boolean)

    fun read(): Reading {
        val minimum = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val level = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        check(minimum >= 0 && maximum >= minimum && level in minimum..maximum) { "invalid_media_volume" }
        return Reading(level, minimum, maximum, !audio.isVolumeFixed && maximum > minimum)
    }

    fun set(level: Int): Reading {
        val current = read()
        check(current.adjustable) { "media_volume_fixed" }
        require(level in current.minimum..current.maximum) { "media_volume_out_of_range" }
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        // Android may cap a user request; always display its actual resulting level.
        return read()
    }
}
