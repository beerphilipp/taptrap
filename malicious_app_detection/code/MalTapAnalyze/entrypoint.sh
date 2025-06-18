#!/bin/bash
#
# Script: entrypoint.sh
#
# Description:
#   Runs Gradle instrumentation tests for the given project.
#   Assumes that a Gradle task named `testRelease` is configured to run tests,
#   and that the tests accept a system property `-Ddatabase=...` for specifying the database path.
#
# Usage:
#   ./run_tests.sh <project_dir> <db_path>
#
# Arguments:
#   project_dir:  Path to the Android project directory containing the Gradle wrapper.
#   db_path:      Path to the SQLite database used during execution.

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <project_dir> <db_path>"
    exit 1
fi

PROJECT_DIR="$1"
DB_PATH="$2"

cd "$PROJECT_DIR" && gradle testRelease --tests "*.run" -Ddatabase="$DB_PATH" --warning-mode all