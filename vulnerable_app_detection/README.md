# Vulnerable App Detection Pipeline

Pipeline to detect apps that are vulnerable to TapTrap. 

## Folder Structure

- [`code/`](code/) - contains the source code of the pipeline
- [`results/`](results/) - contains the results of the pipeline produced in the paper
- [`report/`](report/) - contains code to summarize the results of the analysis and produce a report
- [`validation/`](validation/) - information on the pipeline validation

## Install and Run

You can run the pipeline locally or via Docker.

### Run with Docker

> [!NOTE]
> **Prerequisites:** 
> - Docker (refer to [this guide](https://www.docker.com/get-started/) to install it)

- Build the Docker image:
```sh
docker build -t taptrap_vulntap code
```
- Run the analysis:
```sh
docker run --rm \
-v <APK_DIR>:/apks \
-v <OUTPUT_DIR>:/output \
taptrap_vulntap /apks /output <PARALELLISM>
```
Replace `<APK_DIR>` with the directory containing the APKs to analyse, `OUTPUT_DIR` with the directory to write the output to, and `<PARALELLISM>` with the amount of concurrently running instances.

> [!WARNING]
>  Note that `<APK_DIR>` and `<OUTPUT_DIR>` have to be **absolute paths**.

The output will contain:
- `OUTPUT_DIR/logs` - log files
- `OUTPUT_DIR/output` - files named `<package_name>.json` for every APK analyzed.

### Run Locally

> [!NOTE]
> We assume the following dependencies to be installed:
> - Python >= 3.10 

- Create a virtual environment and install the dependencies: 
```sh
python3 -m venv .venv && \
source .venv/bin/activate && \
pip install -r code/requirements.txt && \
pip install ./code
```
- Run the tool:
```sh
vulntap-analyze -apk <APK_PATH> -output <OUTPUT_DIR>
```
Replace:
- `APK_PATH` - path to the APK file to analyze
- `OUTPUT_DIR` - directory to save the result in

The output will be a file named `<package-name>.json` in `OUTPUT_DIR`. 

## Report

You can run the report generation with Docker or locally.

### Run with Docker

- Build the Docker image:
```sh
docker build -t taptrap_vulntap_report report
```
- Run the report generation:
```sh
docker run --rm \
-v <RESULT_DIR>:/result \
-v <REPORT_DIR>:/report \
taptrap_vulntap_report --result_dir /result --report_dir /report
```
Replace `<RESULT_DIR>` with the directory containing the JSON results of the analysis and `<REPORT_DIR>` with the directory to write the report to.
The result directory will contain `report.tex`, LaTeX macros with summary results used in the paper, and `vulnerability.json`, JSON file mapping vulnerable apps to their respective vulnerable activities (keys = package names, values = activities).

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
Replace `<RESULT_DIR>` with the directory containing the JSON results of the analysis and `<REPORT_DIR>` with the directory to write the report to.
The result directory will contain `report.tex`, LaTeX macros with summary results used in the paper, and `vulnerability.json`, JSON file mapping vulnerable apps to their respective vulnerable activities (keys = package names, values = activities).

## Troubleshooting

#### I get `sh: 1: cannot open /dev/tty: No such device or address` when I run the tool using Docker

You can safely ignore this message