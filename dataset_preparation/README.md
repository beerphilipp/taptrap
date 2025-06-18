# Dataset Preparation

Pipeline to crawl the Play Store, download apps, and merge split APKs.

## Folder Structure

- [`crawler/`](crawler/) - Crawls the Play Store to collect package names
- [`downloader/`](downloader/) - Downloads apps from the Play Store
- [`merger/`](merger/) - Merges split APKs into standalone APKs
- [`results/`](results/) - Contains metadata about the dataset used in the paper

## Overview

The dataset preparation pipeline consists of three components: (1) the crawler, (2) the downloader, and (3) the merger, which are intended to be run in sequence.

### 1. Crawler

Used to explore package names from the Google Play Store.
See [crawler/README.md](crawler/README.md) for usage instructions.

### 2. Downloader

Downloads APKs for a given list of package names from the Play Store.
See [downloader/README.md](downloader/README.md) for usage instructions.

### 3. Merger

Google Play often distributes apps as split APKs.
This component merges them into a single APK.
See [merger/README.md](merger/README.md) for usage instructions.

## Results

The `results/` directory contains the output of the pipeline as used in the paper.

### [`apps.db.zip`](results/apps.db.zip)

A compressed SQLite database with two tables:
- `pids_table` - all package names explored in the crawl including at what depth they were explored and whether similar apps for them were queried
- `info_table` - package names including metadata, e.g., amount of installations and whether they are free or paid

### [`apps_free.csv`](results/apps_free.csv)

A CSV file (single-column) listing all free apps explored on the Play Store that we attempted to download.

### [`apps_downloaded.csv`](results/apps_downloaded.csv)

A CSV file listing all apps that were successfully downloaded.

### [`apps_ready_for_analysis.csv`](results/apps_ready_for_analysis.csv)

A CSV file listing all apps that were successfully downloaded and, if distributed as split APKs, successfully merged. These are considered ready for analysis.