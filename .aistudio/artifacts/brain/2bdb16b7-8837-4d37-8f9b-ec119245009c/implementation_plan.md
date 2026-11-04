# Implementation Plan: OS Control & Notification Panel Changer with Live Battery Widget

Based on your design choices:
1. **Control & Notification Panel Changer**: Dedicated customization box in the studio with live OS previews (Pixel Quick Settings, iOS Control Center, HyperOS 2.0, Nothing OS 3.0, Android 17 Expressive) and interactive drawer triggers.
2. **Pristine Home Screen**: All clutter and unnecessary placeholder widgets removed; only the **Real-Time Battery Indicator Widget** is present by default, showing live phone battery % and all connected Bluetooth accessories (earbuds, watch, etc.).
3. **Interactive & Removable**: The battery widget (and any added widgets) can be long-pressed to remove with instant undo haptics. Long-pressing the clean canvas opens the widget picker.
4. **Android 10–16 Compatibility & Stable Release**: Build optimized APK, update GitHub release notes and workflow.

---

## User Review Required

> [!IMPORTANT]
> - **Control Center Access**: A dedicated Studio Card with interactive live preview allows testing every OS style instantly, plus a quick swipe or status bar tap can invoke the active overlay directly on the phone.
> - **Connected Devices Telemetry**: Battery status is piped via native Android `BluetoothProfile` & `ACTION_BATTERY_CHANGED` broadcast receivers directly into the HTML widget.

---

## Proposed Changes

### 1. Dedicated Control Panel & Notification Panel Studio Box
#### `app/src/main/assets/chromaos/index.html`
- Add a new dedicated card `#osControlPanelCard` in the Studio Customization rail:
  - **Live Mini-Preview**: Displays an animated thumbnail of the active Control Center / Notification Panel.
  - **OS Switcher Segmented Selector**:
    - **Pixel (Material You)**: 2-column rounded capsule toggles (Internet, Bluetooth, Flashlight, Battery Saver, DND), brightness slider, At-a-Glance media pill.
    - **iOS (Liquid Glass)**: Frosted 4-icon connectivity island, dual vertical glass faders (Volume & Brightness), dynamic media island, utility square tiles.
    - **HyperOS 2.0**: Large accent pills for Wi-Fi and Bluetooth with quick expand arrows, split volume/brightness sliders, smart device controls.
    - **Nothing OS 3.0**: Monochrome dot-matrix glyph tiles, circular toggles, signature red accent dot.
    - **Android 17 Expressive**: Organic playful pill shapes, fluid dynamic slider, expressive typography.
  - **Panel Mode Toggle**: Switch preview and overlay between **Control Center** and **Notification Panel** (rich stacked notifications with Dismiss / Reply actions).
  - **Open Overlay Button**: Button inside the studio card and status bar interaction to trigger the full interactive panel on screen with smooth drag/dismiss physics.

### 2. Clean Canvas & Real-Time Battery Indicator Widget
#### `app/src/main/assets/chromaos/index.html`
- Clean the phone home screen so it is completely decluttered.
- Place the dedicated **Real-Time Battery Widget**:
  - Live phone battery percentage with animated charging ring / fluid wave.
  - Charging state badge (`Fast Charging`, `Battery Saver`, or `Discharging`).
  - Sub-cards / connected accessory indicators:
    - Left & Right Earbuds (battery % and charging icon).
    - Smartwatch / Fitness Band (battery % and sync status).
    - Bluetooth Audio / Stylus (if connected).
  - Fallback / empty state when no accessories are connected: "No Bluetooth accessories connected" with quick Bluetooth pairing shortcut.
- Retain the long-press (`420ms`) gesture on the widget to reveal the remove action with undo banner.
- Retain empty screen long-press (`440ms`) to open the widget catalog.

### 3. Native Bluetooth & Battery Bridge Enhancements
#### `app/src/main/java/com/example/MainActivity.kt`
- Expand `ChromaAndroidBridge.getBatteryInfo()` to return a detailed JSON payload containing:
  - Phone battery level and charging status.
  - Connected Bluetooth devices retrieved via `BluetoothManager.getConnectedDevices(GATT)` and `BluetoothHeadset.EXTRA_BATTERY_LEVEL` or battery service UUID.
- Broadcast updates to `window.onNativeBatteryUpdate(json)` whenever Bluetooth connection states or battery percentages change.

### 4. Build, Test & Package
- Compile with `compile_applet` and verify zero errors across Android 10–16 (`minSdk 24`, `targetSdk 36`).
- Run Robolectric tests to verify bridge methods and receiver exports.
- Build updated APK in `public/apk/ChromaOS-v2.0.apk` and update `public/ChromaOS-Project.zip`.
- Update `RELEASE_NOTES.md` and ensure `.github/workflows/release.yml` is ready.

---

## Verification Plan

### Automated Tests
- Run `compile_applet` to verify compilation.
- Run `gradle :app:testDebugUnitTest` to execute Robolectric test suites.

### Manual Verification
- Verify HTML loads properly in WebView with no console errors.
- Test switching between Pixel, iOS, HyperOS, Nothing, and Android 17 panels in the studio box.
- Test opening the interactive Control Center & Notification panels.
- Verify home screen is uncluttered with only the battery widget displayed by default.
- Test long-pressing the battery widget to remove it and undoing removal.
