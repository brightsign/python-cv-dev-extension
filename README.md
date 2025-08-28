# BrightSign Python Extension for CV Development (ALPHA RELEASE)

## Overview

Python CV development extension for BrightSign digital signage players (embedded ARM64 Linux devices with NPU acceleration).

__Provides__: Python 3.8, OpenCV, PyTorch, ONNX, RKNN toolkit, scientific computing stack and an extensible Python development environment

__Target__: Enterprise edge CV applications (audience analytics, interactive displays, retail analytics)

__Key requirement__: x86_64 development host (Apple Silicon incompatible due to RKNN toolchain)

### Core Concepts

- **Extensions**: System-level add-ons that persist across reboots
- __Cross-compilation__: Build on x86_64 for ARM64 target
- **NPU/RKNN**: Rockchip Neural Processing Unit + optimization toolkit (~10x inference speedup)
- **BrightSign OS**: Read-only Linux optimized for signage reliability

---

## Supported Players

| player | minimum OS Version required |
| --- | --- |
| XT-5: XT1145, XT2145 | [9.1.52](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.52/brightsign-xd5-update-9.1.52.zip) |
| _Firebird_ (in process) | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |
| _LS-5: LS445_ (in process) | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |

**NOTE:** This guide is written **ONLY** for the XT-5. Supporting Firebird or LS-5 is a straightforward exercise for the motivated reader.

## Requirements

**Development Host**:

- x86_64 architecture (Intel/AMD only - Apple Silicon incompatible)
- 16GB+ RAM, 50GB+ free space
- Docker, git, cmake, patchelf (for RKNN library patching)

**Target Device**:

- BrightSign Series 5 (XT-5, Firebird, LS-5)
- Firmware 9.1.52+, unsecured, SSH enabled

**Setup**:

```bash
sudo apt-get update && apt-get install -y docker.io git cmake patchelf
# Alternative: pip3 install patchelf
uname -m  # Verify: x86_64
```

__Compatible Hosts__: Intel/AMD Linux/Windows/Mac, x86_64 cloud instances  
__Incompatible__: Apple Silicon, ARM systems, Raspberry Pi

---

## Quick Start

__Time__: 60-90 min | __Prerequisites__: Docker, git, x86_64 host, BrightSign player

```bash
# Build extension (30-60 min)
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension

```

```bash
./setup && ./build --extract-sdk

# Package (5 min)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package

```

```bash

# Deploy & validate
# Transfer .zip via DWS, install via SSH, then:
export PLAYER_IP=${PLAYER_IP:-192.168.1.100}  # Replace with your player IP
export PASSWORD="password"      # Replace with your player password

cd user-init/tools && ./deploy-to-player.sh "${PLAYER_IP}" "${PASSWORD}"
```

**Scripts:**

- `./setup`: Downloads BrightSign source, builds clean Docker image
- `./build --extract-sdk`: Applies patches, cross-compiles Python + CV packages, extracts SDK to `./sdk/`
- `./brightsign-x86_64-*.sh`: Installs cross-compiler toolchain from extracted SDK
- `./package`: Assembles extension packages from SDK and scripts
- `./deploy-to-player.sh`: Copies CV validation scripts to player for testing

__Output__: `pydev-*.zip`, `ext_pydev-*.zip` packages + SDK in `./sdk/`

**Note**: Patches (recipes in `bsoe-recipes/` and `local.conf` modifications) are applied during the build process, allowing quick recipe adjustments without rebuilding the Docker container.

---

## Configuration

### Registry Settings

The Python Development Extension can be configured using BrightSign's registry system through the Diagnostic Web Server (DWS). Registry settings allow you to customize extension behavior without modifying code.

#### Available Registry Keys

| Registry Key | Description | Values | Default |
|--------------|-------------|---------|---------|
| `bsext-pydev-disable-auto-start` | Disable automatic startup of the extension | `true`, `yes`, `1` (case-insensitive) | `false` (extension starts automatically) |
| `bsext-pydev-enable-user-scripts` | Enable user script execution (security: scripts run as root) | `true`, `yes`, `1` (case-insensitive) | `false` (user scripts blocked) |

#### Setting Registry Values

1. **Access DWS**: Open your browser to `http://<player-ip>:8080`
2. **Navigate to Registry Tab**: Click the "Registry" tab
3. **Enter command**: Type the registry command in the command box and click "Submit"

**Example - Disable auto-start**:

```bash
registry write extension bsext-pydev-disable-auto-start true
```

4. **Restart Required**: Registry changes take effect after player restart

#### Common Use Cases

**Enable User Scripts** (Required for user-init functionality):

```bash
# Enable user script execution (required for requirements.txt and user scripts)
registry write extension bsext-pydev-enable-user-scripts true

# Restart player for changes to take effect
# Extension will now run user scripts from /storage/sd/python-init/
```

**Disable Extension for Debugging**:

