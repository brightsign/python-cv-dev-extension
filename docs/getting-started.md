# Getting Started - Complete Guide

This guide walks through setting up the BrightSign Python CV Extension development environment from scratch.

**Prerequisites**: Read [QUICKSTART.md](../QUICKSTART.md) for a condensed version. This guide provides more detail and context.

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Environment Preparation](#environment-preparation)
3. [First-Time Setup](#first-time-setup)
4. [Build Process](#build-process)
5. [Package Creation](#package-creation)
6. [Player Preparation](#player-preparation)
7. [Deployment](#deployment)
8. [Verification](#verification)
9. [Next Steps](#next-steps)

---

## System Requirements

### Development Host

**Critical Requirements**:
- **Architecture**: x86_64 (Intel/AMD) - Apple Silicon NOT compatible
- **RAM**: 16GB minimum, 32GB recommended
- **Disk Space**: 50GB+ free (SSD strongly recommended)
- **OS**: Linux, macOS (x86_64), Windows with WSL2

**Software Dependencies**:
- Docker 20.10+ (Docker Desktop or docker-ce)
- git 2.x
- wget, tar (usually pre-installed)
- bash 4.x+

**Network**:
- Internet connectivity for downloads (~3GB)
- Reasonable speed (10+ Mbps recommended)

### Target Player

**Hardware**:
- BrightSign Series 5: XT-5, Firebird, or LS-5
- RK3588 SoC (for NPU support)
- 2GB+ RAM (standard in these models)

**Firmware**:
- BrightSign OS **9.1.79.3 or later** (REQUIRED)
- Download: [OS 9.1.79.3](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip)

**Configuration**:
- Diagnostic Web Server (DWS) enabled
- SSH enabled
- Player unsecured (`SECURE_CHECKS=0`)
- Network connectivity to development host

### Why These Requirements?

**x86_64 only**: RKNN toolkit (Rockchip's NPU compiler) only provides x86_64 binaries for model compilation. ARM64 hosts cannot cross-compile ARM64 targets with this toolchain.

**50GB disk**: Build process creates:
- Docker image with source: ~20GB
- Build artifacts (BitBake cache): ~20GB
- SDK output: ~10GB
- Packages: ~1GB

**OS 9.1.79.3+**: This version includes `librknnrt.so` system library required by RKNN toolkit. Earlier versions need complex workarounds.

---

## Environment Preparation

### Validate Your System

Before starting, validate your system meets requirements:

```bash
# Run prerequisite check
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension
./check-prerequisites
```

The script checks:
- ✅ Architecture (fails fast on Apple Silicon)
- ✅ Docker installation and daemon status
- ✅ Disk space availability
- ✅ Memory
- ✅ Required tools
- ✅ Internet connectivity

**If checks fail**: Fix issues before proceeding. The script provides specific fix instructions.

### Install Dependencies

**Ubuntu/Debian**:
```bash
sudo apt-get update
sudo apt-get install -y docker.io git wget tar

# Add user to docker group (avoid sudo)
sudo usermod -aG docker $USER
# Log out and back in for group change to take effect
```

**macOS** (x86_64 only):
```bash
brew install docker git wget
# Or install Docker Desktop from docker.com
```

**Windows WSL2**:
```bash
# In WSL2 terminal
sudo apt-get update
sudo apt-get install -y docker.io git wget tar

# Ensure Docker Desktop has WSL2 integration enabled
```

### Verify Docker

```bash
# Check Docker is running
docker info

# Test Docker permissions
docker ps

# If permission denied, add user to docker group (Linux only)
sudo usermod -aG docker $USER
# Then log out and back in
```

---

## First-Time Setup

### Clone Repository

```bash
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension
```

**Note**: The repository contains:
- Build scripts (`setup`, `build`, `package`)
- BitBake recipe overlays (`bsoe-recipes/`)
- User initialization examples (`user-init/`)
- Documentation (`docs/`)

### Run Setup Script

The setup script:
1. Builds Docker image with BrightSign OS source
2. Clones RKNN toolkit repositories
3. Clones model zoo examples

```bash
./setup -y
```

**Time**: 5-10 minutes (downloads ~2-3GB inside Docker build)

**What Happens**:
```
Setup Steps:
1. Prerequisites check ✓
2. Build Docker image with source (5-10 min)
3. Clone rknn-toolkit2 (v2.3.2)
4. Clone rknn_model_zoo (v2.3.2)
5. Verify installations
```

**Expected Output**:
```
✅ Prerequisites check passed
✅ Docker environment ready
✅ rknn-toolkit-lite2 wheel file found
✅ rknn_model_zoo yolox example found
✅ Setup completed successfully in Xm Ys
```

**Troubleshooting**:
- **Docker build fails**: Check internet, Docker daemon running
- **Disk space error**: Free up 50GB+ before retrying
- **Permission denied**: Ensure user in docker group

### What Was Downloaded?

After setup completes:

```
project/
├── toolkit/
│   ├── rknn-toolkit2/          # RKNN toolkit v2.3.2
│   │   ├── rknn-toolkit-lite2/  # ARM64 runtime (for player)
│   │   └── rknn-toolkit2/       # Full toolkit (for model compilation)
│   └── rknn_model_zoo/         # Pre-trained models v2.3.2
└── Docker image: bsoe-build    # Contains BrightSign OS source
```

The Docker image contains:
- BrightSign OS 9.1.52 source (~20GB)
- OpenEmbedded/BitBake build tools
- Cross-compilation toolchain
- Build dependencies

---

## Build Process

### Understanding the Build

The build process:
1. Applies recipe overlays from `bsoe-recipes/`
2. Cross-compiles Python + packages using BitBake
3. Extracts SDK to `./sdk/` directory

**Why so slow?** Cross-compiling ~200+ packages including:
- Python 3.8 (20+ minutes)
- OpenCV with optimizations (15+ minutes)
- PyTorch (10+ minutes)
- NumPy, SciPy, etc.

**Good news**: Subsequent builds are much faster (~5-15 min) due to BitBake caching.

### Build Full SDK

```bash
./build --extract-sdk
```

**Time**: 30-60 minutes first time

**Progress Indicators**:
```
Building brightsign-sdk...
[~15 min] Compiling Python 3.8...
[~30 min] Building OpenCV...
[~45 min] Building PyTorch...
[~60 min] Extracting SDK...
```

**What to Expect**:
- Lots of compilation output (normal)
- Warnings about QA checks (usually safe to ignore)
- Occasional pauses (downloading packages)
- Final SDK extraction to `./sdk/`

**Troubleshooting**:
- **Build fails early**: Check logs for specific package error
- **Out of memory**: Close other applications, increase Docker RAM
- **Disk space**: Monitor with `df -h`, need 50GB+ throughout
- **Network timeout**: Build will retry failed downloads

### Build Individual Packages (Optional)

For faster iteration when developing:

```bash
# Build single package
./build python3-opencv

# Clean rebuild
./build --clean python3-opencv
./build python3-opencv

# Deep clean (removes cache)
./build --distclean python3-opencv
./build python3-opencv
```

**Time**: 5-15 minutes per package

### Build Output

After successful build:

```
project/
├── sdk/                        # Cross-compiler + libraries
│   ├── sysroots/
│   │   ├── x86_64-oesdk-linux/ # Host tools
│   │   └── aarch64-oe-linux/   # Target libraries
│   │       ├── usr/bin/python3.8
│   │       ├── usr/lib/python3.8/
│   │       ├── usr/lib/librknnrt.so
│   │       └── ...
│   └── environment-setup-*     # SDK environment script
└── brightsign-x86_64-cobra-toolchain-*.sh  # SDK installer
```

The SDK contains everything needed for the extension:
- Python 3.8 runtime
- Python libraries (CV, ML, scientific)
- System libraries
- RKNN runtime

---

## Package Creation

### Install SDK Toolchain

Before packaging, install the cross-compiler toolchain:

```bash
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
```

**Time**: 2-3 minutes

This installs to `./sdk/` directory for use by the package script.

### Create Packages

```bash
./package
```

**Time**: 5 minutes

**What Happens**:
1. Creates `install/` staging directory
2. Copies SDK components (Python + libraries)
3. Copies extension scripts
4. Installs RKNN toolkit from wheel
5. Copies user-init examples
6. Creates two packages:
   - `pydev-YYYYMMDD-HHMMSS.zip` (development)
   - `ext_pydev-YYYYMMDD-HHMMSS.zip` (production)

**Package Options**:
```bash
./package --dev-only    # Development package only (faster)
./package --ext-only    # Production extension only
./package --clean       # Clean install/ directory first
./package --verify      # Run validation after packaging
```

### Package Output

Two deployment packages are created:

**Development Package** (`pydev-*.zip` ~420MB):
- Quick deployment for testing
- Installs to `/usr/local/pydev/`
- **Volatile** - lost on reboot
- No system integration
- Faster iteration

**Production Package** (`ext_pydev-*.zip` ~370MB):
- Production deployment
- Installs as extension to `/var/volatile/bsext/ext_pydev/`
- **Persistent** - survives reboots
- Auto-starts on boot
- Requires reboot to activate
- System service integration

---

## Player Preparation

Before deploying, prepare your BrightSign player.

### Update Firmware

If player is not on OS 9.1.79.3+:

1. **Download firmware**: [OS 9.1.79.3](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip)

2. **Update via DWS**:
   - Extract `.bsfw` file from zip
   - Navigate to `http://<player-ip>:8080`
   - Go to "Software Updates" tab
   - Upload `.bsfw` file
   - Reboot player

3. **Verify**:
   ```bash
   ssh brightsign@<player-ip>
   cat /etc/version  # Should show 9.1.79.3 or later
   ```

### Unsecure Player

**Warning**: Only unsecure development/test players, never production units.

**Via serial console** (115200 bps, n-8-1):

```bash
# Press Ctrl-C during boot to interrupt

=> console on
=> reboot

# On next boot, interrupt again

=> setenv SECURE_CHECKS 0
=> envsave
=> reboot
```

**Verify SSH access**:
```bash
ssh brightsign@<player-ip>
# Should connect without errors
```

### Enable Diagnostic Web Server

If DWS is not enabled:

1. In BrightAuthor:connected
2. Create/edit setup files
3. Enable "Diagnostic Web Server"
4. Publish to player

Verify DWS access: `http://<player-ip>:8080`

---

## Deployment

### Transfer Package

**Method 1: Via DWS** (recommended):
1. Open `http://<player-ip>:8080`
2. Go to "SD" tab
3. Upload `pydev-*.zip` or `ext_pydev-*.zip`

**Method 2: Via SCP**:
```bash
scp pydev-*.zip brightsign@<player-ip>:/storage/sd/
```

### Install Development Package

For quick testing and iteration:

```bash
# SSH to player
ssh brightsign@<player-ip>

# Exit BrightScript debugger to Linux shell
# (type 'exit' twice if at BrightScript prompt)

# Install
mkdir -p /usr/local/pydev && cd /usr/local/pydev
unzip /storage/sd/pydev-*.zip

# Source environment
source sh/setup_python_env

# Test
python3 -c "import cv2, torch, numpy; print('Success!')"
```

**Characteristics**:
- Location: `/usr/local/pydev/`
- Persistence: Volatile (lost on reboot)
- Auto-start: No (manual activation)
- Use case: Development, testing

### Install Production Extension

For production deployment:

```bash
# SSH to player
ssh brightsign@<player-ip>

# Install
mkdir -p /usr/local && cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh

# Reboot to activate
reboot
```

**After reboot**:
- Extension auto-starts
- Located at `/var/volatile/bsext/ext_pydev/`
- Persistent across reboots
- Managed as system service

---

## Verification

### Test Python Environment

**Development installation**:
```bash
ssh brightsign@<player-ip>
source /usr/local/pydev/sh/setup_python_env

python3 --version  # Should show 3.8.x
pip3 --version
```

**Production installation**:
```bash
ssh brightsign@<player-ip>
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

python3 --version
```

### Test Package Imports

```bash
# Core packages
python3 -c "import cv2; print(f'OpenCV: {cv2.__version__}')"
python3 -c "import numpy; print(f'NumPy: {numpy.__version__}')"
python3 -c "import torch; print(f'PyTorch: {torch.__version__}')"

# NPU runtime
python3 -c "from rknnlite.api import RKNNLite; r = RKNNLite(); print('NPU ready!')"
```

**Expected**: All imports succeed without errors.

### Run Test Script

Use the included validation script:

```bash
cd /usr/local/pydev  # Or /var/volatile/bsext/ext_pydev
source sh/setup_python_env

# Run comprehensive import test
sh/test_python_imports

# Or just critical packages
python3 examples/test_cv_packages.py
```

### Check Extension Status (Production)

```bash
# Check if running
/var/volatile/bsext/ext_pydev/bsext_init status

# View logs
tail -f /var/log/bsext-pydev.log

# Restart if needed
/var/volatile/bsext/ext_pydev/bsext_init restart
```

---

## Next Steps

### Try NPU Object Detection

Run your first NPU-accelerated inference:

See [docs/model-zoo-guide.md](model-zoo-guide.md) for complete YOLOX example.

Quick version:
```bash
# Download model (on dev machine)
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn
scp yolox_s_rk3588.rknn brightsign@<player-ip>:/usr/local/

# Run inference (on player)
ssh brightsign@<player-ip>
source /usr/local/pydev/sh/setup_python_env
python3 /usr/local/pydev/examples/test_yolox_npu.py \
        /usr/local/yolox_s_rk3588.rknn \
        /usr/local/test_image.jpg
```

### Set Up User Initialization

Enable custom initialization scripts:

1. **Enable user scripts via DWS**:
   - Navigate to `http://<player-ip>:8080` → Registry tab
   - Enter: `registry write extension bsext-pydev-enable-user-scripts true`
   - Reboot player

2. **Deploy examples**:
   ```bash
   cd user-init/tools
   ./deploy-to-player.sh <player-ip> <password>
   ```

3. **Verify**:
   ```bash
   ssh brightsign@<player-ip>
   cat /var/log/bsext-pydev.log  # Check init script ran
   ```

See [user-init/README.md](../user-init/README.md) for details.

### Explore Model Zoo

The RKNN model zoo includes 50+ pre-trained models:
- Object detection (YOLO, RetinaFace)
- Segmentation (SegFormer, U-Net)
- Classification (ResNet, MobileNet)
- Pose estimation

See [docs/model-zoo-guide.md](model-zoo-guide.md) for examples.

### Customize Build

Add your own Python packages:

See [docs/building.md](building.md) for recipe development guide.

---

## Troubleshooting

See [docs/troubleshooting.md](troubleshooting.md) for comprehensive troubleshooting guide.

**Common issues**:
- Build fails → Check logs for specific error
- Extension won't install → Verify player unsecured
- Python imports fail → Check environment sourced
- NPU init fails → Verify OS 9.1.79.3+

**Get help**:
- [FAQ.md](../FAQ.md) - Common questions
- [WORKFLOWS.md](../WORKFLOWS.md) - Common tasks
- [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues) - Report bugs

---

## Summary

You should now have:

- ✅ Development environment configured
- ✅ SDK built and packages created
- ✅ Extension deployed to player
- ✅ Python environment verified
- ✅ Ready for CV/ML development

**Time investment**: ~90 minutes first time
**Payoff**: Full Python + CV + NPU environment on BrightSign player

**Next**: Build your first CV application!
