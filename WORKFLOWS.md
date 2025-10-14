# Common Workflows - Quick Reference

Copy-paste commands for common tasks. Replace `<placeholders>` with your values.

---

## First-Time Setup

### Complete Build from Scratch

```bash
# Validate prerequisites first
./check-prerequisites

# Setup (5-10 min)
./setup -y

# Build SDK (30-60 min)
./build --extract-sdk

# Install toolchain and package (5 min)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package

# Output: pydev-*.zip and ext_pydev-*.zip ready for deployment
```

**Time**: ~60-90 minutes first time

---

## Rebuild After Code Changes

### Rebuild Single Package

```bash
# Clean rebuild of specific package (5-15 min)
./build --clean python3-opencv
./build python3-opencv

# Extract to SDK
./build --extract-sdk

# Repackage
./package
```

**Time**: 5-20 minutes

### Rebuild Full SDK

```bash
# Clean full rebuild (30-60 min)
./build --distclean
./build --extract-sdk

# Repackage
./package
```

**Time**: 30-90 minutes

---

## Deploy to BrightSign Player

### Deploy Development Package (Volatile)

```bash
# Set player info
export PLAYER_IP=192.168.1.100
export PASSWORD=password

# Transfer via SCP
scp pydev-*.zip brightsign@${PLAYER_IP}:/storage/sd/

# Install via SSH
ssh brightsign@${PLAYER_IP}
# At BrightSign prompt, type 'exit' twice to reach Linux shell

mkdir -p /usr/local/pydev && cd /usr/local/pydev
unzip /storage/sd/pydev-*.zip
source sh/setup_python_env

# Test
python3 -c "import cv2, numpy, torch; print('Success!')"
```

**Time**: 5 minutes

### Deploy Production Extension (Persistent)

```bash
# Transfer via SCP
scp ext_pydev-*.zip brightsign@${PLAYER_IP}:/storage/sd/

# Install via SSH
ssh brightsign@${PLAYER_IP}
# Exit to Linux shell

mkdir -p /usr/local && cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot
```

**After reboot**: Extension auto-starts at `/var/volatile/bsext/ext_pydev/`

**Time**: 5 minutes + reboot

---

## Add Python Package to Build

### Method 1: Add to Existing SDK (Quick)

For packages with ARM64 wheels available on PyPI:

```bash
# On player after extension is installed
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
pip3 install --break-system-packages <package-name>

# Test
python3 -c "import <package_name>; print('Installed!')"
```

**Time**: 1-5 minutes
**Note**: Install persists only until reboot (volatile storage)

### Method 2: Add to Requirements.txt (Auto-install)

```bash
# Edit requirements file
echo "<package-name>==<version>" >> user-init/examples/requirements.txt

# Deploy to player
cd user-init/tools
./deploy-to-player.sh ${PLAYER_IP} ${PASSWORD}

# Enable user scripts via DWS
# Navigate to http://${PLAYER_IP}:8080 → Registry tab
# registry write extension bsext-pydev-enable-user-scripts true
# Reboot player

# Package auto-installs on startup
```

**Time**: 5 minutes + startup install time

### Method 3: Build Into SDK (Permanent)

```bash
# Create recipe (see docs/building.md for recipe development)
# Add to bsoe-recipes/meta-bs/recipes-devtools/python/

# Rebuild SDK
./build --clean python3-<package>
./build python3-<package>
./build --extract-sdk
./package

# Deploy new package
```

**Time**: 15-30 minutes

---

## Test NPU / Model Zoo Examples

### Quick YOLOX Object Detection Test

```bash
# On development machine: Download model and test image
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn
wget https://raw.githubusercontent.com/airockchip/rknn_model_zoo/v2.3.2/examples/yolox/model/bus.jpg

# Transfer to player
scp yolox_s_rk3588.rknn bus.jpg brightsign@${PLAYER_IP}:/usr/local/

# On player: Setup and run
ssh brightsign@${PLAYER_IP}

cd /usr/local/pydev  # Or /var/volatile/bsext/ext_pydev for production
source sh/setup_python_env

# Download model_zoo examples
cd /usr/local
wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.2.zip
unzip v2.3.2.zip
mv rknn_model_zoo-2.3.2 rknn_model_zoo

# Copy compatibility wrapper
cp -r /usr/local/pydev/examples/py_utils \
      /usr/local/rknn_model_zoo/examples/yolox/python/

# Run inference
cd /usr/local/rknn_model_zoo/examples/yolox/python
python3 yolox.py --model_path /usr/local/yolox_s_rk3588.rknn \
                 --target rk3588 \
                 --img_folder /usr/local/ \
                 --img_save

# Check results
ls -lh ./result/bus.jpg
```

**Expected**: Detects bus with 90%+ confidence, people with 85%+ confidence

**Time**: 10 minutes first time, 2 minutes subsequent

---

## User Initialization Scripts

### Deploy User Init Examples

```bash
# Enable user scripts first (via DWS at http://${PLAYER_IP}:8080)
# registry write extension bsext-pydev-enable-user-scripts true
# Restart player

# Deploy examples
cd user-init/tools
./deploy-to-player.sh ${PLAYER_IP} ${PASSWORD}

# On player: Make scripts executable (required)
ssh brightsign@${PLAYER_IP}
chmod +x /storage/sd/python-init/*.sh

# Restart extension to run scripts
/var/volatile/bsext/ext_pydev/bsext_init restart

# Check logs
cat /var/log/bsext-pydev.log
cat /storage/sd/python-init/cv_test.log
```

