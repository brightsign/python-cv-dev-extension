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
- Automatically installs Python packages at startup
- Processed first, before any shell scripts run
- Edit to add/remove packages as needed
- **Important**: Only packages with ARM64/aarch64 wheels will install (no build system on player)

### 01_validate_cv.sh  
- Shell script that runs CV package validation
- Executes test_cv_packages.py and logs results
- Results saved to `/storage/sd/python-init/cv_test.log`
- Runs after requirements.txt installation

### test_cv_packages.py
- Python script that tests importing all CV/ML packages  
- Called by 01_validate_cv.sh
- Provides detailed pass/fail results for each package

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

## Customization

1. **Modify requirements.txt**: Add/remove Python packages
2. **Edit 01_validate_cv.sh**: Change logging behavior or add custom validation
3. **Extend test_cv_packages.py**: Add tests for additional packages

See the `../templates/` directory for templates to create your own scripts.