NativeMacBrowser
================

A lightweight, native macOS web browser built with WebKit and Cocoa. It includes a macOS-style menu bar, toolbar with navigation controls, tabs, and basic bookmarks. The project ships with scripts to build a `.app`, package a `.dmg`, and publish a GitHub Release with downloadable artifacts.

Features
--------
- Native UI: standard menu bar and customizable toolbar
- Tabs: multiple `WKWebView` instances via `NSTabView`
- Navigation: Back/Forward, Reload/Stop, Home, URL field with search fallback
- Bookmarks: add current page and open from the Bookmarks menu
- Packaging: produces `.app` and `.dmg` artifacts
- Publishing: one‑command GitHub release with assets

Requirements
------------
- macOS with Xcode Command Line Tools: `xcode-select --install`
- Optional (recommended for Swift build): Full Xcode and select it: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`

Quick Start
-----------
- Build artifacts
  - `cd NativeMacBrowser && chmod +x build.sh && ./build.sh`
- Run locally
  - `open NativeMacBrowser/dist/NativeMacBrowser.app`
- Create a DMG (done by the build script)
  - `NativeMacBrowser/dist/NativeMacBrowser.dmg`

Keyboard Shortcuts
------------------
- New Tab: `Cmd+T`
- Close Tab: `Cmd+W`
- Reload: `Cmd+R`
- Back/Forward: `Cmd+[` / `Cmd+]`

Publishing a Release (GitHub)
-----------------------------
The publish script builds artifacts (if needed), pushes code, tags, creates/uses a GitHub Release, and uploads the `.dmg` and a zipped `.app`.

- Export token (needs repo write permission)
  - `export GITHUB_TOKEN=YOUR_TOKEN`
- Optional overrides
  - `export GITHUB_REPO=allthingssecurity/browser`
  - `export RELEASE_TAG=v1.0-b1-$(date +%Y%m%d-%H%M%S)`
  - `export RELEASE_NAME="NativeMacBrowser 1.0 (build 1)"`
- Run publish
  - `cd NativeMacBrowser && chmod +x publish.sh && ./publish.sh`

Artifacts
---------
- App bundle: `NativeMacBrowser/dist/NativeMacBrowser.app`
- Disk image: `NativeMacBrowser/dist/NativeMacBrowser.dmg`
- Release assets (after publish): on your GitHub repo’s Releases page

Android Browser (WebView)
------------------------
An Android variant lives under `android-browser/` and builds a simple WebView-based browser with a Chrome-like address bar that clears on focus, auto‑prefixes `https://`, and falls back to search.

- Path: `android-browser/`
- App id: `com.example.northstarquest`
- Entry: `app/src/main/java/com/example/northstarquest/MainActivity.kt`
- UI: `app/src/main/java/com/example/northstarquest/ui/BrowserScreen.kt`
- Ignore rules: `android-browser/.gitignore` (keeps Gradle/IDE/build files out of git)

Setup (JDK + Android SDK)
- JDK 17 (Homebrew):
  - `brew install openjdk@17`
  - `export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"`
  - `export PATH="$JAVA_HOME/bin:$PATH"`
- Android commandline tools + platform-tools:
  - `brew install --cask android-commandlinetools android-platform-tools`
  - `export ANDROID_SDK_ROOT="$(brew --prefix)/share/android-commandlinetools"`
  - Accept/install packages:
    - `yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses`
    - `sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "build-tools;34.0.0" "platforms;android-34"`
- SDK path for Gradle:
  - Create `android-browser/local.properties` with:
    - `sdk.dir=/Users/<you>/homebrew/share/android-commandlinetools`

Build APK
- `cd android-browser && ./gradlew assembleDebug`
- Output: `android-browser/app/build/outputs/apk/debug/app-debug.apk`

Run on Emulator
- Create AVD (one-time):
  - `sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "system-images;android-34;google_apis;arm64-v8a"`
  - `echo no | avdmanager create avd -n Pixel_34 -k "system-images;android-34;google_apis;arm64-v8a"`
- Start + install + launch:
  - `"$ANDROID_SDK_ROOT/emulator/emulator" -avd Pixel_34 -netdelay none -netspeed full -no-snapshot &`
  - `adb wait-for-device; until adb shell getprop sys.boot_completed | grep -m1 1; do sleep 2; done`
  - `adb install -r android-browser/app/build/outputs/apk/debug/app-debug.apk`
  - `adb shell am start -n com.example.northstarquest/.MainActivity`

Run on Device
- Enable Developer options and USB debugging.
- `adb devices` shows the phone; then `adb install -r <apk>` and launch as above.

Notes
- The Android app handles `VIEW` intents for `http/https` and allows clear‑text traffic for local testing. Remove `usesCleartextTraffic` before production.
- To change the default start page, edit `MainActivity.kt` (`startUrl`).

Implementation Notes
--------------------
- Primary implementation (Objective‑C fallback): `NativeMacBrowser/ObjCSources/AppDelegate.m:1`, `NativeMacBrowser/ObjCSources/main.m:1`
- Swift version (not compiled if local Swift toolchain has module conflicts): `NativeMacBrowser/Sources/AppDelegate.swift:1`, `NativeMacBrowser/Sources/main.swift:1`
- App metadata: `NativeMacBrowser/Info.plist:1`
- Build script: `NativeMacBrowser/build.sh:1`
- Publish script: `NativeMacBrowser/publish.sh:1`

Build Details
-------------
- The build prefers Swift (`swiftc` with `Cocoa` + `WebKit`). If the local toolchain has module issues, it falls back to Objective‑C (`clang -fobjc-arc -fmodules`). Either path produces the same `.app` and `.dmg` layout.
- The build uses ad‑hoc codesigning for local runs. For distribution, use your Developer ID identity.

Signing & Notarization (for distribution)
-----------------------------------------
- Replace ad‑hoc codesign step in `build.sh` with your Developer ID certificate, e.g.:
  - `codesign --force --deep --options runtime --sign "Developer ID Application: Your Name (TEAMID)" dist/NativeMacBrowser.app`
- Notarize and staple (example):
  - `xcrun notarytool submit dist/NativeMacBrowser.dmg --apple-id <id> --team-id <team> --password <app-specific-pass> --wait`
  - `xcrun stapler staple dist/NativeMacBrowser.dmg`

Troubleshooting
---------------
- Swift toolchain errors (e.g., `SwiftBridging redefinition` / `could not build module 'Foundation'`):
  - Install full Xcode and select it: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
  - Re‑run: `./build.sh`
- Gatekeeper blocks the app: right‑click the `.app` → Open, or clear quarantine:
  - `xattr -dr com.apple.quarantine NativeMacBrowser/dist/NativeMacBrowser.app`
- Release already exists (tag collision): set a fresh `RELEASE_TAG` and re‑run `publish.sh`.

Roadmap
-------
- Persistent bookmarks/history (~/Library/Application Support)
- Downloads manager UI
- Address bar improvements: `Cmd+L`, autocomplete, search suggestions
- Private windows, per‑site permissions, content blocking via `WKContentRuleList`
- Xcode project + CI workflow for build/sign/notarize on tag

Project Structure
-----------------
- Source (ObjC): `NativeMacBrowser/ObjCSources/`
- Source (Swift): `NativeMacBrowser/Sources/`
- Packaging: `NativeMacBrowser/build.sh:1`
- Publishing: `NativeMacBrowser/publish.sh:1`
- Artifacts: `NativeMacBrowser/dist/`

Contributing
------------
- Open issues and PRs are welcome. Proposed areas: UI polish, downloads/history/bookmarks persistence, security/privacy features, build/release automation.
