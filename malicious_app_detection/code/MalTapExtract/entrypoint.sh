#!/bin/bash
# 
# Script: entrypoint.sh
# 
# Description:
#   Extracts animation information from APKs.
#   1. Extracts framework animations from an Android framework APK.
#   2. Extracts app animations from a directory of APKs.
#   This script requires that the framework APK is already extracted and is located in the output directory.
#
# Usage:
#     ./entrypoint.sh <jar_file> <output_dir> <apk_dir> <parallelism>
#
# Arguments:
#   jar_file: Path to the MalTapExtract JAR file.
#   output_dir: Directory to store the output files.
#   apk_dir: Directory containing APK files to analyze.
#   parallelism: Number of parallel jobs to run for app animation extraction.


if [[ $# -ne 4 ]]; then
    echo "Usage: $0 <jar_file> <output_dir> <apk_dir> <parallelism>"
    exit 1
fi

JAR_FILE="$1"
OUTPUT_DIR="$2"
APK_DIR="$3"
PARALLELISM="$4"

FRAMEWORK_DIR="$OUTPUT_DIR/android_framework"
FRAMEWORK_APK="$FRAMEWORK_DIR/framework-res.apk"

LOG_DIR="$OUTPUT_DIR/logs"
DB_PATH="$OUTPUT_DIR/animations.db"
CACHE_DIR="$OUTPUT_DIR/cache"

if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi


# extract framework animations
java -jar "$JAR_FILE" -apk "$FRAMEWORK_APK" -framework -cache "$CACHE_DIR" -database "$DB_PATH"
if [ $? -eq 0 ]; then
    echo "Framework animations extracted successfully."
else
    echo "Error: Framework animation extraction failed. Please check the input files and logs."
    exit 1
fi

# extract app animations
find "$APK_DIR" -maxdepth 1 -type f -name "*.apk" | parallel --joblog "$LOG_DIR/joblog.log" \
         --results "$LOG_DIR" \
         --resume \
         --progress \
         --eta \
         --no-notice \
         --plain \
         --jobs "$PARALLELISM" \
         java -jar "$JAR_FILE" -apk {} -database "$DB_PATH" -cache "$CACHE_DIR"

# --no-notice + --plain: Suppress the notice and plain output and make it non-interactive, otherwise we will get some errors
