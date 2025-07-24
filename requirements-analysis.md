# Python CV/ML Requirements Analysis

## Executive Summary

This analysis compares the desired packages (wmt_requirements.txt) with the actual runtime packages (post-init_requirements.txt) available on the BrightSign player after initialization.

### Key Findings

1. **rknn-toolkit-lite2 Successfully Installed**: Version 2.3.2 is now available at runtime
2. **Critical Missing Packages**: Several essential ML/CV packages are completely missing
3. **Version Mismatches**: Many installed packages have older versions than desired
4. **Total Package Count**: 61 desired vs 55 actual (6 packages missing)

## Detailed Analysis

### 1. Critical Missing Packages (Not Present at Runtime)

| Package | Desired Version | Impact |
|---------|----------------|---------|
| **onnxruntime** | 1.20.0 | **CRITICAL** - Required for ONNX model inference |
| **torch** | 2.5.1 | **CRITICAL** - PyTorch deep learning framework |
| **torchvision** | 0.20.1 | **CRITICAL** - PyTorch computer vision utilities |
| **ultralytics** | 8.3.4 | **HIGH** - YOLO object detection framework |
| **ultralytics-thop** | 2.0.14 | **HIGH** - YOLO model complexity analysis |
| **scikit-image** | 0.24.0 | **HIGH** - Image processing algorithms |
| **tifffile** | 2025.6.11 | **MEDIUM** - TIFF image file support |
| **tzdata** | 2025.2 | **LOW** - Timezone database |

### 2. Successfully Installed Packages (Version Match or Close)

| Package | Desired | Actual | Status |
|---------|---------|--------|---------|
| **rknn-toolkit-lite2** | 2.2.0* | 2.3.2 | ✅ BETTER - Newer version |
| **opencv-python** | 4.11.0.86 | 4.11.0.86 | ✅ EXACT MATCH |
| **numpy** | 2.3.0 | 1.24.4 | ⚠️ OLDER but functional |
| **matplotlib** | 3.10.3 | 3.7.5 | ⚠️ OLDER but functional |
| **scipy** | 1.15.3 | 1.10.1 | ⚠️ OLDER but functional |

*Note: wmt_requirements.txt shows a file path for rknn-toolkit-lite2, but the actual installed version is 2.3.2

### 3. Version Mismatches (Significant Differences)

| Package | Desired | Actual | Difference |
|---------|---------|--------|------------|
| **numpy** | 2.3.0 | 1.24.4 | Major version behind |
| **matplotlib** | 3.10.3 | 3.7.5 | 3 minor versions behind |
| **scipy** | 1.15.3 | 1.10.1 | 5 minor versions behind |
| **pandas** | 2.3.0 | 1.3.5 | Major version behind |
| **Pillow** | 11.2.1 | 6.2.1 | 5 major versions behind |
| **imageio** | 2.37.0 | 2.6.0 | 31 minor versions behind |
| **networkx** | 3.5 | 3.1 | 4 minor versions behind |
| **seaborn** | 0.13.2 | 0.11.2 | 2 minor versions behind |
| **protobuf** | 6.31.1 | 3.20.3 | 3 major versions behind |

### 4. Additional Packages in Runtime (Not in Desired)

| Package | Version | Purpose |
|---------|---------|---------|
| **nose** | 1.3.7 | Testing framework (legacy) |
| **importlib-resources** | 6.4.5 | Python 3.9+ backport |
| **zipp** | 3.20.2 | ZIP file utilities |

## Critical Impact Assessment

### High Priority Issues

1. **Deep Learning Frameworks Missing**:
   - No PyTorch (torch/torchvision) - Cannot run PyTorch models
   - No ONNX Runtime - Cannot run ONNX models
   - No Ultralytics - Cannot run YOLO models

2. **Computer Vision Libraries**:
   - scikit-image missing - Limited image processing capabilities
   - Pillow severely outdated (v6.2.1 vs v11.2.1) - May have compatibility issues

3. **Scientific Computing**:
   - NumPy, SciPy, and pandas are functional but significantly outdated
   - May cause compatibility issues with modern ML models

### Positive Findings

1. **rknn-toolkit-lite2** is successfully installed (v2.3.2)
2. **OpenCV** is at the exact desired version
3. Core dependencies (requests, PyYAML, etc.) are mostly present

## Recommendations

### Immediate Actions Required

1. **Add Missing ML Frameworks**:
   ```
   - python3-onnxruntime (1.20.0)
   - python3-torch (2.5.1)
   - python3-torchvision (0.20.1)
   - python3-ultralytics (8.3.4)
   ```

2. **Add Missing CV Libraries**:
   ```
   - python3-scikit-image (0.24.0)
   - python3-tifffile (2025.6.11)
   ```

3. **Critical Version Updates**:
   ```
   - python3-numpy: 1.24.4 → 2.3.0
   - python3-pillow: 6.2.1 → 11.2.1
   - python3-scipy: 1.10.1 → 1.15.3
   ```

### Build Strategy

1. **Phase 1**: Add missing critical packages (onnxruntime, torch, torchvision)
2. **Phase 2**: Add YOLO support (ultralytics packages)
3. **Phase 3**: Update outdated core packages (numpy, scipy, pillow)
4. **Phase 4**: Add remaining utility packages

## Conclusion

While the rknn-toolkit-lite2 is successfully installed, the environment is missing several critical packages required for comprehensive CV/ML workloads. The most significant gaps are:

1. No PyTorch support
2. No ONNX Runtime support
3. No YOLO/Ultralytics support
4. Severely outdated core scientific packages

These missing components prevent running most modern deep learning models except those specifically designed for RKNN.