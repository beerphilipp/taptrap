#!/usr/bin/env bash
#
# Script: basic_test.sh
#
# Description:
#   Performs a basic test of th TapTrap artifacts.
#   1. Builds the necessary Docker images.
#   2. Checks if the Google credentials are valid.
#   3. Checks if the Android device is connected.

#
# Usage:
#   ./e1.sh <EMAIL> <TOKEN> <OUTPUT_DIR>
#
# Arguments:
#   EMAIL       - Google account email.
#   TOKEN       - Google account token (AAS token).

set -euo pipefail

abort() {
  echo "ERROR: $1" >&2
  exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/test_output"
DATASET_PREPARATION_DIR="${SCRIPT_DIR}/../dataset_preparation"
POC_DIR="${SCRIPT_DIR}/../poc"
VULNERABLE_DIR="${SCRIPT_DIR}/../vulnerable_app_detection"
MALICIOUS_DIR="${SCRIPT_DIR}/../malicious_app_detection"
KILLTHEBUGS_DIR="${SCRIPT_DIR}/../user_study/KillTheBugs"
KILLTHEBUGS_WEB_DIR="${SCRIPT_DIR}/../user_study/web_app"

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <google_email> <google_token>"
  exit 1
fi

GOOGLE_EMAIL="$1"
GOOGLE_TOKEN="$2"

# Check if Docker is installed and the Docker daemon is running
if ! command -v docker &> /dev/null; then
  abort "Docker is not installed. Please install Docker and try again."
fi
if ! docker info &> /dev/null; then
  abort "Docker daemon is not running. Please start Docker and try again."
fi

[[ ! -d "${OUTPUT_DIR}" ]] \
  || abort "Output directory ${OUTPUT_DIR} already exists. Please remove it before running the test and try again."


######### Build Docker Images #########

# ----------- Dataset Preparation -----------

echo "Building Preparation pipeline (1/3)"

if ! docker build -t taptrap_crawler "${DATASET_PREPARATION_DIR}"/crawler 1>/dev/null; then
        abort "Failed to build Docker image 'taptrap_crawler'"
    fi

echo "Building Preparation pipeline (2/3)"
if ! docker build -t taptrap_downloader "${DATASET_PREPARATION_DIR}"/downloader 1>/dev/null; then
        abort "Failed to build Docker image 'taptrap_downloader'"
    fi

echo "Building Preparation pipeline (3/3)"
    if ! docker build -t taptrap_merger "${DATASET_PREPARATION_DIR}"/merger 1>/dev/null; then
        abort "Failed to build Docker image 'taptrap_merger'"
    fi

# ----------- PoC APK Generation -----------

DOCKER_PLATFORM=""
case "$(uname -m)" in
    arm64|aarch64)
        DOCKER_PLATFORM="--platform=linux/amd64"
        ;;
esac
echo "Building the POC Docker image"
docker build ${DOCKER_PLATFORM} -t taptrap_poc "$POC_DIR" 1>/dev/null || abort "Failed to build Docker image 'taptrap_poc'"

# ----------- KillTheBugs App Generation -----------

echo "Building the KillTheBugs Docker image"
docker build ${DOCKER_PLATFORM} -t taptrap_killthebugs "$KILLTHEBUGS_DIR" 1>/dev/null || abort "Failed to build Docker image 'taptrap_killthebugs'"

echo "Building the KillTheBugs Web server Docker image"
docker build -t taptrap_killthebugs_web "$KILLTHEBUGS_WEB_DIR" 1>/dev/null || abort "Failed to build Docker image 'taptrap_killthebugs_web'"

# ----------- Vulnerable App Detection -----------

echo "Building Vulnerable App Detection pipeline (1/2)"
docker build -t taptrap_vulntap "$VULNERABLE_DIR/code" 1>/dev/null || abort "Failed to build Docker image 'taptrap_vulntap'"

echo "Building Vulnerable App Detection pipeline (2/2)"
docker build -t taptrap_vulntap_report "$VULNERABLE_DIR/report" 1>/dev/null || abort "Failed to build Docker image 'taptrap_vulntap_report'"

# ----------- Malicious App Detection -----------

echo "Building Malicious App Detection pipeline (1/3)"
docker build -t taptrap_maltapextract "$MALICIOUS_DIR/code/MalTapExtract" 1>/dev/null || abort "Failed to build Docker image 'taptrap_maltapextract'"

echo "Building Malicious App Detection pipeline (2/3)"
docker build ${DOCKER_PLATFORM} -t taptrap_maltapanalyze "$MALICIOUS_DIR/code/MalTapAnalyze" 1>/dev/null || abort "Failed to build Docker image 'taptrap_maltap'"

echo "Building Malicious App Detection pipeline (3/3)"
docker build -t taptrap_maltapreport "$MALICIOUS_DIR/report" 1>/dev/null || abort "Failed to build Docker image 'taptrap_maltap_report'"

######### Check Google credentials #########

echo "Checking Google credentials..."

# Trying to download the 'com.whatsapp' to check if the credentials are valid
# create a new temp file with the input "com.whatsapp" and save it to a temporary file
TEMP_INPUT_FILE=$(mktemp)
echo "com.whatsapp" > "${TEMP_INPUT_FILE}"
mkdir -p "${OUTPUT_DIR}/logs"
mkdir -p "${OUTPUT_DIR}/output"

docker run --rm \
    -v "${TEMP_INPUT_FILE}:/data/input.csv" \
    -v "${OUTPUT_DIR}/output:/data/output" \
    -v "${OUTPUT_DIR}/logs:/data/logs" \
    taptrap_downloader \
    /data/input.csv /data/output /data/logs "${GOOGLE_EMAIL}" "${GOOGLE_TOKEN}" \
    split_apk=1,device=pixel_6a,locale=at,include_additional_files=1

if [ -z "$(ls -A "${OUTPUT_DIR}/output")" ]; then
  abort "No app was downloaded. Your Google credentials might be invalid. Check the logs in ${OUTPUT_DIR}/logs for more details."
fi


######### Check Android device #########

echo "Checking if an Android device is connected..."

# Check if adb is installed
if ! command -v adb &> /dev/null; then
  abort "adb is not installed. Please install Android SDK Platform Tools."
fi

# Check if an Android device is connected
if ! adb devices | grep -q "device$"; then
  abort "No Android device is connected. Please connect an Android device and ensure USB debugging is enabled."
fi 

echo "--------------------------------"
echo "OK"
echo "--------------------------------"