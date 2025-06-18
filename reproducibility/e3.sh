#!/usr/bin/env bash
#
# Script: e3.sh
#
# Description:
#   Executes experiment 3.
#   Runs the malicious app detection pipeline on a subset of apps.
#
# Usage:
#   ./e3.sh APK_DIR
#
# Arguments:
#   APK_DIR - Path to the directory containing the APKs to be analyzed.

set -euo pipefail

abort() {
    echo "ERROR: $1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MALTAP_DIR="${ROOT_DIR}/malicious_app_detection"
MALTAP_EXTRACT_DIR="${MALTAP_DIR}/code/MalTapExtract"
MALTAP_ANALYZE_DIR="${MALTAP_DIR}/code/MalTapAnalyze"
FRAMEWORK_RES_APK="${MALTAP_DIR}/results/android_framework/framework-res.apk"

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 APK_DIR OUT_DIR"
    exit 1
fi

echo "--------------------------------"
echo "Experiment 3: Malicious Apps"
echo "--------------------------------"

APK_DIR="$(realpath "$1")"
OUT_DIR="$(realpath "$2")"

DATABASE="${OUT_DIR}/animations.db"

# Check if Docker is installed and running
command -v adb >/dev/null || abort "ADB is not installed. Please install it to run this script."
command -v docker >/dev/null || abort "Docker is not installed. Please install it to run this script."

# Run MalTapExtract
echo "> Step 1: Run MalTapExtract"
echo "   Build the MalTapExtract Docker image"
docker build -t taptrap_maltapextract "${MALTAP_EXTRACT_DIR}" > /dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_maltapextract'"
# Re-use the framework-res.apk used in the paper
mkdir -p "$OUT_DIR" || abort "Failed to create output directory"
cp "$FRAMEWORK_RES_APK" "$OUT_DIR/android_framework" || abort "Failed to copy framework-res.apk into output directory"
echo "   Run the MalTapExtract Docker container"
docker run --rm -v "$OUTPUT_DIR:/output" -v "$APKS_DIR:/apks" taptrap_maltapextract /output /apks 6 || abort "Failed to run MalTapExtract"

# Run MalTapAnalyze
echo "> Step 2: Run MalTapAnalyze"
echo "   Build the MalTapAnalyze Docker image"
docker build -t taptrap_maltapanalyze "${MALTAP_ANALYZE_DIR}" > /dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_maltapanalyze'"
echo "   Run the MalTapAnalyze Docker container"
docker run --rm -v "$DATABASE:/animations.db" taptrap_maltapanalyze /animations.db || abort "Failed to run MalTapAnalyze"

echo "> Step 3: Gather the results"
echo "NOT YET IMPLEMENTED"

echo "--------------------------------"
echo "OK."
echo "--------------------------------"