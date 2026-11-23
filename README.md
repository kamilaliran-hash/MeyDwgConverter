# MeyDwgConverter (MDC): offline DWG version converter for Android

A simple Android app that converts AutoCAD DWG files to another version, entirely on the phone. It uses [GNU LibreDWG](https://www.gnu.org/software/libredwg/).

**How it works:** Choose DWG file → the app shows its current version → pick a format under "Save as" → **Convert & Save** → choose where to save it.

## What it can convert

| Input (read) | Output (write) |
|---|---|
| DWG R13 up to AutoCAD 2018+ (AC1012–AC1032); older R2.x–R12 files are partly supported | **DWG R2000 (AC1015)**, the recommended target |
| | DWG R14 / R13, *experimental* |
| | DXF 2000, 2004, 2007, 2010, 2013, 2018 |

LibreDWG **can't write DWG newer than R2000.** If you need a 2004/2007/2010/2013 *DWG*, save to DXF in that version and open it in AutoCAD (or another CAD program), then save it as DWG there. Always check a converted drawing before relying on it. Complex objects (dynamic blocks, some proxies, 3D solids, annotative styles) can come out simplified.

## Build

You need **Android Studio** (with SDK 35, **NDK 27.2.12479018** and CMake 3.22.1, all installed via *SDK Manager → SDK Tools*) and a Linux, macOS or **WSL** shell for step 1.

1. **Compile LibreDWG for Android (one time only, about 5–10 min):**
   ```bash
   export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018
   chmod +x scripts/build-libredwg.sh
   ./scripts/build-libredwg.sh
   ```
   This downloads LibreDWG 0.13.3 and builds `libredwg.a` for arm64-v8a, armeabi-v7a and x86_64 into `app/src/main/cpp/libredwg/`.
   To use a different release, set `LIBREDWG_VERSION=0.x.y` first.

2. **Build the app:** open the folder in Android Studio and press ▶ Run, or:
   ```bash
   ./gradlew assembleDebug
   # APK: app/build/outputs/apk/debug/app-debug.apk
   ```

## Project layout

```
app/src/main/
  java/com/example/dwgconverter/
    MainActivity.kt   UI: pick file, choose version, save
    DwgVersions.kt    output list + version detection from the file header
    DwgNative.kt      JNI bridge
  cpp/
    dwgconv.c         calls dwg_read_file → dwg_write_file / dxf_write_file
    CMakeLists.txt    links the prebuilt static libredwg.a
scripts/build-libredwg.sh
```

## Troubleshooting

- **`LibreDWG for arm64-v8a not found`**: run step 1 first.
- **configure fails with an iconv error**: keep `API=28` or higher in the script. The app's `minSdk` is 28 (Android 9) for this reason.
- **App closes during conversion of a particular file**: LibreDWG crashed on that drawing (native crashes can't be caught). Try DXF output, or a newer LibreDWG release.
- **Linker errors about missing symbols** in a newer LibreDWG: check that `dwg_write_file` and `dxf_write_file` still exist in `include/dwg.h`.

## Developer

Developed by **Kamil**.

## License

LibreDWG is **GPLv3**, so this app (which links to it) must also be distributed under the GPLv3 with its source code. That's fine for personal or in-house use. To publish a closed-source app, you'd need the commercial ODA Drawings SDK instead.

## Download the APK (GitHub Actions)

Every push to `main` builds the APK automatically:
**Actions** tab → latest **Build APK** run → **Artifacts** → `MeyDwgConverter-apk` (a zip containing `MeyDwgConverter.apk`).

To publish a downloadable release, push a tag, e.g. `git tag v1.0 && git push origin v1.0`. The APK is then attached under **Releases**.
