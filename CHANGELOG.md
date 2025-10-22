# Changelog

All notable changes to the BrightSign Python Extension for CV Development will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.3-alpha] - 2025-10-22

### Critical Fixes

#### Production Deployment Issues (PR #10)
- **Fixed**: Read-only filesystem errors preventing production extension startup
  - Removed obsolete lib64 workaround code (42% code reduction in RKNN setup)
  - Extension now uses OS-provided librknnrt.so directly (OS 9.1.79.3+)
  - No filesystem operations in extension code (100% compatible with read-only squashfs)
- **Fixed**: User scripts not executing on noexec filesystems
  - Changed detection logic from `-x` (executable) to `-r` (readable) test
  - All `.sh` files in `/storage/sd/python-init/` now run automatically
  - Documented noexec filesystem behavior and proper disable methods
- **Fixed**: Package installation errors in user-init examples
  - Updated test_cv_packages.py to use rknnlite (not obsolete rknn)
  - Removed pre-installed SDK packages from requirements.txt template
  - Made validation script informational (always succeeds, reports availability)

**Customer Impact**: Production deployments now work correctly on BrightSign players with user scripts executing as expected.

### Added

#### Documentation Suite (PR #9)
- **QUICKSTART.md**: 60-90 minute quick start guide for new users
  - 3-command build process with clear time expectations
  - Common issues and immediate fixes
  - Complete NPU example walkthrough
- **WORKFLOWS.md**: Copy-paste command reference for common tasks
  - First-time setup, rebuild workflows, deployment procedures
  - NPU testing, user init scripts, troubleshooting commands
- **FAQ.md**: 25+ frequently asked questions with detailed answers
  - Architecture compatibility (Apple Silicon workarounds)
  - Build optimization, package types, firmware updates
  - NPU requirements, installation procedures
- **check-prerequisites**: System validation script
  - Validates x86_64 architecture (fails fast on Apple Silicon)
  - Checks Docker, disk space (50GB+), memory (16GB+)
  - Verifies internet connectivity and required tools
- **docs/getting-started.md**: Complete first-time setup guide
- **docs/deployment.md**: Comprehensive deployment procedures
- **docs/model-zoo-guide.md**: NPU usage with 50+ pre-trained models
- **docs/troubleshooting-user-init.md**: 21+ failure point diagnostics

#### NPU/Model Zoo Support (PR #8)
- **Added**: RKNNLite compatibility layer for rknn_model_zoo examples
  - Enables official model_zoo examples to run on BrightSign
  - Patched py_utils with RKNNLite adapter (full toolkit incompatible)
  - Validated YOLOX object detection (93% accuracy on bus.jpg)
- **Added**: Complete py_utils compatibility wrapper in user-init/examples/
  - rknn_executor.py with API adaptations
  - coco_utils.py, onnx_executor.py, pytorch_executor.py
  - __init__.py package marker
- **Added**: Comprehensive session logs documenting technical decisions
  - OS 9.1.79.3 resolution breakthrough
  - Model zoo compatibility implementation
  - Production deployment fixes

### Changed

#### Documentation Restructure (PR #9)
- **README.md**: Complete rewrite with progressive disclosure
  - Reduced from 1270 lines to 456 lines (64% reduction)
  - Added visual ASCII architecture diagram
  - Feature highlights with code examples
  - Quick reference troubleshooting table
  - Links to detailed guides instead of inline detail
- **Enhanced Scripts**: build and setup scripts with rich progress indicators
  - Visual progress with Unicode box drawing
  - Time estimates (30-60min SDK, 5-15min packages)
  - Context-aware next steps suggestions
  - Enhanced error messages with immediate solutions

#### Package Architecture (PR #8)
- **Changed**: RKNNLite-only approach (removed incompatible full rknn-toolkit2)
  - Full toolkit has hardcoded /usr/lib64/ paths (x86_64-specific)
  - RKNNLite designed for ARM64 embedded targets
  - Reduced package complexity while maintaining functionality
