# User Init Examples

This directory contains ready-to-deploy example files for the Python Development Extension's user initialization system.

## Files in This Directory

- **requirements.txt** - Python packages for automatic installation
- **01_validate_cv.sh** - CV environment validation script  
- **test_cv_packages.py** - Python script that tests CV/ML packages

## How to Use

### Prerequisites

User scripts must be enabled via registry first (security requirement):

```bash
# Access DWS at http://<player-ip>:8080, go to Registry tab:
registry write extension bsext-pydev-enable-user-scripts true
# Restart player for changes to take effect
```

### Quick Deployment

From the parent directory, use the deployment tool:

```bash
cd ../tools/
./deploy-to-player.sh <player-ip> [password]
```

This copies all example files to `/storage/sd/python-init/` on the player.

### Manual Deployment

```bash
# First enable user scripts via DWS Registry tab:
# registry write extension bsext-pydev-enable-user-scripts true

# Copy files to player
scp * admin@<player-ip>:/storage/sd/python-init/

# Make shell scripts executable
ssh admin@<player-ip> "chmod +x /storage/sd/python-init/*.sh"

# Restart extension to run initialization
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init restart"
```

## What Each File Does

### requirements.txt
- Example template for user-installable Python packages
- Automatically installs packages at startup
- Processed first, before any shell scripts run
- **Important**: Do NOT list SDK packages (see Pre-installed Packages below)
- **Important**: Only packages with ARM64/aarch64 wheels will install (no build system on player)

### 01_validate_cv.sh  
- Shell script that runs CV package validation
- Executes test_cv_packages.py and logs results
- Results saved to `/storage/sd/python-init/cv_test.log`
- Runs after requirements.txt installation

### test_cv_packages.py
- Python script that validates SDK environment
- Tests importing pre-installed CV/ML packages
- Called by 01_validate_cv.sh
- Reports informationally - missing optional packages don't cause failure

## Verification

After deployment, check that initialization worked:

```bash
# Check extension status
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init status"

# View CV test results
ssh admin@<player-ip> "cat /storage/sd/python-init/cv_test.log"

# Check requirements installation
ssh admin@<player-ip> "cat /storage/sd/python-init/requirements-install.log"
```

## Troubleshooting

**Scripts not running?** See the complete troubleshooting guide: [docs/troubleshooting-user-init.md](../../docs/troubleshooting-user-init.md)

**Quick checks**:
1. **User scripts enabled?** `registry read extension bsext-pydev-enable-user-scripts` (must be "true")
2. **Scripts executable?** `ls -la /storage/sd/python-init/*.sh` (should show 'x' permission)
3. **Check logs**: `tail -f /var/log/bsext-pydev.log`

## Pre-installed SDK Packages

The following packages are **already installed** in the extension and should **NOT** be listed in `requirements.txt`:

### Core Python
- Python 3.8.19 + standard library
- pip, setuptools, wheel

### Computer Vision & Image Processing
- **opencv** (cv2) - OpenCV 4.x
- **pillow** (PIL) - Image processing library
- **scikit-image** (skimage) - Image algorithms

### Machine Learning & Deep Learning
- **torch** - PyTorch 2.4.1
- **torchvision** - PyTorch vision library
- **rknnlite** - RKNN NPU acceleration toolkit

### Scientific Computing
- **numpy** - NumPy arrays
- **scipy** - SciPy scientific functions
- **pandas** - Data analysis
- **matplotlib** - Plotting
- **seaborn** - Statistical visualization

### Common Utilities
- **protobuf** - Protocol Buffers
- **flatbuffers** - FlatBuffers serialization
- **psutil** - System utilities
- **tqdm** - Progress bars
- **filelock** - File locking
- **fsspec** - Filesystem spec
- **networkx** - Graph algorithms
- **mpmath** - Math functions
- **jinja2** - Templating
- **markupsafe** - Safe string handling
- **ruamel.yaml** - YAML processing
- **pyyaml** - YAML processing
- **typing_extensions** - Type hints

## User-Installable Packages

Add to `requirements.txt` only if you need packages **not** listed above. Examples:
- **opencv-contrib-python** - Extended OpenCV modules
- **scikit-learn** - Machine learning algorithms
- **requests** - HTTP library
- **APScheduler** - Task scheduling
- **redis** - Redis client
- **pyserial** - Serial port communication

## Customization

1. **Modify requirements.txt**: Add user-installable packages (not SDK packages)
2. **Edit 01_validate_cv.sh**: Change logging behavior or add custom validation
3. **Extend test_cv_packages.py**: Add tests for user-installed packages

See the `../templates/` directory for templates to create your own scripts.