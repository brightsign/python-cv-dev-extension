#!/bin/bash
# Basic User Initialization Script Template
# File: 02_my_custom_script.sh
#
# This template provides a starting point for creating custom initialization scripts.
# Place in /storage/sd/python-init/ and make executable with: chmod +x script.sh

echo "Starting custom initialization..."

# Your custom initialization code here
# Examples:
# - Set up directories
# - Download models or data
# - Configure environment variables
# - Run validation checks

# Example: Create a models directory
# mkdir -p /storage/sd/models

# Example: Download a file
# wget -O /storage/sd/models/model.rknn https://example.com/model.rknn

# Example: Run a Python command
# python3 -c "import torch; print('PyTorch version:', torch.__version__)"

# Example: Log information
# echo "Custom initialization completed at $(date)" >> /storage/sd/python-init/init.log

echo "Custom initialization completed successfully"