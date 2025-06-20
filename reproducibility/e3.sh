#!/usr/bin/env bash
#
# Script: e3.sh
#
# Description:
#   Executes experiment 3.
#   Runs the malicious app detection pipeline on a subset of apps.
#
# Usage:
#   ./e3.sh APK_DIR OUT_DIR
#
# Arguments:
#   APK_DIR - Path to the directory containing the APKs to be analyzed.
#   OUT_DIR - Directory to store the output and results.

abort() {
    echo "ERROR: $1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MALTAP_DIR="${ROOT_DIR}/malicious_app_detection"
MALTAP_EXTRACT_DIR="${MALTAP_DIR}/code/MalTapExtract"
MALTAP_ANALYZE_DIR="${MALTAP_DIR}/code/MalTapAnalyze"
FRAMEWORK_RES_APK="${MALTAP_DIR}/results/2025-01-09/android_framework/framework-res.apk"

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <APK_DIR> <OUT_DIR>"
    echo "  APK_DIR: Path to the directory containing the APKs to be analyzed."
    echo "  OUT_DIR: Directory to store the output and results."
    exit 1
fi

echo "--------------------------------"
echo "Experiment 3: Malicious Apps"
echo "--------------------------------"

APK_DIR="$(realpath "$1")"
OUT_DIR="$(realpath "$2")"

DATABASE="${OUT_DIR}/animations.db"

# Check if Docker is installed and running
command -v docker >/dev/null || abort "Docker is not installed. Please install it to run this script."

# Run MalTapExtract
#echo "> Step 1: Run MalTapExtract"
#echo "   Build the MalTapExtract Docker image"
#docker build -t taptrap_maltapextract "${MALTAP_EXTRACT_DIR}" > /dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_maltapextract'"
# Re-use the framework-res.apk used in the paper
#mkdir -p "$OUT_DIR" || abort "Failed to create output directory"
#mkdir -p "$OUT_DIR/android_framework" || abort "Failed to create android_framework directory"
#cp "$FRAMEWORK_RES_APK" "$OUT_DIR/android_framework/" || abort "Failed to copy framework-res.apk into output directory"
#echo "   Run the MalTapExtract Docker container"
#docker run --rm -v "$OUT_DIR:/output" -v "$APK_DIR:/apks" taptrap_maltapextract /output /apks 6

# Run MalTapAnalyze
echo "> Step 2: Run MalTapAnalyze"
echo "   Build the MalTapAnalyze Docker image"
docker build -t taptrap_maltapanalyze "${MALTAP_ANALYZE_DIR}" > /dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_maltapanalyze'"
echo "   Run the MalTapAnalyze Docker container"
docker run --rm -v "$DATABASE:/animations.db" taptrap_maltapanalyze /animations.db || abort "Failed to run MalTapAnalyze"

######## Verification ########

echo "> Step 3: Gather the results"
EXPECTED_ANIMS_EXCEEDED="61"
EXPECTED_APPS_SCORE="28"

VALUE_ANIMS_EXCEEDED=$(grep '\\newcommand{\\maltapNumberUniqueAnimationsExtendedDuration}' "${REPORT_FILE}" | \
        sed -E 's/.*\{([^}]*)\}$/\1/')

VALUE_APPS_SCORE=$(grep '\\newcommand{\\maltapNumberAppsAnimationsScoreMin}' "${REPORT_FILE}" | \
        sed -E 's/.*\{([^}]*)\}$/\1/')

# check if expected_anims_exceeded is the same as value_anims_exceeded
if [[ "$VALUE_ANIMS_EXCEEDED" != "$EXPECTED_ANIMS_EXCEEDED" ]]; then
    abort "Expected number of unique animations exceeded: $EXPECTED_ANIMS_EXCEEDED, but got $VALUE_ANIMS_EXCEEDED" >&2
fi

# check if expected_apps_score is the same as value_apps_score
if [[ "$VALUE_APPS_SCORE" != "$EXPECTED_APPS_SCORE" ]]; then
    abort "Expected number of apps with animations score: $EXPECTED_APPS_SCORE, but got $VALUE_APPS_SCORE" >&2
fi

echo "--------------------------------"
echo "OK."
echo "--------------------------------"