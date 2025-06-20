# Malicious App Detection Pipeline

Pipeline to extract and analyze Android app animations for potentially malicious behavior.

## Folder Structure

- [`code/`](code/) - contains the source code of the MalTapExtract tool to extract animations and the MalTapAnalyze tool to analyze animations
- [`results/`](results/) - contains the results of the pipeline produced in the paper
- [`report/`](report/) - contains code to summarize the results of the analysis and produce a report

## Pipeline Components

This pipeline consists of the following components: 
1. `pull_framework.sh` - Pulls the `framework-res.apk` file from a connected Android device (`code/pull_framework.sh`)
2. **MalTapExtract** - extracts animations and interpolators from APKs (`code/MalTapExtract`)
3. **MalTapAnalyze** - analyzes the extracted animations for potentially malicious behavior (`code/MalTapAnalyze`)


### `pull_framework.sh`

Android apps may reference resources that are defined by the system. Therefore, it is first necessary to retrieve the APK that includes these resources, i.e., the `framework-res.apk` file. 

### MalTapExtract

**MalTapExtract** is a Java-based tool for extracting animation-related resources from Android applications. It processes both regular APKs and the Android system’s `framework-res.apk` to resolve resource references correctly.

The tool operates in two stages. First, extract interpolators and value resources from `framework-res.apk`. This step is necessary because app resources may reference system-defined Android resources.
Once the framework resources are extracted, analyze individual APKs using:

The output of the tool is an SQLite database with two tables:

- `anim` - stores the extracted animations. These animations can include references to interpolators (preceeded with `@@`)
- `interpolator` - stores the interpolators that were extracted

The cache directory is only used between runs, but can be safely deleted once all apps are analyzed.

### MalTapAnalyze


**MalTapAnalyze** analyzes the animations retrieved from **MalTapExtract**. It is an Android-based tool and requires the Android SDK. 

The output of the tool is an SQLite database with three tables

- `anim` - animations already extracted by MalTapExtract
- `interpolator` - interpolators already extracted by MalTapExtract
- `score` - stores the scores for potentially malicious animations

## Install and Run

You can run the pipeline locally or via Docker.

### Retrieve the `framework-res.apk` (optional)

