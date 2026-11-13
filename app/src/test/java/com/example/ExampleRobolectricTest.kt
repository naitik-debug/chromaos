package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.WallpaperTarget
import com.example.model.ChromaTheme
import com.example.theme.ChromaThemeManager
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider

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
    fun `verify WallpaperTarget parsing`() {
        assertEquals(WallpaperTarget.HOME, WallpaperTarget.fromString("home"))
        assertEquals(WallpaperTarget.HOME, WallpaperTarget.fromString(" HOME "))
        assertEquals(WallpaperTarget.LOCK, WallpaperTarget.fromString("lock"))
        assertEquals(WallpaperTarget.BOTH, WallpaperTarget.fromString("both"))
        assertEquals(WallpaperTarget.BOTH, WallpaperTarget.fromString(null))
    }

    @Test
    fun `verify offline web assets fonts and phosphor icons exist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rootAssets = context.assets.list("chromaos")?.toList() ?: emptyList()
        assertTrue("index.html should exist", rootAssets.contains("index.html"))
        assertTrue("tailwind.cdn.js should exist", rootAssets.contains("tailwind.cdn.js"))
        assertTrue("recharts.min.js should exist", rootAssets.contains("recharts.min.js"))
        assertTrue("react.production.min.js should exist", rootAssets.contains("react.production.min.js"))
        assertTrue("react-dom.production.min.js should exist", rootAssets.contains("react-dom.production.min.js"))

        val fontAssets = context.assets.list("chromaos/fonts")?.toList() ?: emptyList()
        listOf(
            "syne.ttf",
            "cormorant_garamond.ttf",
            "plus_jakarta_sans.ttf",
            "space_grotesk.ttf",
            "jetbrains_mono.ttf",
            "outfit.ttf"
        ).forEach { fontFile ->
            assertTrue("Font asset $fontFile should exist", fontAssets.contains(fontFile))
        }

        val phosphorStyles = context.assets.list("chromaos/phosphor/regular")?.toList() ?: emptyList()
        assertTrue("Phosphor regular style.css should exist", phosphorStyles.contains("style.css"))
    }

    @Test
    fun `verify manifest HOME launcher filter and policy compliant permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertFalse("Must not request READ_EXTERNAL_STORAGE", permissions.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        assertFalse("Must not request READ_MEDIA_IMAGES", permissions.contains("android.permission.READ_MEDIA_IMAGES"))
        assertFalse("Must not declare QUERY_ALL_PACKAGES", permissions.contains("android.permission.QUERY_ALL_PACKAGES"))
        assertTrue("Should declare SET_WALLPAPER", permissions.contains(android.Manifest.permission.SET_WALLPAPER))

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            `package` = context.packageName
        }
        val resolvedHome = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue("MainActivity must handle CATEGORY_HOME", resolvedHome.any { it.activityInfo.packageName == context.packageName })

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = context.packageName
        }
        val resolvedLauncher = pm.queryIntentActivities(launcherIntent, 0)
        assertTrue("MainActivity must also handle CATEGORY_LAUNCHER", resolvedLauncher.any { it.activityInfo.packageName == context.packageName })
    }

    @Test
    fun `verify ChromaTheme validation clamping and roundtrip JSON serialization`() {
        val inputJson = JSONObject().apply {
            put("themeId", "abyssal-lagoon")
            put("themeName", "Abyssal Lagoon")
            put("osPreset", "nothing")
            put("colors", JSONArray(listOf("#05161A", "#072e33", "#0c7075", "#abc", "#6DA5C0")))
            put("fontPack", "ndot")
            put("iconStyle", "monochrome")
            put("iconShape", "50%")
            put("iconSize", 99) // clamped to 64
            put("homeGridColumns", 8) // clamped to 5
            put("dockStyle", "floating")
            put("dockOpacity", 0.01) // clamped to 0.15
            put("dockCornerRadius", 99) // clamped to 36
            put("drawerBackgroundMode", "oled")
            put("drawerColumns", 1) // clamped to 3
            put("drawerSortOrder", "za")
            put("drawerSearchStyle", "minimal")
            put("controlSurfaceMode", "high-contrast")
            put("controlButtonShape", "sharp")
            put("controlCornerRadius", 2) // clamped to 8
            put("controlTransparency", 1.5) // clamped to 0.98
            put("controlIconAppearance", "outlined")
            put("lockDateStyle", "numeric")
            put("lockTextColorMode", "custom")
            put("lockCustomTextColor", "#f0a")
            put("lockBackdropDim", 0.99) // clamped to 0.75
            put("homePinnedPackages", JSONArray(listOf("com.android.dialer", "com.android.dialer", "", "com.android.settings")))
            put("dockPinnedPackages", JSONArray(listOf("com.android.chrome", "com.android.chrome", "a", "b", "c", "d", "e")))
        }.toString()

        val parsed = ChromaTheme.fromJson(inputJson)
        assertEquals("abyssal-lagoon", parsed.themeId)
        assertEquals("nothing", parsed.osPreset)
        assertEquals("#AABBCC", parsed.paletteColors[3])
        assertEquals("ndot", parsed.fontPack)
        assertEquals("monochrome", parsed.iconStyle)
        assertEquals(64, parsed.iconSize)
        assertEquals(5, parsed.homeGridColumns)
        assertEquals("floating", parsed.dockStyle)
        assertEquals(0.15f, parsed.dockOpacity, 0.001f)
        assertEquals(36, parsed.dockCornerRadius)
        assertEquals("oled", parsed.drawerBackgroundMode)
        assertEquals(3, parsed.drawerColumns)
        assertEquals("za", parsed.drawerSortOrder)
        assertEquals("minimal", parsed.drawerSearchStyle)
        assertEquals("high-contrast", parsed.controlSurfaceMode)
        assertEquals("sharp", parsed.controlButtonShape)
        assertEquals(8, parsed.controlCornerRadius)
        assertEquals(0.98f, parsed.controlTransparency, 0.001f)
        assertEquals("outlined", parsed.controlIconAppearance)
        assertEquals("numeric", parsed.lockDateStyle)
        assertEquals("custom", parsed.lockTextColorMode)
        assertEquals("#FF00AA", parsed.lockCustomTextColor)
        assertEquals(0.75f, parsed.lockBackdropDim, 0.001f)
        assertEquals(listOf("com.android.dialer", "com.android.settings"), parsed.homePinnedPackages)
        assertEquals(listOf("com.android.chrome", "a", "b", "c", "d"), parsed.dockPinnedPackages)

        val reparsed = ChromaTheme.fromJson(parsed.toJson().toString())
        assertEquals(parsed.themeName, reparsed.themeName)
        assertEquals(parsed.iconStyle, reparsed.iconStyle)
        assertEquals(parsed.drawerBackgroundMode, reparsed.drawerBackgroundMode)
        assertEquals(parsed.controlSurfaceMode, reparsed.controlSurfaceMode)
        assertEquals(parsed.lockDateStyle, reparsed.lockDateStyle)
        assertEquals(parsed.homePinnedPackages, reparsed.homePinnedPackages)
        assertEquals(parsed.dockPinnedPackages, reparsed.dockPinnedPackages)
    }

    @Test
    fun `verify ChromaThemeManager persists switches resets and returns honest apply outcome`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager1 = ChromaThemeManager(context)
        manager1.resetToDefault()

        val customJson = JSONObject().apply {
            put("themeId", "matcha-botanical")
            put("themeName", "Matcha Botanical")
            put("osPreset", "pixel")
            put("fontPack", "space")
            put("iconStyle", "outlined")
            put("homeGridColumns", 5)
            put("dockStyle", "tinted")
            put("drawerBackgroundMode", "light")
            put("drawerColumns", 5)
            put("drawerSortOrder", "system")
            put("controlSurfaceMode", "solid-card")
            put("controlButtonShape", "squircle")
            put("lockDateStyle", "long")
            put("wallpaperMode", "botanical")
        }.toString()

        val outcome = manager1.applyTheme(customJson, applyWallpaper = false)
        assertTrue(outcome.success)
        assertEquals("Matcha Botanical", outcome.theme.themeName)
        assertEquals("Matcha Botanical", manager1.themeFlow.value.themeName)
        assertTrue(outcome.appliedFeatures.any { it.contains("outlined") })
        assertTrue(outcome.appliedFeatures.any { it.contains("light") })
        assertTrue(outcome.appliedFeatures.any { it.contains("solid-card") })
        assertTrue(outcome.restrictedFeatures.any { it.contains("Keyguard") })

        // Verify persistence across a new ChromaThemeManager instance
        val manager2 = ChromaThemeManager(context)
        val restored = manager2.getCurrentTheme()
        assertEquals("matcha-botanical", restored.themeId)
        assertEquals("space", restored.fontPack)
        assertEquals("outlined", restored.iconStyle)
        assertEquals("light", restored.drawerBackgroundMode)
        assertEquals("solid-card", restored.controlSurfaceMode)
        assertEquals("long", restored.lockDateStyle)

        val reset = manager2.resetToDefault()
        assertNotEquals("matcha-botanical", reset.themeId)
        assertEquals("espresso-roast", reset.themeId)
    }

    @Test
    fun `verify MainActivity bridge theme lifecycle preferences and system hardware payloads`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        val bridge = activity.ChromaAndroidBridge(activity)

        val sysInfo = JSONObject(bridge.getSystemInfo())
        assertEquals("ChromaOS", sysInfo.getString("appName"))
        assertEquals(BuildConfig.VERSION_NAME, sysInfo.getString("versionName"))
        assertEquals(BuildConfig.VERSION_CODE, sysInfo.getInt("versionCode"))

        val draftPayload = JSONObject().apply {
            put("themeId", "nordic-fjord")
            put("themeName", "Nordic Fjord")
            put("activeOs", "ios")
            put("iconStyle", "nothing")
            put("drawerBackgroundMode", "oled")
            put("controlButtonShape", "rounded")
        }.toString()

        val savedDraft = JSONObject(bridge.saveThemeDraft(draftPayload))
        assertEquals("Nordic Fjord", savedDraft.getString("themeName"))
        assertEquals("nothing", savedDraft.getString("iconStyle"))

        val currentTheme = JSONObject(bridge.getCurrentTheme())
        assertEquals("nordic-fjord", currentTheme.getString("themeId"))
        assertEquals("oled", currentTheme.getString("drawerBackgroundMode"))
        assertEquals("rounded", currentTheme.getString("controlButtonShape"))

        val loadedPrefs = JSONObject(bridge.loadPreferences())
        assertEquals("Nordic Fjord", loadedPrefs.getString("themeName"))

        val resetTheme = JSONObject(bridge.resetThemeToDefault())
        assertEquals("espresso-roast", resetTheme.getString("themeId"))

        val batteryPayload = JSONObject(bridge.getBatteryInfo())
        assertTrue(batteryPayload.has("phone"))
        assertTrue(batteryPayload.has("connectedDevices"))
        assertTrue(batteryPayload.has("hasBtPermission"))

        val hardwarePayload = JSONObject(bridge.getHardwareState())
        assertTrue(hardwarePayload.has("volume"))
        assertTrue(hardwarePayload.has("brightness"))
        assertTrue(hardwarePayload.has("isDefaultLauncher"))
        assertEquals(BuildConfig.VERSION_NAME, hardwarePayload.getString("versionName"))

        val launchMissing = JSONObject(bridge.launchApp("com.nonexistent.fake.pkg"))
        assertFalse(launchMissing.getBoolean("success"))
        assertTrue(launchMissing.getString("error").isNotBlank())

        val launchBadComp = JSONObject(bridge.launchComponent("com.nonexistent.fake.pkg", "com.nonexistent.fake.MainActivity"))
        assertFalse(launchBadComp.getBoolean("success"))
        assertTrue(launchBadComp.getString("error").isNotBlank())

        val flashResult = JSONObject(bridge.toggleFlashlight(true))
        assertTrue(flashResult.has("success"))
        assertTrue(flashResult.has("enabled"))
        assertTrue(flashResult.has("error"))

        assertEquals(
            "isLaunchedAsHome should reflect default launcher status on normal launch",
            activity.isDefaultHomeLauncher(),
            bridge.isLaunchedAsHome()
        )

        val homeController = Robolectric.buildActivity(
            MainActivity::class.java,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        ).create()
        val homeBridge = homeController.get().ChromaAndroidBridge(homeController.get())
        assertTrue("Launching with CATEGORY_HOME must report isLaunchedAsHome=true", homeBridge.isLaunchedAsHome())
        homeController.destroy()

        controller.destroy()
    }
}
