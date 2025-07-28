# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This project is a set of instructions for a 3rd party developer to create an ___Extension___ for Brightsign embedded players that will add a Python environment to the BrightSign OS.

These instructions are completely contained in the README file which includes many code blocks intended to be executed in the terminal or directly in the file using the VSCode RUNME extension.

### About the BrightSign OS

BrightSign players are embedded linux systems where the Operating System (linux) is built using the Open Embedded tool, bitbake. Such builds are commonly referred to as Yocto builds.

The filesystem on the player is mounted read-only with only two exceptions:

- `/storage/sd` is mounted read-write, but is also `noexec`.  You can store things here to persist across reboots, but files will be generally available for anyone to inspect, so it is not secure. Additionally, the storage is on an SD Card and will be slower than the main filesystem.
- `/usr/local` is read-write AND executable.  But it is ephemeral. That is, you can store files there and execute them, but it will not persist across reboots.

### Extensions

- installed as firmware updates to a player, which will expand a `squashfs` archive into the `/var/volatile/bsext` filesystem as read-only, executable.  This location is not added to the PATH, but a special script, `bsext_init` can start any daemons.  It is a SysV init script.

## Build Process

* the `setup` tool handles downloading all external sources: brightsign source release, rknn projects, etc. This tool also creates the docker container which will hold the downloaded source.
    - downloaded source is immutable -- being in the container. but can be searched by running the container interactively or with a script.
    - if faster access to the reference source is needed, ask the user to download and expand the release.
* the `build` tool runs the docker container and the `bsbb` command (wrapper for bitbake)
   - the command `setup-patches.sh` is included in the container and can be used to overlay recipes from `bsoe-recipes` to the container's source tree.
   - all changes to the source build are accomplished via this kind of overlay.
   - **IMPORTANT**: Inside the container, use `bsbb` command (not `bitbake`) for all build operations
   - **BrightSign source tree location**: `/home/builder/bsoe/brightsign-oe/` (inside container)
   - For exploring available recipes: search the BrightSign source at `/home/scott/workspace/Brightsign/brightsign-npu-yolo-extension/brightsign-oe` (host)
* the `package` tool handles the assembly of files from the expanded SDK and other sources (rknn-toolkit) into a staging directory - `install`

### Prerequisites

- Must be built on x86_64 architecture (not ARM/Apple Silicon)
- Requires BrightSign SDK installed to `./sdk/`

## Deploying, Testing, and Validating

* Extensions must be hand-deployed to the player and cannot be automated. Ask the user to copy them, expand them, and reboot the player to get them installed.
* once installed, the extension is validated by creating an SSH connection to the player, getting to a shell, starting python, and using the REPL to load packages, etc.

### BrightSign SSH/Shell Limitations

BrightSign players use **busybox/dropbear** SSH, which has significant limitations compared to standard SSH:

- **No standard shell**: Commands like `ssh user@host "command"` don't work normally
- **Limited command execution**: Standard SSH command execution (`-c` flag behavior) is not supported
- **File operations only**: SCP works for file transfers, but interactive shell commands via SSH are limited
- **Manual interaction required**: Most administrative tasks require interactive SSH sessions rather than scripted commands

**Deployment implications**:

- Use SCP for file transfers (works normally)
- Directory creation must be done via file operations or manual SSH
- Permission changes (chmod) require manual SSH session
- Status checks and command execution require manual SSH session

## Important Notes

- Model compilation requires x86_64 host architecture
- Extension runs as system service on player boot

## Development Constraints and Instructions

### Build Time Management

- **CRITICAL**: BitBake builds are extremely slow (30+ minutes for full SDK)
- **Testing approach**:
   1. Verify recipe syntax first (check imports, variables, inheritance)
   2. Test individual packages: `./build python3-packagename` (5-15 min)
   3. Only do full SDK builds when individual packages work (30+ min)
   4. Use timeouts of 900000ms (15 min) minimum for individual packages
   5. Use timeouts of 1800000ms (30 min) minimum for SDK builds

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

