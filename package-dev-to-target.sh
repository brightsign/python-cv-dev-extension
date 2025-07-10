#!/bin/bash

TARGET_IP=${1:-192.168.6.93}
PASSWORD=${2:-password}


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(dirname "$SCRIPT_DIR")"

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


cp bsext_init install/ && chmod +x install/bsext_init
cp sh/setup_python_env install/ && chmod +x install/setup_python_env
cp sh/uninstall.sh install/ && chmod +x install/uninstall.sh

# cp -rf model install/

cd "${project_root:-.}/install"

ZIP_NAME="pydev-$(date +%s).zip"
zip -r "../${ZIP_NAME}" ./

# copy to target location
sshpass -p "${PASSWORD}" scp "../${ZIP_NAME}" brightsign@${TARGET_IP}:/storage/sd/