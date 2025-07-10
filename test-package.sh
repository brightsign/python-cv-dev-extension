#!/bin/bash

# Test individual package build
if [ $# -ne 1 ]; then
    echo "Usage: $0 <package-name>"
    echo "Example: $0 python3-tqdm"
    exit 1
fi

PACKAGE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Sync changes
rsync -a "${SCRIPT_DIR}/bsoe-recipes/" "${SCRIPT_DIR}/brightsign-oe/"

# Clean previous build artifacts for this package
echo "Cleaning previous build for $PACKAGE..."
docker run --rm \
    -v "${SCRIPT_DIR}/brightsign-oe:/home/builder/bsoe" \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c "MACHINE=cobra bitbake -c cleanall $PACKAGE 2>/dev/null || true"

# Build the package
echo "Building $PACKAGE..."
docker run --rm \
    -v "${SCRIPT_DIR}/brightsign-oe:/home/builder/bsoe" \
    -v "${SCRIPT_DIR}/srv:/srv" \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c "MACHINE=cobra ./bsbb $PACKAGE"

if [ $? -eq 0 ]; then
    echo "✅ $PACKAGE built successfully"
else
    echo "❌ $PACKAGE build failed"
    exit 1
fi