#!/usr/bin/env bash
#
# Script: e4.sh
#
# Description:
#   Executes experiment 4.
#   Runs the vulnerability detection pipeline on a subset of apps.
#
# Usage:
#   ./e4.sh APK_DIR OUT_DIR
#
# Arguments:
#   APK_DIR - Path to the directory containing the APKs to be analyzed.
#   OUT_DIR - Directory to store the output and report.

abort() {
    echo "ERROR: $1" >&2
    exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VULN_APP_DIR="${SCRIPT_DIR}/../vulnerable_app_detection"

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <APK_DIR> <OUT_DIR>"
    echo "  APK_DIR: Path to the directory containing the APKs to be analyzed."
    echo "  OUT_DIR: Directory to store the output and report."
    exit 1
fi

APK_DIR="$(realpath "$1")"
OUT_DIR="$(realpath "$2")"
REPORT_FILE="${OUT_DIR}/report/report.tex"

mkdir -p "${OUT_DIR}" || abort "Failed to create output directory"

echo "> Step 1: Run vulnerability detection pipeline"

docker build -t taptrap_vulntap "${VULN_APP_DIR}/code" || abort "Failed to build Docker image 'taptrap_vulntap'"

#docker run -it --rm \
#    -v "${APK_DIR}:/apks" \
#    -v "${OUT_DIR}:/output" \
#    taptrap_vulntap \
#    /apks /output 4

echo "> Step 2: Generate report"

mkdir -p "${OUT_DIR}/report" || abort "Failed to create report directory"

docker build -t taptrap_vulntap_report "${VULN_APP_DIR}/report" >/dev/null 2>&1 || abort "Failed to build Docker image 'taptrap_vulntap_report'"

docker run --rm \
    -v "${OUT_DIR}:/output" \
    -v "${OUT_DIR}/report:/report" \
    taptrap_vulntap_report \
    --result_dir /output --report_dir /report || abort "Failed to generate the report"

####### Verification #######

echo "> Step 3: Verification"

EXPECTED_VALUE="76.3"
LOWER_BOUND=$(echo "$EXPECTED_VALUE * 0.90" | bc -l)
UPPER_BOUND=$(echo "$EXPECTED_VALUE * 1.00" | bc -l)

VALUE=$(grep '\\newcommand{\\vulntapAmountAppsMinOneActivityVulnerablePercent}' "${REPORT_FILE}" | \
        sed -E 's/.*\{([^}]*)\}$/\1/')

IS_WITHIN_RANGE=$(echo "$VALUE >= $LOWER_BOUND && $VALUE <= $UPPER_BOUND" | bc -l)

# if not within range, abort

if [[ "$IS_WITHIN_RANGE" -ne 1 ]]; then
   abort "$VALUE% is outside ±10% of $EXPECTED_VALUE%" >&2
fi

echo "--------------------------------"
echo "OK."
echo "--------------------------------"