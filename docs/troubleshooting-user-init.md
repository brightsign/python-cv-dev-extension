# User Script Initialization Troubleshooting Guide

__Problem__: Your user program isn't running automatically via the `bsext_init` / user-init system.

**Prerequisites**:

- Extension is installed (you can run your program manually)
- You want it to run automatically at player startup

This guide provides a systematic approach to diagnose why your user scripts aren't running through the initialization system.

---

## Quick Diagnostic Summary

Run this command block to get a complete status overview:

```bash
# SSH into player first
ssh brightsign@<player-ip>

# Run comprehensive status check
/var/volatile/bsext/ext_pydev/bsext_init status
echo "---"
echo "Extension logs (last 30 lines):"
tail -n 30 /var/log/bsext-pydev.log
echo "---"
echo "User init directory contents:"
ls -lah /storage/sd/python-init/ 2>/dev/null || echo "Directory does not exist"
```

**Common quick fixes**:

- **Scripts not enabled**: `registry write extension bsext-pydev-enable-user-scripts true` → Reboot
- __Script not executable__: `chmod +x /storage/sd/python-init/your_script.sh`
- **Wrong location**: Move scripts to `/storage/sd/python-init/`

---

## Complete Initialization Flow

Understanding the flow helps identify where things break:

```ini
┌─────────────────────────────────────────────────────────────┐
│ 1. PLAYER BOOTS                                             │
│    BrightSign OS starts, mounts filesystems                 │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. EXTENSION AUTO-START CHECK                              │
│    SysV init system calls: /var/volatile/bsext/ext_pydev/  │
│                           bsext_init start                  │
│                                                             │
│    ✓ Checks registry: bsext-pydev-disable-auto-start       │
│      - If "true" → STOPS HERE (extension disabled)         │
│      - If "false" or unset → Continues                      │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. EXTENSION INITIALIZATION                                 │
│    Calls: sh/init-extension                                 │
│                                                             │
│    ✓ Sources Python environment (sh/setup_python_env)      │
│    ✓ Verifies RKNN library availability                    │
│    ✓ Sets up PYTHONPATH, LD_LIBRARY_PATH, etc.            │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. USER INITIALIZATION CHECK                               │
│    Calls: sh/run-user-init                                  │
│                                                             │
│    ✓ Checks registry: bsext-pydev-enable-user-scripts      │
│      - If NOT "true" → STOPS HERE (security: disabled)     │
│      - If "true" → Continues                                │
│                                                             │
│    ✓ Checks directory: /storage/sd/python-init/            │
│      - If missing → STOPS (no scripts to run)              │
│      - If exists → Continues                                │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. REQUIREMENTS INSTALLATION (Optional)                     │
│    If requirements.txt exists:                              │
│                                                             │
│    ✓ Runs: pip3 install --only-binary=:all:                │
│              -r /storage/sd/python-init/requirements.txt    │
│    ✓ Logs to: /storage/sd/python-init/                     │
│                requirements-install.log                      │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. SHELL SCRIPT EXECUTION                                   │
│    For each *.sh file in /storage/sd/python-init/:         │
│                                                             │
│    ✓ Checks if executable (chmod +x)                       │
│      - If NOT executable → SKIPS (with message)            │
│      - If executable → Runs with bash                       │
│                                                             │
│    ✓ Executes: bash /storage/sd/python-init/script.sh      │
│    ✓ Scripts run in ALPHABETICAL order                     │
│    ✓ Scripts run as ROOT user                              │
│                                                             │
│    Output logged to: /var/log/bsext-pydev.log              │
└─────────────────────────────────────────────────────────────┘
```

---

## Systematic Troubleshooting Steps

Work through these checks in order. Each section includes copy-paste commands.

### Check 0: "Read-only file system" Error (lib64)

**Symptoms**: Extension fails with error like `mkdir: cannot create directory '/var/volatile/bsext/ext_pydev/lib64': Read-only file system`

**Cause**: You're using an **older version** of the extension that tries to create directories in the read-only production deployment location.

**Background**: Early versions (pre-October 2025) included legacy RKNN workaround code that created `/lib64/` directories. This code became obsolete when BrightSign OS 9.1.79.3+ started providing `/usr/lib/librknnrt.so` natively. The workaround code was removed in later versions.

**Fix - Upgrade Extension**:

