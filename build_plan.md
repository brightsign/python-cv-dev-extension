# Python CV/ML Package Integration Build Plan

## Executive Summary

This document outlines the strategy to integrate Python packages from `user-init-examples/requirements.txt` into the BrightSign SDK build using BitBake recipes. After analyzing existing recipes in the BrightSign OE tree, many packages are already available.

**Current Status in brightsign-sdk.bb:**
- ✅ Already included: opencv, python3-core, python3-modules, python3-dev, python3-numpy, python3-pillow

**Package Categories (Updated):**
- ✅ **Already in SDK (5 packages)**: numpy, pillow, opencv-python, python3-core, python3-dev
- 🟢 **Easy Integration (8 packages)**: Standard OE recipes available in source tree
- ⚠️ **Moderate Complexity (5 packages)**: Custom recipes needed, manageable dependencies  
- 🔴 **High Complexity (4 packages)**: Significant build challenges, may need wheel approach
- 🎯 **Custom/Proprietary (2 packages)**: Rockchip-specific
- 🚨 **HIGH PRIORITY**: python3-pip and python3-setuptools (required for package management)

## Detailed Package Analysis

### 🚨 HIGH PRIORITY - Package Management

#### 0. python3-pip (CRITICAL)
- **Status**: ✅ RECIPE EXISTS 
- **Recipe Source**: `oe-core/meta/recipes-devtools/python/python3-pip_20.0.2.bb`
- **Dependencies**: python3-setuptools, python3-wheel
- **Issues**: None - standard package
- **Integration**: Add to SDK immediately - enables runtime package installation

#### 0.1. python3-setuptools  
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `oe-core/meta/recipes-devtools/python/python3-setuptools_45.2.0.bb`
- **Dependencies**: python3-core
- **Issues**: None - standard package
- **Integration**: Add to SDK (dependency of pip)

### ✅ Already in SDK

#### 1. numpy<=1.26.4
- **Status**: ✅ ALREADY IN SDK
- **Location**: brightsign-sdk.bb line 23

#### 2. Pillow  
- **Status**: ✅ ALREADY IN SDK
- **Location**: brightsign-sdk.bb line 24

#### 3. opencv-python>=4.5.5.64
- **Status**: ✅ ALREADY IN SDK (as opencv)
- **Location**: brightsign-sdk.bb line 19

### 🟢 Easy Integration - Standard OE Recipes Available

#### 4. protobuf==3.20.3
- **Recipe Source**: Need to search for protobuf recipe
- **Dependencies**: protobuf, protobuf-native
- **Issues**: Version pinning may require adjustment
- **Integration**: Check if available, may need custom recipe

#### 5. jinja2
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `oe-core/meta/recipes-devtools/python/python3-jinja2_2.11.3.bb`  
- **Dependencies**: python3-markupsafe
- **Issues**: None
- **Integration**: Add to SDK

#### 6. markupsafe
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `oe-core/meta/recipes-devtools/python/python3-markupsafe_1.1.1.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None
- **Integration**: Add to SDK (dependency of jinja2)

#### 7. psutil>=5.9.0
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-psutil_5.7.0.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None - system utilities binding
- **Integration**: Add meta-python layer dependency

#### 8. tqdm>=4.64.0
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-tqdm_4.43.0.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None - pure Python progress bars
- **Integration**: Add meta-python layer dependency

#### 9. typing-extensions
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-typing-extensions_3.7.4.2.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None - pure Python
- **Integration**: Add meta-python layer dependency

#### 10. filelock
- **Recipe Source**: Need to check if available
- **Dependencies**: python3-setuptools
- **Issues**: None - pure Python
- **Integration**: Add meta-python layer dependency

#### 11. fsspec
- **Recipe Source**: Need to check if available
- **Dependencies**: python3-setuptools
- **Issues**: None - filesystem interfaces
- **Integration**: Add meta-python layer dependency

#### 12. networkx
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-networkx_2.4.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None - pure Python graph library
- **Integration**: Add meta-python layer dependency

### ⚠️ Moderate Complexity - Custom Recipes Needed

#### 13. mpmath
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-mpmath_1.1.0.bb`
- **Dependencies**: python3-setuptools
- **Issues**: None - pure Python arbitrary precision math
- **Integration**: Add to SDK