- **Changed**: User-init examples updated to match SDK architecture
  - Test script validates actual SDK contents (18/20 packages)
  - Requirements template shows only user-installable packages
  - Clear documentation of pre-installed vs user packages

### Fixed

#### Core Stability
- Extension initialization on production deployments (read-only filesystem)
- User script execution on noexec mounts (/storage/sd)
- Package installation errors from SDK/user package conflicts
- RKNN model_zoo example compatibility (RKNNLite adapter)

#### Documentation
- Inconsistent installation instructions
- Missing troubleshooting for common deployment issues
- Unclear prerequisites and compatibility requirements
- Time expectations for build process

### Documentation

- Added 6+ comprehensive guides covering setup, deployment, NPU usage
- Enhanced README with visual architecture diagram and progressive disclosure
- Created troubleshooting guide with 21+ diagnostic procedures
- Added session logs documenting technical decisions and validation
- Improved inline script documentation with progress indicators

### Breaking Changes

- **Requires BrightSign OS 9.1.79.3+**: Earlier versions lack /usr/lib/librknnrt.so
  - Extension uses OS-provided RKNN runtime library
  - All workarounds for older OS versions removed
  - Update player firmware before deploying this version

### Migration Guide

**From v0.1.2-alpha**:
1. Ensure BrightSign players are running OS 9.1.79.3 or later
2. Rebuild extension: `./build --extract-sdk && ./package`
3. Deploy production package: `ext_pydev-YYYYMMDD-HHMMSS.zip`
4. User scripts in `/storage/sd/python-init/` now run automatically
5. Update any custom scripts using `from rknn.api import RKNN` to use `from rknnlite.api import RKNNLite`

### Known Issues

- Minor package warnings (pandas, scikit-image) due to NumPy ABI mismatch (non-blocking)
- Apple Silicon requires x86_64 cloud VM or Docker emulation (significant performance penalty)
- Build requires 50GB+ disk space and 30-60 minutes on first run

### Validation Results

- ✅ Production deployment tested on BrightSign XT-5 with OS 9.1.79.3
- ✅ User scripts execute correctly from /storage/sd/python-init/
- ✅ RKNN model_zoo YOLOX example: 93% detection confidence
- ✅ Core CV/ML packages validated: OpenCV 4.5.5, PyTorch 2.4.1, RKNNLite
- ✅ 18/20 test packages available (2 informational warnings)

### Contributors

- Scott Francis <scott.russell.francis@gmail.com>
- Claude (AI pair programming assistant)

---

## [0.1.2-alpha] - 2025-01-31

### Added
- OS 9.1.79.3 support with native librknnrt.so
- Automatic RKNN library detection and configuration
- NPU inference validation with YOLOX model

### Changed
- Removed 460+ lines of binary patching workarounds
- Simplified RKNN library setup (uses OS-provided library)

### Fixed
- RKNN initialization on BrightSign OS 9.1.79.3+
- Library path resolution for ARM64 architecture

---

## [0.1.1-alpha] - 2025-01-28

### Added
- Initial alpha release
- Python 3.8 cross-compilation for ARM64
- OpenCV, PyTorch, RKNN toolkit integration
- Comprehensive package gap analysis
- Docker-based build environment

### Known Issues
- RKNN library path issues (resolved in 0.1.2-alpha)
- Requires complex binary patching (resolved in 0.1.2-alpha)

---

[0.1.3-alpha]: https://github.com/brightsign/python-cv-dev-extension/compare/v0.1.2-alpha...v0.1.3-alpha
[0.1.2-alpha]: https://github.com/brightsign/python-cv-dev-extension/compare/v0.1.1-alpha...v0.1.2-alpha
[0.1.1-alpha]: https://github.com/brightsign/python-cv-dev-extension/releases/tag/v0.1.1-alpha