```bash
# Get the latest extension package from your build system
# Then deploy via DWS and install:

ssh brightsign@<player-ip>
cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot
```

**Fix - Temporary Workaround** (if you cannot upgrade immediately):

Deploy as development package instead of production extension:

```bash
# Deploy pydev-*.zip (not ext_pydev-*.zip)
ssh brightsign@<player-ip>

mkdir -p /usr/local/pydev && cd /usr/local/pydev
unzip /storage/sd/pydev-*.zip
source sh/setup_python_env

# Note: This is volatile and will not persist across reboots
# User scripts must be run manually or via custom init
```

**Verification**: After upgrade, run:

```bash
/var/volatile/bsext/ext_pydev/bsext_init run

# Should NOT see "Read-only file system" errors
# Should see "RKNN Runtime library found (system - OS 9.1.79.3+)"
```

---

### Check 1: Is the Extension Installed?

**Symptoms**: Nothing works, extension commands fail

```bash
# Verify extension exists
ls -la /var/volatile/bsext/ext_pydev/

# Expected: Directory exists with usr/, sh/, bsext_init files
# If missing: Extension not installed - use ext_pydev_install-lvm.sh
```

**Fix if missing**:

```bash
# Install extension from zip file
cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot
```

---

### Check 2: Is Extension Auto-Start Disabled?

**Symptoms**: Extension exists but never initializes on boot

```bash
# Check registry setting
registry extension bsext-pydev-disable-auto-start

# Expected: Empty, "false", or error (means enabled)
# If "true", "yes", or "1": Auto-start is DISABLED
```

**Fix if disabled**:

```bash
# Enable auto-start (or delete the key)
registry extension bsext-pydev-disable-auto-start ""

# OR explicitly set to false
registry extension bsext-pydev-disable-auto-start false

# Reboot for changes to take effect
reboot
```

---

### Check 3: Is Extension Running?

**Symptoms**: Extension should be running but status is unclear

```bash
# Check extension status
/var/volatile/bsext/ext_pydev/bsext_init status

# Check extension logs
cat /var/log/bsext-pydev.log

# Manually start (for testing)
/var/volatile/bsext/ext_pydev/bsext_init run
```

**Expected output**:

```yaml
Extension installed: YES
Python binary: Found
Python version: Python 3.8.19
User scripts: ENABLED (or DISABLED)
```

**Fix if not running**:

```bash
# Try manual start to see errors
/var/volatile/bsext/ext_pydev/bsext_init run

# Check for errors in output
# If errors, review /var/log/bsext-pydev.log
```

---

### Check 4: Are User Scripts Enabled in Registry?

**Symptoms**: Extension runs but user scripts never execute

**This is the #1 most common issue!** User scripts are **disabled by default** for security (they run as root).

```bash
# Check if user scripts are enabled
registry extension bsext-pydev-enable-user-scripts

# Expected: "true", "yes", or "1"
# If empty or "false": User scripts are DISABLED
```

**Fix**:

```bash
# Enable user scripts
registry extension bsext-pydev-enable-user-scripts true

# Verify setting
registry extension bsext-pydev-enable-user-scripts

# Restart extension for change to take effect
/var/volatile/bsext/ext_pydev/bsext_init restart

# OR reboot player
reboot
```

**Security Note**: User scripts run as root. Only enable if you trust the scripts in `/storage/sd/python-init/`.

---

### Check 5: Does User Init Directory Exist?

**Symptoms**: User scripts enabled but nothing runs

```bash
# Check if directory exists
ls -la /storage/sd/python-init/

# Expected: Directory exists with your .sh files
# If missing: Create it
```

**Fix if missing**:

```bash
# Create directory
mkdir -p /storage/sd/python-init

# Verify creation
ls -la /storage/sd/python-init/
```

---

### Check 6: Are Scripts in the Correct Location?

**Symptoms**: Scripts exist but aren't found

```bash
# List all .sh files in user-init directory
ls -lah /storage/sd/python-init/*.sh

# Check if your script is listed
# Expected: Your script appears with permissions shown
```

**Fix if in wrong location**:

```bash
# Move script to correct location
mv /storage/sd/my_script.sh /storage/sd/python-init/

# Verify
ls -la /storage/sd/python-init/
```

---

### Check 7: Are Scripts Readable? (noexec Filesystem)

