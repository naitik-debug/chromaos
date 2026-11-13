package com.example.model

import com.example.data.WallpaperTarget
import org.json.JSONArray
import org.json.JSONObject

data class ChromaTheme(
    val themeId: String = "espresso-roast",
    val themeName: String = "Espresso Roast",
    val osPreset: String = "android17",
    val isCustomRemix: Boolean = false,
    val activePaletteIndex: Int = 0,

    // Core Color Tokens
    val backgroundColor: String = "#291C0E",
    val secondaryColor: String = "#6E473B",
    val primaryColor: String = "#A78D78",
    val accentColor: String = "#BEB5A9",
    val surfaceColor: String = "#1B1510",
    val cardColor: String = "#241C16",
    val textColor: String = "#FFF8F0",
    val paletteColors: List<String> = listOf("#291C0E", "#6E473B", "#A78D78", "#BEB5A9", "#E1D4C2"),

    // Typography
    val fontPack: String = "editorial",

    // Home Screen & Icon Customization
    val iconStyle: String = "tinted", // "tinted" | "monochrome" | "outlined" | "nothing" | "native"
    val iconShape: String = "cookie", // "cookie" | "26%" | "50%" | "34% 16% 34% 16%" | "14%"
    val iconSize: Int = 54,
    val iconSpacing: String = "balanced", // "compact" | "balanced" | "spacious"
    val homeGridColumns: Int = 4, // 3 | 4 | 5
    val showAppLabels: Boolean = true,
    val showFolders: Boolean = true,
    val folderStyle: String = "glass", // "glass" | "tinted" | "solid" | "outline"

    // Dock Customization
    val dockStyle: String = "frosted", // "frosted" | "tinted" | "solid" | "floating" | "minimal"
    val dockBackgroundColor: String = "#12181C",
    val dockOpacity: Float = 0.72f,
    val dockCornerRadius: Int = 28,

    // App Drawer Customization
    val drawerBackgroundMode: String = "themed", // "themed" | "dark" | "light" | "oled"
    val drawerColumns: Int = 4, // 3 | 4 | 5
    val drawerIconSize: Int = 52,
    val drawerShowLabels: Boolean = true,
    val drawerSearchStyle: String = "pill", // "pill" | "rounded" | "minimal"
    val drawerSortOrder: String = "az", // "az" | "za" | "system"
    val drawerCornerRadius: Int = 28,

    // Control Panel Customization
    val controlPanelStyle: String = "android17",
    val controlSurfaceMode: String = "themed-glass", // "themed-glass" | "solid-card" | "translucent" | "high-contrast"
    val controlButtonShape: String = "pill", // "pill" | "rounded" | "squircle" | "sharp"
    val controlCornerRadius: Int = 22,
    val controlTransparency: Float = 0.78f,
    val controlIconAppearance: String = "filled", // "filled" | "tinted" | "monochrome" | "outlined"

    // Widgets & Components
    val widgetOpacity: Float = 0.88f,
    val widgetCornerRadius: Int = 24,
    val customComponents: Map<String, String> = defaultComponentsMap("android17"),

    // Lock Screen Customization (ChromaOS preview mode; does not bypass Android Keyguard)
    val lockDateStyle: String = "short", // "short" | "long" | "numeric" | "minimal"
    val lockTextColorMode: String = "accent", // "accent" | "light" | "contrast" | "custom"
    val lockCustomTextColor: String = "#FFFFFF",
    val lockShowWidgets: Boolean = true,
    val lockBackdropDim: Float = 0.20f,

    // Wallpaper Configuration
    val wallpaperMode: String = "mesh", // "mesh" | "botanical" | "oled" | "custom"
    val wallpaperTarget: String = "both", // "home" | "lock" | "both"
    val customWallpaperDataUrl: String = "",

    // Visibility Toggles & Pinned Launcher Shortcuts
    val showWeatherPill: Boolean = true,
    val showBatteryPill: Boolean = true,
    val showLockStatusText: Boolean = true,
    val appVisibility: Map<String, Boolean> = emptyMap(),
    val homePinnedPackages: List<String> = emptyList(),
    val dockPinnedPackages: List<String> = emptyList(),

    val updatedAtEpochMs: Long = 0L
) {
    fun validated(): ChromaTheme {
        val cleanColors = paletteColors
            .mapNotNull { normalizeHex(it) }
            .ifEmpty { listOf("#291C0E", "#6E473B", "#A78D78", "#BEB5A9", "#E1D4C2") }
        val paddedColors = if (cleanColors.size >= 5) cleanColors.take(5) else {
            cleanColors + listOf("#291C0E", "#6E473B", "#A78D78", "#BEB5A9", "#E1D4C2").drop(cleanColors.size)
        }

        val validIconStyles = setOf("tinted", "monochrome", "outlined", "nothing", "native")
        val validSpacing = setOf("compact", "balanced", "spacious")
        val validFolderStyles = setOf("glass", "tinted", "solid", "outline")
        val validDockStyles = setOf("frosted", "tinted", "solid", "floating", "minimal")
        val validDrawerModes = setOf("themed", "dark", "light", "oled")
        val validSearchStyles = setOf("pill", "rounded", "minimal")
        val validSortOrders = setOf("az", "za", "system")
        val validControlSurfaces = setOf("themed-glass", "solid-card", "translucent", "high-contrast")
        val validControlShapes = setOf("pill", "rounded", "squircle", "sharp")
        val validControlIcons = setOf("filled", "tinted", "monochrome", "outlined")
        val validDateStyles = setOf("short", "long", "numeric", "minimal")
        val validLockColorModes = setOf("accent", "light", "contrast", "custom")
        val validWallpaperModes = setOf("mesh", "botanical", "oled", "custom")
        val validFonts = setOf("syne", "editorial", "jakarta", "space", "ndot")
        val validOs = setOf("android17", "pixel", "ios", "nothing", "hyperos", "poco", "coloros", "originos")

        val safeOs = if (osPreset in validOs) osPreset else "android17"
        val safeControlOs = if (controlPanelStyle in validOs) controlPanelStyle else safeOs
        val mergedComponents = defaultComponentsMap(safeOs).toMutableMap()
        customComponents.forEach { (k, v) ->
            if (v.isNotBlank()) mergedComponents[k] = v
        }

        return copy(
            themeId = themeId.ifBlank { "custom-theme" },
            themeName = themeName.ifBlank { "Custom Theme" },
            osPreset = safeOs,
            activePaletteIndex = activePaletteIndex.coerceAtLeast(0),
            backgroundColor = normalizeHex(backgroundColor) ?: paddedColors[0],
            secondaryColor = normalizeHex(secondaryColor) ?: paddedColors[1],
            primaryColor = normalizeHex(primaryColor) ?: paddedColors[2],
            accentColor = normalizeHex(accentColor) ?: paddedColors[3],
            surfaceColor = normalizeHex(surfaceColor) ?: paddedColors[0],
            cardColor = normalizeHex(cardColor) ?: paddedColors[1],
            textColor = normalizeHex(textColor) ?: paddedColors[4],
            paletteColors = paddedColors,
            fontPack = if (fontPack in validFonts) fontPack else "editorial",
            iconStyle = if (iconStyle in validIconStyles) iconStyle else "tinted",
            iconShape = iconShape.ifBlank { "cookie" },
            iconSize = iconSize.coerceIn(40, 64),
            iconSpacing = if (iconSpacing in validSpacing) iconSpacing else "balanced",
            homeGridColumns = homeGridColumns.coerceIn(3, 5),
            folderStyle = if (folderStyle in validFolderStyles) folderStyle else "glass",
            dockStyle = if (dockStyle in validDockStyles) dockStyle else "frosted",
            dockBackgroundColor = normalizeHex(dockBackgroundColor) ?: paddedColors[0],
            dockOpacity = dockOpacity.coerceIn(0.15f, 1.0f),
            dockCornerRadius = dockCornerRadius.coerceIn(8, 36),
            drawerBackgroundMode = if (drawerBackgroundMode in validDrawerModes) drawerBackgroundMode else "themed",
            drawerColumns = drawerColumns.coerceIn(3, 5),
            drawerIconSize = drawerIconSize.coerceIn(40, 64),
            drawerSearchStyle = if (drawerSearchStyle in validSearchStyles) drawerSearchStyle else "pill",
            drawerSortOrder = if (drawerSortOrder in validSortOrders) drawerSortOrder else "az",
            drawerCornerRadius = drawerCornerRadius.coerceIn(12, 36),
            controlPanelStyle = safeControlOs,
            controlSurfaceMode = if (controlSurfaceMode in validControlSurfaces) controlSurfaceMode else "themed-glass",
            controlButtonShape = if (controlButtonShape in validControlShapes) controlButtonShape else "pill",
            controlCornerRadius = controlCornerRadius.coerceIn(8, 32),
            controlTransparency = controlTransparency.coerceIn(0.25f, 0.98f),
            controlIconAppearance = if (controlIconAppearance in validControlIcons) controlIconAppearance else "filled",
            widgetOpacity = widgetOpacity.coerceIn(0.15f, 1.0f),
            widgetCornerRadius = widgetCornerRadius.coerceIn(10, 34),
            customComponents = mergedComponents,
            lockDateStyle = if (lockDateStyle in validDateStyles) lockDateStyle else "short",
            lockTextColorMode = if (lockTextColorMode in validLockColorModes) lockTextColorMode else "accent",
            lockCustomTextColor = normalizeHex(lockCustomTextColor) ?: "#FFFFFF",
            lockBackdropDim = lockBackdropDim.coerceIn(0.0f, 0.75f),
            wallpaperMode = if (wallpaperMode in validWallpaperModes) wallpaperMode else "mesh",
            wallpaperTarget = WallpaperTarget.fromString(wallpaperTarget).name.lowercase(),
            homePinnedPackages = homePinnedPackages.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().take(20),
            dockPinnedPackages = dockPinnedPackages.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().take(5),
            updatedAtEpochMs = if (updatedAtEpochMs > 0L) updatedAtEpochMs else System.currentTimeMillis()
        )
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("themeId", themeId)
        obj.put("themeName", themeName)
        obj.put("activeOs", osPreset)
        obj.put("osPreset", osPreset)
        obj.put("isCustomRemix", isCustomRemix)
        obj.put("activePaletteIndex", activePaletteIndex)

        obj.put("backgroundColor", backgroundColor)
        obj.put("secondaryColor", secondaryColor)
        obj.put("primaryColor", primaryColor)
        obj.put("accentColor", accentColor)
        obj.put("surfaceColor", surfaceColor)
        obj.put("cardColor", cardColor)
        obj.put("textColor", textColor)
        obj.put("colors", JSONArray(paletteColors))

        obj.put("activeFontPack", fontPack)
        obj.put("fontPack", fontPack)

        obj.put("iconStyle", iconStyle)
        obj.put("iconRadius", iconShape)
        obj.put("iconShape", iconShape)
        obj.put("iconScale", iconSize)
        obj.put("iconSize", iconSize)
        obj.put("iconSpacing", iconSpacing)
        obj.put("homeGridColumns", homeGridColumns)
        obj.put("showFolders", showFolders)
        obj.put("folderStyle", folderStyle)

        obj.put("dockStyle", dockStyle)
        obj.put("dockBackgroundColor", dockBackgroundColor)
        obj.put("dockOpacity", dockOpacity.toDouble())
        obj.put("dockCornerRadius", dockCornerRadius)

        obj.put("drawerBackgroundMode", drawerBackgroundMode)
        obj.put("drawerColumns", drawerColumns)
        obj.put("drawerIconSize", drawerIconSize)
        obj.put("drawerShowLabels", drawerShowLabels)
        obj.put("drawerSearchStyle", drawerSearchStyle)
        obj.put("drawerSortOrder", drawerSortOrder)
        obj.put("drawerCornerRadius", drawerCornerRadius)

        obj.put("controlPanelStyle", controlPanelStyle)
        obj.put("controlSurfaceMode", controlSurfaceMode)
        obj.put("controlButtonShape", controlButtonShape)
        obj.put("controlCornerRadius", controlCornerRadius)
        obj.put("controlTransparency", controlTransparency.toDouble())
        obj.put("controlIconAppearance", controlIconAppearance)

        obj.put("widgetOpacity", widgetOpacity.toDouble())
        obj.put("widgetCornerRadius", widgetCornerRadius)

        val compObj = JSONObject()
        customComponents.forEach { (k, v) -> compObj.put(k, v) }
        obj.put("customComponents", compObj)

        val visObj = JSONObject().apply {
            put("weatherPill", showWeatherPill)
            put("batteryPill", showBatteryPill)
            put("appLabels", showAppLabels)
            put("lockStatusText", showLockStatusText)
        }
        obj.put("visibility", visObj)

        obj.put("lockDateStyle", lockDateStyle)
        obj.put("lockTextColorMode", lockTextColorMode)
        obj.put("lockCustomTextColor", lockCustomTextColor)
        obj.put("lockShowWidgets", lockShowWidgets)
        obj.put("lockBackdropDim", lockBackdropDim.toDouble())

        obj.put("wallpaperMode", wallpaperMode)
        obj.put("wallpaperTarget", wallpaperTarget)
        obj.put("customWallpaperDataUrl", customWallpaperDataUrl)

        val appVisObj = JSONObject()
        appVisibility.forEach { (k, v) -> appVisObj.put(k, v) }
        obj.put("appVisibility", appVisObj)

        obj.put("homePinnedPackages", JSONArray(homePinnedPackages))
        obj.put("dockPinnedPackages", JSONArray(dockPinnedPackages))

        obj.put("updatedAtEpochMs", updatedAtEpochMs)
        return obj
    }

    companion object {
        fun defaultComponentsMap(os: String = "android17"): Map<String, String> = mapOf(
            "cutout" to os,
            "statusBar" to os,
            "lockClock" to os,
            "lockShortcuts" to os,
            "heroWidget" to os,
            "secondaryWidget" to "none",
            "batteryWidget" to "batteryWidget",
            "bluetoothWidget" to "bluetoothWidget",
            "connectedDevicesWidget" to "none",
            "icons" to os,
            "dock" to os,
            "searchBar" to os,
            "pageDots" to os
        )

        fun normalizeHex(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.trim().removePrefix("#")
            val expanded = if (cleaned.length == 3 && cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                cleaned.map { "$it$it" }.joinToString("")
            } else {
                cleaned
            }
            return if (expanded.length == 6 && expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                "#${expanded.uppercase()}"
            } else {
                null
            }
        }

        fun fromJson(rawJson: String): ChromaTheme {
            val base = ChromaTheme()
            if (rawJson.isBlank()) return base.validated()
            return try {
                val obj = JSONObject(rawJson)
                val colorsArr = obj.optJSONArray("colors")
                val parsedColors = if (colorsArr != null && colorsArr.length() > 0) {
                    (0 until colorsArr.length()).mapNotNull { idx ->
                        normalizeHex(colorsArr.optString(idx))
                    }
                } else {
                    base.paletteColors
                }.ifEmpty { base.paletteColors }

                val os = obj.optString("osPreset", obj.optString("activeOs", base.osPreset))
                    .ifBlank { base.osPreset }

                val compMap = defaultComponentsMap(os).toMutableMap()
                obj.optJSONObject("customComponents")?.let { compObj ->
                    compObj.keys().forEach { key ->
                        compMap[key] = compObj.optString(key, os)
                    }
                }

                val visObj = obj.optJSONObject("visibility")
                val appVisMap = mutableMapOf<String, Boolean>()
                obj.optJSONObject("appVisibility")?.let { appVisObj ->
                    appVisObj.keys().forEach { key ->
                        appVisMap[key] = appVisObj.optBoolean(key, true)
                    }
                }

                val parsePkgList = { key: String ->
                    val arr = obj.optJSONArray(key)
                    if (arr != null) {
                        (0 until arr.length()).mapNotNull { idx ->
                            arr.optString(idx, "").trim().takeIf(String::isNotBlank)
                        }
                    } else {
                        emptyList()
                    }
                }

                ChromaTheme(
                    themeId = obj.optString("themeId", base.themeId),
                    themeName = obj.optString("themeName", base.themeName),
                    osPreset = os,
                    isCustomRemix = obj.optBoolean("isCustomRemix", base.isCustomRemix),
                    activePaletteIndex = obj.optInt("activePaletteIndex", base.activePaletteIndex),
                    backgroundColor = obj.optString("backgroundColor", parsedColors.getOrElse(0) { base.backgroundColor }),
                    secondaryColor = obj.optString("secondaryColor", parsedColors.getOrElse(1) { base.secondaryColor }),
                    primaryColor = obj.optString("primaryColor", parsedColors.getOrElse(2) { base.primaryColor }),
                    accentColor = obj.optString("accentColor", parsedColors.getOrElse(3) { base.accentColor }),
                    surfaceColor = obj.optString("surfaceColor", parsedColors.getOrElse(0) { base.surfaceColor }),
                    cardColor = obj.optString("cardColor", parsedColors.getOrElse(1) { base.cardColor }),
                    textColor = obj.optString("textColor", parsedColors.getOrElse(4) { base.textColor }),
                    paletteColors = parsedColors,
                    fontPack = obj.optString("fontPack", obj.optString("activeFontPack", base.fontPack)),
                    iconStyle = obj.optString("iconStyle", base.iconStyle),
                    iconShape = obj.optString("iconShape", obj.optString("iconRadius", base.iconShape)),
                    iconSize = obj.optInt("iconSize", obj.optInt("iconScale", base.iconSize)),
                    iconSpacing = obj.optString("iconSpacing", base.iconSpacing),
                    homeGridColumns = obj.optInt("homeGridColumns", base.homeGridColumns),
                    showAppLabels = visObj?.optBoolean("appLabels", true) ?: obj.optBoolean("showAppLabels", true),
                    showFolders = obj.optBoolean("showFolders", base.showFolders),
                    folderStyle = obj.optString("folderStyle", base.folderStyle),
                    dockStyle = obj.optString("dockStyle", base.dockStyle),
                    dockBackgroundColor = obj.optString("dockBackgroundColor", parsedColors.getOrElse(0) { base.dockBackgroundColor }),
                    dockOpacity = obj.optDouble("dockOpacity", base.dockOpacity.toDouble()).toFloat(),
                    dockCornerRadius = obj.optInt("dockCornerRadius", base.dockCornerRadius),
                    drawerBackgroundMode = obj.optString("drawerBackgroundMode", base.drawerBackgroundMode),
                    drawerColumns = obj.optInt("drawerColumns", base.drawerColumns),
                    drawerIconSize = obj.optInt("drawerIconSize", base.drawerIconSize),
                    drawerShowLabels = obj.optBoolean("drawerShowLabels", base.drawerShowLabels),
                    drawerSearchStyle = obj.optString("drawerSearchStyle", base.drawerSearchStyle),
                    drawerSortOrder = obj.optString("drawerSortOrder", base.drawerSortOrder),
                    drawerCornerRadius = obj.optInt("drawerCornerRadius", base.drawerCornerRadius),
                    controlPanelStyle = obj.optString("controlPanelStyle", os),
                    controlSurfaceMode = obj.optString("controlSurfaceMode", base.controlSurfaceMode),
                    controlButtonShape = obj.optString("controlButtonShape", base.controlButtonShape),
                    controlCornerRadius = obj.optInt("controlCornerRadius", base.controlCornerRadius),
                    controlTransparency = obj.optDouble("controlTransparency", base.controlTransparency.toDouble()).toFloat(),
                    controlIconAppearance = obj.optString("controlIconAppearance", base.controlIconAppearance),
                    widgetOpacity = obj.optDouble("widgetOpacity", base.widgetOpacity.toDouble()).toFloat(),
                    widgetCornerRadius = obj.optInt("widgetCornerRadius", base.widgetCornerRadius),
                    customComponents = compMap,
                    lockDateStyle = obj.optString("lockDateStyle", base.lockDateStyle),
                    lockTextColorMode = obj.optString("lockTextColorMode", base.lockTextColorMode),
                    lockCustomTextColor = obj.optString("lockCustomTextColor", base.lockCustomTextColor),
                    lockShowWidgets = obj.optBoolean("lockShowWidgets", base.lockShowWidgets),
                    lockBackdropDim = obj.optDouble("lockBackdropDim", base.lockBackdropDim.toDouble()).toFloat(),
                    wallpaperMode = obj.optString("wallpaperMode", base.wallpaperMode),
                    wallpaperTarget = obj.optString("wallpaperTarget", base.wallpaperTarget),
                    customWallpaperDataUrl = obj.optString("customWallpaperDataUrl", base.customWallpaperDataUrl),
                    showWeatherPill = visObj?.optBoolean("weatherPill", true) ?: true,
                    showBatteryPill = visObj?.optBoolean("batteryPill", true) ?: true,
                    showLockStatusText = visObj?.optBoolean("lockStatusText", true) ?: true,
                    appVisibility = appVisMap,
                    homePinnedPackages = parsePkgList("homePinnedPackages"),
                    dockPinnedPackages = parsePkgList("dockPinnedPackages"),
                    updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                ).validated()
            } catch (_: Exception) {
                base.validated()
            }
        }
    }
}