```bash
# Disable automatic startup
registry write extension bsext-pydev-disable-auto-start true

# Restart player, then manually control extension
/var/volatile/bsext/ext_pydev/bsext_init run    # Run in foreground
/var/volatile/bsext/ext_pydev/bsext_init start  # Run as service
```

**Temporarily Disable User Scripts** (Security):

```bash
# Disable user script execution for security
registry write extension bsext-pydev-enable-user-scripts false
# OR remove the key entirely
registry delete extension bsext-pydev-enable-user-scripts

# Re-enable when needed
registry write extension bsext-pydev-enable-user-scripts true
```

**Temporarily Disable Extension**:

```bash
# Disable for maintenance or troubleshooting
registry write extension bsext-pydev-disable-auto-start yes

# Re-enable by removing the registry key
registry delete extension bsext-pydev-disable-auto-start
```

#### Removing Registry Overrides

To restore default behavior, delete the registry key:

```bash
# Remove disable flag (restores auto-start)
registry delete extension bsext-pydev-disable-auto-start

# Remove user scripts flag (disables user scripts - default security behavior)
registry delete extension bsext-pydev-enable-user-scripts
```

**Note**: Removing a registry key restores the default behavior. This is particularly useful when troubleshooting or when you want to revert configuration changes.

#### Viewing Current Registry Settings

You can view all extension registry settings in the DWS Registry tab, or check specific values:

```bash
# View all extension settings
registry read extension

# Check specific setting
registry read extension bsext-pydev-disable-auto-start
```

---

## Extension Script System

⚠️ **Security Notice**: User scripts are disabled by default as they execute with root privileges. You must explicitly enable them via registry:

```bash
registry write extension bsext-pydev-enable-user-scripts true
```

**Restart the player** after setting this registry key for changes to take effect.

### Automatic Package Installation with requirements.txt

🚀 **Key Feature**: The extension automatically installs Python packages at startup using a standard `requirements.txt` file.
**How it works**:

- Place a `requirements.txt` file in `/storage/sd/python-init/`
- On extension startup, packages are automatically installed via pip3
- Installation happens **before** any user scripts run
- Results logged to `/storage/sd/python-init/requirements-install.log`

**Deployment**:

```sh
export PLAYER_IP=192.168.1.100  # Replace with your player IP

# Copy requirements.txt to player
scp requirements.txt brightsign@"${PLAYER_IP}":/storage/sd/python-init/

# packages will automatically install on next boot

# Check installation log
scp brightsign@"${PLAYER_IP}":/storage/sd/python-init/requirements-install.log .
```

**Benefits**:

- **Standard format**: Uses pip's standard requirements.txt syntax
- **Automatic execution**: Runs on every boot/restart
- **Build-time alternative**: Faster than build-time inclusion for changing dependencies
- **Development friendly**: Easy to update and test different package versions

**Important Limitation**: Only PyPI packages with pre-compiled wheels for ARM64/aarch64 architecture will install successfully. The BrightSign player has no build system (no cmake, gcc, etc.), so packages requiring compilation will fail to install.

**Important considerations**:

- Installation time depends on package size and network speed
- For production deployments, consider adding packages to the SDK build instead
- Packages install to `/usr/local/lib/python3.8/site-packages` (volatile storage)

### User Scripts Directory

**Location**: `/storage/sd/python-init/`

- Only `.sh` shell scripts are supported (due to `/storage/sd` noexec mount restrictions)
- Scripts must have executable bit set to run (allows toggling scripts on/off)
- Scripts execute in alphabetical order (use prefixes like `01_`, `02_` to control order)
- Scripts run with root privileges and are executed using `bash`

### Deploying Scripts

**Prerequisites**: User scripts must be enabled via registry first:

```bash
# Enable user script execution (required)
registry write extension bsext-pydev-enable-user-scripts true
# Restart player for changes to take effect
```

**Quick Deployment** (recommended):

```bash
export PLAYER_IP=192.168.1.100  # Replace with your player IP
export PASSWORD="password"  # Replace with your player password

# From your development host
cd user-init/tools/
./deploy-to-player.sh "${PLAYER_IP}" "${PASSWORD}"

# This deploys:
# - requirements.txt (automatically installs Python packages at startup)
# - 01_validate_cv.sh (CV package validation script)
# - test_cv_packages.py (Python CV test script)
```

**Manual Deployment**:

```bash
# First, enable user scripts via DWS registry
# registry write extension bsext-pydev-enable-user-scripts true

# Copy example files to player
scp user-init/examples/* admin@<player-ip>:/storage/sd/python-init/

# Make shell scripts executable (required to run)
ssh admin@<player-ip> "chmod +x /storage/sd/python-init/*.sh"

# Restart extension to install packages and run scripts
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init restart"
```

### Advanced Package Installation

While the automatic `requirements.txt` installation covers most use cases, you can also create custom installation scripts for more complex scenarios:

