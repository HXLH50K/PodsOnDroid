package moe.chenxy.hyperpods.utils

import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey as Key
import org.junit.Assert.*
import org.junit.Test

class HeadphoneCompatibilityTest {
    @Test fun declaredBasicAndTimingFeaturesPreserveEveryRegisteredModel() {
        AirPodsModels.models.forEach { model ->
            assertTrue(model.supports(Capability.MICROPHONE_SELECTION))
            assertTrue(model.supports(Capability.TONE_VOLUME))
            val timing = model !is AirPods && model !is AirPods2
            assertEquals(timing, model.supports(Capability.PRESS_TIMING))
            assertEquals(timing, PodsSettings.supports(Key.PRESS_SPEED, model))
            assertEquals(timing, PodsSettings.supports(Key.HOLD_DURATION, model))
            assertEquals(model.supports(Capability.SWIPE_FOR_VOLUME),
                PodsSettings.supports(Key.ADJUST_VOLUME_BY_SWIPER, model))
        }
    }

    @Test fun protocolAndRecoveryQuirksBelongToTheModelTable() {
        val ap5 = setOf("A3531", "A3532", "A3533", "A3439", "A3440", "A3441")
        val recovery = setOf("A3439", "A3440", "A3441")
        AirPodsModels.models.forEach { model ->
            model.modelNumber.forEach { number ->
                assertEquals(if (number in ap5) HeadTrackingStream.Variant.AIRPODS5
                    else HeadTrackingStream.Variant.ALTERNATE, model.headTrackingVariant)
                assertEquals(number in recovery, model.needsSinglePodAncRecovery)
            }
        }
    }

    @Test fun futureModelsDoNotInheritUndeclaredSettingFeatures() {
        val model = AirPodsBase(listOf("future"), "Future", budCaseRes = 0, budsRes = 0,
            leftBudsRes = 0, rightBudsRes = 0, caseRes = 0, capabilities = emptySet())
        for (key in listOf(Key.MICROPHONE_MODE, Key.CHIME_VOLUME, Key.PRESS_SPEED, Key.ADJUST_VOLUME_BY_SWIPER))
            assertFalse(PodsSettings.supports(key, model))
        assertFalse(model.needsSinglePodAncRecovery)
        assertNull(AirPodsModels.getModelByModelNumber("future"))
    }
}
