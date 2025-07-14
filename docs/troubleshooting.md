# BrightSign Python Extension Troubleshooting Guide

This guide helps diagnose and fix common issues encountered when building Python extensions for BrightSign players.

## Quick Diagnosis Flowchart

```
Build Failed?
├─ Check Docker is running
├─ Permission error? → Run with --distclean
├─ "Nothing PROVIDES"? → Missing dependency
├─ "do_compile" error? → Check build logs
├─ "Files not shipped"? → Fix FILES definition
└─ "QA Issue"? → Add INSANE_SKIP flags
```

## Common Build Errors and Solutions

### 1. Docker-Related Issues

#### Error: "docker: command not found"
```bash
# Install Docker
sudo apt-get update
sudo apt-get install docker.io
sudo usermod -aG docker $USER
# Log out and back in
```

#### Error: "Cannot connect to Docker daemon"
```bash
# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker
```

#### Error: "bsoe-build image not found"
```bash
# Build the Docker image
docker build --rm --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) -t bsoe-build .
```

### 2. Permission Errors

#### Error: "Permission denied" during build
```bash
# Clean everything and rebuild
./patch-n-build.sh --distclean python3-package

# If persists, check file ownership
ls -la brightsign-oe/build/
# Fix if needed
sudo chown -R $(id -u):$(id -g) brightsign-oe/
```

#### Error: "Pseudo abort" or "pseudo: fatal error"
```bash
# This is often transient, retry the build
./patch-n-build.sh python3-package

# If persistent, clean pseudo database
rm -rf brightsign-oe/build/tmp-glibc/cache/pseudo/
./patch-n-build.sh --clean python3-package
```

### 3. Dependency Errors

#### Error: "Nothing PROVIDES 'python3-xyz'"
**Diagnosis**: Missing dependency recipe or typo in package name

```bash
# Search for correct package name
find bsoe-recipes -name "*xyz*.bb" -o -name "*XYZ*.bb"

# Check if it's a PyPI package
pip search xyz  # or check pypi.org

# If found, create recipe
cp recipe-template.bb python3-xyz_1.0.0.bb
# Edit and add package details
```

#### Error: "ERROR: Required build target 'python3-package' has no buildable providers"
**Diagnosis**: Recipe file not found or syntax error

```bash
# Verify recipe exists
ls bsoe-recipes/meta-bs/recipes-devtools/python/python3-package*.bb

# Check recipe syntax
./check-recipe-syntax.py recipe.bb
```

### 4. Compilation Errors

#### Error: "do_compile: Execution of 'compile' failed"
**Diagnosis**: Cross-compilation issue or missing build dependencies

```bash
# Find and read the compile log
find brightsign-oe/build/tmp-glibc/work -name "log.do_compile.*" | grep python3-package
cat [log file path]

# Common fixes:
# Missing headers: Add to DEPENDS
DEPENDS += "python3-numpy-native"

# Wrong architecture: Ensure cross-compilation
# Don't use pip install in recipes!
```

#### Error: "error: Microsoft Visual C++ 14.0 is required"
**Diagnosis**: Package requires compilation but using wrong compiler

```bash
# For packages with C extensions, ensure proper toolchain
DEPENDS += "python3-setuptools-native python3-wheel-native"
inherit setuptools3

# Never use pip in cross-compilation!
```

### 5. Packaging Errors

#### Error: "Files/directories were installed but not shipped"
**Diagnosis**: FILES definition doesn't match installed files

```bash
# Find what was installed
find brightsign-oe/build/tmp-glibc/work/*/image -name "*package*"

# Update FILES definition
FILES:${PN} = " \
    ${PYTHON_SITEPACKAGES_DIR}/package_name \
    ${PYTHON_SITEPACKAGES_DIR}/package_name-${PV}.dist-info \
"
```

#### Error: "No packages to build"
**Diagnosis**: Empty or missing FILES definition

```bash
# Ensure FILES is defined
FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/*"
```

### 6. QA Errors

#### Error: "already-stripped"
```bash
# Add to recipe:
INSANE_SKIP:${PN} += "already-stripped"
```

#### Error: "arch" or "textrel"
```bash
# For binary packages:
INSANE_SKIP:${PN} += "arch textrel"
```

#### Error: "file-rdeps"
```bash
# For packages with unmet dependencies:
INSANE_SKIP:${PN} += "file-rdeps"
```

### 7. Runtime Errors

#### Error: "ModuleNotFoundError" on target device
**Diagnosis**: Missing runtime dependencies

```bash
# Add to recipe:
RDEPENDS:${PN} += "python3-numpy python3-core"
```

#### Error: "wrong ELF class: ELFCLASS32"
**Diagnosis**: Architecture mismatch

```bash
# Ensure target architecture
# Never use x86_64 wheels for ARM target!
# Use source packages with proper cross-compilation
```

## Advanced Debugging Techniques

### 1. Verbose Build Output
```bash
# Enable verbose logging
./patch-n-build.sh -v python3-package

# Or modify local.conf
echo 'BB_VERBOSE_LOGS = "1"' >> brightsign-oe/build/conf/local.conf
```

### 2. Interactive Debugging
```bash
# Enter build environment
docker run --rm -it -u "$(id -u):$(id -g)" \
    -v "$(pwd)/brightsign-oe:/home/builder/bsoe" \
    -v "$(pwd)/srv:/srv" \
    -w /home/builder/bsoe/build \
    bsoe-build bash

# Inside container
source oe-init-build-env
bitbake -c devshell python3-package
```

### 3. Dependency Graph
```bash
# Generate dependency graph
docker run --rm -u "$(id -u):$(id -g)" \
    -v "$(pwd)/brightsign-oe:/home/builder/bsoe" \
    bsoe-build bash -c "cd /home/builder/bsoe/build && bitbake -g python3-package"

# View with graphviz
dot -Tpng pn-depends.dot -o dependencies.png
```

## Prevention Strategies

1. **Always validate recipes before building**
   ```bash
   ./check-recipe-syntax.py recipe.bb
   ```

2. **Test in order: syntax → individual → full**
   ```bash
   ./check-recipe-syntax.py recipe.bb
   ./patch-n-build.sh python3-package
   ./patch-n-build.sh brightsign-sdk
   ```

3. **Use the recipe template**
   ```bash
   cp recipe-template.bb python3-newpackage_1.0.bb
   ```

4. **Check requirements first**
   ```bash
   # Verify package exists on PyPI
   pip search packagename
   # Check version compatibility
   ```

## Getting Help

### Log Locations
- **Build logs**: `brightsign-oe/build/tmp-glibc/work/*/temp/log.*`
- **Console output**: Redirect with `2>&1 | tee build.log`
- **BitBake cache**: `brightsign-oe/build/tmp-glibc/cache/`

### Information to Provide
When asking for help, include:
1. Exact error message
2. Recipe file content
3. Relevant log excerpts
4. Build command used
5. Recent changes made

### Clean Slate Recovery
If all else fails:
```bash
# Complete clean
rm -rf brightsign-oe/build/tmp-glibc
rm -rf brightsign-oe/sstate-cache
./patch-n-build.sh brightsign-sdk
```