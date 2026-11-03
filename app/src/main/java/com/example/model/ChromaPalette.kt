package com.example.model

import androidx.compose.ui.graphics.Color

data class ChromaPalette(
    val id: String,
    val name: String,
    val tag: String,
    val hexColors: List<String>
) {
    val colors: List<Color> by lazy {
        hexColors.map { parseHexColor(it) }
    }

    val deepBase: Color get() = colors.getOrElse(0) { Color(0xFF10232A) }
    val secondaryBg: Color get() = colors.getOrElse(1) { Color(0xFF3D4D55) }
    val primaryAccent: Color get() = colors.getOrElse(2) { Color(0xFFA79E9C) }
    val highlight: Color get() = colors.getOrElse(3) { Color(0xFFD3C3B9) }
    val tertiaryDepth: Color get() = colors.getOrElse(4) { Color(0xFFB58863) }

    companion object {
        fun parseHexColor(hex: String): Color {
            return try {
                val cleanHex = hex.removePrefix("#")
                val colorInt = when (cleanHex.length) {
                    6 -> (0xFF000000 or cleanHex.toLong(16)).toInt()
                    8 -> cleanHex.toLong(16).toInt()
                    else -> 0xFF10232A.toInt()
                }
                Color(colorInt)
            } catch (e: Exception) {
                Color(0xFF10232A)
            }
        }

        val ALL_PALETTES = listOf(
            ChromaPalette(
                id = "slate-ochre",
                name = "Slate Ochre",
                tag = "Nordic Stone",
                hexColors = listOf("#10232A", "#3D4D55", "#A79E9C", "#D3C3B9", "#B58863")
            ),
            ChromaPalette(
                id = "espresso-roast",
                name = "Espresso Roast",
                tag = "Warm Editorial",
                hexColors = listOf("#291C0E", "#6E473B", "#A78D78", "#BEB5A9", "#E1D4C2")
            ),
            ChromaPalette(
                id = "alabaster-graphite",
                name = "Alabaster Graphite",
                tag = "Monolith Minimal",
                hexColors = listOf("#1B1E23", "#2B363B", "#56636A", "#A79E9C", "#E8E2D5")
            ),
            ChromaPalette(
                id = "matcha-botanical",
                name = "Matcha Botanical",
                tag = "Organic Sage",
                hexColors = listOf("#728156", "#88976C", "#98A77C", "#B6C99B", "#E7F5DC")
            ),
            ChromaPalette(
                id = "nordic-fjord",
                name = "Nordic Fjord",
                tag = "Glacial Slate",
                hexColors = listOf("#06141B", "#11212D", "#253745", "#4A5C6A", "#9BA8AB")
            ),
            ChromaPalette(
                id = "abyssal-lagoon",
                name = "Abyssal Lagoon",
                tag = "Aquamorphic Cyan",
                hexColors = listOf("#05161A", "#072E33", "#0C7075", "#0F969C", "#6DA5C0")
            ),
            ChromaPalette(
                id = "moss-terracotta",
                name = "Moss Terracotta",
                tag = "Forest Copper",
                hexColors = listOf("#122324", "#2F3A32", "#545748", "#DB9F75", "#804012")
            ),
            ChromaPalette(
                id = "twilight-crimson",
                name = "Twilight Crimson",
                tag = "Velvet Peach",
                hexColors = listOf("#161E2F", "#242F49", "#384358", "#FFA586", "#B51A2B")
            ),
            ChromaPalette(
                id = "stormy-harbor",
                name = "Stormy Harbor",
                tag = "Misty Teal",
                hexColors = listOf("#0D1F23", "#132E35", "#2D4A53", "#69818D", "#AFB3B7")
            ),
            ChromaPalette(
                id = "emerald-canopy",
                name = "Emerald Canopy",
                tag = "Deep Rainforest",
                hexColors = listOf("#051F20", "#0B2B26", "#163832", "#235347", "#8EB69B")
            )
        )

        fun getById(id: String): ChromaPalette {
            return ALL_PALETTES.find { it.id == id } ?: ALL_PALETTES.first()
        }
    }
}