#### 14. ruamel.yaml>=0.17.4
- **Status**: ✅ RECIPE EXISTS
- **Recipe Source**: `meta-oe/meta-python/recipes-devtools/python/python3-ruamel-yaml_0.16.5.bb`
- **Dependencies**: python3-setuptools
- **Issues**: Version 0.16.5 vs required >=0.17.4
- **Integration**: Add to SDK, may need version update

#### 15. scipy>=1.5.4
- **Recipe Source**: `meta-openembedded/meta-python/recipes-devtools/python/python3-scipy_*.bb`
- **Dependencies**: python3-numpy, BLAS, LAPACK, gfortran
- **Issues**: Fortran cross-compilation, large binary size (~50MB)
- **Build Impact**: Requires gfortran-cross, increases build time significantly
- **Integration**: Add meta-python dependency, ensure Fortran toolchain available

#### 16. flatbuffers
- **Recipe Source**: `meta-openembedded/meta-oe/recipes-devtools/flatbuffers/python3-flatbuffers_*.bb`
- **Dependencies**: flatbuffers, cmake, python3-setuptools
- **Issues**: CMake cross-compilation complexity
- **Integration**: Add meta-oe dependency, verify CMake cross-compilation

#### 17. fast-histogram>=0.11
- **Status**: 🚫 CURRENTLY BROKEN - custom recipe needed
- **Recipe Source**: Custom recipe needed
- **Dependencies**: python3-numpy, Cython, python3-setuptools
- **Issues**: Cython compilation, cross-compilation of C extensions
- **Integration**: Fix existing custom recipe or create new one

### 🔴 High Complexity - Significant Build Challenges

#### 18. torch>=1.10.1,<=2.1.0 (PyTorch)
- **Recipe Source**: `meta-pytorch/recipes-devtools/python/python3-torch_*.bb` (often broken)
- **Alternative**: Pre-built wheel approach
- **Dependencies**: MASSIVE - CUDA, OpenMP, BLAS, LAPACK, protobuf, 100+ libraries
- **Issues**: 
  - Extremely complex build system (1-2 hours compile time)
  - Binary size: 1GB+ 
  - Cross-compilation nearly impossible for full build
  - CUDA dependencies optional but complex
- **Build Impact**: Dramatic increase in build time and image size
- **Integration Strategy**: Use pre-built ARM64 wheel, create stub recipe

#### 19. onnx==1.14.1
- **Status**: 🚫 CURRENTLY BROKEN - build failures
- **Recipe Source**: Custom recipe exists but fails
- **Dependencies**: protobuf, cmake, python3-numpy, python3-setuptools
- **Issues**: 
  - CMake cross-compilation complexity
  - Protobuf version conflicts
  - Setup.py AttributeError with newer setuptools
- **Integration Strategy**: Fix CMake configuration or use wheel approach

#### 20. onnxruntime==1.16.0  
- **Status**: 🚫 CURRENTLY BROKEN - build failures
- **Recipe Source**: Custom recipe exists but fails
- **Dependencies**: onnx, many native libraries, potentially CUDA
- **Issues**:
  - Microsoft's complex build system
  - Cross-compilation extremely difficult
  - Large binary size
- **Integration Strategy**: Use pre-built ARM64 wheel

#### 21. tensorflow==2.8.0
- **Recipe Source**: `meta-tensorflow/recipes-devtools/python/python3-tensorflow_*.bb` (often broken)
- **Alternative**: Pre-built wheel approach  
- **Dependencies**: ENORMOUS - bazel, protobuf, grpc, 200+ libraries
- **Issues**:
  - Bazel build system incompatible with cross-compilation
  - Binary size: 500MB-1GB+
  - Build time: 3-6 hours
  - Memory requirements: 8GB+ RAM for compilation
