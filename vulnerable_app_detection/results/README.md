# Vulnerable App Detection Pipeline - Results

This directory contains the results of the vulnerable app detection pipeline as used in the paper.

## Folder Structure

- [`2025-05-20/output`](2025-05-20/output/) - JSON files containing the raw analysis results
- [`2025-05-20/logs/joblog.log`](2025-05-20/logs/joblog.log) - Log file tracking succeeded and failed app analyses
- [`reports/report.tex`](reports/report.tex) - LaTeX macros with summary results used in the paper
- [`reports/vulnerability.json`](reports/vulnerability.json) - JSON file mapping vulnerable apps to their respective vulnerable activities (keys = package names, values = activities)