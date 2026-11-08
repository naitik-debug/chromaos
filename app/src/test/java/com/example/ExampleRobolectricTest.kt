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
@Config(sdk = [36])
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

  @Test
  fun `verify Recharts and web assets exist in chromaos assets`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assets = context.assets.list("chromaos")?.toList() ?: emptyList()
    assertNotNull(assets)
    org.junit.Assert.assertTrue("index.html should exist", assets.contains("index.html"))
    org.junit.Assert.assertTrue("tailwind.cdn.js should exist", assets.contains("tailwind.cdn.js"))
    org.junit.Assert.assertTrue("recharts.min.js should exist", assets.contains("recharts.min.js"))
    org.junit.Assert.assertTrue("react.production.min.js should exist", assets.contains("react.production.min.js"))
    org.junit.Assert.assertTrue("react-dom.production.min.js should exist", assets.contains("react-dom.production.min.js"))
  }
}