- To access logs, use `docker exec` or run container interactively.  Note that any build changes -- including logs are lost when the container exits (build tool)

- Inside container, logs are at: `/home/builder/bsoe/brightsign-oe/build/tmp-glibc/work/*/temp/log.*`

- This allows reading detailed build logs and debugging specific package failures

### Python Package Recipe Guidelines

1. Prefer existing recipes in the BrightSign distribution -- if the major version is different, ask the user, but otherwise proceed
2. Use widely available recipes from OpenEmbedded or other well-known source if no Brightsign recipe is available
3. only create a custom recipe if the user approves -- be sure to advise user of sources that were searched

For pip-based Python packages, ensure all recipes include:

1. **Proper FILES definition** (critical for main package creation):

```sh
FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/* /packagename* /packagename-${PV}.dist-info*"
```

2. **QA skip flags** for cross-compilation issues:

```sh
INSANE_SKIP:${PN} += "already-stripped file-rdeps arch installed-vs-shipped"
```

3. **Stripping controls** for binary packages:

```sh
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"
```

4. **Standard source-based installation pattern**:

```sh
SRC_URI = "https://pypi.io/packages/source/${SRCNAME_FIRSTCHAR}/${SRCNAME}/${SRCNAME}-${PV}.tar.gz"

inherit setuptools3

# Standard setuptools3 compilation - no manual do_install needed
# BitBake will automatically compile and install the Python package
```

### Cross-Compilation Considerations

- __CRITICAL: pip cannot be used in cross-compilation__: pip installs packages for the HOST architecture (x86_64) not the TARGET architecture (ARM64). Any recipes using pip will fail to work on the target device.
- **Proper cross-compilation approaches**: Use source-based recipes with setuptools3 inheritance, or pre-downloaded ARM64 wheel files
- __Architecture mismatches__: Only use pre-compiled ARM64 binaries, never x86_64 wheels
- **Binary dependencies**: Pre-compiled binaries need `file-rdeps` skip flags
- **Proprietary packages**: Use target-architecture wheel files with stub fallbacks for missing dependencies

### Common Build Issues

- **Missing main packages**: Usually caused by incorrect FILES definitions
- __QA failures__: Add appropriate INSANE_SKIP flags
- **Dependency resolution**: Ensure all Python dependencies have proper main packages
- **Pseudo errors**: Often transient, retry the build
- **File ownership warnings**: Common with pip-installed packages, can be ignored

### Build Validation

- Verify main .ipk packages are created in `brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/`
- Check package dependencies are resolved by looking for `python3-packagename_version.ipk` files
- Test clean builds by removing `tmp-glibc` directory completely

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
- __Solution__: Run container interactively and check logs at `/home/builder/bsoe/brightsign-oe/build/tmp-glibc/work/*/temp/log.do_compile.*`
- **Common fixes**: Add missing DEPENDS, ensure proper cross-compilation flags

### "Files/directories were installed but not shipped"

- **Cause**: Missing or incorrect FILES definition
- **Solution**: Add proper FILES:${PN} definition including all installed paths
- __Template__: `FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/*"`

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
- **Memory**: fully test your changes with a build
- **Memory**: commit messages should use structured commits conventions

## Development Environment Memories

- the install directory is a staging directory.  when making changes, they should be made elsewhere and copied into install

## Design and Architecture Memories

- consider separation of concerns and SOLID design principles
- bear in mind separation of concerns for all scripts, instructions, and build process

## Test and Validation Memories

- Always test your recipe overlay changes by running the `build --extract-sdk` tool every time
- Fix all warnings and errors until the build comes up clean
- you cannot test directly on the player, but if testing python code, create a conda environment to most closely match the SDK versions (python 3.8) and install packages to match versions.  These tests will be incomplete, but may be helpful

## Script and Command Memories

- In all scripts use `source` instead of `.` for clarity and compatibility