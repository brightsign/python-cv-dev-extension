# Docker Volumes Migration Guide

## Overview

This guide explains the migration from bind mounts to Docker volumes for the BrightSign Python CV Extension build system. This change resolves persistent file permission issues with Docker Desktop.

## Problem

Docker Desktop's VM-based architecture causes file permission issues with bind mounts, even when matching UID/GID between host and container. This manifests as:
- `sed permission errors` during BitBake builds
- Failed writes to files during compilation
- BitBake's pseudo utility failing silently

## Solution

The solution moves BrightSign OS source management inside Docker containers using named volumes, eliminating bind mount permission issues entirely.

## Key Changes

### 1. Dockerfile Updates
- Added `BRIGHTSIGN_OS_VERSION` build argument
- Created scripts for source management inside container
- Added entrypoint script for automatic setup

### 2. New Scripts
- `scripts/setup-source.sh`: Downloads and extracts BrightSign OS source inside container
- `scripts/setup-patches.sh`: Applies patches from mounted directory
- `scripts/docker-entrypoint.sh`: Automatically runs setup on container start
- `patch-n-build-v2.sh`: New build script using Docker volumes

### 3. Docker Volume Usage
- `bsoe-source`: Named volume for BrightSign OS source (persistent)
- Bind mounts only for:
  - `/bsoe-recipes` (read-only patches)
  - `/srv` (TFTP/NFS exports)

## Migration Steps

### 1. Build New Docker Image
```bash
docker build --rm \
  --build-arg USER_ID=$(id -u) \
  --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 \
  -t bsoe-build .
```

### 2. Use New Build Script
```bash
# Build full SDK
./patch-n-build-v2.sh

# Build specific package
./patch-n-build-v2.sh python3-tqdm

# Clean build
./patch-n-build-v2.sh --clean python3-opencv

# Extract SDK after build
./patch-n-build-v2.sh brightsign-sdk --extract-sdk
```

### 3. Manual Container Access
```bash
docker run -it --rm \
  -v bsoe-source:/home/builder/bsoe \
  -v $(pwd)/bsoe-recipes:/home/builder/patches:ro \
  -v $(pwd)/srv:/srv \
  bsoe-build
```

## Benefits

1. **Eliminates Permission Issues**: Container has full control over files
2. **Consistent Behavior**: Works identically on Docker Desktop and Docker Engine
3. **Better Isolation**: Source code is fully contained in Docker volume
4. **Cleaner Host**: No large source directories on host filesystem
5. **Automatic Setup**: Source downloads automatically on first run

## Cleanup

To remove old bind-mounted source:
```bash
rm -rf brightsign-oe/
rm -f brightsign-*.tar.gz
```

To remove Docker volume (if needed):
```bash
docker volume rm bsoe-source
```

## Troubleshooting

### Source Re-download
The container checks for `.source-${VERSION}` marker file. To force re-download:
```bash
docker run --rm -v bsoe-source:/data busybox rm -rf /data/*
```

### Accessing Build Artifacts
Build artifacts are still accessible via srv directory or can be extracted:
```bash
./patch-n-build-v2.sh brightsign-sdk --extract-sdk
```

### Debugging Inside Container
```bash
docker run -it --rm \
  -v bsoe-source:/home/builder/bsoe \
  -v $(pwd)/bsoe-recipes:/home/builder/patches:ro \
  bsoe-build bash
```

## Backward Compatibility

The original `patch-n-build.sh` script remains available but is deprecated. Users should migrate to `patch-n-build-v2.sh` to avoid permission issues.