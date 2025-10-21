#!/bin/bash
# CV Environment Validation Script
# This script runs Python CV package tests and logs results
# Place in /storage/sd/python-init/ to run automatically at startup
#
# NOTE: This is an informational validation - it reports what packages
# are available but does NOT fail if optional packages are missing

LOG_FILE="/storage/sd/python-init/cv_test.log"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_SCRIPT="${SCRIPT_DIR}/test_cv_packages.py"

echo "Starting CV environment validation..." | tee "${LOG_FILE}"
echo "Timestamp: $(date)" | tee -a "${LOG_FILE}"
echo "----------------------------------------" | tee -a "${LOG_FILE}"

# Check if Python test script exists
if [ ! -f "${TEST_SCRIPT}" ]; then
    echo "ERROR: Test script not found: ${TEST_SCRIPT}" | tee -a "${LOG_FILE}"
    echo "Make sure test_cv_packages.py is in the same directory" | tee -a "${LOG_FILE}"
    exit 1
fi

# Run the Python CV tests
echo "Running CV package tests..." | tee -a "${LOG_FILE}"
if python3 "${TEST_SCRIPT}" >> "${LOG_FILE}" 2>&1; then
    echo "✓ CV environment validation completed" | tee -a "${LOG_FILE}"
    echo "See ${LOG_FILE} for package availability report" | tee -a "${LOG_FILE}"
    exit 0
else
    echo "✗ CV validation script error - check ${LOG_FILE} for details" | tee -a "${LOG_FILE}"
    exit 1
fi