# ChromaOS v2.1.0 Release Notes — Native Battery Telemetry, Dynamic Bluetooth Indicator & Human-Centred Dark Theme

**Application Name**: ChromaOS  
**Version**: 2.1.0 (Build 2)  
**Release Date**: November 2026  
**Minimum Supported Android Version**: Android 7.0+ (API Level 24)  
**Target Platform Compatibility**: Android 10 (API 29), 11 (API 30), 12 (API 31), 13 (API 33), 14 (API 34), 15 (API 35), and 16 (API 36)

---

## 🌟 What's New in Version 2.1.0

### 1. Real-Time Native Battery Indicator Widget
- **Authentic Android Telemetry**: Connects directly to Android's native `Intent.ACTION_BATTERY_CHANGED` sticky broadcast receiver and dynamic power receivers (`ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED`).
- **Real-Time Accuracy**: Continuously reflects actual device percentage without polling or timer loops.
- **Charging & Discharging Intelligence**: Accurately detects AC Fast Charger, USB Charging, Qi Wireless Charging, and battery discharge state with an adaptive subtle indicator icon (`ph-lightning` when charging, proportional battery icon when discharging).
- **Thermal & Health Diagnostics**: Displays real device battery temperature (°C) and health metrics (`Good`, `Overheat`, `Over Voltage`, `Critical`) where OEM hardware sensors allow.
- **Zero Simulation Policy**: Never uses hardcoded, simulated, or randomized numbers. Gracefully handles unread state with "Reading Battery...".

### 2. Dynamic Bluetooth Device Indicator Widget
- **Hardware-Connected Detection**: Utilizes native Android `BluetoothManager`, `BluetoothProfile.A2DP` proxy (wireless audio/speakers), `BluetoothProfile.HEADSET` proxy, and real-time `ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED` broadcast events.
- **Auto-Hide When Disconnected**: If no Bluetooth device is currently connected to your Android device, the widget is **completely hidden** from the home screen, keeping the canvas pristine.
- **Dynamic Connection Updates**: Immediately appears upon connecting a device (showing genuine device name and battery percentage if supported by the accessory), and disappears the moment the device disconnects.
- **Safe Multi-Version Permissions**: Conditionally handles `BLUETOOTH_CONNECT` on Android 12+ (API 31–36) and `BLUETOOTH` on Android 10–11 (API 29–30), prompting for permission only when necessary and never crashing.

### 3. Removed Unwanted Home Screen Elements & Buttons
- **Removed "Chroma High Resolution"**: Legacy placeholder card permanently eliminated.
- **Removed Old Battery Widget**: Replaced the previous non-realtime card with the new native broadcast-driven Real-Time Battery Indicator.
- **Removed "Download Runnable App" & "Save App" Buttons**: Removed these redundant buttons from the interface toolbar and summary bar while preserving underlying export/build functionality.
- **Clean Default Home Screen**: By default, the clean home screen displays only the Real-Time Battery widget (and the Bluetooth indicator only if an accessory is active), with long-press removal and catalog re-addition.

### 4. Dark Human-Centred Design System
- **Sophisticated Palette**: Replaced harsh neon glows, saturated magenta/cyan gradients, and generic AI dashboard cards with deep obsidian (`#0D0F12`), graphite (`#121418`), and muted warm bronze accents (`#A78D78`, `#BEB5A9`).
- **Balanced Commercial Aesthetics**: Consistent 16–20px corner radii, subtle borders (`rgba(255, 255, 255, 0.08)`), natural soft shadows, readable typography, and fluid transitions.

---

## 🛠️ Bug Fixes & Architectural Optimizations
- **Android 14+ Broadcast Security**: Enforced `Context.RECEIVER_EXPORTED` flags for system broadcasts on API 34+ to prevent security exceptions.
- **Hardware Acceleration**: Set `setLayerType(View.LAYER_TYPE_HARDWARE, null)` on the host WebView to ensure 60fps/120fps UI animations.
- **Memory Leak Protection**: Closed Bluetooth profile proxies (`closeProfileProxy`) and unregistered receivers cleanly in `onDestroy()`.
- **Hardware Back Navigation**: Integrated `OnBackPressedCallback` with JavaScript modal stack and native `moveTaskToBack(true)` behavior.

---

## ⚠️ Known Limitations
- **Bluetooth Name Resolution**: On Android 12+ (API 31+), Android requires the `BLUETOOTH_CONNECT` runtime permission to access the user-assigned name of connected accessories. If not granted, generic labels or device addresses may be displayed.
- **Battery Temperature Precision**: Battery temperature readouts are provided by OEM kernel sensors and may not be exposed on select carrier-locked devices or emulators.

---

## 📦 APK Build & Release Workflow

### Reproducible Build Outputs
- **Debug APK Location**:
  ```bash
  app/build/outputs/apk/debug/app-debug.apk
  ```
- **Build Command**:
  ```bash
  gradle assembleDebug
  ```

### Signed Release APK Build Instructions
1. Set keystore environment variables or place your production keystore in the project root:
   ```bash
   export KEYSTORE_PATH="/path/to/your/upload-keystore.jks"
   export STORE_PASSWORD="your_store_password"
   export KEY_PASSWORD="your_key_password"
   ```
2. Run the Gradle release task:
   ```bash
   gradle assembleRelease
   ```
3. The signed APK will be output to:
   ```bash
   app/build/outputs/apk/release/app-release.apk
   ```

### Automated GitHub Actions CI/CD Workflow
Located at `.github/workflows/release.yml`:
- Triggers on tag pushes (`v*`) or manual `workflow_dispatch`.
- Builds the APK via `gradle assembleDebug`.
- Automatically packages and attaches the APK as a downloadable release asset with this changelog body.
