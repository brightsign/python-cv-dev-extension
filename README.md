# BrightSign Python Extension for CV Development (ALPHA RELEASE)

This is an example BrightSign System Extension that provides Python and related packages necessary for developing Computer Vision systems on a BrightSign player. This project can also be used as foundation for developing an extension to deliver a CV System using Python.

## Supported Players

| player | minimum OS Version required |
| --- | --- |
| XT-5: XT1145, XT2145 | [9.1.52](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.52/brightsign-xd5-update-9.1.52.zip) |
| _Firebird_ | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |
| _LS-5: LS445_ | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |

**NOTE:** This guide is written **ONLY** for the XT-5. Supporting Firebird or LS-5 is a straightforward exercise for the motivated reader.

## Quick Start (Experienced Users)

This project includes automation scripts to streamline the build process:

- `setup` - Automates prerequisites checking and source download
- `build` - Build script using pre-built Docker image with source included
- `package` - Automates extension packaging with multiple output formats
- `check-recipe-syntax.py` - Pre-build recipe validation tool
- `validate` - Comprehensive recipe validation suite

## Project Overview & Requirements

__IMPORTANT: THE TOOLCHAIN REFERENCED BY THIS PROJECT REQUIRES A DEVELOPMENT HOST WITH x86_64 (aka AMD64) INSTRUCTION SET ARCHITECTURE.__ This means that many common dev hosts such as Macs with Apple Silicon or ARM-based Windows and Linux computers __WILL NOT WORK.__  That also includes the OrangePi5Plus (OPi) as it is ARM-based. The OPi ___can___ be used to develop the application with good effect, but the model compilation and final build for BrightSign OS (BSOS) ___must___ be performed on an x86_64 host.

### Requirements

1. A Series 5 player running an experimental, debug build of BrightSign OS -- signed and unsigned versions
2. A development computer with x86_64 instruction architecture to compile the model and cross-compile the executables
3. The cross compile toolchain ___matching the BSOS version___ of the player

#### Software Requirements -- Development Host

