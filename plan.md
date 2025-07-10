# RKNN Toolkit2 Integration Plan for BrightSign OE

## Project Overview
Integrate Rockchip RKNN Toolkit2 Python wheels into the BrightSign SDK build system for RK3588 hardware with Python 3.8 support. The toolkit enables model conversion and runtime inference on Rockchip NPU.

## Current BrightSign OE Analysis

### Directory Structure
- **brightsign-oe/**: Main OE build directory with meta layers
- **bsoe-recipes/meta-bs/**: BrightSign proprietary recipes location
- **meta-oe/meta-python/**: 700+ existing Python package recipes
- **meta-bs/conf/machine/**: Hardware configurations for RK platforms

### Existing Patterns
- Uses **setuptools3** and **pypi** classes for Python packages
- Hardware-specific recipes in **meta-bs-husk/**
- Local recipe overrides in **bsoe-recipes/meta-bs/recipes-open/**
- Existing Rockchip support: MPP, RGA, GStreamer plugins

## Implementation Plan

### 1. New Recipe Structure
Create in `bsoe-recipes/meta-bs/recipes-open/python-rknn/`:

```
python-rknn/
├── python3-rknn-toolkit2/
│   ├── python3-rknn-toolkit2_2.3.0.bb      # Host-side model conversion
│   └── files/
│       └── rknn_toolkit2-*-x86_64.whl      # x86_64 wheel
├── python3-rknnlite2/
│   ├── python3-rknnlite2_2.3.0.bb          # Target-side runtime
│   └── files/
│       └── rknnlite2-*-aarch64.whl         # ARM64 wheel
└── packagegroup-rknn/
    └── packagegroup-rknn.bb                # Package group for easy install
```

### 2. Recipe Specifications

#### Host Toolkit Recipe (python3-rknn-toolkit2_2.3.0.bb)
- **Purpose**: Model conversion and quantization (development host)
- **Architecture**: x86_64 only (COMPATIBLE_HOST restriction)
- **Dependencies**: numpy, opencv, onnx, tensorflow, torch
- **Installation**: Direct wheel install via pip3
- **Source**: Download x86_64 wheel from RKNN toolkit2 releases

#### Target Runtime Recipe (python3-rknnlite2_2.3.0.bb)
- **Purpose**: Model inference on device
- **Architecture**: ARM64 for RK3588 targets
- **Dependencies**: numpy, opencv (minimal set)
- **Machine Compatibility**: cobra, pantera, impala (RK3588 machines)
- **Source**: Download aarch64 wheel from RKNN toolkit2 releases

#### Package Group Recipe (packagegroup-rknn.bb)
- **packagegroup-rknn**: Runtime components for target
- **packagegroup-rknn-dev**: Development components for host
- **Integration**: Easy inclusion in image recipes

### 3. Key Recipe Features

#### Wheel-based Installation
```bitbake
inherit setuptools3
DEPENDS += "${PYTHON_PN}-wheel-native ${PYTHON_PN}-pip-native"

do_install() {
    pip3 install --target=${D}${PYTHON_SITEPACKAGES_DIR} ${WORKDIR}/*.whl
}
```

#### Architecture Controls
```bitbake
# Host toolkit - x86_64 only
COMPATIBLE_HOST = "x86_64.*-linux"

# Target runtime - RK3588 machines only  
COMPATIBLE_MACHINE = "cobra|pantera|impala"
```

#### Dependency Management
```bitbake
RDEPENDS_${PN} += " \
    ${PYTHON_PN}-numpy \
    ${PYTHON_PN}-opencv \
    python3-core \
"
```

### 4. Integration Points

#### Image Recipe Updates
Add to relevant BrightSign image recipes:
```bitbake
IMAGE_INSTALL += "packagegroup-rknn"
# Or for development images:
IMAGE_INSTALL += "packagegroup-rknn-dev"
```

#### Local Configuration (local.conf)
```bitbake
# Ensure Python 3.8 compatibility
PREFERRED_VERSION_python3 = "3.8%"
PREFERRED_VERSION_python3-opencv = "4.5.5"

# Enable RKNN features
MACHINE_FEATURES += "rknn-npu"
```

### 5. Files to Create/Modify

#### New Files
1. `bsoe-recipes/meta-bs/recipes-open/python-rknn/python3-rknn-toolkit2/python3-rknn-toolkit2_2.3.0.bb`
2. `bsoe-recipes/meta-bs/recipes-open/python-rknn/python3-rknnlite2/python3-rknnlite2_2.3.0.bb`
3. `bsoe-recipes/meta-bs/recipes-open/python-rknn/packagegroup-rknn/packagegroup-rknn.bb`
4. Wheel files in respective `files/` directories

#### Modified Files
1. Target image recipes (add packagegroup-rknn)
2. `conf/local.conf` (Python version preferences)
3. Machine configurations if hardware-specific settings needed

### 6. Build Strategy

#### Development Workflow
1. **Host Build**: Build toolkit2 on x86_64 for model conversion
2. **Cross Build**: Build rknnlite2 for ARM64 target
3. **Integration**: Include in SDK image recipes
4. **Testing**: Verify NPU functionality with existing rockchip libraries

#### Version Management
- Pin wheel versions to specific RKNN toolkit2 releases
- Use consistent versioning with upstream (currently 2.3.0)
- Track Python 3.8 compatibility requirements

### 7. Dependencies & Compatibility

#### Python Environment
- **Version**: Python 3.8 (matches BrightSign standard)
- **Packages**: Leverage existing meta-python recipes
- **Wheels**: Use pre-built wheels to avoid complex ML framework builds

#### Hardware Integration
- **NPU Access**: Coordinate with existing rockchip-mpp/rga
- **Video Pipeline**: Integration with V4L2 and GStreamer
- **Memory**: Consider NPU memory allocation requirements

#### Size Optimization
- Split toolkit (host) vs runtime (target) packages
- Minimal dependencies for target runtime
- Optional development components

### 8. Testing Plan

#### Build Testing
1. Verify recipes parse correctly (`bitbake-layers show-recipes python3-rknn*`)
2. Build individual packages (`bitbake python3-rknnlite2`)
3. Build complete image with RKNN support
4. Cross-compilation verification

#### Runtime Testing  
1. Import rknnlite2 on target device
2. Load and run RKNN model files
3. NPU resource utilization
4. Integration with existing computer vision pipeline

## Implementation Priority

1. **Phase 1**: Create basic rknnlite2 runtime recipe
2. **Phase 2**: Add toolkit2 recipe for development host
3. **Phase 3**: Create package group and image integration
4. **Phase 4**: Optimization and testing

This plan provides a systematic approach to integrating RKNN toolkit2 into the BrightSign OE build system while following established patterns and maintaining compatibility with existing Rockchip hardware support.