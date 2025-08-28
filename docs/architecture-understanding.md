# BrightSign Python CV Extension - Architecture Understanding

## Critical Distinction: Three Separate Environments

### 1. Build Machine (x86_64 Host)
**Location**: Developer's machine (your local workspace)
**Purpose**: Cross-compilation and packaging
**Key Directories**:
```
/home/scott/workspace/Brightsign/python-cv-dev-extension/
├── sdk/                    # Extracted cross-compilation toolchain
├── install/                 # Staging directory for packaging
├── bsoe-recipes/           # BitBake recipe overlays
├── toolkit/                # Downloaded RKNN wheels and tools
└── build                   # Build script using Docker
```

### 2. SDK (Cross-Compilation Toolchain)
**Location**: `./sdk/` on build machine
**Purpose**: Contains target libraries and cross-compiler
**Key Structure**:
```
sdk/sysroots/
├── aarch64-oe-linux/       # Target sysroot (ARM64)
│   └── usr/
│       ├── lib/
│       │   ├── librknnrt.so    # ← Library IS here
│       │   └── python3.8/site-packages/
│       └── bin/
└── x86_64-oesdk-linux/     # Build tools (x86_64)
```

### 3. Target Player (ARM64 BrightSign Device)
**Two deployment modes**:

#### Development Mode (Volatile)
```
/usr/local/                 # Read-write, executable, volatile
├── pydev/                  # Development extraction point
│   ├── usr/
│   │   ├── lib/
│   │   │   ├── librknnrt.so
│   │   │   └── python3.8/site-packages/
│   │   └── bin/
│   └── sh/setup_python_env
└── lib64/                  # Created by setup script
    └── librknnrt.so       # Symlink to pydev/usr/lib/librknnrt.so
```

#### Production Mode (Persistent Extension)
```
/var/volatile/bsext/        # Extension mount point
└── ext_pydev/              # Extension directory
    ├── usr/
    │   ├── lib/
    │   │   ├── librknnrt.so
    │   │   └── python3.8/site-packages/
    │   └── bin/
    ├── lib64/              # Created at runtime
    │   └── librknnrt.so   # Symlink
    └── bsext_init          # Service startup script
```

---

## The Library Situation - CORRECTED UNDERSTANDING

### What You Found on the Player

```bash
# find / -name librknnrt.so
/usr/local/lib64/librknnrt.so              # ← Symlink created by setup_python_env
/usr/local/usr/lib/librknnrt.so            # ← From development extraction to /usr/local
/var/volatile/bsext/ext_npu_obj/.../       # ← From OTHER extensions
```

**Key Insight**: 
- `/usr/local/lib64/librknnrt.so` is a **symlink** created by `setup_python_env`
- `/usr/local/usr/lib/librknnrt.so` is from **development deployment** (unzip to /usr/local)
- The library **IS present and accessible**

### What pip3 freeze Shows

```
# pip3 freeze
imageio==2.6.0
numpy==1.17.4
pandas==1.0.5
...
# NO rknn-toolkit-lite2!
```

**This is the REAL problem**: The Python package `rknn-toolkit-lite2` is NOT installed!

---

## The Package Installation Problem

### Current (Broken) Flow

1. **Build time**: 
   - SDK includes librknnrt.so ✅
   - SDK does NOT include rknn-toolkit-lite2 Python package ❌

2. **Package time**:
   - `copy_rknn_wheel()` copies wheel to `install/usr/lib/python3.8/wheels/` ❌
   - Wheel is NOT extracted/installed to site-packages ❌

3. **Runtime**:
   - librknnrt.so is available ✅
   - Python package rknn-toolkit-lite2 is missing ❌
   - User expected to `pip install rknn-toolkit-lite2` (from post-init_requirements.txt)
   - But pip can't write to read-only areas!

### Why BitBake Recipe Isn't Working

The recipe `python3-rknn-toolkit2_2.3.0.bb`:
- References wrong wheel name (toolkit2 vs toolkit_lite2)
- Isn't included in the SDK build
- Isn't listed in packagegroup-rknn dependencies

---

## Correct Solution Path

### Option 1: Fix Package Script (Immediate)
Modify `package` script to extract wheel contents into site-packages:

```bash
copy_rknn_wheel() {
    # Extract wheel and install to site-packages
    local wheel_path="toolkit/rknn-toolkit2/rknn-toolkit-lite2/packages/rknn_toolkit_lite2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
    
    if [[ -f "$wheel_path" ]]; then
        # Extract to temp directory
        local temp_dir=$(mktemp -d)
        unzip -q "$wheel_path" -d "$temp_dir"
        
        # Copy to site-packages in install directory
        local site_packages="install/usr/lib/python3.8/site-packages"
        mkdir -p "$site_packages"
        
        # Copy the package and metadata
        cp -r "$temp_dir"/rknnlite "$site_packages/" 2>/dev/null || true
        cp -r "$temp_dir"/rknn_toolkit_lite2*.dist-info "$site_packages/" 2>/dev/null || true
        
        rm -rf "$temp_dir"
    fi
}
```

### Option 2: Fix BitBake Recipe (Proper)
1. Fix recipe to use correct wheel name
2. Ensure wheel is copied to correct location for BitBake
3. Add python3-rknn-toolkit2 to packagegroup-rknn
4. Rebuild SDK with package included

### Option 3: User-Space Installation (Workaround)
For existing deployments, manually extract wheel:

```bash
# On player, after sourcing environment
cd /tmp
cp /path/to/rknn_toolkit_lite2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl .
unzip rknn_toolkit_lite2*.whl
cp -r rknnlite /usr/local/lib/python3.8/site-packages/
cp -r rknn_toolkit_lite2*.dist-info /usr/local/lib/python3.8/site-packages/
```

---

## File System Constraints Summary

### Read-Only Areas
- `/usr/lib/` - System libraries (OS-managed)
- `/lib/` - System libraries
- Most of root filesystem

### Read-Write Executable
- `/usr/local/` - User software (volatile)
- `/var/volatile/` - Temporary files and extensions

### Read-Write Non-Executable
- `/storage/sd/` - Persistent storage (SD card)

---

## Development vs Production Paths

### Development Path
1. Build SDK with `./build --extract-sdk`
2. Package with `./package` → creates `pydev-*.zip`
3. Transfer to player
4. Extract to `/usr/local/` (volatile)
5. Source `setup_python_env`
6. **Lost on reboot**

### Production Path
1. Build SDK with `./build --extract-sdk`
2. Package with `./package` → creates `ext_pydev-*.zip`
3. Transfer to player
4. Install with `ext_pydev_install-lvm.sh`
5. Creates LVM volume, mounts at `/var/volatile/bsext/ext_pydev`
6. **Persists across reboots**

---

## The Real Fix Needed

**The rknn-toolkit-lite2 Python package must be pre-installed into the extension's site-packages directory during packaging, not left for runtime pip installation.**

This matches how all other Python packages (numpy, opencv, pandas) are handled - they're pre-installed in the SDK/extension, not pip-installed at runtime.