- **Build Impact**: Makes build nearly impossible on constrained systems
- **Integration Strategy**: Use pre-built ARM64 wheel, create stub recipe

### 🎯 Custom/Proprietary

#### 22. rknn-toolkit2
- **Status**: ⚠️ PARTIALLY IMPLEMENTED - needs wheel file
- **Recipe Source**: Custom recipe exists but missing wheel file
- **Dependencies**: python3-numpy, proprietary Rockchip libraries
- **Issues**: Requires manual wheel file placement
- **Integration**: Provide wheel file, complete custom recipe

#### 23. onnxoptimizer==0.2.7
- **Recipe Source**: Custom recipe needed
- **Dependencies**: onnx (which is broken)
- **Issues**: Depends on onnx being functional first
- **Integration Strategy**: Address after onnx is resolved

## Layer Dependencies Required

### Core Layers (Already Available)
- `oe-core` - Base OpenEmbedded recipes
- `meta-bs` - BrightSign custom layer

### Additional Layers Needed
- `meta-openembedded/meta-python` - Python package recipes
- `meta-openembedded/meta-oe` - Additional tools and libraries  
- `meta-pytorch` - PyTorch recipes (if attempting source build)
- `meta-tensorflow` - TensorFlow recipes (if attempting source build)

### Layer Configuration
Add to `conf/bblayers.conf`:
```
${BSPDIR}/meta-openembedded/meta-python \
${BSPDIR}/meta-openembedded/meta-oe \
```

## Implementation Strategy

### Phase 0: CRITICAL - Package Management (Immediate)
**Packages**: python3-pip, python3-setuptools

**Actions**:
1. Add python3-pip to brightsign-sdk.bb IMMEDIATELY
2. Add python3-setuptools as dependency
3. This enables runtime package installation fallback

**Risk**: None - standard packages, required for any Python development

### Phase 1: Easy Wins (Immediate - 1-2 days)
**Packages**: protobuf, jinja2, markupsafe, psutil, tqdm, typing-extensions, networkx, mpmath, ruamel.yaml

**Actions**:
1. These recipes already exist in the source tree
2. Add to brightsign-sdk.bb TOOLCHAIN_TARGET_TASK
3. No new layers needed - already in meta-python
4. Verify package functionality

**Risk**: Low - recipes already exist, just need to include them

### Phase 2: Moderate Complexity (1-2 weeks)  
**Packages**: scipy, flatbuffers, mpmath, ruamel.yaml, fast-histogram

**Actions**:
1. Add meta-oe layer dependency  
2. Investigate and fix fast-histogram recipe
3. Test scipy with Fortran dependencies
4. Verify all packages build and function

**Risk**: Medium - build complexity, dependency chain issues

### Phase 3: High Complexity - Wheel Approach (2-3 weeks)
**Packages**: torch, tensorflow, onnx, onnxruntime, onnxoptimizer

**Strategy**: Create wheel-based recipes that:
1. Download pre-built ARM64 wheels
2. Extract and install without compilation
3. Handle dependency stubbing
4. Skip QA checks for pre-built binaries

**Actions**:
1. Research available ARM64 wheels on PyPI
2. Create stub recipes for wheel installation
3. Test import functionality
4. Document limitations (may not have full functionality)

**Risk**: High - wheel availability, dependency satisfaction, reduced functionality

### Phase 4: Custom/Proprietary (Ongoing)
**Packages**: rknn-toolkit2  

**Actions**:
1. Obtain Rockchip wheel files
2. Complete integration with librknnrt
3. Test full RKNN functionality

## Build Impact Assessment

### Storage Requirements
- **Current baseline**: ~200MB Python environment
- **After Phase 1**: +50MB (pure Python packages)
- **After Phase 2**: +200MB (scipy, scientific computing)  
- **After Phase 3**: +1.5GB (PyTorch, TensorFlow wheels)
- **Total**: ~2GB Python environment