> [!NOTE]
> **Prerequisites:** 
> - ADB (if you want to retrieve one yourself, refer to [this guide](https://developer.android.com/tools/adb) to install it)

Android apps may reference resources that are defined by the system. Therefore, it is first necessary to retrieve the APK that includes these resources, i.e., the `framework-res.apk` file on the device.
We provide the `framework-res.apk` of a Pixel 6a device running Android 15 in `results/2025-01-09/android_framework/framework-res.apk`. 

To retrieve the file from arbitrary devices, perform the following steps:

- Connect a device and enable USB debugging (*Settings* > *About phone* > Tap *Build number* 7x, then *Developer Options* > *Enable USB Debugging*). USB Debugging does not need to be enabled for emulators.
- Pull the framework APK:
```sh
code/pull_framework.sh OUTPUT_DIR [SERIAL]
```
- Replace:
  - `OUT_DIR` with the output directory to save the result to
  - `SERIAL` with the serial number of the device in case multiple devices are connected. You can find the serial number using `adb devices`

`OUT_DIR` will contain the file `device_info.md` listing information about the device and the `framework-res.apk`.

### Run with Docker

> [!NOTE]
> **Prerequisites:** 
> - Docker (refer to [this guide](https://www.docker.com/get-started/) to install it)

> [!WARNING]
> We recommend using an AMD64 (x86_64) device. The Android SDK is not available for ARM64 Linux, and while macOS provides ARM64 binaries, it does not support Docker containers with ARM64 Android SDK tooling. As a result, running the pipeline on ARM64-based Linux machines requires emulating an AMD64 environment using `--platform=linux/amd64`, which is significantly slower and not recommended.

1. Build the MalTapExtract Docker image:
```sh
docker build -t taptrap_maltapextract code/MalTapExtract
```
2. Run MalTapExtract to extract the animations:
```sh
docker run --rm \
      -v <OUTPUT_DIR>:/output \
      -v <APK_DIR>:/apks \
      taptrap_maltapextract \
      /output /apks <PARALLELISM>
```
  - Replace the following placeholders:
    - `<OUTPUT_DIR>` - The output directory you want the results to be stored in. This *must* contain the `framework-res.apk` in `<OUTPUT_DIR>/android_framework/framework-res.apk`
    - `<APK_DIR>` - The directory of the APK files you want to analyze
    - `<PARALLELISM>` - How many apps at a time should be analyzed

   - The database will be stored in `<OUTPUT_DIR>/animations.db`

3. Build MalTapAnalyze Docker image:

```sh
docker build -t taptrap_maltapanalyze code/MalTapAnalyze
```

4. Run MalTapAnalyze:
```sh
docker run --rm \
      -v <DATABASE>:/animations.db \
      taptrap_maltapanalyze \
      /animations.db
```
   - Replace the following placeholder:
     - `DATABASE` with the path to the database retrieved in the last step.

`DATABASE` will contain a new table `score` with the scores of the animations.ls


## Run locally

> [!NOTE]
> **Prerequisites:** 
> - Java 11
> - The Android SDK is installed and `ANDROID_HOME` points to the SDK location

1. Build MalTapExtract
```sh
code/MalTapExtract/gradlew --project-dir code/MalTapExtract clean build
```
2. Run MalTapExtract on the `framework-res.apk`
```sh
java -jar code/MalTapExtract/build/libs/MalTapExtract-1.0-SNAPSHOT.jar \
-apk <FRAMEWORK_APK> -framework -cache <CACHE_DIR> -database <DB_PATH>
```
  - Replace `<FRAMEWORK_APK>` with the `framework-res.apk`, `<CACHE_DIR>` with a temporary cache directory, and `<DB_PATH>` with the path to the database where the results should be stored in.
3. Run MalTapExtract on the target app
```sh
java -jar code/MalTapExtract/build/libs/MalTapExtract-1.0-SNAPSHOT.jar \
-apk <APK> -cache <CACHE_DIR> -database <DB_PATH>
```
  - Replace `<APK>` with the APK to analyze, `<CACHE_DIR>` with the cache directory specified in step 2, and `<DB_PATH>` with the path to the database specified in step 2.
4. Run MalTapAnalyze
```sh
code/MalTapAnalyze/gradlew \
  --project-dir code/MalTapAnalyze \
  testRelease \
  --no-build-cache \
  --rerun-tasks \
  --tests "*.run" \
  -Ddatabase=<DB_PATH> \
  --warning-mode all
```
  - Replace `<DB_PATH>` with the **absolute** path to database specified in step 2.

## Report

You can run the report generation with Docker or locally.

### Run with Docker

- Build the Docker image:
```sh
docker build -t taptrap_maltap_report report
```
- Run the report generation:
```sh
docker run --rm \
-v <RESULT_DIR>:/result \
-v <REPORT_DIR>:/report \
taptrap_maltap_report --result_dir /result --report_dir /report
```
Replace `<RESULT_DIR>` with the result directory of the analysis and `<REPORT_DIR>` with the directory to write the report to. This directory must already exist.
The result directory will contain `report.tex`, LaTeX macros with summary results used in the paper.

> [!WARNING]
>  Note that `<RESULT_DIR>` and `<REPORT_DIR>` must be **absolute paths**.

### Run Locally

> [!NOTE]
> We assume the following dependencies to be installed:
> - Python >= 3.10 

- Create a virtual environment and install the dependencies: 
```sh
python3 -m venv .venv && \
source .venv/bin/activate && \
pip install -r report/requirements.txt
```

- Run the report generation:
```sh
python report/report.py --result_dir <RESULT_DIR> --report_dir <REPORT_DIR>
```
Replace `<RESULT_DIR>` with the result directory of the analysis and `<REPORT_DIR>` with the directory to write the report to. This directory must already exist.
The result directory will contain `report.tex`, LaTeX macros with summary results used in the paper.

## Troubleshooting

#### `permission denied: code/pull_framework.sh` when pulling the `framework-res.apk`

This error indicates that the script does not have executable permissions. To fix this, run the following command:

```bash
chmod +x ./code/pull_framework.sh
```


#### I get `sh: 1: cannot open /dev/tty: No such device or address` when I run the tool using Docker

You can safely ignore this message