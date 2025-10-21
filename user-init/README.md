# User Initialization System

This directory contains tools, examples, and templates for the Python Development Extension's user initialization system.

## Directory Structure

```
user-init/
├── README.md              # This file - overview of the system
├── tools/                 # Developer tools (run on development host)
│   └── deploy-to-player.sh   # Deploys examples to BrightSign player
├── examples/              # Ready-to-deploy user-init files
│   ├── README.md             # Examples-specific documentation  
│   ├── requirements.txt      # Python packages for automatic installation
│   ├── 01_validate_cv.sh     # CV environment validation script
│   └── test_cv_packages.py   # Python CV package tests
└── templates/             # Templates for creating custom scripts
    ├── basic_script.sh       # Basic shell script template
    ├── python_wrapper.sh     # Template for running Python scripts
    └── requirements_template.txt # Template requirements file
```

## How the User Init System Works

The Python Development Extension automatically processes files in `/storage/sd/python-init/` at startup:

1. **requirements.txt**: Automatically installs Python packages via pip3
2. **Shell scripts (.sh)**: Executes scripts with executable permission using bash
3. **Execution order**: Scripts run alphabetically after requirements installation

## Registry Configuration

The extension uses registry keys to control behavior. Configure these via the player's Diagnostic Web Server (DWS) at `http://<player-ip>:8080` → Registry tab:

| Registry Key | Default | Description |
|--------------|---------|-------------|
| `bsext-pydev-disable-auto-start` | `false` | Set to `true` to prevent extension from starting automatically |
| `bsext-pydev-enable-user-scripts` | `false` | **Must be `true`** to enable user script execution (security requirement) |

**Security Note**: User scripts run as root, so they must be explicitly enabled.

## Quick Start

### Enable User Scripts (Required)

User scripts are disabled by default for security (they run as root). Enable them first:

```bash
# Access player's DWS at http://<player-ip>:8080
# Go to Registry tab and enter:
registry write extension bsext-pydev-enable-user-scripts true
# Restart player for changes to take effect
```

### Deploy Examples to Player

```bash
cd tools/
export PLAYER_IP=192.168.1.100    # Replace with your player IP
export PASSWORD="password"         # Replace with your password

./deploy-to-player.sh "${PLAYER_IP}" "${PASSWORD}"
```

This deploys all files from `examples/` to `/storage/sd/python-init/` on the player.

### Verify Deployment

```bash
# Check extension status
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init status"

# View initialization logs
ssh admin@<player-ip> "cat /var/log/bsext-pydev.log"

# Check CV test results
ssh admin@<player-ip> "cat /storage/sd/python-init/cv_test.log"
```

## Creating Custom Scripts

### 1. Use Templates

Start with a template from `templates/`:

- **basic_script.sh** - Simple shell script template
- **python_wrapper.sh** - Template for running Python scripts  
- **requirements_template.txt** - Template for package installation

### 2. Customize for Your Needs

Copy and modify templates:

```bash
# Copy template
cp templates/basic_script.sh my_custom_script.sh

# Edit for your use case
# Add your initialization logic

# Test locally if possible
bash my_custom_script.sh
```

### 3. Deploy and Test

```bash
# Copy to player
scp my_custom_script.sh admin@<player-ip>:/storage/sd/python-init/

# Make executable
ssh admin@<player-ip> "chmod +x /storage/sd/python-init/my_custom_script.sh"

# Restart extension to run script
ssh admin@<player-ip> "/var/volatile/bsext/ext_pydev/bsext_init restart"
```

## Key Features

### Automatic Package Installation
- Drop `requirements.txt` file in `/storage/sd/python-init/`
- Packages install automatically at startup
- Uses standard pip requirements format
- Installation logged to `requirements-install.log`

**Important**: PyPI packages must have pre-compiled wheels for the target platform (aarch64/ARM64). The player has no build system (no cmake, gcc, etc.), so packages requiring compilation will fail to install.

### Script Toggle Control

**Note**: `/storage/sd` is mounted with `noexec` flag, so the executable bit (`chmod +x`) has no effect on script execution. All `.sh` files in `/storage/sd/python-init/` are automatically run via `bash`.

**To disable a script**, rename it to not end in `.sh`:
```bash
# Disable a script
mv /storage/sd/python-init/01_validate_cv.sh /storage/sd/python-init/01_validate_cv.sh.disabled

# Re-enable it
mv /storage/sd/python-init/01_validate_cv.sh.disabled /storage/sd/python-init/01_validate_cv.sh
```

**Alternative**: Make file unreadable (requires root):
```bash
chmod -r /storage/sd/python-init/01_validate_cv.sh  # Disable
chmod +r /storage/sd/python-init/01_validate_cv.sh  # Enable
```

### Execution Order Control
- Scripts run in alphabetical order
- Use numeric prefixes: `01_setup.sh`, `02_validate.sh`, `99_cleanup.sh`
- Requirements installation always happens first

### Built-in Logging
- Extension logs to `/var/log/bsext-pydev.log`
- Requirements installation to `/storage/sd/python-init/requirements-install.log`
- Script output can be redirected to custom log files

## File Organization

### For Development Host
- **tools/**: Scripts for developers to run on their development machine
- **templates/**: Starting points for creating custom scripts

### For Player Deployment  
- **examples/**: Ready-to-use files that get deployed to `/storage/sd/python-init/`
- These files run automatically when the extension starts

## Best Practices

1. **Start with examples**: Use provided examples as reference
2. **Use templates**: Templates include proper error handling and logging
3. **Test incrementally**: Test simple scripts before complex ones
4. **Use descriptive names**: `setup_models.sh` vs `script.sh`
5. **Control execution order**: Use numeric prefixes when order matters
6. **Enable/disable easily**: Use executable bit for script toggling
7. **Log appropriately**: Direct output to files for debugging

## Troubleshooting

**Complete troubleshooting guide**: See [docs/troubleshooting-user-init.md](../docs/troubleshooting-user-init.md) for comprehensive diagnostics.

Common issues:
- **Scripts not running**: Verify scripts end in `.sh` and check registry settings
  (`/storage/sd` is `noexec`, so `chmod +x` doesn't matter - all `.sh` files run)
- **Package installation fails**: Check network connectivity and logs
- **Import errors**: Verify packages installed correctly
- **Execution order**: Use numeric prefixes to control order

Quick checks:
```bash
# 1. Are user scripts enabled? (MOST COMMON ISSUE)
registry read extension bsext-pydev-enable-user-scripts
# Should return "true" - if not: registry write extension bsext-pydev-enable-user-scripts true

# 2. Do scripts exist and end in .sh?
ls -la /storage/sd/python-init/*.sh
# Should list .sh files (executable bit doesn't matter - noexec filesystem)

# 3. Check logs
tail -f /var/log/bsext-pydev.log
```

For more detailed documentation, see:
- [docs/troubleshooting-user-init.md](../docs/troubleshooting-user-init.md) - Complete troubleshooting guide
- [examples/README.md](examples/README.md) - Example-specific documentation
- [templates/](templates/) - Script templates