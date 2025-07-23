#!/bin/bash
# Python Script Wrapper Template
# File: run_my_python_script.sh
#
# Since only .sh files are executed by the extension, use this template
# to wrap Python scripts for execution at startup.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="${SCRIPT_DIR}/my_python_script.py"
LOG_FILE="/storage/sd/python-init/my_script.log"

echo "Running Python script wrapper..."
echo "Timestamp: $(date)" > "${LOG_FILE}"

# Check if Python script exists
if [ ! -f "${PYTHON_SCRIPT}" ]; then
    echo "ERROR: Python script not found: ${PYTHON_SCRIPT}" | tee -a "${LOG_FILE}"
    echo "Create your Python script at: ${PYTHON_SCRIPT}" | tee -a "${LOG_FILE}"
    exit 1
fi

# Execute Python script with logging
echo "Executing: ${PYTHON_SCRIPT}" | tee -a "${LOG_FILE}"
if python3 "${PYTHON_SCRIPT}" >> "${LOG_FILE}" 2>&1; then
    echo "✓ Python script completed successfully" | tee -a "${LOG_FILE}"
    exit 0
else
    echo "✗ Python script failed - check ${LOG_FILE} for details" | tee -a "${LOG_FILE}"
    exit 1
fi

# Template Python script (create as my_python_script.py):
#
# #!/usr/bin/env python3
# """
# My custom Python initialization script.
# """
# import logging
# 
# logging.basicConfig(level=logging.INFO)
# logger = logging.getLogger(__name__)
# 
# def main():
#     logger.info("Starting Python initialization...")
#     
#     # Your Python code here
#     
#     logger.info("Python initialization completed")
#     return 0
# 
# if __name__ == "__main__":
#     exit(main())