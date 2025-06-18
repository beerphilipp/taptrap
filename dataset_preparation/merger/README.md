# APK Merger

Apps downloaded from Google Play are often distributed as **split APKs**, meaning a single app package consists of multiple APK files rather than one. These must be merged into a single installable APK before analysis.

This tool provides a wrapper script around [REAndroid/APKEditor](https://github.com/REAndroid/APKEditor), which is included as a prebuilt `.jar` binary.

The included `APKEditor` binary is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).  
The original license text is available in [APKEditor-LICENSE](APKEditor-LICENSE).

## File Structure

- [`merge.py`](merge.py) - Main Python script to merge APKs
- [`requirements.txt`](requirements.txt) - Python requirements
- [`APKEditor-1.4.1.jar`](APKEditor-1.4.1.jar) - Compiled JAR of [REAndroid/APKEditor](https://github.com/REAndroid/APKEditor), responsible for merging split APKs
- [`Dockerfile`](Dockerfile) - Build file for running the merger in a container
- [`entrypoint.sh`](entrypoint.sh) - Docker entrypoint script
- [`APKEditor-LICENSE`](APKEditor-LICENSE) - License text of APKEditor

## Install and Run

You can run the merger locally or via Docker.

### Run with Docker (large-scale)

> [!NOTE]
> **Prerequisites:** 
> - Docker (refer to [this guide](https://www.docker.com/get-started/) to install it)

- Build the Docker image:
```sh
docker build -t taptrap_merger .
```

- Run the container:
```sh
docker run --rm \
  -v <APK_DIR>:/input/apks \
  -v <OUT_DIR>:/output/results \
  -v <APK_LIST>:/input/apk_list.txt \
  taptrap_merger \
  /input/apks /output/results /input/apk_list.txt
```
  - Replace:
    - `<APK_DIR>` with the apk directory. Single apks are assumed to be stored as `<package_name>.apk` in `<APK_DIR>`. Multiple apks are assumed to be stored in a folder named `<PACKAGE_NAME>` in `<APK_DIR>`.
    - `<OUT_DIR>` with the output directory where the *merged* APKs are stored. Single APKs are ommitted
    - `<APK_LIST>` with the file containing all package names to attempt to merge (one package name per line)

> [!WARNING]
>  `<APK_DIR>`, `<OUT_DIR>`, and `<API_LIST>` must be **absolute paths**.

### Run Locally (single APK)

> [!NOTE]
> **Prerequisites**:
> - Python >= 3.10 (refer to [this guide](https://www.python.org/downloads/) to install it)
> - Java >= 11 (refer to [this guide](https://openjdk.org/) to install it)

- Create a virtual environment and install the dependencies: 
```sh
python3 -m venv .venv && \
source .venv/bin/activate && \
pip install -r requirements.txt
```
- Run the tool:
```sh
python merge.py merge <APK_DIR> <PACKAGE_NAME> <OUT_DIR>
```
- Replace:
  - `<APK_DIR>` with the apk directory. Single apks are assumed to be stored as `<package_name>.apk` in `<APK_DIR>`. Multiple apks are assumed to be stored in a folder named `<PACKAGE_NAME>` in `<APK_DIR>`.
  - `<PACKAGE_NAME>` with the package name of the app to merge
  - `<OUT_DIR>` with the output directory where the *merged* APKs are stored. Single APKs are ommitted

## Troubleshooting

#### I get `sh: 1: cannot open /dev/tty: No such device or address` when I run the tool using Docker
You can safely ignore this message