**Use cases for custom scripts**:

- Conditional package installation based on hardware detection
- Installing from private repositories or custom URLs
- Post-installation configuration or setup steps
- Installing non-Python dependencies

__Example custom installation script__ (`01_custom_packages.sh`):

```bash
#!/bin/bash
# Custom package installation with hardware detection

# Install different packages based on SOC type
if grep -q "rk3588" /sys/firmware/devicetree/base/compatible; then
    pip3 install --break-system-packages torch==1.13.0+cpu
else
    pip3 install --break-system-packages torch==1.12.0+cpu
fi

# Install from private repository
pip3 install --break-system-packages \
    --index-url https://private.pypi.com/simple \
    my-private-package
```

### Creating Custom Scripts

The `user-init/templates/` directory provides starting points for common scenarios:

__1. Basic Script Template__ (`templates/basic_script.sh`):

- Simple initialization script template
- Includes error handling and logging examples
- Ready to customize for your needs

__2. Python Wrapper Template__ (`templates/python_wrapper.sh`):

- Template for running Python scripts (since only .sh files execute)
- Includes logging and error checking
- Shows how to call Python scripts from shell

__3. Requirements Template__ (`templates/requirements_template.txt`):

- Comprehensive requirements.txt template
- Organized by package category
- Includes comments and version examples

**Usage**:

```bash
# Start with a template
cp user-init/templates/basic_script.sh my_custom_script.sh

# Customize for your needs
# Edit the script to add your initialization logic

# Deploy and test
scp my_custom_script.sh admin@<player-ip>:/storage/sd/python-init/
ssh admin@<player-ip> "chmod +x /storage/sd/python-init/my_custom_script.sh"
```

**Best Practices**:

- Use templates as starting points for consistency
- Use numeric prefixes (01_, 02_, etc.) to control execution order
- Use `chmod +x/-x` to enable/disable scripts
- Scripts run after automatic requirements.txt installation

### Service Control

**Manual Control**:

```bash
# Start extension (happens automatically at boot)
/var/volatile/bsext/ext_pydev/bsext_init start

# Stop extension
/var/volatile/bsext/ext_pydev/bsext_init stop

# Restart extension (re-runs user scripts)
/var/volatile/bsext/ext_pydev/bsext_init restart

# Run extension in foreground (for debugging)
/var/volatile/bsext/ext_pydev/bsext_init run

# Check status
/var/volatile/bsext/ext_pydev/bsext_init status
```

