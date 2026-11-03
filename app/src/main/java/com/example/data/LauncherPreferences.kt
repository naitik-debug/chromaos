package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ChromaPalette
import com.example.model.IconMaskShape
import com.example.model.LauncherSettings
import com.example.model.OsPreset

class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chromaos_launcher_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_OS = "active_os"
        private const val KEY_ACTIVE_PALETTE = "active_palette"
        private const val KEY_IS_CUSTOM_COLORS = "is_custom_colors"
        private const val KEY_CUSTOM_COLORS = "custom_colors_hex"
        private const val KEY_FONT_PACK = "font_pack"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_ICON_SHAPE = "icon_shape"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_SHOW_SEARCH = "show_search"
        private const val KEY_SHOW_SECONDARY = "show_secondary"
        private const val KEY_SHOW_HERO = "show_hero"
        private const val KEY_SHOW_DOCK = "show_dock"
        private const val KEY_WALLPAPER_MODE = "wallpaper_mode"
        private const val KEY_CUSTOM_WALLPAPER_URI = "custom_wallpaper_uri"
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_HIDDEN = "hidden_packages"
    }

    fun loadSettings(): LauncherSettings {
        val osId = prefs.getString(KEY_ACTIVE_OS, "pixel") ?: "pixel"
        val paletteId = prefs.getString(KEY_ACTIVE_PALETTE, "slate-ochre") ?: "slate-ochre"
        val isCustomColors = prefs.getBoolean(KEY_IS_CUSTOM_COLORS, false)
        val customColorsString = prefs.getString(KEY_CUSTOM_COLORS, "") ?: ""
        val customColorsList = if (customColorsString.isNotBlank()) {
            customColorsString.split(",").filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val fontPack = prefs.getString(KEY_FONT_PACK, "jakarta") ?: "jakarta"
        val iconSize = prefs.getInt(KEY_ICON_SIZE, 54)
        val iconShapeId = prefs.getString(KEY_ICON_SHAPE, IconMaskShape.CIRCLE.id) ?: IconMaskShape.CIRCLE.id
        val showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true)
        val showSearch = prefs.getBoolean(KEY_SHOW_SEARCH, true)
        val showSecondary = prefs.getBoolean(KEY_SHOW_SECONDARY, true)
        val showHero = prefs.getBoolean(KEY_SHOW_HERO, true)
        val showDock = prefs.getBoolean(KEY_SHOW_DOCK, true)
        val wallpaperMode = prefs.getString(KEY_WALLPAPER_MODE, "mesh") ?: "mesh"
        val wallpaperUri = prefs.getString(KEY_CUSTOM_WALLPAPER_URI, null)

        val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

        return LauncherSettings(
            activeOsId = osId,
            activePaletteId = paletteId,
            isCustomColors = isCustomColors,
            customColorsHex = customColorsList,
            fontPackId = fontPack,
            iconSizeDp = iconSize,
            iconShape = IconMaskShape.fromId(iconShapeId),
            showAppLabels = showLabels,
            showSearchBar = showSearch,
            showSecondaryWidget = showSecondary,
            showHeroWidget = showHero,
            showDock = showDock,
            wallpaperMode = wallpaperMode,
            customWallpaperUri = wallpaperUri,
            favoritePackageNames = favorites,
            hiddenPackageNames = hidden
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putString(KEY_ACTIVE_OS, settings.activeOsId)
            .putString(KEY_ACTIVE_PALETTE, settings.activePaletteId)
            .putBoolean(KEY_IS_CUSTOM_COLORS, settings.isCustomColors)
            .putString(KEY_CUSTOM_COLORS, settings.customColorsHex.joinToString(","))
            .putString(KEY_FONT_PACK, settings.fontPackId)
            .putInt(KEY_ICON_SIZE, settings.iconSizeDp)
            .putString(KEY_ICON_SHAPE, settings.iconShape.id)
            .putBoolean(KEY_SHOW_LABELS, settings.showAppLabels)
            .putBoolean(KEY_SHOW_SEARCH, settings.showSearchBar)
            .putBoolean(KEY_SHOW_SECONDARY, settings.showSecondaryWidget)
            .putBoolean(KEY_SHOW_HERO, settings.showHeroWidget)
            .putBoolean(KEY_SHOW_DOCK, settings.showDock)
            .putString(KEY_WALLPAPER_MODE, settings.wallpaperMode)
            .putString(KEY_CUSTOM_WALLPAPER_URI, settings.customWallpaperUri)
            .putStringSet(KEY_FAVORITES, settings.favoritePackageNames)
            .putStringSet(KEY_HIDDEN, settings.hiddenPackageNames)
            .apply()
    }

    fun resetToDefaults(): LauncherSettings {
        prefs.edit().clear().apply()
        return loadSettings()
    }
}
