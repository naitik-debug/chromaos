package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LauncherPreferences
import com.example.model.ChromaPalette
import com.example.model.IconMaskShape
import com.example.model.OsPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChromaOS", appName)
  }

  @Test
  fun `verify ChromaOS palettes and presets count`() {
    assertEquals(10, ChromaPalette.ALL_PALETTES.size)
    assertEquals(8, OsPreset.PRESETS.size)
    assertNotNull(ChromaPalette.getById("slate-ochre"))
  }

  @Test
  fun `test launcher preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = LauncherPreferences(context)
    val initialSettings = prefs.loadSettings()
    assertEquals("pixel", initialSettings.activeOsId)

    val updatedSettings = initialSettings.copy(
      activeOsId = "android17",
      activePaletteId = "espresso-roast",
      iconShape = IconMaskShape.COOKIE
    )
    prefs.saveSettings(updatedSettings)

    val reloaded = prefs.loadSettings()
    assertEquals("android17", reloaded.activeOsId)
    assertEquals("espresso-roast", reloaded.activePaletteId)
    assertEquals(IconMaskShape.COOKIE, reloaded.iconShape)
  }
}