**Time**: 5 minutes

### Create Custom Init Script

```bash
# Start from template
cp user-init/templates/basic_script.sh user-init/examples/02_my_custom.sh

# Edit script (add your initialization logic)

# Deploy
cd user-init/tools
./deploy-to-player.sh ${PLAYER_IP} ${PASSWORD}

# Make executable on player
ssh brightsign@${PLAYER_IP}
chmod +x /storage/sd/python-init/02_my_custom.sh

# Restart to run
/var/volatile/bsext/ext_pydev/bsext_init restart
```

**Time**: 10 minutes

---

## Troubleshooting

### Check Extension Status

```bash
# On player
ssh brightsign@${PLAYER_IP}

# Check if running
/var/volatile/bsext/ext_pydev/bsext_init status

# View logs
tail -f /var/log/bsext-pydev.log

# Test Python environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
python3 -c "import sys; print(sys.path)"
python3 -c "import cv2, numpy; print('OK')"
```

### Restart Extension

```bash
# On player
/var/volatile/bsext/ext_pydev/bsext_init stop
/var/volatile/bsext/ext_pydev/bsext_init start

# Or combined
/var/volatile/bsext/ext_pydev/bsext_init restart
```

### Test NPU Initialization

```bash
# On player
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

python3 << 'EOF'
from rknnlite.api import RKNNLite
rknn = RKNNLite()
print("RKNNLite object created successfully")
print("NPU runtime available!")
EOF
```

### Clean Build (When Build Fails)

```bash
# Option 1: Clean specific package
./build --clean python3-opencv
./build python3-opencv

# Option 2: Deep clean (removes cache)
./build --distclean
./build --extract-sdk

# Option 3: Complete fresh start
rm -rf sdk install srv/*
./setup -y
./build --extract-sdk
```

---

## Quick Reference: File Locations

### On Development Host

```
.
├── setup                # Setup script (run once)
├── build                # Build script (main build tool)
├── package              # Packaging script
├── check-prerequisites  # Validate system before building
├── sdk/                 # Extracted SDK (after build)
├── install/             # Staging directory (before packaging)
├── user-init/           # User initialization examples
└── *.zip                # Output packages (pydev-*.zip, ext_pydev-*.zip)
```

### On BrightSign Player

**Development Installation** (`/usr/local/pydev/`):
```
/usr/local/pydev/
├── sh/setup_python_env      # Source this to use Python
├── usr/bin/python3          # Python 3.8
├── usr/lib/python3.8/       # Python libraries
├── examples/                # Example scripts (test_yolox_npu.py, etc.)
└── bsext_init               # Service control script
```

**Production Installation** (`/var/volatile/bsext/ext_pydev/`):
```
/var/volatile/bsext/ext_pydev/
├── sh/setup_python_env      # Source this to use Python
├── usr/bin/python3          # Python 3.8
├── usr/lib/python3.8/       # Python libraries
├── examples/                # Example scripts
└── bsext_init               # Service control script (auto-starts)
```

**User Init Scripts** (`/storage/sd/python-init/`):
```
/storage/sd/python-init/
├── requirements.txt         # Auto-installed Python packages
├── 01_validate_cv.sh        # Example validation script
├── test_cv_packages.py      # Python test script
└── *.sh                     # Your custom scripts (must be executable)
```

**Logs**:
```
/var/log/bsext-pydev.log                          # Extension log
/storage/sd/python-init/requirements-install.log  # Package install log
/storage/sd/python-init/cv_test.log               # CV validation log
```

---

## Common Commands Quick Reference

```bash
# Prerequisites
./check-prerequisites        # Validate system before building

# Build
./setup -y                   # First-time setup
./build --extract-sdk        # Build full SDK
./build python3-opencv       # Build single package
./build --clean <package>    # Clean rebuild
./build --distclean          # Deep clean + rebuild

# Package
./package                    # Create both dev and prod packages
./package --dev-only         # Dev package only
./package --ext-only         # Production only
./package --verify           # Package + validation

# Deploy
cd user-init/tools
./deploy-to-player.sh <IP> <password>

# On Player
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
/var/volatile/bsext/ext_pydev/bsext_init status
/var/volatile/bsext/ext_pydev/bsext_init restart
tail -f /var/log/bsext-pydev.log
```

---

## Environment Variables

Useful environment variables for scripts:

```bash
# Player connection
export PLAYER_IP=192.168.1.100
export PASSWORD=password

# Build configuration
export BRIGHTSIGN_OS_VERSION=9.1.52  # OS version to build for

# Paths (for reference, usually auto-detected)
export EXT_HOME="/var/volatile/bsext/ext_pydev"  # Production
export EXT_HOME="/usr/local/pydev"                # Development
```

---

## Need More Help?

- **Quick start**: See [QUICKSTART.md](QUICKSTART.md)
- **Full documentation**: See [README.md](README.md)
- **User init system**: See [user-init/README.md](user-init/README.md)
- **Troubleshooting**: See [docs/troubleshooting.md](docs/troubleshooting.md)
- **Build process**: See [docs/building.md](docs/building.md)
- **Issues**: [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues)