### Build Time Impact
- **Current baseline**: 30-45 minutes
- **After Phase 1**: +5 minutes (quick Python packages)
- **After Phase 2**: +15 minutes (scipy compilation)
- **After Phase 3**: +10 minutes (wheel downloads/extraction)
- **Total**: 60-75 minutes

### Memory Requirements
- **Phase 1-2**: Current 4GB RAM sufficient
- **Phase 3**: If building from source would need 8GB+, but wheel approach avoids this

## Risk Assessment & Mitigation

### High Risks
1. **Wheel Availability**: ARM64 wheels may not exist for all packages
   - *Mitigation*: Test wheel availability first, have fallback strategies
   
2. **Dependency Conflicts**: Version mismatches between packages  
   - *Mitigation*: Careful version pinning, dependency testing
   
3. **Storage Constraints**: 2GB Python environment may be too large
   - *Mitigation*: Modular approach, optional package groups

4. **Cross-compilation Failures**: Complex packages may not cross-compile
   - *Mitigation*: Wheel approach for problematic packages

### Medium Risks  
1. **Build Time**: Extended build times may impact development cycle
   - *Mitigation*: Incremental builds, sstate caching

2. **Maintenance Burden**: Many custom recipes to maintain
   - *Mitigation*: Prefer standard OE recipes, document customizations

## Success Metrics

### Phase 1 Success Criteria
- [ ] All 10 easy packages build successfully
- [ ] Package imports work in test environment
- [ ] Build time increase <10 minutes
- [ ] No conflicts with existing packages

### Phase 2 Success Criteria  
- [ ] scipy builds with Fortran support
- [ ] fast-histogram recipe fixed
- [ ] All moderate complexity packages functional
- [ ] Storage increase <300MB total

### Phase 3 Success Criteria
- [ ] PyTorch wheel installs and imports successfully
- [ ] TensorFlow wheel installs and imports successfully
- [ ] ONNX packages functional via wheel approach
- [ ] Basic deep learning functionality verified

### Final Success Criteria
- [ ] All 23 packages pass test_cv_packages.py validation
- [ ] RKNN model zoo examples run successfully  
- [ ] Total environment size <2.5GB
- [ ] Build completes in <90 minutes

## Immediate Action - Update brightsign-sdk.bb

Add the following to TOOLCHAIN_TARGET_TASK in brightsign-sdk.bb:

```bash
TOOLCHAIN_TARGET_TASK += "\
    libstdc++ \
    opencv \
    python3-core \
    python3-modules \
    python3-dev \
    python3-numpy \
    python3-pillow \
    python3-pip \                    # CRITICAL - package management
    python3-setuptools \             # CRITICAL - pip dependency
    python3-protobuf \               # Protocol buffers
    python3-jinja2 \                 # Template engine
    python3-markupsafe \             # Jinja2 dependency
    python3-psutil \                 # System utilities
    python3-tqdm \                   # Progress bars
    python3-typing-extensions \      # Type hints
    python3-networkx \               # Graph algorithms
    python3-mpmath \                 # Arbitrary precision math
    python3-ruamel-yaml \            # YAML processing
"
```

## Recommendations

### Immediate Actions (This Week)
1. **Add python3-pip FIRST** - enables runtime package installation
2. **Add all Phase 1 packages** that have existing recipes
3. **Test build** with these additions
4. **Create custom recipes** for missing packages (filelock, fsspec, flatbuffers)

### Development Approach
1. **Incremental integration** - add packages in phases, not all at once
2. **Comprehensive testing** - verify each package before moving to next
3. **Fallback strategies** - have wheel approach ready for complex packages
4. **Documentation** - document all customizations and integration choices

### Long-term Strategy
1. **Monitor upstream** - track OE recipe improvements for complex packages
2. **Contribute back** - submit working recipes to appropriate layers
3. **Optimize size** - investigate package minimization strategies
4. **Modular deployment** - consider optional package groups for different use cases

---

*This build plan provides a structured approach to integrating all required Python packages while managing risk and complexity. The phased approach allows for early wins while tackling the most challenging packages with appropriate strategies.*