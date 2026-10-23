package moe.chenxy.hyperpods.utils

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class AppAppearanceTest {
    @Test fun newNavigationIsDefaultWithoutALegacyLayoutSwitch() {
        val preferences = mock(SharedPreferences::class.java)
        assertEquals(AppAppearance(), AppAppearance.read(preferences))
        assertTrue(AppAppearance().effectsAllowed(true, true, false, false))
        verify(preferences).getBoolean(AppAppearance.REDUCED, false)
        verifyNoMoreInteractions(preferences)
    }

    @Test fun allVisibilityPowerAndReadabilityGatesMustAllowEffects() {
        for (reduced in listOf(false, true))
            for (visible in listOf(false, true)) for (resumed in listOf(false, true))
                for (powerSave in listOf(false, true)) for (contrast in listOf(false, true)) {
                    val expected = !reduced && visible && resumed && !powerSave && !contrast
                    assertEquals(expected, AppAppearance(reduced)
                        .effectsAllowed(visible, resumed, powerSave, contrast))
                }
    }

    @Test fun legacyPreviewFlagDoesNotDisableTheNewNavigation() {
        val preferences = mock(SharedPreferences::class.java)
        assertEquals(AppAppearance(false), AppAppearance.read(preferences))
        `when`(preferences.getBoolean(AppAppearance.REDUCED, false)).thenReturn(true)
        assertEquals(AppAppearance(true), AppAppearance.read(preferences))
        verify(preferences, never()).getBoolean("glass_preview", false)
    }

    @Test fun saveTouchesOnlyLocalAppearanceKeysWithoutClearingOtherSettings() {
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.commit()).thenReturn(true)
        assertTrue(AppAppearance(true).save(preferences))
        verify(editor).putBoolean(AppAppearance.REDUCED, true)
        verify(editor).commit()
        verifyNoMoreInteractions(editor)
    }

    @Test fun failedPersistenceIsNotReportedAsSuccess() {
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.commit()).thenReturn(false)
        assertFalse(AppAppearance(true).save(preferences))
    }

    @Test(expected = ClassCastException::class)
    fun corruptStoredTypeIsSurfacedToTheUi() {
        val preferences = mock(SharedPreferences::class.java)
        `when`(preferences.getBoolean(AppAppearance.REDUCED, false)).thenThrow(ClassCastException())
        AppAppearance.read(preferences)
    }
}
