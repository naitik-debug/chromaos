package com.example.model

data class OsPreset(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val defaultIconShape: IconMaskShape,
    val defaultPaletteId: String
) {
    companion object {
        val PRESETS = listOf(
            OsPreset(
                id = "pixel",
                name = "Google Pixel 10",
                shortName = "Pixel 10 Clean",
                description = "At A Glance date, weather, minimal search pill & circle icons",
                defaultIconShape = IconMaskShape.CIRCLE,
                defaultPaletteId = "slate-ochre"
            ),
            OsPreset(
                id = "android17",
                name = "Android 17",
                shortName = "A17 Expressive",
                description = "Organic scallop geometry, bold split clock, dynamic colors",
                defaultIconShape = IconMaskShape.COOKIE,
                defaultPaletteId = "espresso-roast"
            ),
            OsPreset(
                id = "ios",
                name = "iOS Glass",
                shortName = "iOS Glass",
                description = "Frosted glass surfaces, squircle icons, depth clock",
                defaultIconShape = IconMaskShape.SQUIRCLE,
                defaultPaletteId = "alabaster-graphite"
            ),
            OsPreset(
                id = "nothing",
                name = "Nothing OS 3.0",
                shortName = "Nothing Dot",
                description = "Cyber dot-matrix typography, monochrome aesthetic, red accents",
                defaultIconShape = IconMaskShape.CIRCLE,
                defaultPaletteId = "alabaster-graphite"
            ),
            OsPreset(
                id = "hyperos",
                name = "Xiaomi HyperOS 2",
                shortName = "HyperOS 2.0",
                description = "Artistic typography cards, smooth squircle dock",
                defaultIconShape = IconMaskShape.SQUIRCLE,
                defaultPaletteId = "nordic-fjord"
            ),
            OsPreset(
                id = "poco",
                name = "POCO Turbo",
                shortName = "POCO Turbo",
                description = "Yellow cyber chrono widget, turbo performance aesthetic",
                defaultIconShape = IconMaskShape.CIRCLE,
                defaultPaletteId = "stormy-harbor"
            ),
            OsPreset(
                id = "coloros",
                name = "OPPO ColorOS 15",
                shortName = "ColorOS 15",
                description = "Aquamorphic fluid capsule, soft modern gradients",
                defaultIconShape = IconMaskShape.SQUIRCLE,
                defaultPaletteId = "abyssal-lagoon"
            ),
            OsPreset(
                id = "originos",
                name = "vivo OriginOS 5",
                shortName = "OriginOS 5",
                description = "Atomic component island, media tile, dual rounded curves",
                defaultIconShape = IconMaskShape.TEARDROP,
                defaultPaletteId = "emerald-canopy"
            )
        )

        fun getById(id: String): OsPreset {
            return PRESETS.find { it.id == id } ?: PRESETS.first()
        }
    }
}
