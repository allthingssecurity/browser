#!/usr/bin/env bash
set -euo pipefail

APP_NAME="NativeMacBrowser"
BUNDLE_ID="com.example.NativeMacBrowser"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/Sources"
INFO_PLIST_SRC="$ROOT_DIR/Info.plist"
BUILD_DIR="$ROOT_DIR/.build"
DIST_DIR="$ROOT_DIR/dist"
APP_DIR="$DIST_DIR/$APP_NAME.app"
CONTENTS_DIR="$APP_DIR/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RES_DIR="$CONTENTS_DIR/Resources"
DMG_STAGING="$DIST_DIR/dmg"

echo "==> Cleaning output dirs"
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$MACOS_DIR" "$RES_DIR" "$DMG_STAGING"

echo "==> Compiling sources"
SDK_PATH="$(xcrun --show-sdk-path --sdk macosx)"
BIN_PATH="$BUILD_DIR/$APP_NAME"

set +e
if command -v swiftc >/dev/null 2>&1; then
  echo "Attempting Swift build with $(swiftc --version | head -n1)"
  swiftc \
    -sdk "$SDK_PATH" \
    -framework Cocoa \
    -framework WebKit \
    "$SRC_DIR"/*.swift \
    -o "$BIN_PATH"
  SWIFT_STATUS=$?
else
  SWIFT_STATUS=127
fi
set -e

if [ ${SWIFT_STATUS:-1} -ne 0 ]; then
  echo "Swift build failed or swiftc not available (code $SWIFT_STATUS). Falling back to Objective-C."
  OBJC_SRC_DIR="$ROOT_DIR/ObjCSources"
  clang \
    -fobjc-arc \
    -fmodules \
    -isysroot "$SDK_PATH" \
    -framework Cocoa \
    -framework WebKit \
    "$OBJC_SRC_DIR"/*.m \
    -o "$BIN_PATH"
fi

echo "==> Assembling .app bundle"
cp "$INFO_PLIST_SRC" "$CONTENTS_DIR/Info.plist"
install -m 0755 "$BIN_PATH" "$MACOS_DIR/$APP_NAME"

if command -v plutil >/dev/null 2>&1; then
  echo "==> Validating Info.plist"
  plutil -lint "$CONTENTS_DIR/Info.plist" >/dev/null
fi

echo "==> Ad-hoc signing (optional)"
if command -v codesign >/dev/null 2>&1; then
  codesign --force --deep -s - "$APP_DIR" || true
fi

echo "==> Preparing DMG staging"
cp -R "$APP_DIR" "$DMG_STAGING/$APP_NAME.app"
ln -s /Applications "$DMG_STAGING/Applications" || true

DMG_PATH="$DIST_DIR/$APP_NAME.dmg"
echo "==> Creating DMG: $DMG_PATH"
hdiutil create -volname "$APP_NAME" -srcfolder "$DMG_STAGING" -ov -format UDZO "$DMG_PATH"

echo "\nBuild complete:" 
echo "  .app => $APP_DIR"
echo "  .dmg => $DMG_PATH"
