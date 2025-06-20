#!/usr/bin/env bash
#
# Script: e1.sh
#
# Description:
#   Executes experiment 1.
#   Executes the full dataset preparation pipeline:
#   1. Builds and runs a crawler to fetch Google Play metadata.
#   2. Samples a subset of apps.
#   3. Downloads the APKs and additional files.
#   4. Merges split APKs.
#   6. Verifies the dataset.
#
# Usage:
#   ./e1.sh <EMAIL> <TOKEN> <OUTPUT_DIR>
#
# Arguments:
#   EMAIL       - Google account email.
#   TOKEN       - Google account token (AAS token).
#   OUTPUT_DIR  - Directory to store output data.
#
# Example:
#   ./e1.sh user@gmail.com AAS_token /path/to/output

abort() {
    echo "ERROR: $1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATASET_PREPARATION_DIR="${SCRIPT_DIR}/../dataset_preparation"

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 <EMAIL> <TOKEN> <OUTPUT_DIR>"
    exit 1
fi

EMAIL="$1"
TOKEN="$2"
OUTPUT_DIR="$(realpath "$3")"

OUTPUT_CRAWLER_DIR="${OUTPUT_DIR}/crawl"
OUTPUT_DOWNLOAD_DIR="${OUTPUT_DIR}/download"
OUTPUT_DOWNLOAD_APP_DIR="${OUTPUT_DOWNLOAD_DIR}/apps"
OUTPUT_DOWNLOAD_LOG_DIR="${OUTPUT_DOWNLOAD_DIR}/logs"
OUTPUT_MERGED_DIR="${OUTPUT_DIR}/apps"

APPS_CSV="${OUTPUT_CRAWLER_DIR}/apps.csv"
SAMPLED_CSV="${OUTPUT_CRAWLER_DIR}/75_apps.csv"

echo "--------------------------------"
echo "Experiment 1: Dataset Preparation"
echo "--------------------------------"

echo "> Step 1: Run Google Play crawler"

echo "Building crawler Docker image..."
docker build -t taptrap_crawler "${DATASET_PREPARATION_DIR}/crawler" 1>/dev/null || abort "Failed to build crawler image"

echo "Running crawler..."
docker run --rm -v "${OUTPUT_CRAWLER_DIR}:/app/out" taptrap_crawler \
    --output /app/out --onlyFree --onlyMinInstalls 0 --max 25000 --log /app/out/logfile.log || abort "Crawler failed"

[[ -f "${APPS_CSV}" ]] || abort "apps.csv not found after crawling"

if ! shuf "${APPS_CSV}" | head -n 50 > "${SAMPLED_CSV}"; then
    abort "Failed to sample apps"
fi

echo " > Step 2: Download APKs"

mkdir -p "${OUTPUT_DOWNLOAD_APP_DIR}" "${OUTPUT_DOWNLOAD_LOG_DIR}" || abort "Failed to create output directories"

echo "Building downloader Docker image..."
docker build -t taptrap_downloader "${DATASET_PREPARATION_DIR}/downloader" 1>/dev/null || abort "Failed to build downloader image"

echo "Running downloader..."
docker run -it --rm \
    -v "${SAMPLED_CSV}:/data/input.csv" \
    -v "${OUTPUT_DOWNLOAD_APP_DIR}:/data/output" \
    -v "${OUTPUT_DOWNLOAD_LOG_DIR}:/data/logs" \
    taptrap_downloader \
    /data/input.csv /data/output /data/logs "${EMAIL}" "${TOKEN}" \
    split_apk=1,device=pixel_6a,locale=at,include_additional_files=1

echo " > Step 3: Merge split APKs"

docker build -t taptrap_merger "${DATASET_PREPARATION_DIR}/merger" 1>/dev/null || abort "Failed to build merger image"

docker run -it --rm \
    -v "${OUTPUT_DOWNLOAD_APP_DIR}:/input/apks" \
    -v "${OUTPUT_MERGED_DIR}:/output/results" \
    -v "${SAMPLED_CSV}:/input/apk_list.txt" \
    taptrap_merger \
    /input/apks /output/results /input/apk_list.txt

echo "Copying raw APKs..."
find "${OUTPUT_DOWNLOAD_APP_DIR}" -maxdepth 1 -type f -name '*.apk' ! -name '*_merged.apk' \
    -exec cp {} "${OUTPUT_MERGED_DIR}" \; || abort "Failed to copy APKs"


######### Verification #########

echo "> Step 4: Verification"

LINE_COUNT=$(wc -l < "${APPS_CSV}")
if [[ $LINE_COUNT -lt 15000 ]]; then
    abort "apps.csv contains fewer than 15000 lines (actual: ${LINE_COUNT})"
fi
echo "apps.csv contains at least 15000 lines"

APK_COUNT=$(find "${OUTPUT_MERGED_DIR}" -type f -name '*.apk' | wc -l)
if [[ $APK_COUNT -lt 30 ]]; then
    abort "Merged APK directory contains fewer than 30 files (actual: ${APK_COUNT})"
fi
echo "Merged APK directory contains at least 30 files"

echo "--------------------------------"
echo "OK."
echo "--------------------------------"