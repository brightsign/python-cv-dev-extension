#!/bin/bash

# Patch local.conf for BrightSign build
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOCAL_CONF="${PROJECT_ROOT}/brightsign-oe/build/conf/local.conf"

AUTO_CONFIRM=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -y|--yes)
            AUTO_CONFIRM=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [-y|--yes]"
            echo "Patch local.conf for BrightSign build"
            echo ""
            echo "Options:"
            echo "  -y, --yes    Auto-confirm all prompts"
            echo "  -h, --help   Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if local.conf exists
if [ ! -f "$LOCAL_CONF" ]; then
    echo "Error: local.conf not found at $LOCAL_CONF"
    echo "Please ensure BrightSign OE is properly extracted."
    exit 1
fi

echo "Patching local.conf for Python CV Extension build..."

# Backup original
if [ ! -f "${LOCAL_CONF}.orig" ]; then
    cp "$LOCAL_CONF" "${LOCAL_CONF}.orig"
    echo "Created backup: ${LOCAL_CONF}.orig"
fi

# Check if already patched
if grep -q "# Python CV Extension patches applied" "$LOCAL_CONF"; then
    echo "local.conf already patched. Skipping."
    exit 0
fi

# Apply patches
{
    echo ""
    echo "# Python CV Extension patches applied"
    echo "# Added on $(date)"
    echo ""
    echo "# Enable Python 3.8"
    echo "PACKAGECONFIG:append:pn-python3 = \" sqlite3\""
    echo ""
    echo "# Add Python packages to SDK"
    echo "TOOLCHAIN_HOST_TASK:append = \" nativesdk-python3-numpy\""
    echo "TOOLCHAIN_TARGET_TASK:append = \" python3-dev python3-setuptools\""
    echo ""
    echo "# Increase parallel build tasks"
    echo "BB_NUMBER_THREADS ?= \"${BB_NUMBER_THREADS:-8}\""
    echo "PARALLEL_MAKE ?= \"-j ${PARALLEL_MAKE:-8}\""
    echo ""
    echo "# Enable shared state cache"
    echo "SSTATE_DIR ?= \"\${TOPDIR}/../../sstate-cache\""
    echo ""
} >> "$LOCAL_CONF"

echo "✅ local.conf patched successfully"

# Show what was added
if [ "$AUTO_CONFIRM" != true ]; then
    echo ""
    echo "The following was added to local.conf:"
    echo "----------------------------------------"
    tail -n 20 "$LOCAL_CONF" | grep -A 20 "# Python CV Extension patches applied" || true
    echo "----------------------------------------"
fi