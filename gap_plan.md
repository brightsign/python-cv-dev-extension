# Gap Analysis: wmt_requirements.txt vs post-init_requirements.txt

## Overview

Direct comparison of desired packages in `wmt_requirements.txt` with actual runtime environment in `post-init_requirements.txt` (pip freeze output after extension initialization).

**Last Updated**: 2025-07-29

## Executive Summary

- **All required packages are present** - No missing packages
- **96.7% package coverage** (58 of 60 active packages, 2 intentionally commented out)
- **Major version gaps exist** for several critical packages due to Python 3.8 constraints
- **4 additional packages** in post-init that are not in wmt requirements (likely dependencies)

## Package Comparison Analysis

### Missing Packages
**None** - All packages from wmt_requirements.txt are present in post-init_requirements.txt

### Additional Packages (in post-init but not in wmt)
- `importlib-resources==6.4.5` - Backport for resource access
- `nose==1.3.7` - Testing framework (likely a dependency)
- `PyWavelets==1.4.1` - Wavelet transforms (scikit-image dependency)
- `zipp==3.20.2` - ZIP archive utilities (backport dependency)

### Commented Out Packages (intentionally excluded)
- `onnxruntime==1.20.0` - ONNX model runtime
- `opencv-python==4.11.0.86` - Using native python3-opencv from SDK instead
- `ultralytics==8.3.4` - YOLO framework
- `ultralytics-thop==2.0.14` - Model complexity analysis

## Version Comparison Table

### Critical Version Differences (2+ major versions behind)

| Package | Desired (wmt) | Actual (post-init) | Gap | Impact |
|---------|---------------|-------------------|-----|---------|
| **pandas** | 2.3.0 | 1.0.5 | 1.2.5 major versions | Missing modern data analysis features |
| **numpy** | 2.3.0 | 1.24.4 | ~1 major version | Missing latest performance optimizations |
| **protobuf** | 6.31.1 | 3.20.3 | 3 major versions | Compatibility issues with modern protocols |
| **pytz** | 2025.2 | 2021.3 | 4 years | Outdated timezone database |
| **tifffile** | 2025.6.11 | 2023.7.10 | 2 years | Missing recent TIFF format support |
| **tzlocal** | 5.3.1 | 2.1 | 3 major versions | Limited timezone detection |

### Significant Version Differences (1 major version behind)

| Package | Desired (wmt) | Actual (post-init) | Notes |
|---------|---------------|-------------------|--------|
| matplotlib | 3.10.3 | 3.7.5 | Missing latest plotting features |
| networkx | 3.5 | 3.1 | Graph algorithm improvements |
| pillow | 11.2.1 | 10.4.0 | Image processing enhancements |
| seaborn | 0.13.2 | 0.11.2 | Statistical visualization updates |
| watchdog | 6.0.0 | 4.0.2 | File system monitoring improvements |

### Minor Version Differences (<1 major version)

| Package | Desired (wmt) | Actual (post-init) |
|---------|---------------|-------------------|
| APScheduler | 3.11.0 | 3.10.1 |
| contourpy | 1.3.2 | 1.1.1 |
| filelock | 3.18.0 | 3.16.1 |
| fonttools | 4.58.2 | 4.57.0 |
| fsspec | 2025.5.1 | 2025.3.0 |
| imageio | 2.37.0 | 2.35.1 |
| kiwisolver | 1.4.8 | 1.4.7 |
| MarkupSafe | 3.0.2 | 2.1.5 |
| pyparsing | 3.2.3 | 3.1.4 |
| python-dateutil | 2.9.0.post0 | 2.8.2 |
| ruamel.yaml | 0.18.14 | 0.17.21 |
| ruamel.yaml.clib | 0.2.12 | 0.2.7 |
| scikit-image | 0.24.0 | 0.21.0 |
| scipy | 1.15.3 | 1.10.1 |
| termcolor | 2.5.0 | 2.4.0 |
| torch | 2.5.1 | 2.4.1 |
| torchvision | 0.20.1 | 0.19.1 |
| typing_extensions | 4.14.0 | 4.13.2 |
| urllib3 | 2.4.0 | 2.2.3 |

### Special Cases

- **rknn-toolkit-lite2**: 
  - wmt: Specified as local wheel file path
  - post-init: Version 2.3.2 (successfully installed)

## Python 3.8 Compatibility Impact

Many desired package versions require Python 3.9+ or even 3.10+:

- **numpy 2.3.0**: Requires Python ≥3.10
- **pandas 2.3.0**: Requires Python ≥3.9  
- **matplotlib 3.10.3**: Requires Python ≥3.9
- **pillow 11.2.1**: Requires Python ≥3.9
- **scikit-image 0.24.0**: Requires Python ≥3.10
- **tifffile 2025.6.11**: Requires Python ≥3.10

## Recommendations

### High Priority Updates
1. **pandas**: Critical for data analysis - explore backporting features or upgrading Python
2. **numpy**: Foundation package - performance and compatibility concerns
3. **protobuf**: Severe version gap - may cause protocol compatibility issues

### Medium Priority Updates  
1. **matplotlib**: Visualization capabilities limited by older version
2. **scikit-image**: Advanced image processing features missing
3. **pillow**: Image format support may be limited

### Low Priority Updates
1. Minor version differences generally don't impact functionality
2. Additional packages in post-init are beneficial dependencies

## Conclusion

While all required packages are installed, significant version gaps exist primarily due to Python 3.8 constraints. The environment is functional but lacks modern features and optimizations available in newer package versions. Consider:

1. Upgrading to Python 3.9+ to access modern package versions
2. Accepting current limitations with documented workarounds
3. Selectively updating critical packages through custom builds