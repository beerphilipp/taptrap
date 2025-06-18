#!/usr/bin/env bash
set -euo pipefail

abort() {
  echo "ERROR: $1" >&2
  exit 1
}


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_APK_DIR="${SCRIPT_DIR}/../example_apks"
OUTPUT_DIR="${SCRIPT_DIR}/test_output"
DATASET_PREPARATION_DIR="${SCRIPT_DIR}/../dataset_preparation"
VULNERABLE_DIR="${SCRIPT_DIR}/../vulnerable_app_detection"
MALICIOUS_DIR="${SCRIPT_DIR}/../malicious_app_detection"

# first argument: email
# second argument: token

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


[[ -d "${EXAMPLE_APK_DIR}" ]] \
  || abort "Example APK directory ${EXAMPLE_APK_DIR} does not exist."

[[ ! -d "${OUTPUT_DIR}" ]] \
  || abort "Output directory ${OUTPUT_DIR} already exists. Please remove it before running the test and try again."

# ----------- Dataset Preparation -----------

echo "Building Preparation pipeline (1/3)"

if ! docker build -t taptrap_crawler "${DATASET_PREPARATION_DIR}"/crawl/crawler > /dev/null 2>&1; then
        abort "Failed to build Docker image 'taptrap_crawler'"
    fi

echo "Building Preparation pipeline (2/3)"
if ! docker build -t taptrap_downloader "${DATASET_PREPARATION_DIR}"/download/downloader > /dev/null 2>&1; then
        abort "Failed to build Docker image 'taptrap_downloader'"
    fi

echo "Building Preparation pipeline (3/3)"
    if ! docker build -t taptrap_merger "${DATASET_PREPARATION_DIR}"/merge > /dev/null 2>&1; then
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
docker build ${DOCKER_PLATFORM} -t taptrap_poc "$POC_DIR" >/dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_poc'"

# ----------- Malicious App Detection -----------




# User Study



echo "--------------------------------"
echo "OK"
echo "--------------------------------"