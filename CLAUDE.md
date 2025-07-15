# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This project is a set of instructions for a 3rd party developer to create an **_Extension_** for Brightsign embedded players that will add a Python environment to the BrightSign OS.

These instructions are completely contained in the README file which includes many code blocks intended to be executed in the terminal or directly in the file using the VSCode RUNME extension. 

### About the BrightSign OS

BrightSign players are embedded linux systems where the Operating System (linux) is built using the Open Embedded tool, bitbake. Such builds are commonly referred to as Yocto builds.

The filesystem on the player is mounted read-only with only two exceptions:
- `/storage/sd` is mounted read-write, but is also `noexec`.  You can store things here to persist across reboots, but files will be generally available for anyone to inspect, so it is not secure. Additionally, the storage is on an SD Card and will be slower than the main filesystem.
- `/usr/local` is read-write AND executable.  But it is ephemeral. That is, you can store files there and execute them, but it will not persist across reboots.

### Extensions

- installed as firmware updates to a player, which will expand a `squashfs` archive into the `/var/volatile/bsext` filesystem as read-only, executable.  This location is not added to the PATH, but a special script, `bsext_init` can start any daemons.  It is a SysV init script.

## Build Process

* the project is built from build instructions that are publicly released by BrightSign in compliance with various Open Source licenses.
* the Developer will download these archives and expand them locally
* This project contains additional bitbake recipes and 'patches' to apply over the public release.
  - ALL CHANGES SHOULD BE MADE AS PATCHES IN THE `bsoe-recipes` folder and not the expanded `brightsign-oe` folder.
* an "SDK" is built using bitbake in the modified tree
* the built libraries and binaries are collected to an `install` folder which will form the input to the squashfs filesystem creation for the Extension
* a build script creates an update package from the `install` dir

### Prerequisites

- Must be built on x86_64 architecture (not ARM/Apple Silicon)
- Requires BrightSign SDK installed to `./sdk/`
- Requires Docker for model compilation

## Deploying, Testing, and Validating

* Extensions must be hand-deployed to the player and cannot be automated. I copy them, expand them, and reboot the player to get them installed.
* once installed, the extension is validated bny creating an SSH connection to the player, getting to a shell, starting python, and using the REPL to load packages, etc.
* the final test is to run the `yolox` example (python) from the `rknn_model_zoo`

## Important Notes

- Model compilation requires x86_64 host architecture
- Different video devices used per SOC platform
- Extension runs as system service on player boot
- Debug builds include symbols for GDB debugging
- Library dependencies are automatically copied to install directory

## Development Constraints and Instructions

### Build Time Management
- **CRITICAL**: BitBake builds are extremely slow (30+ minutes for full SDK)
- **Testing approach**: 
  1. Verify recipe syntax first (check imports, variables, inheritance)
  2. Test individual packages: `./build python3-packagename` (5-15 min)
  3. Only do full SDK builds when individual packages work (30+ min)
  4. Use timeouts of 900000ms (15 min) minimum for individual packages
  5. Use timeouts of 1800000ms (30 min) minimum for SDK builds
- **Avoid unnecessary clean builds**: Only remove `tmp-glibc` when recipe structure changes
- **Batch testing**: Fix multiple recipes before testing rather than test-fix-test cycles
- **Build options**:
  - `./build` - Build with patches applied (default)
  - `./build --no-patch` - Build without any patches (vanilla)

### File Modification Rules
- **NEVER** modify BrightSign OS source files directly (source exists only inside Docker container)
- **ALL** changes must be made via rsync/patches from the `bsoe-recipes` directory
- Use the `build` script to apply changes and build targets
- Source files exist at `/home/builder/bsoe/` inside container only

### Build Testing
- Use `build` script for testing changes
- Clean builds: use `./build --clean TARGET` or `./build --distclean TARGET`
  - `--clean`: Runs bitbake -c cleanall for the target
  - `--distclean`: Removes tmp-glibc and sstate-cache directories (implies --clean)
- Individual packages can be built/tested: `./build python3-package-name`
- Full SDK build: `./build brightsign-sdk` (default)

### Error Log Access
- Build error logs exist only inside the Docker container
- To access logs, use `docker exec` or run container interactively:
  ```bash
  # Run container interactively to examine logs
  docker run -it --rm \
    -v "$(pwd)/bsoe-recipes:/home/builder/patches:ro" \
    -v "$(pwd)/srv:/srv" \
    -w /home/builder/bsoe/brightsign-oe/build \
    bsoe-build bash
  ```
- Inside container, logs are at: `/home/builder/bsoe/brightsign-oe/build/tmp-glibc/work/*/temp/log.*`
- This allows reading detailed build logs and debugging specific package failures

### Python Package Recipe Guidelines
For pip-based Python packages, ensure all recipes include:

