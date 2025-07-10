# BrightSign Python Extension for CV Development (ALPHA RELEASE)

This is an example BrightSign System Extension that provides Python and related packages necessary for developing Computer Vision systems on a BrightSign player. This project can also be used as foundation for developing an extension to deliver a CV System using Python.

## Supported Players

| player | minimum OS Version required |
| --- | --- |
| XT-5: XT1145, XT2145 | [9.1.52](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.52/brightsign-xd5-update-9.1.52.zip) |
| _Firebird_ | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |
| _LS-5: LS445_ | [BETA-9.1.52](https://bsnbuilds.s3.us-east-1.amazonaws.com/firmware/brightsign-demos/9.1.52-BETA/BETA-cobra-9.1.52-update.bsfw) |

**NOTE-** This guide is written **ONLY** for the XT-5. Supporting Firebird or LS-5 is a straightforward exercise for the motivated reader.

**NOTE-** This project will describe using the XT-5 ONLY. Extending the project to Firebird or LS-5 is a straightforward exercise that is readily achievable by the interested reader.

## Supported YOLO Models

This extension supports two YOLO model types with automatic detection:

| Model Type | Description | Processing Path |
|------------|-------------|-----------------|
| __YOLO Simplified__ | Latest YOLO architecture with DFL (Distribution Focal Loss) | `YOLO_SIMPLIFIED` - Uses DFL-based post-processing with separated tensor outputs |
| __YOLOX__ | YOLO with unified tensor format | `YOLO_STANDARD` - Uses unified tensor processing with exponential coordinate transforms |

The system automatically detects the model type based on output tensor structure and applies the appropriate post-processing pipeline. No manual configuration is required.

### Model Compatibility

- ✅ **YOLO Simplified models (nano, small, medium, large, extra-large)** - Full support with DFL processing
- ✅ **YOLOX-Nano, YOLOX-Tiny, YOLOX-S, YOLOX-M, YOLOX-L, YOLOX-X** - Full support with unified tensor processing
- ✅ **COCO 80-class models** - Default class set supported
- ✅ **Custom trained models** - Compatible if following YOLO Simplified or YOLOX output formats

## Overview

This repository gives the steps and tools to:

1. Download the BrightSign Open Source packages
2. Build an SDK with Python and common packages
3. Package and Install the Extension on the player
4. Setup and use Python on the player for development

## Project Overview & Requirements

__IMPORTANT: THE TOOLCHAIN REFERENCED BY THIS PROJECT REQUIRES A DEVELOPMENT HOST WITH x86_64 (aka AMD64) INSTRUCTION SET ARCHITECTURE.__ This means that many common dev hosts such as Macs with Apple Silicon or ARM-based Windows and Linux computers __WILL NOT WORK.__  That also includes the OrangePi5Plus (OPi) as it is ARM-based. The OPi ___can___ be used to develop the application with good effect, but the model compilation and final build for BrigthSign OS (BSOS) ___must___ be performed on an x86_64 host.

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

## Step 1 Download the BSOS SDK

**Build a custom SDK from public source**

The platform SDK can be built from public sources. Browse OS releases from the [BrightSign Open Source](https://docs.brightsign.biz/releases/brightsign-open-source) page.  Set the environment variable in the next code block to the desired os release version.

```sh {"promptEnv":"never"}
```sh {"promptEnv":"never"}
# Download BrightSign OS and extract
cd "${project_root:-.}"

export BRIGHTSIGN_OS_MAJOR_VERION=9.1
export BRIGHTSIGN_OS_MINOR_VERION=52
export BRIGHTSIGN_OS_MAJOR_VERION=9.1
export BRIGHTSIGN_OS_MINOR_VERION=52
export BRIGHTSIGN_OS_VERSION=${BRIGHTSIGN_OS_MAJOR_VERION}.${BRIGHTSIGN_OS_MINOR_VERION}

```

```sh

wget https://brightsignbiz.s3.amazonaws.com/firmware/opensource/${BRIGHTSIGN_OS_MAJOR_VERION}/${BRIGHTSIGN_OS_VERSION}/brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
wget https://brightsignbiz.s3.amazonaws.com/firmware/opensource/${BRIGHTSIGN_OS_MAJOR_VERION}/${BRIGHTSIGN_OS_VERSION}/brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz

```

```sh
rm -rf brightsign-oe

tar -xzf brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
tar -xzf brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz

# Clean up disk space
# rm brightsign-${BRIGHTSIGN_OS_VERSION}-src-dl.tar.gz
# rm brightsign-${BRIGHTSIGN_OS_VERSION}-src-oe.tar.gz

```

## Step 2 - Build the SDK

To add Python and support libraries, patch the recipes.

You may wish to inspect the file `bsoe-recipes/meta-bs/recipes-open/brightsign-sdk.bb` for packages of interest.

Patch the recipes, pre-cache the RKNN API, and the local.conf.

```sh
# Download RKNN Toolkit2 wheel files (required for build)
mkdir -p brightsign-oe/downloads && pushd $_
# Download full RKNN Toolkit2 (includes complete RKNN API)
wget https://raw.githubusercontent.com/airockchip/rknn-toolkit2/v2.3.2/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl
# Also download lite version for reference (optional)
wget https://raw.githubusercontent.com/airockchip/rknn-toolkit2/v2.3.2/rknn-toolkit-lite2/packages/rknn_toolkit_lite2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl
popd

# Configure build settings for RKNN toolkit integration
rm brightsign-oe/build/conf/local.conf 2>/dev/null || true
./sh/patch-local-conf.sh -y
```

```sh {"terminalRows":"40"}
cd "${project_root:-.}"

./patch-n-build.sh
```

or manually to hold the shell

```sh
YOCTO_DIR="${SCRIPT_DIR:-.}/brightsign-oe"

# diretory of patch files
PATCH_DIR="${SCRIPT_DIR:-.}/bsoe-recipes"

rsync -av "${PATCH_DIR}/" "${YOCTO_DIR}/"

docker run --rm -it \
    -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c 'MACHINE=cobra ./bsbb brightsign-sdk'
    
```

The recipe from this repository provides:

- **Python 3.8 interpreter** with full standard library
- **NumPy, OpenCV, Pillow** for scientific computing and image processing
- **pip and setuptools** for package management
- **pytest** for testing capabilities
- **RKNN Toolkit2** for Rockchip NPU model conversion and inference
- **Environment setup script** to configure paths correctly within the extension

___IMPORTANT___: Building an OpenEmbedded project can be very particular in terms of packages and setup. For that reason it __strongly recommended__ to use the [Docker build](https://github.com/brightsign/extension-template/blob/main/README.md#recommended-docker) approadh.

```sh {"terminalRows":"45"}
# Build the SDK in Docker -- RECOMMENDED
cd "${project_root:-.}"

wget https://raw.githubusercontent.com/brightsign/extension-template/refs/heads/main/Dockerfile
docker build --rm --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) --ulimit memlock=-1:-1 -t bsoe-build .

mkdir -p srv
# the build process puts some output in srv

```

```sh

docker run -it --rm \
  -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
  bsoe-build

```

```sh
docker run --rm \
    -v $(pwd)/brightsign-oe:/home/builder/bsoe -v $(pwd)/srv:/srv \
    -w /home/builder/bsoe/build \
    bsoe-build \
    bash -c 'MACHINE=cobra ./bsbb brightsign-sdk'

```

Then in the docker container shell

```sh
cd /home/builder/bsoe/build

MACHINE=cobra ./bsbb brightsign-sdk
# This will build the entire system and may take up to several hours depending on the speed of your build system.
```

Exit the Docker shell with `Ctl-D`

**INSTALL INTO `./sdk`**

You can access the SDK from BrightSign.  The SDK is a shell script that will install the toolchain and supporting files in a directory of your choice.  This [link](https://brightsigninfo-my.sharepoint.com/:f:/r/personal/gherlein_brightsign_biz/Documents/BrightSign-NPU-Share-Quividi?csf=1&web=1&e=bgt7F7) is limited only to those with permissions to access the SDK.

```sh
cd "${project_root:-}"

# copy the SDK to the project root
cp brightsign-oe/build/tmp-glibc/deploy/sdk/brightsign-x86_64-cobra-toolchain-*.sh ./
cp brightsign-oe/build/tmp-glibc/deploy/sdk/brightsign-x86_64-cobra-toolchain-*.sh ./

# can safely remove the source if you want to save space
#rm -rf brightsign-oe
```

```sh
cd "${project_root:-.}"

./brightsign-x86_64-cobra-toolchain-*.sh  -d ./sdk -y
./brightsign-x86_64-cobra-toolchain-*.sh  -d ./sdk -y
# installs the sdk to ./sdk
```

Patch the SDK to include the Rockchip binary libraries that are closed source

```sh
cd "${project_root:-.}"/sdk/sysroots/aarch64-oe-linux/usr/lib

wget https://github.com/airockchip/rknn-toolkit2/blob/v2.3.2/rknpu2/runtime/Linux/librknn_api/aarch64/librknnrt.so
```

## Step 3 - Package and Install the Extension

### Copy Python Components to Install Directory

To include Python capabilities in your BrightSign extension, you need to copy the Python interpreter, libraries, and your Python application from the SDK sysroot to the install directory.

```sh
cd "${project_root:-.}"

rm -rf install

# Create Python directory structure in install
mkdir -p install/usr/bin
mkdir -p install/usr/lib
mkdir -p install/usr/lib/python3.8
mkdir -p install/usr/lib/python3.8/site-packages
mkdir -p install/python-apps

# Copy Python interpreter and essential binaries (excluding symlinks)
# find sdk/sysroots/aarch64-oe-linux/usr/bin/ -name "python3*" ! -type l -exec cp {} install/usr/bin/ \;
# find sdk/sysroots/aarch64-oe-linux/usr/bin/ -name "pip3*" ! -type l -exec cp {} install/usr/bin/ \;
cp sdk/sysroots/aarch64-oe-linux/usr/bin/python3* install/usr/bin/ || true
cp sdk/sysroots/aarch64-oe-linux/usr/bin/pip3* install/usr/bin/ || true

# # Recreate essential symlinks to preserve filesystem structure
# if [ -f install/usr/bin/python3.8 ]; then
#     ln -sf python3.8 install/usr/bin/python3
# fi
# if [ -f install/usr/bin/python3.8-config ]; then
#     ln -sf python3.8-config install/usr/bin/python3-config
# fi

cp -r sdk/sysroots/aarch64-oe-linux/usr/lib install/usr/

# Copy Python libraries and modules (with error checking)
# if [ -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8 ]; then
    # cp -r sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/* install/usr/lib/python3.8/
# fi
# cp -r sdk/sysroots/aarch64-oe-linux/usr/lib/libpython3.8* install/usr/lib/ 2>/dev/null || true
# cp -r sdk/sysroots/aarch64-oe-linux/usr/lib/libpython3.8* install/usr/lib/

# Copy site-packages (numpy, opencv, pillow, etc.) if they exist
# if [ -d sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages ]; then
    # cp -r sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/* install/usr/lib/python3.8/site-packages/
# fi

# Copy your Python YOLO application (with error checking)
# if [ -f toolkit/rknn_model_zoo/examples/yolov8/python/yolov8.py ]; then
    # cp toolkit/rknn_model_zoo/examples/yolov8/python/yolov8.py install/python-apps/
# else
#     echo "Warning: Python YOLO application not found at toolkit/rknn_model_zoo/examples/yolov8/python/yolov8.py"
# fi

# Copy the CV/ML package test script
cp test_cv_packages.py install/python-apps/

# Make Python binaries executable (only if they exist)
# chmod +x install/usr/bin/python3* 2>/dev/null || true
# chmod +x install/usr/bin/pip3* 2>/dev/null || true

cp sh/setup_python_env install/

# chmod +x install/setup_python_env.sh
```

### Package the Extension

Copy the supporting scripts to the install dir

```sh
cd "${project_root:-.}"

cp bsext_init install/ && chmod +x install/bsext_init
cp sh/setup_python_env install/ && chmod +x install/setup_python_env
cp sh/uninstall.sh install/ && chmod +x install/uninstall.sh

# cp -rf model install/
```

#### To test the program without packing into an extension

```sh
cd "${project_root:-.}/install"

# remove any old zip files
rm -f ../pydev-*.zip

zip -r ../pydev-$(date +%s).zip ./
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

### Unsecure the Player

* Enabling the Diagnostic Web Server (DWS) is recommended as it's a handy way to transfer files and check various things on the player. This can be done in BrightAuthor:connected when creating setup files for a new player.

0. Power off the player
1. __Enable serial control__ | Connect a serial cable from the player to your development host.  Configure your terminal program for 115200 bps, no parity, 8 data bits, 1 stop bit (n-8-1) and start the terminal program.  Hold the __`SVC`__ button while applying power. _Quick_, like a bunny, type Ctl-C in your serial terminal to get the boot menu -- you have 3 seconds to do this.  type

```bash
=> console on
=> reboot
```

2. __Reboot the player again__ using the __`RST`__ button or the _Reboot_ button from the __Control__ tab of DWS for the player.  Within the first 3 seconds after boot, again type Ctl-C in your serial terminal program to get the boot prompt and type:

```bash
=> setenv SECURE_CHECKS 0
=> env save
=> printenv
```

Verify that `SECURE_CHECKS` is set to 0. And type `reboot`.

To enable `Ctl-C` in the ssh session, set the debug registry key from the **Registry** tab on DWS.

```bash
registry write brightscript debug 1
```

On some newer OS versions, the tool `disable_security_checks` is availabe that will handle the above for you.

**The player is now unsecured.**

### Install the Python Dev Environment on the Player

Copy the zip to the target and expand it.

* Transfer the files `ext_pydev-*.zip` to an unsecured player with the _Browse_ and _Upload_ buttons from the __SD__ tab of DWS or other means.
* Connect to the player via ssh, telnet, or serial.
* Type Ctl-C to drop into the BrightScript Debugger, then type `exit` to the BrightSign prompt and `exit` again to get to the linux command prompt.

```bash
# from the player ssh prompt
mkdir -p /usr/local/pydev && cd $_
# if you have multiple builds on the card, you might want to delete old ones
# or modify the unzip command to ONLY unzip the version you want to install
unzip /storage/sd/ext_pydev-*.zip

source ./setup_pydev_env
```

The current shell is now set up for python development.  Note that the interpreter is `python3` and not `python`.  Additionally, the normal package tool is `pip3`, not `pip`. However the setup script mapped the package installation directory to `/usr/local/lib/python3.8/site-packages` (wich is read-write and executable).

**NOTE THIS STORAGE IS VOLATILE AND WILL NOT PERSIST ACROSS REBOOTS**

Proceed to Step 4 to run a sample program or continue below to package an installable extension.

#### Run the make extension script on the install dir to create an installable extension

```sh
cd "${project_root:-.}"/install

../sh/make-extension-lvm
# zip for convenience to transfer to player
#rm -f ../pydev-*.zip 
zip ../pydev-$(date +%s).zip ext_pydev*
# clean up
rm -rf ext_pydev*
```

Copy the zip to the target and expand it.

* Transfer the files `ext_pydev-*.zip` to an unsecured player with the _Browse_ and _Upload_ buttons from the __SD__ tab of DWS or other means.
* Connect to the player via ssh, telnet, or serial.
* Type Ctl-C to drop into the BrightScript Debugger, then type `exit` to the BrightSign prompt and `exit` again to get to the linux command prompt.

At the command prompt, **install** the extension with:

```bash
mkdir -p /usr/local/pydev && cd $_
# if you have multiple builds on the card, you might want to delete old ones
# or modify the unzip command to ONLY unzip the version you want to install
unzip /storage/sd/ext_pydev-*.zip
# you may need to answer prompts to overwrite old files

# install the extension
bash ./ext_pydev_install-lvm.sh

# the extension will be installed on reboot
#reboot
```

### for production

* Submit the extension to BrightSign for signing
* Contact BrightSign

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

## Licensing

This project is released under the terms of the [Apache 2.0 License](./LICENSE.txt).  Any model used in a BSMP must adhere to the license terms for that model.  This is discussed in more detail [here](./model-licenses.md).

Components that are part of this project are licensed seperately under their own open source licenses.

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

For convenience, an `uninstall.sh` script is packaged with the extension and can be run from the player shell.  **However**, you cannot simply run the script from the extension volume as that will lock the mount.  Copy to an executable mount first.

```bash
/var/volatile/bsext/ext_pydev/uninstall.sh
# will remove the extension from the system

# reboot to apply changes
#reboot
#reboot

```