**IMPORTANT**: `/storage/sd` is mounted with `noexec` flag. The executable bit (`chmod +x`) **does NOT control** script execution. All `.sh` files are run via `bash` regardless of the executable bit.

**Symptoms**: Scripts present but get skipped

```bash
# Check what scripts exist
ls -lah /storage/sd/python-init/*.sh

# The executable bit (x) doesn't matter for execution!
# Scripts run if they:
# 1. End in .sh
# 2. Are readable (should be by default)
```

**Why "not executable" is misleading**: The old check used `[ -x ]` which fails on `noexec` filesystems even when the permission bit is set. **As of this fix, all `.sh` files run automatically.**

**To disable a script** (since chmod +x/-x doesn't work):

```bash
# Method 1: Rename to remove .sh extension
mv /storage/sd/python-init/script.sh /storage/sd/python-init/script.sh.disabled

# Method 2: Make unreadable (requires root)
chmod -r /storage/sd/python-init/script.sh
```

**To enable a script**:

```bash
# Method 1: Rename to add .sh extension
mv /storage/sd/python-init/script.sh.disabled /storage/sd/python-init/script.sh

# Method 2: Make readable
chmod +r /storage/sd/python-init/script.sh
```

---

### Check 8: Is Script Naming Correct?

**Symptoms**: Script exists, is executable, but never runs

```bash
# Verify script has .sh extension
ls /storage/sd/python-init/

# Scripts MUST end in .sh
# Good: my_script.sh, 01_init.sh
# Bad:  my_script, my_script.txt, my_script.py
```

**Fix if wrong extension**:

```bash
# Rename to add .sh extension
mv /storage/sd/python-init/my_script /storage/sd/python-init/my_script.sh

# Make executable
chmod +x /storage/sd/python-init/my_script.sh
```

---

### Check 9: Review Extension Logs

**Symptoms**: Scripts should run but you're not sure what's happening

```bash
# View full extension log
cat /var/log/bsext-pydev.log

# View last 50 lines
tail -n 50 /var/log/bsext-pydev.log

# Watch log in real-time (then restart extension in another session)
tail -f /var/log/bsext-pydev.log
```

**What to look for**:

- "User scripts are disabled" → Go to Check 4
- "User scripts enabled but no /storage/sd/python-init directory" → Go to Check 5
- "Skipping disabled script: your_script.sh (not executable)" → Go to Check 7
- "Running user init script: your_script.sh" → Script IS running!
- "Failed: your_script.sh (exit code: X)" → Script has errors (go to Check 10)

---

### Check 10: Test Script Manually

**Symptoms**: Script runs but fails, or you want to see detailed errors

```bash
# Source Python environment first (scripts may need this)
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Run script manually to see errors
bash /storage/sd/python-init/your_script.sh

# Check exit code
echo $?
# Expected: 0 = success, non-zero = failure

# Or run with debug output
bash -x /storage/sd/python-init/your_script.sh
```

**Common script errors**:

- **Missing shebang**: Add `#!/bin/bash` as first line
- __Python not found__: Add `source /var/volatile/bsext/ext_pydev/sh/setup_python_env`
- **Cannot execute Python from /storage/sd**: `/storage/sd` is mounted `noexec` - copy .py files to `/usr/local` or use inline Python

---

### Check 11: Verify Python Environment in Script

**Symptoms**: Script runs but Python commands fail

**Critical**: Scripts need to source the Python environment first!

```bash
# Your script MUST include:
#!/bin/bash
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Then your Python commands
python3 --version
python3 -c "import torch; print('Success')"
```

**Test Python availability**:

```bash
# After sourcing environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Test Python
which python3
python3 --version

# Test packages
python3 -c "import numpy, cv2, torch; print('All packages work')"
```

See [user-init/templates/python_wrapper.sh](../user-init/templates/python_wrapper.sh) for complete template.

---

### Check 12: Handle the noexec Mount Issue

**Symptoms**: "Permission denied" when trying to execute Python files from /storage/sd

**Issue**: `/storage/sd` is mounted with `noexec` flag - no files can execute directly from this location.

```bash
# This FAILS:
/storage/sd/python-init/my_script.py  # Permission denied

# This WORKS - Option 1: Use bash to run Python
bash -c "python3 << 'EOF'
import torch
print('Running inline Python')
EOF"

# This WORKS - Option 2: Copy .py to executable location
cp /storage/sd/my_script.py /usr/local/
chmod +x /usr/local/my_script.py
python3 /usr/local/my_script.py

# This WORKS - Option 3: Read and pipe
python3 < /storage/sd/python-init/script.py
```

See [FAQ.md](../FAQ.md#can-user-scripts-run-python-code) for more examples.

---

### Check 13: Check Script Execution Order

**Symptoms**: Scripts run in unexpected order

Scripts execute in **alphabetical order**. Use numeric prefixes to control order:

```bash
# List scripts in execution order
ls /storage/sd/python-init/*.sh | sort

# Good naming for controlled order:
# 01_setup_environment.sh
# 02_install_packages.sh
# 50_main_program.sh
# 99_cleanup.sh
```

**Rename for correct order**:

```bash
# Add numeric prefix
mv /storage/sd/python-init/setup.sh /storage/sd/python-init/01_setup.sh
mv /storage/sd/python-init/main.sh /storage/sd/python-init/50_main.sh
```

---

### Check 14: Review Requirements Installation

**Symptoms**: Python packages missing even though requirements.txt exists

```bash
# Check if requirements.txt exists
cat /storage/sd/python-init/requirements.txt

# Check installation log
cat /storage/sd/python-init/requirements-install.log

# Look for errors in log
tail -n 20 /storage/sd/python-init/requirements-install.log
```

**Common requirements.txt issues**:

- **Packages need compilation**: Use `--only-binary=:all:` (automatic in system)
- **Network issues**: Check player network connectivity
- **Wrong package names**: Verify package exists on PyPI
- **Architecture mismatch**: Must have ARM64/aarch64 wheels available

**Test package installation manually**:

```bash
# Source Python environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Try installing package manually
pip3 install --only-binary=:all: package-name

# Check if package imports
python3 -c "import package_name"
```

---

### Check 15: Verify Script Output and Logging

**Symptoms**: Script seems to run but you don't see expected output

```bash
# Check extension log for script output
cat /var/log/bsext-pydev.log | grep -A 10 "your_script.sh"

# If script creates its own logs, check those
cat /storage/sd/python-init/your_script.log  # if your script logs here

# Check if script writes to wrong location
find /tmp -name "*.log" -mtime -1  # logs from last 24 hours
```

**Add logging to your script**:

```bash
#!/bin/bash
# Redirect all output to log file
LOG_FILE="/storage/sd/python-init/my_script.log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Script starting at $(date)"
# Your commands here
echo "Script completed at $(date)"
```

---

## Complete Diagnostic Command Sequence

Run this complete diagnostic to check everything at once:

```bash
#!/bin/bash
# User-init diagnostic script - Run on BrightSign player

echo "=========================================="
echo "BrightSign Python Extension User-Init Diagnostics"
echo "=========================================="
echo ""

echo "1. Extension Installation Check"
echo "---"
if [ -d "/var/volatile/bsext/ext_pydev" ]; then
    echo "✓ Extension directory exists"
    /var/volatile/bsext/ext_pydev/bsext_init status
else
    echo "✗ Extension NOT installed"
    exit 1
fi
echo ""

echo "2. Registry Settings Check"
echo "---"
echo -n "Auto-start disabled: "
registry read extension bsext-pydev-disable-auto-start 2>/dev/null || echo "[not set - enabled]"

echo -n "User scripts enabled: "
USER_SCRIPTS=$(registry read extension bsext-pydev-enable-user-scripts 2>/dev/null)
if [[ "${USER_SCRIPTS,,}" =~ ^(true|yes|1)$ ]]; then
    echo "✓ YES"
else
    echo "✗ NO - This is likely your problem!"
    echo "  Fix: registry write extension bsext-pydev-enable-user-scripts true"
fi
echo ""

echo "3. User Init Directory Check"
echo "---"
if [ -d "/storage/sd/python-init" ]; then
    echo "✓ Directory exists"
    echo "Contents:"
    ls -lah /storage/sd/python-init/
else
    echo "✗ Directory does NOT exist"
    echo "  Fix: mkdir -p /storage/sd/python-init"
fi
echo ""

echo "4. Script Files Check"
echo "---"
if ls /storage/sd/python-init/*.sh 1>/dev/null 2>&1; then
    echo "Shell scripts found:"
    for script in /storage/sd/python-init/*.sh; do
        if [ -x "$script" ]; then
            echo "  ✓ $(basename $script) - EXECUTABLE"
        else
            echo "  ✗ $(basename $script) - NOT EXECUTABLE"
            echo "    Fix: chmod +x $script"
        fi
    done
else
    echo "No .sh scripts found in /storage/sd/python-init/"
fi
echo ""

echo "5. Requirements File Check"
echo "---"
if [ -f "/storage/sd/python-init/requirements.txt" ]; then
    echo "✓ requirements.txt exists"
    if [ -f "/storage/sd/python-init/requirements-install.log" ]; then
        echo "Last installation:"
        tail -n 3 /storage/sd/python-init/requirements-install.log
    fi
else
    echo "No requirements.txt (this is optional)"
fi
echo ""

echo "6. Recent Extension Logs"
echo "---"
tail -n 30 /var/log/bsext-pydev.log
echo ""

echo "=========================================="
echo "Diagnostic Complete"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Review any ✗ items above"
echo "2. Apply suggested fixes"
echo "3. Restart extension: /var/volatile/bsext/ext_pydev/bsext_init restart"
echo "4. Check logs: tail -f /var/log/bsext-pydev.log"
```

**Save and run**:

```bash
# Copy above script to player
vi /usr/local/diagnose_user_init.sh
# Paste content, save

# Make executable and run
chmod +x /usr/local/diagnose_user_init.sh
bash /usr/local/diagnose_user_init.sh
```

---

## Common Scenarios and Solutions

### Scenario 0: "Extension fails with 'Read-only file system' lib64 error"

**Error message**: `mkdir: cannot create directory '/var/volatile/bsext/ext_pydev/lib64': Read-only file system`

**Cause**: Using an outdated extension version with legacy RKNN workaround code

**Quick fix**:

```bash
# Build and deploy latest extension package
# On dev machine:
./build --extract-sdk
./package

# Transfer ext_pydev-*.zip to player and install
# See Check 0 above for detailed instructions
```

**Why this happens**:
- Production deployments (`/var/volatile/bsext/ext_pydev`) use read-only squashfs
- Old extension code tried to create writable directories in read-only location
- OS 9.1.79.3+ provides libraries natively - workarounds no longer needed
- Latest extension version (Oct 2025+) removes obsolete workaround code

---

### Scenario 1: "My script runs manually but not at startup"

**Most likely causes**:

1. User scripts not enabled in registry → **Check 4**
2. Script not executable → **Check 7**
3. Script doesn't source Python environment → **Check 11**

**Quick fix**:

```bash
# Enable user scripts
registry write extension bsext-pydev-enable-user-scripts true

# Make script executable
chmod +x /storage/sd/python-init/your_script.sh

# Restart extension
/var/volatile/bsext/ext_pydev/bsext_init restart
```

---

### Scenario 2: "Script runs but Python commands fail"

**Most likely causes**:

1. Missing `source setup_python_env` → __Check 11__
2. Trying to execute .py files from /storage/sd → **Check 12**
3. Missing Python packages → **Check 14**

**Quick fix - Add to top of your script**:

```bash
#!/bin/bash
# Source Python environment FIRST
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Now Python commands work
python3 --version
python3 -c "import torch; print('Success')"
```

---

### Scenario 3: "Extension says 'User scripts are disabled'"

**Cause**: Registry setting not enabled (security default)

**Fix**:

```bash
# Via DWS (http://<player-ip>:8080 → Registry tab):
registry write extension bsext-pydev-enable-user-scripts true

# Via SSH:
registry write extension bsext-pydev-enable-user-scripts true
reboot
```

---

### Scenario 4: "Script creates files but they disappear after reboot"

**Cause**: Writing to ephemeral locations (`/tmp`, `/usr/local`)

**Fix - Write to persistent storage**:

```bash
# Use /storage/sd for persistent files
OUTPUT_DIR="/storage/sd/my_app_data"
mkdir -p "$OUTPUT_DIR"

# Write files here
python3 << 'EOF'
import torch
torch.save(model, '/storage/sd/my_app_data/model.pt')
EOF
```

**Note**: `/storage/sd` is persistent but `noexec` - can't execute files from there.

---

### Scenario 5: "I don't see any output from my script"

**Cause**: Output goes to `/var/log/bsext-pydev.log` by default

**Fix - Add explicit logging**:

```bash
#!/bin/bash
LOG_FILE="/storage/sd/python-init/my_script.log"

# Redirect all output to both console and log file
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Script started at $(date)"
echo "Running my commands..."

# Your commands here

echo "Script completed at $(date)"
```

**View output**:

```bash
# System log
tail -f /var/log/bsext-pydev.log

# Your script log
tail -f /storage/sd/python-init/my_script.log
```

---

## Testing Your Script

### Step 1: Test Manually First

```bash
# SSH to player
ssh brightsign@<player-ip>

# Source environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Run your script manually
bash /storage/sd/python-init/your_script.sh

# Check exit code
echo $?  # Should be 0 for success
```

### Step 2: Test via Extension Run

```bash
# Enable user scripts if not already
registry write extension bsext-pydev-enable-user-scripts true

# Run extension in foreground (see all output)
/var/volatile/bsext/ext_pydev/bsext_init run

# Look for your script in output
```

### Step 3: Test via Restart

```bash
# Restart extension (runs in background)
/var/volatile/bsext/ext_pydev/bsext_init restart

# Watch logs
tail -f /var/log/bsext-pydev.log
```

### Step 4: Test via Reboot

```bash
# Full reboot test
reboot

# After reboot, check logs
tail -n 100 /var/log/bsext-pydev.log
```

---

## Reference Documentation

### Related Guides

- **[user-init/README.md](../user-init/README.md)** - User initialization system overview
- **[user-init/examples/README.md](../user-init/examples/README.md)** - Example scripts
- **[user-init/templates/](../user-init/templates/)** - Script templates
- **[FAQ.md](../FAQ.md)** - Frequently asked questions
- **[docs/deployment.md](../docs/deployment.md)** - Extension deployment details

### Template Scripts

- __[user-init/templates/basic_script.sh](../user-init/templates/basic_script.sh)__ - Basic shell script template
- __[user-init/templates/python_wrapper.sh](../user-init/templates/python_wrapper.sh)__ - Python execution template
- __[user-init/examples/01_validate_cv.sh](../user-init/examples/01_validate_cv.sh)__ - Working example

### Key Concepts

- **noexec mount**: `/storage/sd` cannot execute files directly - use `bash script.sh`
- __Script execution order__: Alphabetical - use numeric prefixes (01_, 02_, etc.)
- **Security default**: User scripts disabled by default (run as root)
- **Persistent storage**: `/storage/sd` persists across reboots
- **Ephemeral storage**: `/tmp`, `/usr/local` cleared on reboot

---

## Quick Reference Commands

```bash
# Enable user scripts (REQUIRED)
registry write extension bsext-pydev-enable-user-scripts true

# Check extension status
/var/volatile/bsext/ext_pydev/bsext_init status

# View logs
tail -f /var/log/bsext-pydev.log

# Make script executable
chmod +x /storage/sd/python-init/*.sh

# Test script manually
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
bash /storage/sd/python-init/your_script.sh

# Restart extension
/var/volatile/bsext/ext_pydev/bsext_init restart

# Run extension in foreground (see all output)
/var/volatile/bsext/ext_pydev/bsext_init run
```

---

## Still Having Issues?

If you've worked through this guide and scripts still don't run:

1. **Capture diagnostic output**:

```bash
# Run complete diagnostic
/var/volatile/bsext/ext_pydev/bsext_init run > /storage/sd/diagnostic.log 2>&1

# Include registry settings
registry read extension bsext-pydev-enable-user-scripts >> /storage/sd/diagnostic.log 2>&1

# Include directory listing
ls -laR /storage/sd/python-init/ >> /storage/sd/diagnostic.log 2>&1
```

2. **Review your script** against working examples in [user-init/examples/](../user-init/examples/)

3. **Check for these common mistakes**:

   - Script missing `#!/bin/bash` shebang
   - Script not sourcing Python environment
   - Script in wrong directory (must be `/storage/sd/python-init/`)
   - Script doesn't have `.sh` extension
   - Registry setting not enabled (check spelling exactly)
   - Changes made but extension not restarted

4. **Provide information when asking for help**:

   - Output from diagnostic script above
   - Your script content
   - Extension logs: `/var/log/bsext-pydev.log`
   - Expected vs actual behavior

---

**Last Updated**: 2025-10-21
**Document Version**: 1.0
