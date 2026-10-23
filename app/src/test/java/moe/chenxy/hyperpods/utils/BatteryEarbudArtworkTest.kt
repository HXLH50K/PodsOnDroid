package moe.chenxy.hyperpods.utils

import android.graphics.drawable.Icon
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verifyNoInteractions

class BatteryEarbudArtworkTest {
    @Test fun wirelessAirPods5UsesTheExistingSeparatePictorialEarResources() {
        for (model in listOf("A3439", "A3440", "A3441")) {
            assertEquals(BatteryEarbudArtwork.Resources(R.drawable.airpods_4_left, R.drawable.airpods_4_right),
                BatteryEarbudArtwork.resources(model))
        }
    }

    @Test fun standardAirPods5KeepsItsOwnArtworkInsteadOfABatteryGauge() {
        for (model in listOf("A3531", "A3532", "A3533")) {
            assertEquals(BatteryEarbudArtwork.Resources(R.drawable.airpods_5_left, R.drawable.airpods_5_right),
                BatteryEarbudArtwork.resources(model))
        }
    }

    @Test fun unknownModelsUseNeutralEarbudPlaceholdersWithoutClaimingAKnownModel() {
        for (model in listOf(null, "", "unknown", "A9999")) {
            assertEquals(BatteryEarbudArtwork.Resources(R.drawable.pods_earbud_left, R.drawable.pods_earbud_right),
                BatteryEarbudArtwork.resources(model))
        }
    }

    @Test fun iconCreationNeverAppliesTintOrColorFiltersToTheProductArtwork() {
        val left = mock(Icon::class.java)
        val right = mock(Icon::class.java)
        mockStatic(Icon::class.java).use { icons ->
            icons.`when`<Icon> {
                Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.airpods_4_left)
            }.thenReturn(left)
            icons.`when`<Icon> {
                Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.airpods_4_right)
            }.thenReturn(right)
            val result = BatteryEarbudArtwork.icons("A3439")
            assertSame(left, result.first)
            assertSame(right, result.second)
            verifyNoInteractions(left, right)
        }
    }
}