* [Docker](https://docs.docker.com/engine/install/) - it should be possible to use podman or other, but these instructions assume Docker
* [git](https://git-scm.com/)
* [cmake](https://cmake.org/download/)

```bash
# consult Docker installation instructions

# for others, common package managers should work
# for Ubuntu, Debian, etc.
sudo apt-get update && sudo apt-get install -y \
    cmake git
    
```

## Step 0 - Setup

1. Install [Docker](https://docs.docker.com/engine/install/)
2. Clone this repository -- later instructions will assume to start from this directory unless otherwise noted.

```bash
#cd path/to/your/directory
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension

export project_root=$(pwd)
# this environment variable is used in the following scripts to refer to the root of the project
```

**Automated approach** (recommended):

```bash
# Clone and setup (5-10 minutes)
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension
./setup

# Build SDK with automatic extraction (30-90 minutes)
./build --extract-sdk

# Install SDK and package extension (2-5 minutes)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package

# Validate recipes (optional)
./validate

# Deploy: Transfer packages to player via DWS, then install
```

**Note**: We use `build` which uses a pre-built Docker image with BrightSign OS source already included, eliminating download time and permission issues.

**Automation Script Options:**

```bash
# View help for any script
./setup --help
./build --help
./package --help
./validate --help

# Recipe validation tools
./check-recipe-syntax.py --help            # Validate individual recipes
./validate                                 # Validate all Python recipes

# Advanced usage examples
./build python3-numpy --clean              # Clean build individual package
./build --distclean python3-pkg            # Deep clean with cache removal
./build --no-patch python3-pkg             # Build without patches (vanilla)
./package --dev-only --verify              # Create development package and validate
./validate                                 # Run comprehensive recipe validation
```

**Manual approach** (for full control):

```bash
# 1. Prerequisites: x86_64 host, Docker, 25+ GB free space
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension && export project_root=$(pwd)

# 2. Build Docker container with pre-built source (~30 min)
# Source will be downloaded during container build
docker build --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 -t bsoe-build-v3 .

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
  -t bsoe-build-v3 .

# Verify the image was created successfully
docker images | grep bsoe-build-v3
```

### Run Container Interactively

```bash
# Interactive shell with patches available
docker run -it --rm \
  -v "$(pwd)/bsoe-recipes:/home/builder/patches:ro" \
  -v "$(pwd)/srv:/srv" \
  -w /home/builder/bsoe/brightsign-oe/build \
  bsoe-build-v3 bash

# Interactive shell for vanilla builds (no patches)
docker run -it --rm \
  -v "$(pwd)/srv:/srv" \
  -w /home/builder/bsoe/brightsign-oe/build \
  bsoe-build-v3 bash
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

**Latest Build Results**: The Docker image `bsoe-build-v3` has been successfully built and contains:

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

## Step 2 - Build the SDK (⏱️ ~30-90 minutes)

**⚠️ Important Build Notes:**

- Full SDK builds typically take **30-90 minutes** on modern systems
- Ensure **10+ GB additional free space** for build artifacts
- **Do not interrupt** the build process - use `screen` or `tmux` for long sessions
- Individual package builds take **5-15 minutes** each for testing

To add Python and support libraries, patch the recipes.

You may wish to inspect the file `bsoe-recipes/meta-bs/recipes-open/brightsign-sdk.bb` for packages of interest.

Patch the recipes and configure the local.conf for RKNN support. The RKNN toolkit and runtime libraries will be automatically downloaded during the build process.

```sh
# Configure build settings for RKNN toolkit integration
rm brightsign-oe/build/conf/local.conf 2>/dev/null || true
./sh/patch-local-conf.sh -y
```

**Build the SDK** (⏱️ ~30-60 minutes depending on system):

```sh {"terminalRows":"42"}
cd "${project_root:-.}"

# Build the complete SDK with Python and RKNN support (pre-built image)
./build

# Or build and immediately extract SDK to host
./build --extract-sdk
```

**Build System:**

- `./build` - Uses pre-built Docker image with embedded source for reliable, fast builds

For advanced users who want to build specific packages first:

```sh
# Validate all recipes before building (recommended)
./validate

# Test individual package syntax
./check-recipe-syntax.py bsoe-recipes/meta-bs/recipes-open/python3-numpy/python3-numpy_*.bb

# Test individual packages (5-15 minutes each) using pre-built image
./build python3-numpy
./build python3-opencv  
./build python3-rknn-toolkit2

# Test vanilla build without patches
./build --no-patch python3-numpy

# Build complete SDK after testing
./build
```

The recipe from this repository provides:

- **Python 3.8 interpreter** with full standard library
- **NumPy, OpenCV, Pillow** for scientific computing and image processing
- **pip and setuptools** for package management
- **pytest** for testing capabilities
- **RKNN Toolkit2** for Rockchip NPU model conversion and inference
- **Environment setup script** to configure paths correctly within the extension

___IMPORTANT___: Building an OpenEmbedded project can be very particular in terms of packages and setup. The build uses Docker automatically for consistency across different host systems.

**First-time setup** (one-time only):

```sh
cd "${project_root:-.}"

# Build the container image with pre-built source (~30 minutes)
docker build --rm --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) \
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 -t bsoe-build-v3 .

# Create output directory
mkdir -p srv
```

The `build` script will automatically use this Docker container for all builds.

### Manual Docker Container Usage (Advanced Debugging)

For troubleshooting build issues or developing recipes manually, you can run the Docker container interactively:

```sh
# Enter interactive shell in build environment
docker run --rm -it \
    -v "$(pwd)/bsoe-recipes:/home/builder/patches:ro" \
    -v "$(pwd)/srv:/srv" \
    -w /home/builder/bsoe/brightsign-oe/build \
    bsoe-build-v3 bash

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

**Note**: The Rockchip RKNN runtime library (`librknnrt.so`) and Python packages are automatically included through BitBake recipes - no manual patching required.

### Docker Image Management

The pre-built image system embeds source code directly in the Docker image. Here are useful commands for managing it:

```sh
# Check image size and details
docker images | grep bsoe-build-v3
docker system df

# Rebuild image with different OS version
docker build --build-arg BRIGHTSIGN_OS_VERSION=9.1.53 -t bsoe-build-v3 .

# Access container for debugging (source already included)
docker run -it --rm \
  -v $(pwd)/bsoe-recipes:/home/builder/patches:ro \
  -v $(pwd)/srv:/srv \
  bsoe-build-v3 bash

# Clean up old images (frees space)
docker image prune -f
docker rmi bsoe-build-v2 bsoe-build  # Remove old versions if they exist
```

## Step 3 - Package and Install the Extension (⏱️ ~5-10 minutes)

### Copy Python Components to Install Directory

Create the extension package by copying the Python interpreter, libraries, and applications from the SDK sysroot:

```sh
cd "${project_root:-.}"

# Clean and create directory structure
echo "Creating extension package structure..."
rm -rf install
mkdir -p install/{usr/{bin,lib},python-apps}

# Copy Python binaries and libraries
echo "Copying Python runtime and libraries..."
cp sdk/sysroots/aarch64-oe-linux/usr/bin/python3* install/usr/bin/ 2>/dev/null || true
cp sdk/sysroots/aarch64-oe-linux/usr/bin/pip3* install/usr/bin/ 2>/dev/null || true

# Copy the complete library directory (includes Python modules, RKNN libs, etc.)
cp -r sdk/sysroots/aarch64-oe-linux/usr/lib install/usr/

# Copy test and setup scripts
cp test_cv_packages.py install/python-apps/
cp sh/setup_python_env install/

echo "✅ Python components copied to install directory"
```

**Package Size Verification:**

```sh
# Check the installation size
du -sh install/
echo "Extension package ready for deployment"
```

### Package the Extension

Add the remaining extension scripts and create deployment packages:

```sh
cd "${project_root:-.}"

# Copy extension management scripts
echo "Adding extension management scripts..."
cp bsext_init install/ && chmod +x install/bsext_init
cp sh/setup_python_env install/ && chmod +x install/setup_python_env
cp sh/uninstall.sh install/ && chmod +x install/uninstall.sh

echo "✅ Extension scripts added"
```

### Create Deployment Packages

**Option 1: Development/Testing Package** (for `/usr/local` deployment):

```sh
cd "${project_root:-.}/install"

# Create timestamped development package
export PACKAGE_NAME="pydev-$(date +%Y%m%d-%H%M%S)"
zip -r "../${PACKAGE_NAME}.zip" ./

echo "✅ Development package created: ${PACKAGE_NAME}.zip"
```

**Option 2: Production Extension Package** (for permanent installation):

```sh
cd "${project_root:-.}/install"

# Create production extension using the make-extension script
../sh/make-extension-lvm

# Package for transfer to player
EXTENSION_NAME="ext_pydev-$(date +%Y%m%d-%H%M%S)"
zip "../${EXTENSION_NAME}.zip" ext_pydev*

# Clean up temporary files
rm -rf ext_pydev*

echo "✅ Production extension created: ${EXTENSION_NAME}.zip"
```

#### Testing Python Package Installation

After deploying the extension to a BrightSign player, you can verify that all Python CV/ML packages are properly installed using the included test script:

```sh
# On the BrightSign player (via SSH or serial console)
cd /var/volatile/bsext/python-apps/

# Run the test script to verify all packages
./test_cv_packages.py

# Or run with explicit python3
python3 test_cv_packages.py
```

The test script will:

- Check that all core CV/ML packages can be imported (OpenCV, PyTorch, ONNX, etc.)
- Verify scientific computing libraries (NumPy, SciPy)
- Test dependency packages (protobuf, typing-extensions, etc.)
- Attempt to load the native RKNN runtime library
- Provide a summary of successful vs. failed package imports

Expected output should show ✓ marks for all packages. Any ✗ marks indicate missing or broken packages that need investigation.

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
cd "${EXT_HOME}" && source ./setup_python_env
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
- **Docker issues**: Ensure Docker daemon is running and image `bsoe-build-v3` exists
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
  --build-arg BRIGHTSIGN_OS_VERSION=9.1.52 -t bsoe-build-v3 .

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
