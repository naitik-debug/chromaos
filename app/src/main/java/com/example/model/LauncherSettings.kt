package com.example.model

data class LauncherSettings(
    val activeOsId: String = "pixel",
    val activePaletteId: String = "slate-ochre",
    val isCustomColors: Boolean = false,
    val customColorsHex: List<String> = emptyList(),
    val fontPackId: String = "jakarta",
    val iconSizeDp: Int = 54,
    val iconShape: IconMaskShape = IconMaskShape.CIRCLE,
    val showAppLabels: Boolean = true,
    val showSearchBar: Boolean = true,
    val showSecondaryWidget: Boolean = true,
    val showHeroWidget: Boolean = true,
    val showDock: Boolean = true,
    val wallpaperMode: String = "mesh", // "mesh", "botanical", "oled", "custom"
    val customWallpaperUri: String? = null,
    val favoritePackageNames: Set<String> = emptySet(),
    val hiddenPackageNames: Set<String> = emptySet()
) {
    fun getResolvedPalette(): ChromaPalette {
        if (isCustomColors && customColorsHex.size >= 5) {
            return ChromaPalette(
                id = "custom",
                name = "Custom Color Mix",
                tag = "User Custom Harmony",
                hexColors = customColorsHex
            )
        }
        return ChromaPalette.getById(activePaletteId)
    }
}
