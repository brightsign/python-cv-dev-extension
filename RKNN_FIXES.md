# RKNN Python Extension Fixes

This document outlines the fixes applied to resolve the RKNN Python runtime issues encountered when running sample RKNN applications.

## Issues Addressed

### 1. Missing RKNN Runtime Library (`librknnrt.so`)

**Problem**: The RKNN Python toolkit was looking for `librknnrt.so` in `/usr/lib64/` but the library was not included in the build.

**Solution**: 
- Created new recipe `bsoe-recipes/meta-bs/recipes-open/librknnrt/librknnrt_2.3.2.bb`
- Downloads the official RKNN runtime library from Rockchip's repository
- Installs the library in both `/usr/lib` and `/usr/lib64` locations
- Added `librknnrt` to the SDK build dependencies

### 2. Python Package Version Conflicts

**Problem**: Several Python packages had version mismatches with RKNN toolkit requirements:
- `torch` needed to be ≤2.2.0 (user had 2.4.1)
- Missing packages: `fast-histogram`, `onnx==1.16.1`, `onnxoptimizer==0.2.7`, `onnxruntime>=1.16.0`, `scipy>=1.5.4`, `tqdm>=4.64.0`

**Solution**: Created BitBake recipes for all missing packages:
- `python3-torch_2.2.0.bb` - Compatible PyTorch version
- `python3-onnx_1.16.1.bb` - Exact ONNX version required
- `python3-onnxruntime_1.16.0.bb` - ONNX Runtime for inference
- `python3-onnxoptimizer_0.2.7.bb` - ONNX model optimization
- `python3-fast-histogram_0.11.bb` - Fast histogram calculations
- `python3-scipy_1.10.1.bb` - Scientific computing library  
- `python3-tqdm_4.64.1.bb` - Progress bars

### 3. Library Path Configuration Issues

**Problem**: The RKNN Python toolkit couldn't find the runtime library even when present due to incorrect library paths.

**Solution**: Enhanced the setup script (`sh/setup_python_env`) to:
- Create multiple symlink paths (`lib64`, `/usr/local/lib64`)
- Set proper `LD_LIBRARY_PATH` environment variables
- Add diagnostic output for troubleshooting

### 4. RKNN Module Import Wrapper

**Problem**: The RKNN Python module needed better integration with the BrightSign extension environment.

**Solution**: Modified `python3-rknn-toolkit2_2.3.2.bb` to create a wrapper `__init__.py` that:
- Automatically detects the extension home directory
- Sets up library paths before importing RKNN modules
- Preloads the `librknnrt.so` library using `ctypes.CDLL`
- Provides informative error messages for troubleshooting

## Files Modified/Created

### New Recipe Files:
- `bsoe-recipes/meta-bs/recipes-open/librknnrt/librknnrt_2.3.2.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-torch/python3-torch_2.2.0.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-onnx/python3-onnx_1.16.1.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-onnxruntime/python3-onnxruntime_1.16.0.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-onnxoptimizer/python3-onnxoptimizer_0.2.7.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-fast-histogram/python3-fast-histogram_0.11.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-scipy/python3-scipy_1.10.1.bb`
- `bsoe-recipes/meta-bs/recipes-open/python3-tqdm/python3-tqdm_4.64.1.bb`

### Modified Files:
- `bsoe-recipes/meta-bs/recipes-open/brightsign-sdk/brightsign-sdk.bb` - Added all new packages to SDK
- `bsoe-recipes/meta-bs/recipes-open/python3-rknn-toolkit2/python3-rknn-toolkit2_2.3.2.bb` - Enhanced with dependencies and library wrapper
- `sh/setup_python_env` - Improved library path handling and diagnostics

### New Test/Debug Files:
- `install/test_rknn_setup.py` - Validation script to test RKNN setup

## Next Steps

1. **Rebuild the SDK** with the new recipes:
   ```bash
   cd brightsign-oe/build
   MACHINE=cobra ./bsbb brightsign-sdk
   ```

2. **Install the updated SDK**:
   ```bash
   ./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y
   ```

3. **Rebuild and package the extension** following the README instructions

4. **Test the extension** on the target device:
   ```bash
   # After installing the extension and sourcing setup_python_env
   python3 test_rknn_setup.py
   ```

## Expected Results

After applying these fixes:
- All required Python packages will be available with correct versions
- The RKNN runtime library will be properly located and loaded
- RKNN Python applications should run without import errors
- Users can run the provided test script to validate the setup

## Troubleshooting

If issues persist:
1. Run `python3 test_rknn_setup.py` to diagnose the problem
2. Check that `source ./setup_python_env` was run in the current shell
3. Verify `librknnrt.so` exists in the extension's `usr/lib/` directory
4. Check the `LD_LIBRARY_PATH` includes the correct library directories