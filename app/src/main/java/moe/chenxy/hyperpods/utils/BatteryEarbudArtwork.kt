package moe.chenxy.hyperpods.utils

import android.graphics.drawable.Icon
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R

/** Product artwork keeps its original material and color; thresholds belong to text only. */
internal object BatteryEarbudArtwork {
    data class Resources(val left: Int, val right: Int)

    fun resources(modelNumber: String?): Resources {
        val model = PodsIslandData.modelForIcons(modelNumber)
        return Resources(model?.leftBudsRes ?: R.drawable.pods_earbud_left,
            model?.rightBudsRes ?: R.drawable.pods_earbud_right)
    }

    fun icons(modelNumber: String?): Pair<Icon, Icon> {
        val ids = resources(modelNumber)
        return Icon.createWithResource(BuildConfig.APPLICATION_ID, ids.left) to
            Icon.createWithResource(BuildConfig.APPLICATION_ID, ids.right)
    }
}
