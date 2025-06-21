# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a BrightSign Model Package (BSMP) that implements YOLO object detection using the Rockchip NPU on BrightSign players. The project creates a BrightSign Extension that:
- Loads YOLO models on the RK3588/RK3576/RK3568 NPU
- Captures video from USB cameras via OpenCV
- Performs real-time object detection 
- Outputs decorated images to `/tmp/output.jpg` and results to `/tmp/results.json`

## Build Commands

### Prerequisites
- Must be built on x86_64 architecture (not ARM/Apple Silicon)
- Requires BrightSign SDK installed to `./sdk/`
- Requires Docker for model compilation

### Model Compilation (one-time setup)
```bash
# Build RKNN toolkit Docker image
cd toolkit/rknn-toolkit2/rknn-toolkit2/docker/docker_file/ubuntu_20_04_cp38
docker build --rm -t rknn_tk2 -f Dockerfile_ubuntu_20_04_for_cp38 .

# Compile YOLO model for RK3588 (XT-5 players)
cd toolkit/rknn_model_zoo/
docker run -it --rm -v $(pwd):/zoo rknn_tk2 /bin/bash \
    -c "cd /zoo/examples/yolov8/python && python convert.py ../model/yolov8n.onnx rk3588 i8 ../model/RK3588/yolov8n.rknn"
```

### Application Build
```bash
# Setup environment (must be sourced in each shell)
source ./sdk/environment-setup-aarch64-oe-linux

# Build for XT-5 (RK3588)
mkdir -p build_xt5 && cd build_xt5
cmake .. -DOECORE_TARGET_SYSROOT="${OECORE_TARGET_SYSROOT}" -DTARGET_SOC="rk3588"
make && make install

# Build for LS-5 (RK3568)  
mkdir -p build_ls5 && cd build_ls5
cmake .. -DOECORE_TARGET_SYSROOT="${OECORE_TARGET_SYSROOT}" -DTARGET_SOC="rk3568"
make && make install

# Build for Firebird (RK3576)
mkdir -p build_firebird && cd build_firebird
cmake .. -DOECORE_TARGET_SYSROOT="${OECORE_TARGET_SYSROOT}" -DTARGET_SOC="rk3576"
make && make install
```

### Development Testing
```bash
# Test without packaging
cd install
zip -r ../yolo-dev-$(date +%s).zip ./
# Transfer to player and run: ./bsext_init run
```

### Extension Packaging
```bash
# Create BrightSign Extension
cd install
../sh/make-extension-lvm
zip ../yolo-extension-$(date +%s).zip ext_npu_*
```

## Architecture

### Core Components
- **main.cpp**: Entry point, handles video capture via OpenCV
- **inference.cpp**: RKNN model loading and inference management  
- **yolo.cc**: YOLO-specific post-processing and object detection logic
- **postprocess.cc**: General post-processing utilities
- **publisher.cpp**: Outputs results to JSON and decorated images
- **image_utils.c**: Image format conversion and preprocessing
- **bsext_init**: Extension initialization script with SOC detection

### Key Libraries
- **RKNN Runtime** (`librknnrt.so`): Rockchip NPU inference engine
- **RGA** (`librga.so`): Rockchip Graphics Accelerator for image processing
- **OpenCV**: Video capture and image manipulation
- **TurboJPEG**: Fast JPEG encoding for output images

### Platform Support
- **RK3588** (XT-5): Uses `/dev/video1` by default
- **RK3568** (LS-5): Uses `/dev/video0` by default  
- **RK3576** (Firebird): Uses `/dev/video0` by default

### Configuration
Extension behavior controlled via BrightSign registry keys in `extension` section:
- `bsext-yolo-auto-start`: Set to `false` to disable auto-start
- `bsext-yolo-video-device`: Override video device path (e.g., `/dev/video1`)

## Development Workflow

### Local Testing (Orange Pi)
Optional native development on Orange Pi with same SOC:
```bash
mkdir -p build && cd build
cmake .. -DTARGET_SOC="rk3588"
make && make install
```

### Debugging
```bash
# Library debugging
./check_opencv_libs.sh

# Runtime debugging  
./run_with_debug.sh model/yolov8n.rknn /dev/video0

# GDB debugging
./debug_with_gdb.sh
```

### Cross-compilation Notes
- CMake automatically detects cross-compilation vs native build
- Cross-builds use SDK sysroot libraries
- Native builds use system package manager libraries
- RGA is disabled for XT-5 via `-DDISABLE_RGA` due to compatibility issues

## File Structure
```
src/           - C++ source files
include/       - Headers and third-party libraries  
toolkit/       - Rockchip model compilation tools (cloned)
install/       - Build output and extension files
sdk/           - BrightSign cross-compilation toolchain
bsext_init     - Extension service script
sh/            - Build and packaging scripts
```

## Important Notes
- Model compilation requires x86_64 host architecture
- Different video devices used per SOC platform
- Extension runs as system service on player boot
- Debug builds include symbols for GDB debugging
- Library dependencies are automatically copied to install directory