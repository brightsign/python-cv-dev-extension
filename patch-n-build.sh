#!/bin/bash

# get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# directory of target yocto tree
YOCTO_DIR="${SCRIPT_DIR}/brightsign-oe"

# diretory of patch files
PATCH_DIR="${SCRIPT_DIR}/bsoe-recipes"

rsync -av "${PATCH_DIR}/" "${YOCTO_DIR}/"

# set the default target if not provided
if [ -z "$1" ]; then
    TARGET="brightsign-sdk"
else
    TARGET="$1"
fi

# build with docker - use quieter output
docker run --rm \
    -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c "MACHINE=cobra ./bsbb -q ${TARGET}"
    