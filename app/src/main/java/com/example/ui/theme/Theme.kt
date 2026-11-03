package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ChromaPalette

@Composable
fun ChromaOSTheme(
    palette: ChromaPalette = ChromaPalette.getById("slate-ochre"),
    fontPackId: String = "jakarta",
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val c1 = palette.deepBase
    val c2 = palette.secondaryBg
    val c3 = palette.primaryAccent
    val c4 = palette.highlight
    val c5 = palette.tertiaryDepth

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = c3,
            onPrimary = Color.White,
            primaryContainer = c2,
            onPrimaryContainer = c4,
            secondary = c4,
            onSecondary = Color(0xFF111827),
            secondaryContainer = c2,
            onSecondaryContainer = Color.White,
            tertiary = c5,
            onTertiary = Color.White,
            background = c1,
            onBackground = Color(0xFFF1F5F9),
            surface = c1,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = c2,
            onSurfaceVariant = c4,
            outline = c3.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = c2,
            onPrimary = Color.White,
            primaryContainer = c4,
            onPrimaryContainer = c1,
            secondary = c3,
            onSecondary = Color.White,
            tertiary = c5,
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = c4.copy(alpha = 0.3f),
            onSurfaceVariant = c1,
            outline = c3.copy(alpha = 0.3f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypographyForPack(fontPackId),
        content = content
    )
}
