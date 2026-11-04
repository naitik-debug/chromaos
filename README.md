# ChromaOS — Multi-OS Mobile Environment & Home Launcher

[![Android 10 - 16](https://img.shields.io/badge/Android-10%20to%2016-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-amber.svg)](LICENSE)
[![Latest Release](https://img.shields.io/badge/Release-v2.0-blue.svg)](RELEASE_NOTES.md)

An Android Home launcher featuring bespoke multi-OS design systems (Pixel 10, Android 17 Expressive, iOS Glass, Nothing OS 3.0, HyperOS 2.0, POCO Turbo, ColorOS 15, and OriginOS 5), curated pill-stack color harmonies, real-time hardware battery telemetry, and a clean-slate minimal canvas.

---

## 📥 Direct APK Download

Download the compiled, installable `.apk` file directly:
- **[⬇️ Download ChromaOS-v2.0.apk (Direct Release Asset)](../../releases/latest/download/ChromaOS-v2.0.apk)**
- Pre-built local APK: `public/apk/ChromaOS.apk`

---

## ✨ Features

- **Android 10, 11, 12, 13, 14, 15, and 16 Support**: Fully tested with modern `RECEIVER_EXPORTED` flags, edge-to-edge window insets, and runtime Bluetooth permissions.
- **Clean Minimal Canvas**: Starts with a serene, zero-clutter canvas by default.
- **Long-Press to Add Widgets**: Press and hold anywhere on empty screen space to open the tactile **Widget Catalog** and add clocks, battery hub, status tiles, app grid, search bar, or dock on demand.
- **Hold-to-Remove Widgets**: Long-press any active widget to reveal the remove action with subtle haptic vibration, accompanied by an instant **Undo Toast Banner**.
- **Human-Crafted Warm Editorial Theme**: Deep obsidian glass (`#080D0B`) with physical top specular lighting, muted antique bronze accents (`#C8A97E`), and Cormorant Garamond serif typography.
- **Real-Time Battery & Connected Accessories Hub**: Live phone battery percentage, charging speeds (AC, USB, Qi Wireless), temperature, and connected Bluetooth earbuds/smartwatch battery levels.
- **Native Android Launcher Integration**: Registers `MAIN`, `HOME`, and `DEFAULT` intent categories in `AndroidManifest.xml` with safe `<queries>` package discovery.
- **System Wallpaper Applicator**: Applies custom theme gradients or selected photos directly to your device Home and Lock screens using Android `WallpaperManager`.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/<your-username>/ChromaOS.git
cd ChromaOS

# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Release Notes

Read the detailed changelog in [RELEASE_NOTES.md](RELEASE_NOTES.md).
