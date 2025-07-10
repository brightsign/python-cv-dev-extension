#!/bin/bash

# BrightSign OE Patch and Build Script
# Consolidates patching and building functionality with clean options

set -e

# get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# directory of target yocto tree
YOCTO_DIR="${SCRIPT_DIR}/brightsign-oe"

# directory of patch files
PATCH_DIR="${SCRIPT_DIR}/bsoe-recipes"

# Default values
TARGET=""
CLEAN=false
QUIET=false
VERBOSE=false

usage() {
    echo "Usage: $0 [OPTIONS] [TARGET]"
    echo "Patch BrightSign OE with custom recipes and build target"
    echo ""
    echo "Arguments:"
    echo "  TARGET              Package or recipe to build (default: brightsign-sdk)"
    echo "                      Examples: python3-tqdm, python3-opencv, brightsign-sdk"
    echo ""
    echo "Options:"
    echo "  -c, --clean         Clean build artifacts before building (bitbake -c cleanall)"
    echo "  -q, --quiet         Use quieter build output"
    echo "  -v, --verbose       Enable verbose output"
    echo "  -h, --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Build full SDK"
    echo "  $0 python3-tqdm            # Build specific package"
    echo "  $0 python3-tqdm --clean    # Clean then build specific package"
    echo "  $0 --clean brightsign-sdk  # Clean then build full SDK"
    echo ""
    echo "Note: Builds require Docker and may take 30+ minutes for full SDK builds"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -q|--quiet)
            QUIET=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
        *)
            if [ -z "$TARGET" ]; then
                TARGET="$1"
            else
                echo "Multiple targets specified. Only one target allowed."
                usage
                exit 1
            fi
            shift
            ;;
    esac
done

# Set default target if not provided
if [ -z "$TARGET" ]; then
    TARGET="brightsign-sdk"
fi

# Verbose output
if [ "$VERBOSE" = true ]; then
    echo "BrightSign OE Patch and Build"
    echo "=============================="
    echo "Script directory: $SCRIPT_DIR"
    echo "Yocto directory: $YOCTO_DIR"
    echo "Patch directory: $PATCH_DIR"
    echo "Target: $TARGET"
    echo "Clean build: $CLEAN"
    echo "Quiet mode: $QUIET"
    echo ""
fi

# Validate directories exist
if [ ! -d "$YOCTO_DIR" ]; then
    echo "Error: BrightSign OE directory not found: $YOCTO_DIR"
    echo "Please ensure you have downloaded and extracted the BrightSign OE source."
    exit 1
fi

if [ ! -d "$PATCH_DIR" ]; then
    echo "Error: Patch directory not found: $PATCH_DIR"
    exit 1
fi

# Check if Docker image exists
if ! docker image inspect bsoe-build >/dev/null 2>&1; then
    echo "Error: Docker image 'bsoe-build' not found."
    echo "Please build the Docker image first:"
    echo "  docker build --rm --build-arg USER_ID=\$(id -u) --build-arg GROUP_ID=\$(id -g) -t bsoe-build ."
    exit 1
fi

# Apply patches
echo "Applying patches from $PATCH_DIR to $YOCTO_DIR..."
rsync -av "${PATCH_DIR}/" "${YOCTO_DIR}/"

# Build quiet flag
QUIET_FLAG=""
if [ "$QUIET" = true ]; then
    QUIET_FLAG="-q"
fi

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo "Cleaning previous build artifacts for $TARGET..."
    docker run --rm \
        -v $(pwd)/brightsign-oe:/home/builder/bsoe \
        -v $(pwd)/srv:/srv \
        -w /home/builder/bsoe/build \
        bsoe-build \
        bash -c "MACHINE=cobra bitbake -c cleanall $TARGET 2>/dev/null || true"
fi

# Build the target
echo "Building $TARGET..."
if [ "$TARGET" = "brightsign-sdk" ]; then
    echo "Warning: Full SDK build may take 30+ minutes depending on your system"
fi

docker run --rm \
    -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c "MACHINE=cobra ./bsbb $QUIET_FLAG ${TARGET}"

if [ $? -eq 0 ]; then
    echo "✅ $TARGET built successfully"
else
    echo "❌ $TARGET build failed"
    exit 1
fi
    