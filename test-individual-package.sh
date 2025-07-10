#!/bin/bash

# Test individual BitBake package
# Usage: ./test-individual-package.sh <package-name>

if [ $# -ne 1 ]; then
    echo "Usage: $0 <package-name>"
    echo "Example: $0 python3-filelock"
    exit 1
fi

PACKAGE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YOCTO_DIR="${SCRIPT_DIR}/brightsign-oe"
PATCH_DIR="${SCRIPT_DIR}/bsoe-recipes"

echo "=== Testing package: $PACKAGE ==="

# Sync changes to Yocto directory
echo "Syncing changes..."
rsync -av "${PATCH_DIR}/" "${YOCTO_DIR}/"

# Run BitBake for the specific package
echo "Building $PACKAGE..."
docker run --rm \
    -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
    -w /home/builder/bsoe \
    bsoe-build \
    bash -c "source oe-init-build-env build && MACHINE=cobra bitbake $PACKAGE"

if [ $? -eq 0 ]; then
    echo "✅ $PACKAGE built successfully"
else
    echo "❌ $PACKAGE build failed"
fi