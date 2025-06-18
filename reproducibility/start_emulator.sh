#!/bin/bash
#
# Script: create_emulator.sh
#
# Description:
#   Creates and launches an Android Virtual Device (AVD) named 'taptrap_emulator'
#   using the Pixel 6a device profile and system image for Android API level 35.
#   Installs required emulator components if missing, and handles architecture-specific constraints.
#
# Usage:
#  ./create_emulator.sh
#
# Arguments:
#   None. The script reads environment variable ANDROID_HOME to locate SDK tools.
#
# Dependencies:
#   - ANDROID_HOME: Must be set to the path of the Android SDK.
#   - Android Command Line Tools: Must be installed and available in the SDK path.

set -euo pipefail

abort() {
  echo "❌ $1" >&2
  exit 1
}

echo "🔍 Checking ANDROID_HOME..."
if [ -z "${ANDROID_HOME:-}" ]; then
  abort "ANDROID_HOME is not set. Please set it to your Android SDK path."
fi

SDKMANAGER_PATH="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER_PATH="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
EMULATOR_PATH="$ANDROID_HOME/emulator/emulator"

echo "🔍 Checking required tools..."
if [ ! -f "$SDKMANAGER_PATH" ]; then
  abort "sdkmanager is not installed at $SDKMANAGER_PATH. Please install it."
fi

if [ ! -f "$AVDMANAGER_PATH" ]; then
  abort "avdmanager is not installed at $AVDMANAGER_PATH. Please install it."
fi

echo "📦 Installing/updating emulator..."
echo "    You may be prompted to accept licenses ..."
sleep 5
if ! "$SDKMANAGER_PATH" "platform-tools" "emulator"; then
  abort "Failed to install/update emulator."
fi

echo "🔍 Determining system architecture..."
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)
    SYSTEM_ARCH="x86_64"
    ;;
  arm64 | aarch64)
    SYSTEM_ARCH="arm64-v8a"
    ;;
  *)
    abort "Unsupported system architecture: $ARCH"
    ;;
esac

if [[ "$SYSTEM_ARCH" == "arm64-v8a" && "$(uname)" != "Darwin" ]]; then
  abort "Android emulators are not supported on arm64 architecture on non-macOS systems."
fi

echo "📦 Installing/updating system image for Android 35 ($SYSTEM_ARCH)..."
echo "    You may be prompted to accept licenses ..."
sleep 5
if ! "$SDKMANAGER_PATH" "system-images;android-35;google_apis;$SYSTEM_ARCH"; then
  abort "Failed to install system image."
fi

echo "🛠️ Creating AVD named 'taptrap_emulator'..."
if ! "$AVDMANAGER_PATH" create avd -n taptrap_emulator \
    --force \
    -k "system-images;android-35;google_apis;$SYSTEM_ARCH" \
    --device "pixel_6a" > /dev/null; then
  abort "Failed to create AVD."
fi

echo "🚀 Starting emulator..."
if ! "$EMULATOR_PATH" -avd taptrap_emulator > /dev/null; then
  abort "Failed to start emulator."
fi