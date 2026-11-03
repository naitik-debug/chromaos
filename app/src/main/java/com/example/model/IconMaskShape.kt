package com.example.model

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class IconMaskShape(
    val id: String,
    val displayName: String,
    val description: String
) {
    CIRCLE("circle", "Pixel Circle", "50% round icon"),
    SQUIRCLE("squircle", "Squircle", "26% curvature smooth"),
    COOKIE("cookie", "A17 Cookie", "Organic scallop contour"),
    TEARDROP("teardrop", "Teardrop", "Asymmetric teardrop"),
    ROUNDED_SQUARE("rounded_square", "Sharp Box", "14% soft corners");

    fun toComposeShape(): Shape {
        return when (this) {
            CIRCLE -> CircleShape
            SQUIRCLE -> RoundedCornerShape(26)
            COOKIE -> RoundedCornerShape(topStartPercent = 45, topEndPercent = 25, bottomEndPercent = 45, bottomStartPercent = 25)
            TEARDROP -> RoundedCornerShape(topStartPercent = 45, topEndPercent = 16, bottomEndPercent = 45, bottomStartPercent = 45)
            ROUNDED_SQUARE -> RoundedCornerShape(14)
        }
    }

    companion object {
        fun fromId(id: String): IconMaskShape {
            return entries.find { it.id == id } ?: CIRCLE
        }
    }
}
