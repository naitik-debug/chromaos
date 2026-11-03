# ChromaOS Launcher — Native Android Home Launcher & Theme Studio

A native Android launcher application built with Kotlin and Jetpack Compose, bringing the multi-OS visual customizability, 10 curated color harmonies, typography options, adaptive icon masks, and wallpaper engine of ChromaOS to real Android devices.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX decisions were confirmed during the interactive clarification phase:

- **Confirmed Default Visual Preset**: **Pixel 10 Clean UI** (At-a-Glance date/weather hero widget, quick search bar, bottom favorites dock, and circular icons) is configured as the out-of-the-box startup theme.
- **Confirmed App Drawer Interaction**: **Swipe-up bottom sheet** with an instant live search bar, alphabetical section headers, and adaptive app icon grid.
- **Confirmed HTML Theme Studio Integration**: Accessible directly from Launcher Settings via an **Embedded WebView Sub-Screen**, serving the bundled local `assets/chromaos/index.html` asset offline with zero external web dependencies or leaky JS bridges.
- **Home Launcher Intent Filters**: Declares `android.intent.category.HOME`, `android.intent.category.DEFAULT`, and `android.intent.action.MAIN` in `AndroidManifest.xml` with `<queries>` to discover installed apps safely without asking for risky system permissions.
- **Data Persistence**: Launcher state, favorites list, selected palette, custom hex colors, font choices, icon shapes, and visibility toggles will be persisted locally using Android `SharedPreferences`.

---

## 1. Overview & Core Concept

ChromaOS Launcher is a modern, lightweight, and deeply customizable Android Home launcher. It enables users to customize their mobile interface across 8 flagship OS design systems (Android 17 Expressive, Google Pixel 10, iOS Glass, Nothing OS 3.0, HyperOS 2.0, POCO Turbo, ColorOS 15, and OriginOS 5) or mix-and-match components into a custom OS remix.

### Key Capabilities
- **True Android Home App**: Can be set as the default launcher in Android Settings (`Settings > Apps > Default apps > Home app`) or run as a standalone app.
- **Native Installed App Discovery & Launching**: Queries the Android `PackageManager` using safe `<queries>` declarations to retrieve installed apps with real icons, labels, and launch intents.
- **ChromaOS Theme Studio (Native Compose)**:
  - 10 curated pill-stack color palettes (Espresso Roast, Slate Ochre, Alabaster Graphite, Matcha Botanical, Nordic Fjord, Abyssal Lagoon, Moss Terracotta, Twilight Crimson, Stormy Harbor, Emerald Canopy) plus live custom HEX color overrides.
  - OS Presets with signature clock widgets (A17 cookie/organic clock, Pixel At-a-Glance, Nothing dot-matrix text, POCO Turbo chrono).
  - Icon geometry customizer (Squircle 26%, Circle 50%, A17 Cookie, Teardrop, Sharp Box 14%) and slider-based icon scale (44dp–64dp).
  - Universal "🚫 None (Hide)" visibility engine for cutting out unwanted widgets, search bars, dock, or specific apps.
