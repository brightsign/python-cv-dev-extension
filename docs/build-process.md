# BrightSign Python Extension Build Process

This document provides a detailed guide to the build process for creating Python extensions for BrightSign players using Yocto/BitBake.

## Overview

The build process follows this high-level workflow:

1. **Recipe Development** → 2. **Validation** → 3. **Individual Package Build** → 4. **Full SDK Build** → 5. **Extension Packaging**

## Prerequisites

- x86_64 Linux host (not ARM/Apple Silicon)
- Docker installed and running
- ~50GB free disk space
- 8GB+ RAM recommended
- Build Docker image created: `docker build -t bsoe-build .`

## Step-by-Step Build Process

### 1. Recipe Development

#### Creating a New Python Package Recipe

```bash
# Copy the template
cp bsoe-recipes/meta-bs/recipes-devtools/python/recipe-template.bb \
   bsoe-recipes/meta-bs/recipes-devtools/python/python3-mypackage_1.0.0.bb

# Edit the recipe with your package details
```

#### Key Recipe Components

- **Metadata**: SUMMARY, HOMEPAGE, LICENSE
- **Source**: SRC_URI with checksums
- **Dependencies**: DEPENDS (build-time), RDEPENDS (runtime)
- **Files**: FILES:${PN} definition for package contents
- **QA Flags**: INSANE_SKIP for cross-compilation issues

### 2. Validation

#### Pre-Build Recipe Validation

```bash
# Check single recipe
./check-recipe-syntax.py bsoe-recipes/meta-bs/recipes-devtools/python/python3-mypackage_1.0.0.bb

# Check all Python recipes
./check-recipe-syntax.py bsoe-recipes/meta-bs/recipes-devtools/python/

# Strict mode (warnings as errors)
./check-recipe-syntax.py --strict recipe.bb
```

### 3. Individual Package Build

#### First Build (No Cache)

```bash
# Build with clean to ensure fresh start
./patch-n-build.sh --clean python3-mypackage

# Expected time: 5-15 minutes
```

#### Iterative Development

```bash
# Make recipe changes, then rebuild
./patch-n-build.sh python3-mypackage

# If you encounter permission errors
./patch-n-build.sh --distclean python3-mypackage
```

#### Verifying Package Creation

```bash
# Check if .ipk was created
ls brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/python3-mypackage*.ipk
```

### 4. Full SDK Build

#### When to Build Full SDK

- After all individual packages build successfully
- Before creating final extension package
- To verify inter-package dependencies

```bash
# Full SDK build (30+ minutes)
./patch-n-build.sh brightsign-sdk

# With progress monitoring
./patch-n-build.sh -v brightsign-sdk
```

### 5. Debugging Build Failures

#### Accessing Build Logs

```bash
# Find failed package work directory
find brightsign-oe/build/tmp-glibc/work -name "python3-mypackage" -type d

# View compile log
cat brightsign-oe/build/tmp-glibc/work/aarch64-oe-linux/python3-mypackage/*/temp/log.do_compile.*
```

#### Common Build Patterns

1. **Missing Dependencies**
   ```
   ERROR: Nothing PROVIDES 'python3-dependency'
   ```
   Solution: Add to DEPENDS or create recipe for missing package

2. **Compilation Errors**
   ```
   error: 'numpy/arrayobject.h' file not found
   ```
   Solution: Add `DEPENDS += "python3-numpy-native"`

3. **QA Issues**
   ```
   ERROR: QA Issue: File /usr/lib/python3.8/site-packages/module.so is already stripped
   ```
   Solution: Add `INSANE_SKIP:${PN} += "already-stripped"`

## Optimization Tips

### Parallel Recipe Development

1. Fix multiple recipes before testing
2. Use batch validation: `./check-recipe-syntax.py bsoe-recipes/meta-bs/recipes-devtools/python/*.bb`
3. Build related packages together

### Build Time Management

| Build Type | Typical Time | When to Use |
|------------|--------------|-------------|
| Individual Package | 5-15 min | During development |
| Clean Individual | 10-20 min | After major changes |
| Full SDK | 30-60 min | Final validation |
| Distclean + SDK | 45-90 min | Fixing permission issues |

### Caching Strategy

- Avoid `--clean` unless necessary
- Use `--distclean` only for permission issues
- Keep sstate-cache between builds when possible

## Build Artifacts

### Output Locations

- **IPK Packages**: `brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/`
- **SDK**: `brightsign-oe/build/tmp-glibc/deploy/sdk/`
- **Work Directories**: `brightsign-oe/build/tmp-glibc/work/`
- **Logs**: `brightsign-oe/build/tmp-glibc/work/*/temp/`

### Extension Packaging

After successful SDK build:
```bash
# Copy artifacts to install directory
./package-dev-to-target.sh

# Create extension package
./package-extension.sh
```

## Best Practices

1. **Always validate recipes before building**
2. **Test individual packages before full builds**
3. **Monitor first few minutes of builds for early failures**
4. **Keep build logs for debugging**
5. **Use version control for recipe changes**
6. **Document custom patches and modifications**

## Quick Reference

```bash
# New recipe from template
cp recipe-template.bb python3-newpkg_1.0.bb

# Validate recipe
./check-recipe-syntax.py python3-newpkg_1.0.bb

# Build individual package
./patch-n-build.sh python3-newpkg

# Clean and rebuild
./patch-n-build.sh --clean python3-newpkg

# Full SDK build
./patch-n-build.sh brightsign-sdk

# Check build artifacts
ls brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/
```