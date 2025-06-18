#!/bin/bash
#
# Script: pull_framework.sh
#
# Description:
#   This script pulls the framework-res.apk file from an Android device and retrieves basic device information.
#
# Usage:
#  ./pull_framework.sh OUT_DIR [SERIAL]
#
# Arguments:
#   OUT_DIR: Directory where the APK will be saved
#   SERIAL: (Optional) Serial number of the target device if multiple devices are connected.

output_dir=$1
serial=$2

# Check if the output directory is provided
if [ -z "$output_dir" ]; then
    echo "Usage: pull_framework.sh <output_dir> [serial]"
    exit 1
fi

# Create the output directory if it doesn't exist
mkdir -p "$output_dir"

echo "Output directory: $output_dir"

# Pull the file from the device
if [ -z "$serial" ]; then
    # No serial provided, pull from the default device
    echo "Pulling framework-res.apk from default device..."
    adb pull /system/framework/framework-res.apk "$output_dir/framework-res.apk"
else
    # Pull from the specified device
    echo "Pulling framework-res.apk from device with serial $serial..."
    adb -s "$serial" pull /system/framework/framework-res.apk "$output_dir/framework-res.apk"
fi

# Verify if the adb pull command was successful
if [ $? -ne 0 ]; then
    echo "Error: Failed to pull framework-res.apk"
    exit 1
else
    echo "Success: framework-res.apk pulled successfully"
fi

# Create a device_info.txt file to store device properties
info_file="$output_dir/device_info.md"
echo "Collecting device information..."

if [ -z "$serial" ]; then
    adb_prefix="adb"
else
    adb_prefix="adb -s $serial"
fi

# Retrieve and save device properties
{
    echo "Device Information:"
    echo "-------------------"
    echo "Model: $($adb_prefix shell getprop ro.product.model)"
    echo "Manufacturer: $($adb_prefix shell getprop ro.product.manufacturer)"
    echo "Android Version: $($adb_prefix shell getprop ro.build.version.release)"
    echo "API Level: $($adb_prefix shell getprop ro.build.version.sdk)"
    echo "Serial Number: $($adb_prefix get-serialno)"
    echo "Architecture: $($adb_prefix shell getprop ro.product.cpu.abi)"
    echo "Current Date: $(date)"
} > "$info_file"

echo "Device information saved to $info_file"

