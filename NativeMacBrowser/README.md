NativeMacBrowser
=================

Simple macOS-native web browser using Swift + WebKit.

Prereqs
-------
- Xcode Command Line Tools (`xcode-select --install`)

Build
-----
```
cd NativeMacBrowser
chmod +x build.sh
./build.sh
```

Artifacts
---------
- App bundle: `NativeMacBrowser/dist/NativeMacBrowser.app`
- Disk image: `NativeMacBrowser/dist/NativeMacBrowser.dmg`

Run
---
- Double-click the `.app` or mount the `.dmg` and drag the app to Applications.

Notes
-----
- The app is ad-hoc signed; for distribution/notarization, sign with a Developer ID and notarize.

