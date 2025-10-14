# Quick Start Guide

**Goal**: Get your first BrightSign Python CV extension built and deployed in 60-90 minutes.

**Prerequisites**: x86_64 Linux/Mac/Windows host, Docker installed, 16GB+ RAM, 50GB+ free disk space

> **Apple Silicon Users**: This project requires x86_64 architecture due to RKNN toolchain limitations. Use a cloud VM (AWS, Google Cloud) or x86_64 machine instead.

---

## Before You Start

Run this quick validation to avoid wasting time:

```bash
# Check your system meets requirements
uname -m                    # Must show: x86_64 (NOT arm64 or aarch64)
docker --version            # Must be installed
df -h .                     # Need 50GB+ free space
```

If any check fails, **stop now** and fix the issue first. See [Prerequisites](#prerequisites) below.

---

## Three-Command Quick Start

For the impatient - this will build everything:

```bash
# 1. Clone and setup (5-10 min)
git clone git@github.com:brightsign/python-cv-dev-extension.git
cd python-cv-dev-extension
./setup -y

# 2. Build SDK (30-60 min - go get coffee)
./build --extract-sdk

# 3. Package extension (5 min)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package
```

**Output**: Two package files ready for deployment:
- `pydev-YYYYMMDD-HHMMSS.zip` (development/testing)
- `ext_pydev-YYYYMMDD-HHMMSS.zip` (production)

---

## What Each Step Does

### Step 1: Setup (5-10 minutes)
```bash
./setup -y
```

Downloads and prepares everything needed:
- BrightSign OS source code (~2GB download inside Docker container)
- RKNN toolkit for NPU acceleration
- Model zoo examples
- Docker build environment

**What to expect**:
- Docker image build: ~5-10 minutes
- Source download happens during Docker build
- Total: ~5-10 minutes

**If it fails**: Check internet connection, Docker daemon running, disk space

### Step 2: Build SDK (30-60 minutes)
```bash
./build --extract-sdk
```

Cross-compiles Python + CV packages for ARM64:
- Python 3.8 + standard library
- OpenCV, NumPy, PyTorch, ONNX
- RKNN toolkit for NPU

**What to expect**:
- First build: 30-60 minutes (compiling everything)
- Subsequent builds: 5-15 minutes (only changed packages)
- Progress updates every few minutes
- **This is safe to background** - use `screen` or `tmux`

**If it fails**: See [Common Build Issues](#common-build-issues) below

### Step 3: Package (5 minutes)
```bash
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
./package
```

Creates deployment-ready packages:
- Installs cross-compiler toolchain
- Packages Python runtime + libraries
- Creates both dev and production packages

**What to expect**:
- SDK installation: ~2-3 minutes
- Packaging: ~2-3 minutes
- Total: ~5 minutes

**If it fails**: Check that build completed successfully, SDK exists in `./sdk/`

---

## Deploy to BrightSign Player

### Quick Deploy (Development Package)

**Requirements**:
- BrightSign Series 5 player (XT-5, Firebird, or LS-5)
- Firmware **9.1.79.3 or later** (critical for NPU support)
- Player unsecured with SSH enabled
- Network connectivity to player

**Steps**:

1. **Transfer package via DWS** (Diagnostic Web Server):
   - Open browser to `http://<player-ip>:8080`
   - Go to SD tab
   - Upload `pydev-YYYYMMDD-HHMMSS.zip`

2. **Install via SSH**:
   ```bash
   ssh brightsign@<player-ip>
   # At BrightSign prompt, exit twice to get Linux shell

   # Install development package
   mkdir -p /usr/local/pydev && cd /usr/local/pydev
   unzip /storage/sd/pydev-*.zip
   source sh/setup_python_env

   # Test it works
   python3 -c "import cv2, numpy; print('Success!')"
   ```

3. **Test NPU acceleration** (optional):
   ```bash
   cd /usr/local/pydev
   source sh/setup_python_env
   python3 -c "from rknnlite.api import RKNNLite; r = RKNNLite(); print('NPU ready!')"
   ```

**Next steps**: See [Model Zoo Examples](#model-zoo-examples) to run object detection on NPU.

### Production Deploy (Extension Package)

For permanent installation that persists across reboots:

```bash
ssh brightsign@<player-ip>
# Exit to Linux shell
mkdir -p /usr/local && cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot  # Extension activates on reboot
```

After reboot, extension auto-starts at `/var/volatile/bsext/ext_pydev/`

---

## Model Zoo Examples

Run official RKNN model zoo examples for object detection with NPU acceleration.

### Quick YOLOX Example

1. **Download model and test image** (on your dev machine):
   ```bash
   wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn
   wget https://raw.githubusercontent.com/airockchip/rknn_model_zoo/v2.3.2/examples/yolox/model/bus.jpg

   # Transfer to player
   scp yolox_s_rk3588.rknn bus.jpg brightsign@<player-ip>:/usr/local/
   ```

2. **Run inference on player**:
   ```bash
   ssh brightsign@<player-ip>

   # Set up environment
   cd /usr/local/pydev
   source sh/setup_python_env

   # Download model_zoo
   cd /usr/local
   wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.2.zip
   unzip v2.3.2.zip && mv rknn_model_zoo-2.3.2 rknn_model_zoo

   # Copy compatibility wrapper
   cp -r /usr/local/pydev/examples/py_utils /usr/local/rknn_model_zoo/examples/yolox/python/

   # Run detection
   cd /usr/local/rknn_model_zoo/examples/yolox/python
   python3 yolox.py --model_path /usr/local/yolox_s_rk3588.rknn \
                    --target rk3588 \
                    --img_folder /usr/local/ \
                    --img_save
   ```

3. **Check results**:
   ```bash
   ls -lh ./result/bus.jpg  # Result image with bounding boxes
   ```

Expected: Detects bus, people with 85-95% confidence using NPU acceleration (~10ms inference time).

---

## Common Build Issues

### Build Fails: "Not x86_64 architecture"
**Problem**: Running on Apple Silicon (arm64) or ARM system
**Fix**: Use x86_64 cloud VM or Intel/AMD machine
**Why**: RKNN toolchain only supports x86_64

### Build Fails: "Docker daemon not running"
**Problem**: Docker not started
**Fix**: Start Docker Desktop or `sudo systemctl start docker`

### Build Fails: "No space left on device"
**Problem**: Insufficient disk space
**Fix**: Free up 50GB+ space, builds are large
**Check**: `df -h .` to see available space

### Build Fails After 30 Minutes
**Problem**: BitBake errors during compilation
**Fix**:
1. Check logs: Look at error message for specific package
2. Try clean rebuild: `./build --clean <package-name>`
3. If persist: See full [Troubleshooting](README.md#troubleshooting) guide

### Player: Extension Won't Install
**Problem**: Player is secured
**Fix**: Unsecure player via serial console:
```
# Interrupt boot, then:
=> setenv SECURE_CHECKS 0
=> envsave
=> reboot
```

### Player: RKNN Init Fails
**Problem**: OS version too old
**Fix**: Upgrade to OS 9.1.79.3 or later
**Check**: `cat /etc/version` on player
**Download**: [BrightSign Firmware Downloads](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip)

---

## Next Steps

### Essential Reading
- **[README.md](README.md)** - Complete documentation
- **[docs/deployment.md](docs/deployment.md)** - Detailed deployment guide
- **[user-init/README.md](user-init/README.md)** - Custom init scripts

### Advanced Features
- **User initialization scripts**: Auto-run Python code at startup
- **Custom package installation**: Add your own Python packages to build
- **Model zoo examples**: 50+ pre-trained models for NPU
- **NPU performance**: ~10x faster inference vs CPU

### Get Help
- **Build issues**: Check [Troubleshooting](README.md#troubleshooting)
- **BitBake errors**: See [docs/troubleshooting.md](docs/troubleshooting.md)
- **Questions**: [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues)

---

## Prerequisites

### Development Host Requirements

**Architecture** (CRITICAL):
- ✅ x86_64 (Intel/AMD)
- ❌ ARM64 / Apple Silicon (M1/M2/M3)
- ❌ ARM (Raspberry Pi)

**Resources**:
- 16GB+ RAM (32GB recommended)
- 50GB+ free disk space
- Fast internet connection (3GB+ downloads)

**Software**:
- Docker (Docker Desktop or docker-ce)
- git
- wget, tar (usually pre-installed)

**Installation** (Ubuntu/Debian example):
```bash
sudo apt-get update
sudo apt-get install -y docker.io git wget
sudo usermod -aG docker $USER  # Allow docker without sudo
# Log out and back in for group change to take effect
```

### Target Player Requirements

**Hardware**:
- BrightSign Series 5: XT-5, Firebird, or LS-5
- RK3588 SoC (for NPU support)

**Firmware**:
- **Minimum**: OS 9.1.79.3 (REQUIRED for RKNN toolkit)
- **Why**: Earlier versions lack system library for NPU
- **Download**: [BrightSign firmware page](https://docs.brightsign.biz/releases/brightsign-open-source)

**Configuration**:
- Diagnostic Web Server (DWS) enabled
- SSH enabled
- Player unsecured (`SECURE_CHECKS=0`)

---

## Time Estimates

| Task | First Time | Subsequent |
|------|-----------|-----------|
| Prerequisites check | 5 min | - |
| Setup (download sources) | 5-10 min | - |
| Build SDK | 30-60 min | 5-15 min |
| Package extension | 5 min | 5 min |
| Deploy to player | 5 min | 5 min |
| Test NPU example | 10 min | 2 min |
| **Total first time** | **60-95 min** | - |

**Tips for faster builds**:
- Use `screen` or `tmux` to background long builds
- Build incrementally: test individual packages first
- Use `--clean` only when necessary (rebuilds faster)
- Cache Docker image for instant setup on next project

---

## Success Checklist

After completing quick start, you should have:

- [ ] Built SDK with Python 3.8 + CV packages
- [ ] Created development and production packages
- [ ] Deployed to BrightSign player
- [ ] Tested Python imports work
- [ ] (Optional) Ran NPU object detection example

**If any box unchecked**: See section above for that step, or consult [full documentation](README.md).

**All checked?** Congratulations! You're ready to build CV applications on BrightSign players with NPU acceleration.
