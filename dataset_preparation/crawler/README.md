# Google Play Crawler

Explores the Play Store by querying related apps from a given set of apps.

## Folder Structure

- [`crawl.js`](crawl.js) - Main script to crawl the Play Store
- [`db.js`](db.js) - Utility script for database operations
- [`logger.js`](logger.js) - Logger configuration
- [`package.json`](package.json) - Project metadata and dependencies
- [`package-lock.json`](package-lock.json) - Locked dependency versions
- [`Dockerfile`](Dockerfile) - Build file for running the crawler in a container
- [`.dockerignore`](.dockerignore) - Specifies files to exclude from Docker build context

## Install and Run

You can run the crawler locally or via Docker.

### Run with Docker

> [!NOTE]
> **Prerequisites:** 
> - Docker (refer to [this guide](https://www.docker.com/get-started/) to install it)

1. Build the Docker image:
```sh
docker build -t taptrap_crawler .
```

1. Run the container:
```sh
docker run --rm \
  -v OUT_DIR:/app/out \
  taptrap_crawler --output /app/out [options]
```
  - Replace `OUT_DIR` with the desired output directory for the database and resulting CSV.
  - Replace `[options]` with the desired options, e.g., `--onlyFree`

> [!WARNING]
>  `OUT_DIR` must be an **absolute path**.

### Run Locally

> [!NOTE]
> **Prerequisites**:
> - NodeJS >= 22.15.0 (refer to [this guide](https://nodejs.org/en/download) to install it)

1. Install the dependencies:
```sh
npm install .
```
2. Run the crawler:
```sh
npm run crawl -- --output OUT_DIR [options]
```
  - Replace `OUT_DIR` with the desired output directory for the database and resulting CSV. The directory must already exist.
  - Replace `[options]` with the desired options, e.g., `--onlyFree`