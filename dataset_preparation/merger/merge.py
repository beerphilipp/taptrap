"""
APK Merger CLI wrapper for APKEditor

This script provides a CLI tool to merge split APKs using the
APKEditor JAR.

Usage:
    python merge.py merge <apk_dir> <package_name> <out_dir>

Author: Philipp Beer
"""

import os
import sys
import click
import logging
import subprocess

logger = logging.getLogger(__name__)

@click.group()
def cli():
    """APK Merger CLI"""
    pass


@cli.command()
@click.argument("apk_dir", type=click.Path(exists=True))
@click.argument("package_name")
@click.argument("out_dir", type=click.Path())
def merge(apk_dir: str, 
          package_name: str,
          out_dir: str):
    """
    Python wrapper for APKEditor. Merge multiple APKs into a single APK.
    
    - If a single APK exists as <package_name>.apk, it will be ignored.
    - If multiple APKs exist under <apk_dir>/<package_name>/, they will be merged.
    """
    
    if os.path.exists(os.path.join(apk_dir, f"{package_name}.apk")):
        logger.info(f"Single package {package_name} found.")
        return

    if not os.path.isdir(os.path.join(apk_dir, package_name)):
        logger.error(f"The directory '{apk_dir}' does not exist.")
        sys.exit(1)
        return
    
    merge_apk_folder = os.path.join(apk_dir, package_name)
    out_file = os.path.join(out_dir, f"{package_name}_merged.apk")
    
    # output should be text
    process = subprocess.Popen(
        ["java", "-jar", "APKEditor-1.4.1.jar", "m", "-i", merge_apk_folder, "-o", out_file],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
                               )
    stdout, stderr = process.communicate()
    # print stdout
    print(f"out: {stdout}")

    return_code = process.returncode

    if return_code != 0:
        logger.error(f"Error merging apks: {stderr}")
        sys.exit(1)
    else:
        logger.info(f"Apks merged successfully.")
        return

if __name__=="__main__":
    cli()