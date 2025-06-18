#!/usr/bin/env bash
#
# Script: e2.sh
#
# Description:
#   Executes experiment 2.
#   Builds and runs the TapTrap PoC Docker image, installs the generated APK,
#   and starts the PoC app on a connected Android device or emulator.
#
# Usage:
#   ./e2.sh

set -euo pipefail

abort() {
    echo "ERROR: $1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
POC_DIR="${ROOT_DIR}/poc"
POC_DIR_OUTPUT="${SCRIPT_DIR}/out/poc"

echo "--------------------------------"
echo "Experiment 2: POC"
echo "--------------------------------"

# Check dependencies
command -v adb >/dev/null || abort "ADB is not installed. Please install it to run this script."
command -v docker >/dev/null || abort "Docker is not installed. Please install it to run this script."

# Ensure a device is connected
if [ "$(adb devices | grep -w "device" | wc -l)" -eq 0 ]; then
    echo "(?) No device is connected. Do you want to start an emulator? (y/n)?"
    read -r start_emulator
    if [[ "$start_emulator" == "y" ]]; then
        EMU_SCRIPT="$ROOT_DIR/start_emulator.sh"
        [[ -x "$EMU_SCRIPT" ]] || abort "start_emulator.sh is not executable (chmod +x it)"
        "$EMU_SCRIPT" &
        disown
    else
        abort "Please connect a device and try again."
    fi
fi

adb wait-for-device
echo "Device is ready."

# Set platform flag for ARM hosts
DOCKER_PLATFORM=""
case "$(uname -m)" in
    arm64|aarch64)
        DOCKER_PLATFORM="--platform=linux/amd64"
        ;;
esac

# Build Docker image
echo "Building the POC Docker image..."
docker build ${DOCKER_PLATFORM} -t taptrap_poc "$POC_DIR" >/dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_poc'"

# Create output directory
mkdir -p "$POC_DIR_OUTPUT" || abort "Failed to create output directory: ${POC_DIR_OUTPUT}"

# Run container to produce APK
echo "Running the POC Docker container..."
docker run --rm -v "$POC_DIR_OUTPUT:/apk-output" taptrap_poc >/dev/null 2>&1 || abort "Failed to run Docker container"

# Install APK
APK_PATH="${POC_DIR_OUTPUT}/click.taptrap.poc.apk"
[ -f "$APK_PATH" ] || abort "APK not found at $APK_PATH"
echo "Installing the APK..."
adb install -r "$APK_PATH" >/dev/null 2>&1 || abort "Failed to install APK"

# Launch APK
echo "Launching the PoC app..."
adb shell am start -n "click.taptrap.poc/.MainActivity" >/dev/null 2>&1 || abort "Failed to launch PoC app"

echo "==> Continue the experiment on the device"