1. **Proper FILES definition** (critical for main package creation):
   ```
   FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/* /packagename* /packagename-${PV}.dist-info*"
   ```

2. **QA skip flags** for cross-compilation issues:
   ```
   INSANE_SKIP:${PN} += "already-stripped file-rdeps arch installed-vs-shipped"
   ```

3. **Stripping controls** for binary packages:
   ```
   INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
   INHIBIT_PACKAGE_STRIP = "1"
   ```

4. **Standard source-based installation pattern**:
   ```
   SRC_URI = "https://pypi.io/packages/source/${SRCNAME_FIRSTCHAR}/${SRCNAME}/${SRCNAME}-${PV}.tar.gz"
   
   inherit setuptools3
   
   # Standard setuptools3 compilation - no manual do_install needed
   # BitBake will automatically compile and install the Python package
   ```

### Cross-Compilation Considerations
- **CRITICAL: pip cannot be used in cross-compilation**: pip installs packages for the HOST architecture (x86_64) not the TARGET architecture (ARM64). Any recipes using pip will fail to work on the target device.
- **Proper cross-compilation approaches**: Use source-based recipes with setuptools3 inheritance, or pre-downloaded ARM64 wheel files
- **Architecture mismatches**: Only use pre-compiled ARM64 binaries, never x86_64 wheels
- **Binary dependencies**: Pre-compiled binaries need `file-rdeps` skip flags
- **Proprietary packages**: Use target-architecture wheel files with stub fallbacks for missing dependencies

### Common Build Issues
- **Missing main packages**: Usually caused by incorrect FILES definitions
- **QA failures**: Add appropriate INSANE_SKIP flags
- **Dependency resolution**: Ensure all Python dependencies have proper main packages
- **Pseudo errors**: Often transient, retry the build
- **File ownership warnings**: Common with pip-installed packages, can be ignored

### Build Validation
- Verify main .ipk packages are created in `brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/`
- Check package dependencies are resolved by looking for `python3-packagename_version.ipk` files
- Test clean builds by removing `tmp-glibc` directory completely

### Build Time Considerations
- **BitBake builds are VERY slow**: Full SDK builds can take 30+ minutes, individual packages 5-15 minutes
- **Use longer timeouts**: Set command timeouts to at least 900000ms (15 minutes) for individual packages, 1800000ms (30 minutes) for SDK builds
- **Avoid frequent clean builds**: Only clean `tmp-glibc` when absolutely necessary
- **Test strategy**: 
  - First verify recipe syntax and basic structure
  - Test individual packages with `./build python3-packagename` 
  - Only do full SDK builds when individual packages work
  - Use existing build cache when possible
- **Parallel development**: Work on multiple recipe fixes simultaneously, then test in batch
- **Monitor progress**: Check build logs and intermediate results rather than waiting for completion

## General Development Memories
- Test individual packages first before full SDK builds to save time
- Treat all warnings as errors
- Use `build` script for all builds
  - For clean builds: `./build --clean package-name`
  - For distclean: `./build --distclean package-name`
  - For vanilla builds (no patches): `./build --no-patch package-name`
- Validate recipes before building: `./check-recipe-syntax.py bsoe-recipes/meta-bs/recipes-devtools/python/*.bb`
- When you have successful individual package builds, test with full SDK build before committing

## Package Validation
- Validate package versions with the requirements from the rknn_model_zoo v 2.3.2 at https://github.com/airockchip/rknn_model_zoo/blob/v2.3.2/docs/requirements_cp38.txt

## Common BitBake Error Solutions

### "Nothing PROVIDES" Errors
- **Cause**: Missing dependency or incorrect package name
- **Solution**: Check DEPENDS and RDEPENDS, ensure package recipes exist
- **Quick fix**: Search for correct package name: `find bsoe-recipes -name "*packagename*.bb"`

### "do_compile: Execution failed" 
- **Cause**: Cross-compilation issues, missing build dependencies
- **Solution**: Run container interactively and check logs at `/home/builder/bsoe/brightsign-oe/build/tmp-glibc/work/*/temp/log.do_compile.*`
- **Common fixes**: Add missing DEPENDS, ensure proper cross-compilation flags

### "Files/directories were installed but not shipped"
- **Cause**: Missing or incorrect FILES definition
- **Solution**: Add proper FILES:${PN} definition including all installed paths
- **Template**: `FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/*"`

### Permission Errors
- **Cause**: Docker container permission mismatches
- **Solution**: Run `./build --distclean TARGET` to clean and rebuild
- **Prevention**: Never run scripts as root

### Recipe Validation Tools
- **Pre-build check**: `./check-recipe-syntax.py recipe.bb`
- **Validate all recipes**: `./validate`
- **Template for new recipes**: `bsoe-recipes/meta-bs/recipes-devtools/python/recipe-template.bb`

## Git Workflow Memories
- git commit after every successful build