- **Wallpaper Engine**: Integrated Android system document picker (`ActivityResultContracts.GetContent` / `PickVisualMedia`) and `WallpaperManager` to apply wallpapers to Home or Lock screens with error handling.
- **Embedded Original Theme Studio**: Local WebView sub-activity / screen loading the user's provided `ChromaOS-App (1).html` asset.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                          ChromaOS Launcher Home                        │
│                                                                        │
│  [Status Bar: Time, Wi-Fi, Battery]                                    │
│                                                                        │
│  [Hero Widget: Pixel At-a-Glance / A17 Clock / Weather]                │
│                                                                        │
│  [Customizable Home Favorites Grid (4-column)]                         │
│                                                                        │
│  [Google Search Pill]                                                  │
│                                                                        │
│  [Bottom Favorites Dock (Phone, Messages, Camera, Settings)]           │
│                                                                        │
│  ▲ Swipe Up / Tap Arrow Pill                                           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            ▼                                               ▼
┌───────────────────────────────┐               ┌───────────────────────────────┐
│     Native App Drawer         │               │     Launcher Settings         │
│                               │               │                               │
│ [Search installed apps...]    │               │ • ChromaOS Theme Studio       │
│                               │               │ • Color Palettes & Custom HEX │
│ [App Grid: Real Icons+Labels] │               │ • OS Presets & Clocks         │
│ • Calculator                  │               │ • Icon Shape & Size Slider    │
│ • Camera                      │               │ • Wallpaper Picker            │
│ • Chrome                      │               │ • Default Home App Prompt     │
│ • Clock                       │               │ • Open Original HTML Studio   │
│ • Settings                    │               │ • Reset Defaults              │
└───────────────────────────────┘               └───────────────────────────────┘
```

### Visual Identity & Theme Tokens
- **Design System**: Material Design 3 with custom dynamic color scheme reflecting the active ChromaOS palette.
- **Default Mood**: Pixel 10 Minimal Clean (deep warm gray base `#10232A`, accented with `#3D4D55`, `#A79E9C`, `#B58863`).
- **Typography**: Support for Syne Geometric, Cormorant Editorial, Plus Jakarta Sans, Space Grotesk, and JetBrains Mono / NDot.
- **Motion & Interactions**: M3 spring animations for drawer expansion, smooth ripple feedback on app touch targets (minimum 48dp), and animated visibility toggles.
- **Edge-to-Edge**: `enableEdgeToEdge()` with proper handling of top status bar and bottom gesture navigation insets.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale & Trade-Offs |
| :--- | :--- | :--- |
| **Installed Apps Retrieval** | Real Android `PackageManager` + `queryIntentActivities` | True native launcher experience. Apps launch actual device intents (`getLaunchIntentForPackage`). Graceful fallback if an app cannot be opened. |
| **Package Visibility** | Explicit `<queries>` block with `MAIN` + `LAUNCHER` actions | Required for Android 11+ (API 30+) to discover launcher apps without requesting `QUERY_ALL_PACKAGES` (which violates Google Play policies). |
| **App Drawer Navigation** | M3 Modal Bottom Sheet / Animated Sheet | Fast, natural gesture feel matching modern Pixel & Android launcher expectations, leaving home screen uncluttered. |
| **Settings & Theme Persistence** | Local `SharedPreferences` repository | Immediate synchronous read on app startup, instant reactive state flow, zero external cloud dependencies or login requirements. |
| **Wallpaper Setting** | `WallpaperManager.getInstance(context).setBitmap(...)` | Standard Android API; checks `MANAGE_EXTERNAL_STORAGE` is NOT required when using system image picker `ActivityResultContracts.GetContent`. |
| **HTML Studio Asset** | Bundled in `app/src/main/assets/chromaos/index.html` | Completely self-contained and offline. Served via `file:///android_asset/` without unsafe JavaScript bridge or remote network calls. |

---

## 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        com.example (ChromaOS)                          │
├────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer (Jetpack Compose)                                  │
│   ├── HomeScreen.kt (Hero Clock, Favorites Grid, Search Bar, Dock)     │
│   ├── AppDrawerSheet.kt (Search TextField, LazyVerticalGrid of Apps)   │
│   ├── ThemeStudioScreen.kt (Palettes, OS Presets, Sliders, None Mode)  │
│   ├── HtmlThemeStudioActivity.kt (Isolated WebView for HTML asset)     │
│   └── SettingsScreen.kt (Default Home App trigger, Wallpaper, Reset)   │
├────────────────────────────────────────────────────────────────────────┤
│  ViewModel & State Layer                                               │
│   ├── LauncherViewModel.kt (StateFlow<LauncherUiState>)                │
│   └── ThemeViewModel.kt (Active Palette, Icon Shape, Visibility Map)   │
├────────────────────────────────────────────────────────────────────────┤
│  Data & System Services Layer                                          │
│   ├── AppRepository.kt (PackageManager queries, package change receiver)│
│   ├── LauncherPreferences.kt (SharedPreferences key-value storage)     │
│   └── WallpaperHelper.kt (WallpaperManager bitmap applicator)          │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Data Entities

```kotlin
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable?,
    val isFavorite: Boolean = false
)

data class ChromaPalette(
    val id: String,
    val name: String,
    val tag: String,
    val colors: List<Color> // 5-color stack
)

enum class IconMaskShape(val displayName: String, val cornerPercentage: Int) {
    SQUIRCLE("Squircle", 26),
    CIRCLE("Circle", 50),
    COOKIE("A17 Cookie", 38),
    ROUNDED_SQUARE("Sharp Box", 14)
}

data class LauncherSettings(
    val activeOs: String = "pixel",
    val activePaletteId: String = "slate-ochre",
    val iconSizeDp: Int = 54,
    val iconShape: IconMaskShape = IconMaskShape.CIRCLE,
    val showAppLabels: Boolean = true,
    val showSearchBar: Boolean = true,
    val showSecondaryWidget: Boolean = true,
    val customHexColors: List<String> = emptyList(),
    val favoritePackageNames: Set<String> = setOf()
)
```

---

## 5. Verification & Testing Plan

1. **Gradle Build Verification**: Execute `compile_applet` to ensure Kotlin source, Compose BOM, Android M3 components, and Android resources compile cleanly without unresolved references.
2. **Manifest Verification**: Ensure `android.intent.category.HOME` is registered and `<queries>` is declared.
3. **App Discovery Verification**: Confirm `PackageManager` retrieves installed system and user apps with non-null labels and icons.
4. **App Launch Verification**: Verify `context.startActivity(launchIntent)` handles missing or disallowed packages safely with toast feedback.
5. **Theme & Persistence Verification**: Verify changing color palette, icon shape, or favorites persists across app relaunches.
6. **Wallpaper Picker Verification**: Verify system image picker launcher fires and passes bitmap safely to `WallpaperManager`.
