package com.example.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.provider.Settings
import com.example.data.WallpaperHelper
import com.example.data.WallpaperTarget
import com.example.model.ChromaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ChromaThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val wallpaperHelper = WallpaperHelper(context)

    private val _themeFlow = MutableStateFlow(loadPersistedTheme())
    val themeFlow: StateFlow<ChromaTheme> = _themeFlow.asStateFlow()

    data class ThemeApplyOutcome(
        val success: Boolean,
        val wallpaperApplied: Boolean,
        val wallpaperMessage: String,
        val theme: ChromaTheme,
        val appliedFeatures: List<String>,
        val restrictedFeatures: List<String>
    ) {
        fun toJsonString(): String {
            return JSONObject().apply {
                put("success", success)
                put("wallpaperApplied", wallpaperApplied)
                put("wallpaperMessage", wallpaperMessage)
                put("theme", theme.toJson())
                put("appliedFeatures", JSONArray(appliedFeatures))
                put("restrictedFeatures", JSONArray(restrictedFeatures))
                put("summaryMessage", buildSummary())
            }.toString()
        }

        private fun buildSummary(): String {
            return if (wallpaperApplied) {
                "Applied \"${theme.themeName}\" across ChromaOS & ${wallpaperMessage.replaceFirstChar { it.lowercase() }}"
            } else {
                "Applied \"${theme.themeName}\" across ChromaOS (${wallpaperMessage})"
            }
        }
    }

    fun getCurrentTheme(): ChromaTheme = _themeFlow.value

    fun getRawThemeJson(): String {
        val saved = prefs.getString(KEY_SAVED_STATE, null)
        return if (!saved.isNullOrBlank()) saved else _themeFlow.value.toJson().toString()
    }

    fun saveThemeDraft(rawJson: String): ChromaTheme {
        val validated = ChromaTheme.fromJson(rawJson).validated()
        persistTheme(validated)
        _themeFlow.value = validated
        return validated
    }

    suspend fun applyTheme(
        rawJson: String,
        applyWallpaper: Boolean = true,
        widthPx: Int = 1080,
        heightPx: Int = 1920
    ): ThemeApplyOutcome {
        val validated = ChromaTheme.fromJson(rawJson)
            .copy(updatedAtEpochMs = System.currentTimeMillis())
            .validated()

        persistTheme(validated)
        _themeFlow.value = validated

        val appliedFeatures = mutableListOf(
            "Theme palette (${validated.themeName})",
            "Font pack (${validated.fontPack})",
            "Home Screen icons (${validated.iconStyle}, ${validated.iconSize}px, ${validated.homeGridColumns} cols)",
            "Dock (${validated.dockStyle}, ${validated.dockCornerRadius}dp)",
            "App Drawer (${validated.drawerBackgroundMode}, ${validated.drawerColumns} cols, ${validated.drawerSortOrder})",
            "Control Panel (${validated.controlPanelStyle}, ${validated.controlSurfaceMode})",
            "Lock Screen preview (${validated.lockDateStyle}, ${validated.lockTextColorMode})"
        )

        val restrictedFeatures = mutableListOf(
            "System notification icons & third-party app launcher icons outside ChromaOS remain controlled by Android",
            "System status bar, navigation bar, and secure Keyguard authentication cannot be replaced by a standard launcher"
        )

        if (!Settings.System.canWrite(context)) {
            restrictedFeatures.add("System-wide brightness requires 'Modify system settings' access (window brightness fallback active)")
        }

        var wallpaperApplied = false
        var wallpaperMsg = "Wallpaper kept unchanged"

        if (applyWallpaper) {
            if (validated.wallpaperMode == "custom") {
                wallpaperApplied = true
                wallpaperMsg = "Custom wallpaper preserved"
                appliedFeatures.add("Custom wallpaper (${validated.wallpaperTarget})")
            } else {
                val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
                if (wm == null || !wm.isWallpaperSupported) {
                    wallpaperMsg = "WallpaperManager unavailable on this profile"
                    restrictedFeatures.add("System wallpaper setting unsupported on this device profile")
                } else {
                    val colorInts = validated.paletteColors.map { hex ->
                        runCatching { Color.parseColor(hex) }.getOrDefault(Color.parseColor("#291C0E"))
                    }
                    val res = wallpaperHelper.applyThemeWallpaperBitmap(
                        colors = colorInts,
                        wallpaperMode = validated.wallpaperMode,
                        whichScreen = WallpaperTarget.fromString(validated.wallpaperTarget),
                        width = widthPx.coerceAtLeast(720),
                        height = heightPx.coerceAtLeast(1280)
                    )
                    res.fold(
                        onSuccess = { msg ->
                            wallpaperApplied = true
                            wallpaperMsg = msg
                            appliedFeatures.add("System wallpaper (${validated.wallpaperTarget})")
                        },
                        onFailure = { err ->
                            wallpaperMsg = err.message ?: "System wallpaper update restricted"
                            restrictedFeatures.add(wallpaperMsg)
                        }
                    )
                }
            }
        }

        return ThemeApplyOutcome(
            success = true,
            wallpaperApplied = wallpaperApplied,
            wallpaperMessage = wallpaperMsg,
            theme = validated,
            appliedFeatures = appliedFeatures,
            restrictedFeatures = restrictedFeatures
        )
    }

    fun resetToDefault(): ChromaTheme {
        val defaultTheme = ChromaTheme().validated()
        persistTheme(defaultTheme)
        _themeFlow.value = defaultTheme
        return defaultTheme
    }

    private fun loadPersistedTheme(): ChromaTheme {
        val saved = prefs.getString(KEY_SAVED_STATE, "") ?: ""
        return ChromaTheme.fromJson(saved).validated()
    }

    private fun persistTheme(theme: ChromaTheme) {
        prefs.edit()
            .putString(KEY_SAVED_STATE, theme.toJson().toString())
            .apply()
    }

    companion object {
        const val PREFS_NAME = "chromaos_prefs"
        const val KEY_SAVED_STATE = "saved_state"
    }
}