**Configuration Control**:
Use registry settings to control extension behavior. See [Configuration](#configuration) for details:

- Enable/disable user script execution (security)
- Disable auto-start for debugging
- Temporarily disable extension
- View current settings

### Debugging

**Log locations**:

- Extension log: `/var/log/bsext-pydev.log`
- Requirements installation: `/storage/sd/python-init/requirements-install.log`
- CV test results: `/storage/sd/cv_test.log`

**Common issues**:

- **Scripts not running**: First check if user scripts are enabled (`registry write extension bsext-pydev-enable-user-scripts true`)
- **User scripts disabled**: Check extension status - user scripts are disabled by default for security
- **Scripts being skipped**: Scripts without executable bit are intentionally disabled
- **Import errors**: Verify packages installed correctly
- **Permission denied**: Scripts run as root, ensure proper file permissions

**Disable auto-start** (for troubleshooting):
See the [Configuration](#configuration) section for registry settings to disable auto-start and other debugging options.

---

# Complete Guide

## Architecture

```ini
Host (x86_64)                     Player (ARM64)
├── Docker Build                  ├── BrightSign OS (RO)
├── RKNN Compilation              ├── Extension (/var/volatile/bsext/)
├── Cross-compilation             │   ├── Python 3.8 + libs
└── Packaging                     │   ├── CV packages + RKNN
                                  │   └── User init scripts
                                  └── Storage (/storage/sd/)
```

- Build system: x86_64 → ARM64 cross-compilation
- Extension: Persistent Python runtime + libraries
- User scripts: `/storage/sd/python-init/*` (any executable, auto-run at startup)
- NPU: RKNN toolkit for hardware acceleration

## Detailed Build Process

**Build options**:

```bash
# Individual packages (5-15 min each)
./build python3-numpy
./build python3-opencv

# Full SDK (30-90 min)
./build --extract-sdk

# Clean builds
./build --clean python3-pkg        # Clean individual package
./build --distclean                # Deep clean with cache removal

# Validation
./validate                          # Check all recipes
./check-recipe-syntax.py recipe.bb # Check individual recipe
```

**Build notes**:

- Requires 10GB+ free space
- Use `screen`/`tmux` for long builds
- Pre-built Docker image eliminates source download delays
   ./build --no-patch python3-pkg             # Build without patches (vanilla)
   ./package --dev-only --verify              # Create development package and validate
   ./validate                                 # Run comprehensive recipe validation

```sh

**Manual approach** (for full control):

```bash
# 1. Prerequisites: x86_64 host, Docker, 25+ GB free space
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension && export project_root=$(pwd)

# 2. Build Docker container with pre-built source (~30 min)
# Source will be downloaded during container build
docker build --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 -t bsoe-build .

# 3. Build SDK using pre-built image (~30-60 min)
mkdir -p srv && ./sh/patch-local-conf.sh -y
./build --extract-sdk

# 4. Install SDK and package extension (~5 min)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package

# 5. Deploy to unsecured player and install
# Transfer zip file via DWS, then: bash ./ext_pydev_install-lvm.sh && reboot
```

**Key Differences**:

- BrightSign OS source is downloaded during Docker image build (not at runtime)
- No Docker volumes needed - source embedded in image
- No manual source download required - happens during `docker build`
- Clean separation between patched and unpatched builds with `--no-patch` option

## Direct Docker Commands

For users who prefer direct Docker commands instead of the automated scripts:

### Build the Docker Image

```bash
# Build image with BrightSign OS source pre-installed (~30 minutes)
# ✅ COMPLETED SUCCESSFULLY - Image ready for use
docker build --rm \
  --build-arg USER_ID=$(id -u) \
  --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 \
  -t bsoe-build .

# Verify the image was created successfully
docker images | grep bsoe-build
```

### Run Container Interactively

```bash
# Interactive shell with patches available
docker run -it --rm \
  -v "$(pwd)/bsoe-recipes:/home/builder/patches:ro" \
  -v "$(pwd)/srv:/srv" \
  -w /home/builder/bsoe/brightsign-oe/build \
  bsoe-build bash

# Interactive shell for vanilla builds (no patches)
docker run -it --rm \
  -v "$(pwd)/srv:/srv" \
  -w /home/builder/bsoe/brightsign-oe/build \
  bsoe-build bash
```

### Manual Build Commands Inside Container

```bash
# Apply patches (if desired)
/usr/local/bin/setup-patches.sh

# Initialize BitBake environment (required for manual builds)
source ../oe-core/oe-init-build-env . ../bitbake

# Build specific packages
MACHINE=cobra ./bsbb python3-tqdm
MACHINE=cobra ./bsbb python3-opencv
MACHINE=cobra ./bsbb python3-rknn-toolkit2
MACHINE=cobra ./bsbb brightsign-sdk

# Clean builds (recommended for troubleshooting)
MACHINE=cobra ./bsbb -c cleanall python3-tqdm
MACHINE=cobra ./bsbb python3-tqdm

# Advanced build commands
MACHINE=cobra ./bsbb -c fetch python3-numpy     # Download sources only
MACHINE=cobra ./bsbb -c compile python3-numpy   # Compile without packaging
MACHINE=cobra ./bsbb -c devshell python3-numpy  # Enter development shell

# Check dependencies and validate recipes
bitbake-layers show-recipes | grep python3
bitbake -g python3-opencv  # Generate dependency graph
bitbake -e python3-opencv | grep "^SRC_URI="  # Show source URLs

# View build logs for debugging
cat tmp-glibc/work/aarch64-oe-linux/python3-*/*/temp/log.do_*

# Package management
ls tmp-glibc/deploy/ipk/aarch64/python3-*.ipk  # List built packages
```

### Docker Build Status ✅

**Latest Build Results**: The Docker image `bsoe-build` has been successfully built and contains:

- **BrightSign OS v9.1.52 source code** (19GB downloaded and extracted)
- **Complete OpenEmbedded/BitBake build environment**
- **All required dependencies for cross-compilation**
- **Proper user permissions and environment setup**
- **Patch scripts ready for use**

**Build Summary**:

- Total build time: ~17 minutes (cached layers + final export)
- Image size: Includes full source tree (~20GB+ compressed)
- Status: Ready for SDK builds and package development

**Next Steps**:

1. Build the SDK: `./build --extract-sdk`
2. Test individual packages: `./build python3-opencv`
3. Validate recipes: `./validate`
4. Package extension: `./package`

## Detailed Instructions

This repository provides comprehensive steps and tools to:

1. Download the BrightSign Open Source packages
2. Build an SDK with Python and common packages
3. Package and Install the Extension on the player
4. Setup and use Python on the player for development

## Step 1 - Download the BSOS SDK (⏱️ ~5-10 minutes)

**⚠️ Prerequisites Check:**

- Ensure you have **15+ GB free disk space** for source files and build artifacts
- Verify **stable internet connection** for downloading large archives (~2-3 GB total)
- Confirm __x86_64 architecture__ with `uname -m` (should output `x86_64`)

**Build a custom SDK from public source**

The platform SDK can be built from public sources. Browse OS releases from the [BrightSign Open Source](https://docs.brightsign.biz/releases/brightsign-open-source) page.  Set the environment variable in the next code block to the desired os release version.

```sh {"promptEnv":"never"}
# Download BrightSign OS and extract
cd "${project_root:-.}"

export BRIGHTSIGN_OS_MAJOR_VERSION=9.1
export BRIGHTSIGN_OS_MINOR_VERSION=52
export BRIGHTSIGN_OS_VERSION=${BRIGHTSIGN_OS_MAJOR_VERSION}.${BRIGHTSIGN_OS_MINOR_VERSION}

```

```sh
# Download source archives (⏱️ ~3-5 minutes depending on connection)
echo "Downloading BrightSign OS source files (~2-3 GB)..."
wget https://brightsignbiz.s3.amazonaws.com/firmware/opensource/${BRIGHTSIGN_OS_MAJOR_VERSION}/${BRIGHTSIGN_OS_VERSION}/brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
wget https://brightsignbiz.s3.amazonaws.com/firmware/opensource/${BRIGHTSIGN_OS_MAJOR_VERSION}/${BRIGHTSIGN_OS_VERSION}/brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz

echo "✅ Download complete"
```

```sh
# Extract source files (⏱️ ~2-3 minutes)
echo "Extracting source archives..."
rm -rf brightsign-oe

tar -xzf brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
tar -xzf brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz

echo "✅ Source extraction complete"

# Optional: Clean up disk space (uncomment to save ~2-3 GB)
# echo "Cleaning up downloaded archives to save disk space..."
# rm brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
# rm brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz
```

**Packages included**:

- Python 3.8 + standard library
- NumPy, OpenCV, Pillow (CV/ML)
- PyTorch, ONNX, RKNN Toolkit2 (AI/NPU)
- pip, setuptools, pytest

### Manual Docker Container Usage (Advanced Debugging)

For troubleshooting build issues or developing recipes manually, you can run the Docker container interactively:

```sh
# Enter interactive shell in build environment
docker run --rm -it \
    -v "$(pwd)/bsoe-recipes:/home/builder/patches:ro" \
    -v "$(pwd)/srv:/srv" \
    -w /home/builder/bsoe/brightsign-oe/build \
    bsoe-build bash

# Inside the container, initialize the build environment
source ../oe-core/oe-init-build-env . ../bitbake

# Manual recipe validation
bitbake-layers show-recipes | grep python3
bitbake -c devshell python3-package-name  # Enter development shell
bitbake -c compile python3-package-name   # Compile specific package

# Check dependencies and providers
bitbake -g python3-package-name  # Generate dependency graph
bitbake-layers show-cross-depends

# Build specific tasks
bitbake -c fetch python3-package-name     # Download sources
bitbake -c unpack python3-package-name    # Extract sources
bitbake -c patch python3-package-name     # Apply patches
bitbake -c configure python3-package-name # Configure build
bitbake -c compile python3-package-name   # Compile only

# Clean specific package for rebuild
bitbake -c cleanall python3-package-name

# Exit container
exit
```

**Common debugging commands inside the container:**

```sh
# Check recipe parsing
bitbake-layers show-recipes python3-*

# View recipe content and metadata
bitbake -e python3-package-name | grep "^SRC_URI="
bitbake -e python3-package-name | grep "^FILES:"

# Check work directories for build artifacts
ls -la tmp-glibc/work/aarch64-oe-linux/python3-package-name/

# View build logs
cat tmp-glibc/work/aarch64-oe-linux/python3-package-name/*/temp/log.do_*

# Test package installation
ls tmp-glibc/deploy/ipk/aarch64/python3-package-name_*.ipk
```

```sh
cd "${project_root:-.}"

./build copy-to-srv
```

**Extract and Install the SDK**:

After the build completes successfully, extract and install the cross-compilation toolchain:

```sh
cd "${project_root:-}"

# Copy the SDK installer from the build output
cp brightsign-oe/build/tmp-glibc/deploy/sdk/brightsign-x86_64-cobra-toolchain-*.sh ./

# Install the SDK to ./sdk directory (⏱️ ~2-3 minutes)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y

echo "✅ SDK installation complete"
```

**Build Verification**:

```sh
# Verify the SDK contains Python and RKNN components
ls -la sdk/sysroots/aarch64-oe-linux/usr/bin/python3*
ls -la sdk/sysroots/aarch64-oe-linux/usr/lib/librknnrt.so*
ls -la sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/rknn/

# Verify critical Python packages are included
echo "=== Checking Python packages in SDK ==="
ls -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/torch* 2>/dev/null && echo "✓ PyTorch found" || echo "✗ PyTorch missing"
ls -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/tqdm* 2>/dev/null && echo "✓ tqdm found" || echo "✗ tqdm missing"
ls -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/typing_extensions* 2>/dev/null && echo "✓ typing_extensions found" || echo "✗ typing_extensions missing"
ls -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/flatbuffers* 2>/dev/null && echo "✓ flatbuffers found" || echo "✗ flatbuffers missing"
ls -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/jinja2* 2>/dev/null && echo "✓ jinja2 found" || echo "✗ jinja2 missing"

# If packages are missing, they may need to be manually copied from the build output
if [ ! -d "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/torch" ]; then
    echo "⚠️  Some packages may be missing from SDK. Run the following to manually install:"
    echo "   cd brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/"
    echo "   for pkg in python3-torch python3-tqdm python3-typing-extensions python3-flatbuffers python3-jinja2; do"
    echo "     ar x \${pkg}_*_aarch64.ipk data.tar.gz && tar -xzf data.tar.gz -C ../../../../../sdk/sysroots/aarch64-oe-linux/"
    echo "   done"
fi
```

**Note**: The Rockchip RKNN runtime library (`librknnrt.so`) is automatically downloaded during SDK installation. Python packages are included through BitBake recipes.

### Docker Image Management

The pre-built image system embeds source code directly in the Docker image. Here are useful commands for managing it:

```sh
# Check image size and details
docker images | grep bsoe-build
docker system df

# Rebuild image with different OS version
docker build --build-arg BRIGHTSIGN_OS_VERSION=9.1.53 -t bsoe-build .

# Access container for debugging (source already included)
docker run -it --rm \
  -v $(pwd)/bsoe-recipes:/home/builder/patches:ro \
  -v $(pwd)/srv:/srv \
  bsoe-build bash

# Clean up old images (frees space)
docker image prune -f  # Clean up old unused images
```

## Packaging & Deployment

**Create packages** (automated via `./package` script):

```bash
./package  # Creates pydev-*.zip and ext_pydev-*.zip
```

**Package options**:

```bash
./package --dev-only    # Development package only
./package --ext-only    # Production extension only
./package --clean       # Clean install dir first
./package --verify      # Run validation after packaging
```

**Package types**:

- `pydev-*.zip`: Development package (volatile, for testing)
- `ext_pydev-*.zip`: Production extension (persistent)

#### Testing Python Package Installation

After deploying the extension to a BrightSign player, you can verify that all Python CV/ML packages are properly installed:

**Option 1: Comprehensive Import Testing (Recommended)**

```sh
# On the BrightSign player (via SSH)
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
/var/volatile/bsext/ext_pydev/sh/test_python_imports

# For detailed output:
/var/volatile/bsext/ext_pydev/sh/test_python_imports --verbose
```

**Option 2: Quick Manual Testing**

```sh
# On the BrightSign player (via SSH or serial console)
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
python3 -c "import cv2, pandas, torch, numpy; print('Core packages working')"
```

### Package Availability

The extension provides packages through two mechanisms:

__✅ SDK-Built Packages (Always Available)__
These are built into the extension and available immediately after sourcing `setup_python_env`:

```python
import cv2              # OpenCV computer vision
import pandas           # Data analysis (v2.0.3)
import PIL              # Image processing (Pillow)
import networkx         # Graph analysis
import imageio          # Image I/O
import psutil           # System utilities
import tqdm             # Progress bars
import ruamel.yaml      # YAML processing
import jinja2           # Template engine
import markupsafe       # Safe string handling
import google.protobuf  # Protocol buffers
# Plus: pip, setuptools, typing_extensions
```

**🔄 Runtime-Installed Packages (After User Initialization)**
These require user initialization via pip install and are available after running user-init scripts:

```python
import torch            # PyTorch deep learning (v2.4.1)
import torchvision      # PyTorch computer vision (v0.19.1)
import skimage          # Advanced image processing (v0.21.0)
import scipy            # Scientific computing (v1.10.1)
import matplotlib       # Plotting and visualization (v3.7.5)
import numpy            # Numerical computing (v1.24.4)
import rknn_toolkit_lite2  # BrightSign NPU acceleration (v2.3.2)
# Plus: 50+ additional scientific and utility packages
```

The test script provides detailed import testing with success/failure reporting for both package categories.

## Player Setup and Deployment

### Prepare the Player

1. **Enable Diagnostic Web Server (DWS)** in BrightAuthor:connected when creating setup files
2. **Unsecure the Player** (required for extension installation):

```bash
# Connect via serial (115200 bps, n-8-1) and interrupt boot (Ctrl-C within 3 seconds)
=> console on
=> reboot

# On second boot, interrupt again and run:
=> setenv SECURE_CHECKS 0
=> envsave
=> reboot
```

__Alternative__: On newer OS versions, use `disable_security_checks` tool if available.

### Deploy and Install Extensions

**Development Installation** (volatile, for testing):

1. Transfer your `pydev-*.zip` file to the player via DWS SD tab

```bash {"promptEnv":"never"}
export PLAYER_IP=192.168.6.93
export PASSWORD=password

sshpass -p "${PASSWORD}" scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    "${PACKAGE_NAME}.zip" brightsign@"${PLAYER_IP}":/storage/sd/
```

2. Connect via SSH and access the Linux shell:

```bash
# Drop to BrightScript debugger (Ctrl-C), then:
exit  # Exit to BrightSign prompt
exit  # Exit to Linux shell
```

3. Install for development:

```bash
mkdir -p /usr/local/pydev && cd $_
unzip /storage/sd/pydev-*.zip
source ./setup_python_env
echo "Python development environment ready (volatile - will not persist across reboots)"
```

**Production Installation** (permanent):

1. Transfer your `ext_pydev-*.zip` file to the player via DWS SD tab
2. Install the extension:

```bash
mkdir -p /usr/local/pydev && cd $_
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot  # Extension activates on reboot
```

### Production Deployment

For production systems:

- Submit the extension to BrightSign for code signing
- Contact BrightSign for production deployment guidance

## Step 4 - Setup and use the Python development environment

In this step, you will use a sample program to explore the development environment.

### Set up the shell environment

Every time a new shell is created, the environment must be set up.  To do so, `source` the `setup_python_env` from the extension installation location.  If you have followed the __development__ path, this location will be `/usr/local/pydev`. If you have installed the extension, this will be `/var/volatile/bsext/ext_pydev`.

```sh
# in the player shell, run:

EXT_HOME="/var/volatile/bsext/ext_pydev"; [ -d "$EXT_HOME" ] || EXT_HOME="/usr/local/pydev"
source "${EXT_HOME}/setup_python_env"
echo "Python development environment is set up.  Use 'python3' and 'pip3' to work with it."

```

### Download a sample project

For this example, we will use the rknn_model_zoo yolox example.

This example relies on having a YoloX model compiled for the target hardware (XT-5/RK3588 NPU).  Run and install the yolox demo from [https://github.com/brightsign/brightsign-npu-yolox] into `/usr/local/yolo`.

```sh
# after sourcing the environment, you can run these commands in the player shell:
MODEL_PATH=/usr/local/yolo/RK3588/model/yolox_s.rknn

cd /usr/local

wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.2.zip
unzip v2.3.2.zip

mv rknn_model_zoo-2.3.2 rknn_model_zoo

cd rknn_model_zoo/examples/yolox/python
python3 yolox.py --model_path ${MODEL_PATH} --target rk3588 --img_folder /usr/local/yolo/
```

## Troubleshooting

### Common Build Issues

#### **Build Failures**

- **BitBake permission errors**: Use the manual PyTorch fix method above as alternative
- **BitBake errors**: Check build logs for specific error details
- **Docker issues**: Ensure Docker daemon is running and image `bsoe-build` exists
- **Disk space**: Monitor with `df -h` - builds require 25+ GB free space
- **Network timeouts**: Use `wget -c` for resumable downloads

#### **Package Issues**

- **PyTorch import errors**: Use manual fix method above
- **Missing packages**: Check `brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/` for .ipk files
- **RKNN toolkit missing**: Ensure network connectivity for automatic download
- **Python import errors**: Verify SDK contains packages with verification commands in Step 2

#### **Extension Deployment**

- __Mount errors__: Ensure player is unsecured (`SECURE_CHECKS=0`)
- **Installation failures**: Check available space on player `/usr/local`
- __Runtime errors__: Source `setup_python_env` before using Python

#### **Recovery Procedures**

```bash
# Clean rebuild after failure
./build --distclean

# Reset Docker environment
docker system prune -f
docker build --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 -t bsoe-build .

# Validate recipes and diagnose issues
./validate
```

#### **Getting Help**

- Use automation scripts with `--help` flag for usage information
- Run `./validate` for recipe diagnostics
- Check build logs in `brightsign-oe/build/tmp-glibc/work/*/temp/log.*`
- Try manual fix methods when BitBake builds persistently fail

## Licensing

This project is released under the terms of the [Apache 2.0 License](./LICENSE.txt).  Any model used in a BSMP must adhere to the license terms for that model.  This is discussed in more detail [here](./model-licenses.md).

Components that are part of this project are licensed separately under their own open source licenses.

* the signed extension will be packaged as a `.bsfw` file that can be applied to a player running a signed OS.

## Removing the Extension

To remove the extension, you can perform a Factory Reset or remove the extension manually.

1. Connect to the player over SSH and drop to the Linux shell.
2. STOP the extension -- e.g. `/var/volatile/bsext/ext_npu_gaze/bsext_init stop`
3. VERIFY all the processes for your extension have stopped.
4. Unmount the extension filesystem and remove it from BOTH the `/var/volatile` filesystem AND the `/dev/mapper` filesystem.

Following the outline given by the `make-extension` script.

```bash
# EXAMPLE USAGE

# stop the extension
/var/volatile/bsext/ext_pydev/bsext_init stop

# unmount the extension
umount /var/volatile/bsext/ext_pydev
# remove the extension
rm -rf /var/volatile/bsext/ext_pydev

# remove the extension from the system
lvremove --yes /dev/mapper/bsos-ext_pydev

rm -rf /dev/mapper/bsos-ext_pydev

#reboot
```

For convenience, an `uninstall.sh` script is packaged with the extension. **Important**: Copy the script to an executable location before running, as executing it directly from the extension volume may cause mount lock issues.

```bash
# Copy script to executable location first
cp /var/volatile/bsext/ext_pydev/uninstall.sh /tmp/
chmod +x /tmp/uninstall.sh

# Run the uninstall script
/tmp/uninstall.sh

# Reboot to apply changes
reboot
```

## User Initialization Scripts Deployment

The Python Development Extension supports user-defined initialization scripts that run automatically at startup. This section explains how to deploy and manage these scripts separately from the main extension.

### Quick Deployment of CV Test Scripts

The project includes example scripts for validating the Python environment:

```bash
# Deploy CV test scripts from development host
cd user-init/tools/
./deploy-to-player.sh <player-ip> [password]

# Example with specific IP
./deploy-to-player.sh 192.168.1.100 mypassword
```

### Manual Deployment

If you prefer manual deployment or need to customize the process:

```bash
# Copy scripts to player
scp -r user-init/examples/* admin@<player-ip>:/storage/sd/python-init/

# Set permissions on player
ssh admin@<player-ip> "chmod +x /storage/sd/python-init/*"

# Restart extension to run new scripts
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init restart"
```

### Verifying Deployment

After deploying user init scripts:

```bash
# Check if scripts were deployed
ssh admin@<player-ip> "ls -la /storage/sd/python-init/"

# View extension logs to see if scripts ran
ssh admin@<player-ip> "cat /var/log/bsext-pydev.log"

# View CV test results (if using the example scripts)
ssh admin@<player-ip> "cat /storage/sd/cv_test.log"

# Check extension status
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init status"
```

### Customizing User Init Scripts

1. __Create Custom Scripts__: Use templates from `user-init/templates/` to create custom initialization scripts
2. __Control Execution Order__: Use numeric prefixes like `01_setup.sh`, `02_validate.sh`
3. __Script Toggling__: Use `chmod +x/-x` to enable/disable scripts

For detailed information about creating custom user init scripts, see `user-init/README.md` and `user-init/templates/`.

## Design Notes: Pre-Built Docker Image Build System

### Problem

Previous approaches had issues with:

- Large downloads during runtime (19GB source files)
- File permission issues with Docker Desktop bind mounts
- Complex source management with Docker volumes
- Mixed source setup and build operations

### Solution

We've implemented a pre-built Docker image approach that downloads and extracts BrightSign OS source during `docker build`, creating a clean separation between source preparation and build operations.

### Key Design Decisions

1. **Pre-Built Source in Image**: The BrightSign OS source (~20GB) is downloaded and extracted during `docker build`, creating immutable source that's consistent across all builds.
2. **No Runtime Source Management**: Source is ready when the container starts, eliminating download time and complexity from build operations.
3. **Minimal Runtime Bind Mounts**: Only small directories are bind-mounted at runtime:

   - `/bsoe-recipes` (patches, read-only) - only when applying patches
   - `/srv` (TFTP/NFS exports)

4. **Patch/No-Patch Modes**: Build script supports both patched builds (default) and vanilla builds with `--no-patch` option.
5. **SDK Extraction**: Built SDKs can be extracted from the container using `docker cp`, maintaining the ability to use build artifacts on the host.

### Benefits

- **Faster Build Starts**: No 25-minute download wait before building
- **Consistent Source**: Immutable source eliminates version mismatches
- **Cleaner Host System**: No large source directories on host filesystem
- **Build Flexibility**: Easy switching between patched and vanilla builds
- **Simplified Workflow**: Single-command builds with clear separation of concerns

For migration details, see [DOCKER_VOLUMES_MIGRATION.md](DOCKER_VOLUMES_MIGRATION.md).

---

# Appendices

## Reference

### Glossary

- **BSOS**: BrightSign OS (embedded Linux, OpenEmbedded/Yocto-based)
- **Extension**: System add-on in `/var/volatile/bsext/`, persists across reboots
- __Cross-compilation__: Build on x86_64 for ARM64 target
- **NPU**: Neural Processing Unit (AI acceleration hardware)
- **RKNN**: Rockchip toolkit for NPU model optimization
- **BitBake**: Build system for embedded Linux distributions
- **SDK**: Cross-compiler + target libraries for development

### Troubleshooting

__Build Issues__: Check architecture (`uname -m`), Docker permissions, disk space (50GB+)  
__Extension Issues__: Verify player unsecured, check `/var/log/bsext-pydev.log`  
__Python Issues__: Source environment: `. /var/volatile/bsext/ext_pydev/sh/setup_python_env`

**Debug Commands**:

```bash
/var/volatile/bsext/ext_pydev/bsext_init status
python3 -c "import cv2, torch, numpy; print('OK')"
```
