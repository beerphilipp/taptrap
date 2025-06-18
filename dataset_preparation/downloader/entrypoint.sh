#!/bin/bash
#
# Script: entrypoint.sh
#
# Description:
#   Downloads APKs from the Google Play Store using apkeep in parallel.
#   Expects a CSV file with package names and downloads each app using the
#   provided Google Play credentials (email and token).
#
#   Used as the Docker entrypoint for the TapTrap APK downloader container.
#
# Author: Philipp Beer

CSV_FILE="$1"
OUTPUT_DIR="$2"
LOG_DIR="$3"
EMAIL="$4"
TOKEN="$5"
OPTIONS="$6"

# Check if all the required arguments are provided
if [[ $# -ne 6 ]]; then
          echo "Usage: $0 <input.csv> <output_dir> <log_dir> <email> <token> <options>"
          exit 1
fi

echo "Email: $EMAIL"
echo "Token: $TOKEN"

# Check if the CSV file exists
if [[ ! -f "$CSV_FILE" ]]; then
          echo "CSV file not found!"
            exit 1
fi

cat "$CSV_FILE" | \
          parallel \
          --joblog "$LOG_DIR/joblog.log" \
          --results "$LOG_DIR" \
          --resume \
          --progress \
          --delay 7 \
          --eta \
          --jobs 4 \
          apkeep -a {} -d google-play -e "${EMAIL}" -t "${TOKEN}" -o $OPTIONS $OUTPUT_DIR