# Deployment Guide

Complete guide to deploying the BrightSign Python CV Extension to players.

**Prerequisites**: Extension packages built (see [getting-started.md](getting-started.md))

---

## Table of Contents

1. [Package Types](#package-types)
2. [Player Preparation](#player-preparation)
3. [Development Deployment](#development-deployment)
4. [Production Deployment](#production-deployment)
5. [User Initialization Scripts](#user-initialization-scripts)
6. [Verification & Testing](#verification--testing)
7. [Updates & Maintenance](#updates--maintenance)
8. [Troubleshooting](#troubleshooting)

---

## Package Types

### Development Package (`pydev-*.zip`)

**Characteristics**:
- Size: ~420MB
- Location: `/usr/local/pydev/`
- Persistence: **Volatile** (lost on reboot)
- Auto-start: No (manual activation)
- Installation: Simple unzip, no system integration
- Deployment time: ~2 minutes

**Use When**:
- Iterating on code/configuration
- Testing new packages
- Debugging issues
- Development/testing environments
- Need quick deployment cycles

**Advantages**:
- Fast deployment (no reboot needed)
- Easy to remove (just delete directory)
- Multiple versions can coexist
- No system modifications

**Disadvantages**:
- Lost on player reboot
- Must manually activate
- Not suitable for production

### Production Package (`ext_pydev-*.zip`)

**Characteristics**:
- Size: ~370MB (smaller due to squashfs compression)
- Location: `/var/volatile/bsext/ext_pydev/`
- Persistence: **Persistent** (survives reboots)
- Auto-start: Yes (systemd service)
- Installation: System extension via LVM
- Deployment time: ~5 minutes + reboot

**Use When**:
- Production deployments
- Long-term installations
- Need auto-start on boot
- Appliance/kiosk scenarios
- Final release

**Advantages**:
- Survives reboots
- Auto-starts on boot
- System service integration
- Professional deployment

**Disadvantages**:
- Requires reboot to activate
- More complex to update/remove
- System-level installation

### Which Package to Use?

| Scenario | Package Type |
|----------|-------------|
| Development/testing | Development (`pydev-*.zip`) |
| QA validation | Development initially, then Production |
| Production deployment | Production (`ext_pydev-*.zip`) |
| Demo/proof-of-concept | Development (faster iteration) |
| Customer delivery | Production (professional) |

---

## Player Preparation

### Prerequisites Checklist

Before deploying, ensure:

- [ ] **Firmware**: OS 9.1.79.3 or later
- [ ] **Network**: Player accessible from dev host
- [ ] **DWS**: Diagnostic Web Server enabled
- [ ] **SSH**: SSH access enabled
- [ ] **Security**: Player unsecured (`SECURE_CHECKS=0`)
- [ ] **Space**: Adequate storage (~500MB free)

### Update Firmware to 9.1.79.3+

**Why required**: OS 9.1.79.3+ includes `librknnrt.so` system library for NPU support.

**Check current version**:
```bash
ssh brightsign@<player-ip>
cat /etc/version
```

**If < 9.1.79.3, update**:

1. **Download firmware**:
   - XT-5: [OS 9.1.79.3](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip)

2. **Update via DWS**:
   - Extract `.bsfw` file from downloaded zip
   - Navigate to `http://<player-ip>:8080`
   - Click "Software Updates" tab
   - Click "Choose File" and select `.bsfw`
   - Click "Upload"
   - Wait for update to complete
   - Player will reboot automatically

3. **Verify**:
   ```bash
   ssh brightsign@<player-ip>
   cat /etc/version  # Should show 9.1.79.3 or later
   ```

**Alternative: Serial Console Update**:
```bash
# Connect serial cable (115200 bps, n-8-1)
# Follow on-screen firmware update instructions
```

### Unsecure Player

**Warning**: Only unsecure development/test players, NEVER production units in the field.

**Via Serial Console** (required for first-time unsecure):

1. **Connect serial cable** (115200 bps, n-8-1)

2. **Interrupt boot**:
   ```
   # Press Ctrl-C within 3 seconds of power-on
   BrightSign> console on
   BrightSign> reboot
   ```

3. **On next boot, interrupt again**:
   ```
   BrightSign> setenv SECURE_CHECKS 0
   BrightSign> envsave
   BrightSign> reboot
   ```

4. **Verify SSH access**:
   ```bash
   ssh brightsign@<player-ip>
   # Should connect successfully
   ```

**Via Existing SSH** (if player already unsecured but needs reset):
```bash
ssh brightsign@<player-ip>
# At BrightSign prompt
setenv SECURE_CHECKS 0
envsave
reboot
```

### Enable Diagnostic Web Server

**In BrightAuthor:connected**:
1. Open or create setup files
2. Go to Advanced tab
3. Enable "Diagnostic Web Server"
4. Publish to player

**Verify**:
- Open browser to `http://<player-ip>:8080`
- Should see DWS interface

---

## Development Deployment

Fast, volatile deployment for testing and iteration.

### Step 1: Transfer Package

**Method 1: Via DWS** (recommended):

1. Open `http://<player-ip>:8080` in browser
2. Click "SD" tab
3. Click "Choose File" button
4. Select `pydev-YYYYMMDD-HHMMSS.zip`
5. Click "Upload"
6. Wait for upload to complete (shows progress bar)

**Method 2: Via SCP**:
```bash
export PLAYER_IP=192.168.1.100  # Your player IP
scp pydev-*.zip brightsign@${PLAYER_IP}:/storage/sd/
```

**Method 3: Via HTTP**:
```bash
# On player
cd /storage/sd
wget http://<dev-host-ip>/<path>/pydev-*.zip
```

### Step 2: Install Package

**Via SSH**:
```bash
# Connect to player
ssh brightsign@${PLAYER_IP}

# Exit BrightScript debugger to Linux shell
# If at BrightScript prompt (>), type:
exit
exit

# Create installation directory
mkdir -p /usr/local/pydev

# Extract package
cd /usr/local/pydev
unzip /storage/sd/pydev-*.zip

# List extracted files
ls -la
```

**Expected directory structure**:
```
/usr/local/pydev/
├── bsext_init           # Service control script
├── uninstall.sh         # Uninstall script
├── usr/                 # Python runtime and libraries
│   ├── bin/python3
│   └── lib/python3.8/
├── sh/                  # Helper scripts
│   ├── setup_python_env
│   └── test_python_imports
└── examples/            # Example scripts
    ├── test_yolox_npu.py
    └── py_utils/
```

### Step 3: Activate Environment

**Source Python environment**:
```bash
source /usr/local/pydev/sh/setup_python_env
```

**What this does**:
- Sets `PATH` to include Python
- Sets `PYTHONPATH` for libraries
- Sets `LD_LIBRARY_PATH` for shared libraries
- Activates the extension environment

**Verify activation**:
```bash
which python3
# Should show: /usr/local/pydev/usr/bin/python3

python3 --version
# Should show: Python 3.8.x
```

### Step 4: Test Installation

**Quick test**:
```bash
python3 -c "import cv2, torch, numpy; print('Success!')"
```

**Comprehensive test**:
```bash
cd /usr/local/pydev
sh/test_python_imports --verbose
```

**NPU test**:
```bash
python3 -c "from rknnlite.api import RKNNLite; r = RKNNLite(); print('NPU ready!')"
```

### Step 5: Persistent Shell Setup (Optional)

To avoid sourcing environment every login:

**Add to `.profile`**:
```bash
echo 'source /usr/local/pydev/sh/setup_python_env' >> ~/.profile
```

**Or use convenience script**:
```bash
source /usr/local/pydev/sh/pydev-env
# Shorter form that auto-detects location
```

### Development Workflow

**Typical iteration cycle**:

1. **Modify code** on dev host
2. **Rebuild** specific package:
   ```bash
   ./build --clean python3-mypackage
   ./build python3-mypackage
   ```
3. **Repackage**:
   ```bash
   ./package --dev-only
   ```
4. **Transfer** new package to player
5. **Extract** over existing installation:
   ```bash
   cd /usr/local/pydev
   unzip -o /storage/sd/pydev-*.zip
   ```
6. **Test** changes immediately (no reboot)

**Time per cycle**: 10-20 minutes

---

## Production Deployment

Persistent, auto-starting deployment for production use.

### Step 1: Transfer Package

Same as development deployment (via DWS, SCP, or HTTP):

```bash
# Via SCP
scp ext_pydev-*.zip brightsign@${PLAYER_IP}:/storage/sd/

# Verify transfer
ssh brightsign@${PLAYER_IP}
ls -lh /storage/sd/ext_pydev-*.zip
```

### Step 2: Prepare for Installation

**Check available space**:
```bash
ssh brightsign@${PLAYER_IP}
df -h /usr/local
# Need ~500MB free
```

**Create installation directory**:
```bash
mkdir -p /usr/local
cd /usr/local
```

### Step 3: Extract Package

```bash
unzip /storage/sd/ext_pydev-*.zip
```

**Extracted files**:
```
/usr/local/
├── ext_pydev.squashfs           # Compressed filesystem image (~370MB)
├── ext_pydev_install-lvm.sh     # Installation script
└── ext_pydev_install-loop.sh    # Alternative installer (loop device)
```

**Verify integrity**:
```bash
ls -lh ext_pydev.squashfs
# Should be ~370MB
```

### Step 4: Run Installer

**Execute installation script**:
```bash
bash ./ext_pydev_install-lvm.sh
```

**What the installer does**:
1. Creates LVM volume (`/dev/mapper/bsos-ext_pydev`)
2. Copies squashfs image to volume
3. Configures auto-mount at `/var/volatile/bsext/ext_pydev`
4. Sets up systemd service for auto-start
5. Registers extension with BrightSign OS

**Expected output**:
```
Creating LVM volume for extension...
Copying extension image...
Configuring auto-mount...
Installation complete. Reboot required.
```

**Time**: ~2-3 minutes

### Step 5: Reboot Player

```bash
reboot
```

**After reboot**:
- Extension auto-mounts at `/var/volatile/bsext/ext_pydev/`
- Service starts automatically
- Available immediately after boot

### Step 6: Verify Production Installation

**After reboot, SSH back in**:
```bash
ssh brightsign@${PLAYER_IP}

# Check extension is mounted
df -h /var/volatile/bsext/ext_pydev

# Check service status
/var/volatile/bsext/ext_pydev/bsext_init status

# Test Python environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
python3 -c "import cv2, torch; print('Production ready!')"
```

### Production Workflow

**Updating production extension**:

1. **Build new version** on dev host
2. **Test with development package** first
3. **Create production package**:
   ```bash
   ./package --ext-only
   ```
4. **Transfer to player**
5. **Uninstall old version**:
   ```bash
   /var/volatile/bsext/ext_pydev/uninstall.sh
   reboot
   ```
6. **Install new version** (follow Step 2-5 above)

**Time per update**: 10-15 minutes

---

## User Initialization Scripts

Deploy custom initialization scripts for automatic package installation and configuration.

### Prerequisites

**Enable user scripts** (disabled by default for security):

1. Access DWS: `http://<player-ip>:8080`
2. Go to "Registry" tab
3. Enter command:
   ```
   registry write extension bsext-pydev-enable-user-scripts true
   ```
4. Click "Submit"
5. **Reboot player** for changes to take effect

**Why disabled by default**: User scripts run as root. Explicit enablement required.

### Deploy User Init Files

**Quick deployment** (recommended):
```bash
cd user-init/tools
export PLAYER_IP=192.168.1.100
export PASSWORD=password

./deploy-to-player.sh ${PLAYER_IP} ${PASSWORD}
```

**Manual deployment**:
```bash
# Copy files to player
scp user-init/examples/* brightsign@${PLAYER_IP}:/storage/sd/python-init/

# Make scripts executable
ssh brightsign@${PLAYER_IP}
chmod +x /storage/sd/python-init/*.sh
```

### User Init File Types

**requirements.txt** (auto-installs Python packages):
```
# /storage/sd/python-init/requirements.txt
opencv-contrib-python
scikit-learn==1.3.0
requests
```

**Shell scripts** (custom initialization):
```bash
# /storage/sd/python-init/01_setup_models.sh
#!/bin/bash
mkdir -p /usr/local/models
wget -O /usr/local/models/yolox.rknn https://example.com/yolox.rknn
```

**Execution order**:
1. `requirements.txt` → auto-install packages
2. `*.sh` scripts → run in alphabetical order

### Restart to Apply

**Restart extension** (development):
```bash
cd /usr/local/pydev
./bsext_init restart
```

**Reboot player** (production):
```bash
reboot
```

### Verify User Init

**Check logs**:
```bash
# Extension log
cat /var/log/bsext-pydev.log

# Requirements install log
cat /storage/sd/python-init/requirements-install.log

# Custom script logs (if scripts redirect output)
cat /storage/sd/python-init/*.log
```

**See**: [user-init/README.md](../user-init/README.md) for complete guide.

---

## Verification & Testing

### Extension Status

**Check if running** (production):
```bash
/var/volatile/bsext/ext_pydev/bsext_init status
```

**Expected output**:
```
Extension is running (PID: xxxxx)
```

### Service Control

**Start/stop/restart** (production):
```bash
/var/volatile/bsext/ext_pydev/bsext_init start
/var/volatile/bsext/ext_pydev/bsext_init stop
/var/volatile/bsext/ext_pydev/bsext_init restart
```

**Run in foreground** (debugging):
```bash
/var/volatile/bsext/ext_pydev/bsext_init run
# Ctrl-C to stop
```

### Python Environment Test

**Quick validation**:
```bash
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Test Python version
python3 --version

# Test key packages
python3 << 'EOF'
import cv2
import torch
import numpy as np
from rknnlite.api import RKNNLite

print(f"OpenCV: {cv2.__version__}")
print(f"PyTorch: {torch.__version__}")
print(f"NumPy: {np.__version__}")
print("RKNNLite: Available")
print("\nAll packages imported successfully!")
EOF
```

### Comprehensive Test

**Run test script**:
```bash
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
/var/volatile/bsext/ext_pydev/sh/test_python_imports --verbose
```

**Or Python test**:
```bash
python3 /var/volatile/bsext/ext_pydev/examples/test_cv_packages.py
```

### NPU Validation

**Test NPU initialization**:
```bash
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

python3 << 'EOF'
from rknnlite.api import RKNNLite

rknn = RKNNLite()
print("RKNNLite object created successfully")

# This will show error about model not loaded (expected)
# But proves NPU runtime is accessible
try:
    rknn.init_runtime()
except Exception as e:
    if "Model is not loaded" in str(e):
        print("NPU runtime accessible (model not loaded error is expected)")
    else:
        print(f"Unexpected error: {e}")
EOF
```

**Run complete NPU test** (requires model):
```bash
# Download test model first
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn -O /tmp/test.rknn

# Run test
python3 /var/volatile/bsext/ext_pydev/examples/test_yolox_npu.py /tmp/test.rknn /path/to/image.jpg
```

---

## Updates & Maintenance

### Update Development Package

**Simple overwrite**:
```bash
# Build new package on dev host
./build --extract-sdk
./package --dev-only

# Transfer and extract
scp pydev-*.zip brightsign@${PLAYER_IP}:/storage/sd/
ssh brightsign@${PLAYER_IP}

cd /usr/local/pydev
unzip -o /storage/sd/pydev-*.zip  # -o to overwrite
source sh/setup_python_env
```

### Update Production Extension

**Full reinstall** (recommended):

1. **Uninstall old version**:
   ```bash
   # Copy uninstall script to safe location
   cp /var/volatile/bsext/ext_pydev/uninstall.sh /tmp/
   chmod +x /tmp/uninstall.sh

   # Run uninstall
   /tmp/uninstall.sh

   # Reboot
   reboot
   ```

2. **Install new version** (after reboot):
   ```bash
   cd /usr/local
   unzip /storage/sd/ext_pydev-NEW.zip
   bash ./ext_pydev_install-lvm.sh
   reboot
   ```

### Add/Update Python Packages

**Method 1: Runtime install** (volatile):
```bash
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
pip3 install --break-system-packages <package>
# Lost on reboot
```

**Method 2: requirements.txt** (auto-install):
```bash
echo "<package>==<version>" >> /storage/sd/python-init/requirements.txt
# Installs on next reboot
```

**Method 3: Rebuild SDK** (permanent):
```bash
# On dev host
# Add recipe to bsoe-recipes/meta-bs/recipes-devtools/python/
./build python3-<package>
./build --extract-sdk
./package
# Deploy new package
```

---

## Troubleshooting

### Extension Won't Install

**Symptoms**: `ext_pydev_install-lvm.sh` fails

**Common causes**:
1. **Player is secured**:
   ```bash
   # Check
   ssh brightsign@<player-ip>
   # If fails, player is secured

   # Fix: Unsecure via serial console
   ```

2. **Insufficient space**:
   ```bash
   df -h /usr/local
   # Need 500MB+ free

   # Fix: Remove old files, clear cache
   ```

3. **LVM volume exists** (from previous install):
   ```bash
   # Check
   lvs | grep ext_pydev

   # Fix: Remove old volume
   /tmp/uninstall.sh
   ```

### Extension Won't Start

**Symptoms**: `bsext_init status` shows not running

**Check logs**:
```bash
tail -f /var/log/bsext-pydev.log
```

**Common causes**:
1. **Missing dependencies**:
   - Check log for import errors
   - Verify OS version 9.1.79.3+

2. **Permissions**:
   ```bash
   ls -la /var/volatile/bsext/ext_pydev/bsext_init
   # Should be executable

   chmod +x /var/volatile/bsext/ext_pydev/bsext_init
   ```

3. **Mount failed**:
   ```bash
   mount | grep ext_pydev
   # Should show mount at /var/volatile/bsext/ext_pydev

   # Remount
   mount -a
   ```

### Python Import Errors

**Symptoms**: `ModuleNotFoundError`

**Checklist**:
1. **Environment sourced**:
   ```bash
   source /var/volatile/bsext/ext_pydev/sh/setup_python_env
   echo $PYTHONPATH  # Should include extension path
   ```

2. **Using correct Python**:
   ```bash
   which python3
   # Should show: /var/volatile/bsext/ext_pydev/usr/bin/python3
   ```

3. **Package in SDK**:
   ```bash
   ls /var/volatile/bsext/ext_pydev/usr/lib/python3.8/site-packages/
   # Check if package directory exists
   ```

### User Scripts Not Running

**Symptoms**: Scripts in `/storage/sd/python-init/` don't execute

**Checklist**:
1. **User scripts enabled**:
   ```bash
   # Via DWS Registry tab
   registry read extension bsext-pydev-enable-user-scripts
   # Should show: true
   ```

2. **Scripts executable**:
   ```bash
   ls -la /storage/sd/python-init/*.sh
   # Should have +x permission

   chmod +x /storage/sd/python-init/*.sh
   ```

3. **Check logs**:
   ```bash
   cat /var/log/bsext-pydev.log
   # Shows which scripts ran and any errors
   ```

### NPU Initialization Fails

**Symptoms**: `Exception: Can not find dynamic library`

**Causes**:
1. **OS version too old**:
   ```bash
   cat /etc/version
   # Must be 9.1.79.3+

   # Fix: Update firmware
   ```

2. **Library missing**:
   ```bash
   ls -la /usr/lib/librknnrt.so
   # Should exist on 9.1.79.3+
   ```

**See**: [FAQ.md](../FAQ.md) for more troubleshooting.

---

## Summary Checklist

### Development Deployment
- [ ] Package transferred to player
- [ ] Extracted to `/usr/local/pydev/`
- [ ] Environment sourced
- [ ] Python imports tested
- [ ] NPU initialized successfully

### Production Deployment
- [ ] Player unsecured
- [ ] Firmware 9.1.79.3+
- [ ] Package transferred
- [ ] Installer executed successfully
- [ ] Player rebooted
- [ ] Extension mounted and running
- [ ] Service status confirmed
- [ ] Python environment tested

### User Initialization
- [ ] User scripts enabled in registry
- [ ] Files deployed to `/storage/sd/python-init/`
- [ ] Scripts made executable
- [ ] Extension restarted
- [ ] Logs checked for errors

---

**Next Steps**:
- [Model Zoo Examples](model-zoo-guide.md) - Run NPU-accelerated inference
- [User Init Guide](../user-init/README.md) - Custom initialization
- [Troubleshooting](troubleshooting.md) - Solve common issues
