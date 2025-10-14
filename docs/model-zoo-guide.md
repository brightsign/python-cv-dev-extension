# RKNN Model Zoo Guide

Complete guide to using the RKNN Model Zoo for NPU-accelerated inference on BrightSign players.

**Prerequisites**: Extension installed on player (see [deployment.md](deployment.md))

---

## Table of Contents

1. [What is RKNN Model Zoo](#what-is-rknn-model-zoo)
2. [RKNNLite Compatibility](#rknnlite-compatibility)
3. [Quick Start: YOLOX](#quick-start-yolox)
4. [Available Models](#available-models)
5. [Model Conversion](#model-conversion)
6. [Performance Optimization](#performance-optimization)
7. [Custom Models](#custom-models)
8. [Troubleshooting](#troubleshooting)

---

## What is RKNN Model Zoo

**RKNN Model Zoo** is Rockchip's official collection of pre-trained, NPU-optimized models.

**Repository**: https://github.com/airockchip/rknn_model_zoo

**Features**:
- 50+ pre-trained models
- Pre-compiled for RK3588 NPU
- Python examples included
- Models in `.rknn` format (NPU-optimized)

**Model Categories**:
- Object Detection (YOLO, RetinaFace, NanoDet)
- Segmentation (SegFormer, U-Net, DeepLabV3)
- Classification (ResNet, MobileNet, EfficientNet)
- Pose Estimation (YOLOv8-Pose, MediaPipe)
- Face Recognition (ArcFace, FaceNet)

**Version**: This guide uses v2.3.2 (matches extension RKNN toolkit version)

---

## RKNNLite Compatibility

### Why Compatibility Matters

Model zoo examples use `rknn-toolkit2` (full toolkit) which:
- Has hardcoded `/usr/lib64/` paths
- Designed for x86_64 development hosts
- **Does NOT work** on ARM64 BrightSign players

**Solution**: Extension includes patched `py_utils` that use `RKNNLite` API:
- Designed for embedded ARM64
- Works with BrightSign library paths (`/usr/lib/`)
- API-compatible with model zoo examples

### What's in the Compatibility Wrapper?

**Location**: `/usr/local/pydev/examples/py_utils/` (or `/var/volatile/bsext/ext_pydev/examples/py_utils/`)

**Key file**: `rknn_executor.py` - Adapts RKNN API calls to RKNNLite

**API differences handled**:
1. **Import**: `RKNNLite` instead of `RKNN`
2. **init_runtime()**: Simplified signature (no target/device_id parameters)
3. **Batch dimension**: Explicitly added (RKNNLite doesn't auto-add)

**Result**: Model zoo examples "just work" after copying `py_utils`

---

## Quick Start: YOLOX

Run YOLOX object detection with NPU acceleration in <10 minutes.

### Step 1: Download Model and Test Image

**On development machine**:
```bash
# Download pre-compiled YOLOX model for RK3588
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn

# Download test image (COCO bus image)
wget https://raw.githubusercontent.com/airockchip/rknn_model_zoo/v2.3.2/examples/yolox/model/bus.jpg

# Transfer to player
export PLAYER_IP=192.168.1.100
scp yolox_s_rk3588.rknn bus.jpg brightsign@${PLAYER_IP}:/usr/local/
```

### Step 2: Setup on Player

**SSH to player and setup environment**:
```bash
ssh brightsign@${PLAYER_IP}

# Source Python environment
source /usr/local/pydev/sh/setup_python_env
# Or for production: source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Download model_zoo examples
cd /usr/local
wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.2.zip
unzip v2.3.2.zip
mv rknn_model_zoo-2.3.2 rknn_model_zoo

# Copy compatibility wrapper (CRITICAL STEP)
cp -r /usr/local/pydev/examples/py_utils \
      /usr/local/rknn_model_zoo/examples/yolox/python/
```

**Why copy py_utils?** This replaces model_zoo's incompatible RKNN executor with RKNNLite version.

### Step 3: Run Inference

```bash
# Navigate to YOLOX example
cd /usr/local/rknn_model_zoo/examples/yolox/python

# Set paths
export MODEL_PATH=/usr/local/yolox_s_rk3588.rknn
export IMG_FOLDER=/usr/local/

# Run inference with NPU
python3 yolox.py --model_path ${MODEL_PATH} \
                 --target rk3588 \
                 --img_folder ${IMG_FOLDER} \
                 --img_save
```

**Time**: ~2-3 seconds for inference

**Expected output**:
```
--> Init runtime environment
done
--> Running model
infer 1/1
save result to ./result/bus.jpg
```

### Step 4: View Results

**Download result image**:
```bash
# From dev machine
scp brightsign@${PLAYER_IP}:/usr/local/rknn_model_zoo/examples/yolox/python/result/bus.jpg ./
```

**Expected detections**:
- Bus: ~93% confidence
- People: ~85-90% confidence
- Bounding boxes drawn on image

**Performance**:
- Inference time: ~10ms per image
- FPS: ~60+ (with pre/post processing ~30 FPS)
- NPU vs CPU: ~10x faster

---

## Available Models

### Object Detection

| Model | Size | Accuracy | Speed | Use Case |
|-------|------|----------|-------|----------|
| **YOLOX-s** | 9MB | High | Fast (~10ms) | General object detection |
| **YOLOv5-s** | 14MB | Very High | Medium (~15ms) | Accurate detection |
| **YOLOv8-n** | 6MB | Medium | Very Fast (~8ms) | Real-time, resource-constrained |
| **NanoDet** | 1.5MB | Medium | Very Fast (~5ms) | Edge devices, low latency |
| **RetinaFace** | 2MB | High | Fast (~8ms) | Face detection |

**Download pre-compiled models**:
```bash
# From model_zoo releases
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/<model>_rk3588.rknn
```

### Segmentation

| Model | Size | Accuracy | Speed | Use Case |
|-------|------|----------|-------|----------|
| **SegFormer** | 15MB | High | Medium (~30ms) | Semantic segmentation |
| **DeepLabV3** | 45MB | Very High | Slow (~60ms) | High-accuracy segmentation |
| **U-Net** | 30MB | High | Medium (~40ms) | Medical imaging, precise edges |

### Classification

| Model | Size | Accuracy | Speed | Use Case |
|-------|------|----------|-------|----------|
| **ResNet50** | 100MB | Very High | Medium (~20ms) | Image classification |
| **MobileNetV2** | 14MB | High | Fast (~8ms) | Efficient classification |
| **EfficientNet** | 20MB | Very High | Medium (~25ms) | Best accuracy/efficiency trade-off |

### Pose Estimation

| Model | Size | Accuracy | Speed | Use Case |
|-------|------|----------|-------|----------|
| **YOLOv8-Pose** | 12MB | High | Fast (~15ms) | Human pose estimation |
| **MediaPipe Pose** | 5MB | High | Very Fast (~10ms) | Real-time pose tracking |

**Full list**: https://github.com/airockchip/rknn_model_zoo/tree/v2.3.2/examples

---

## Model Conversion

Convert your own PyTorch/ONNX models to RKNN format for NPU acceleration.

### Prerequisites

- **x86_64 development host** (conversion only works on x86_64)
- **RKNN Toolkit 2** (full version)
- **Model in ONNX or PyTorch format**

### Conversion Workflow

**On x86_64 development host**:

1. **Install RKNN Toolkit 2**:
   ```bash
   # From this project's toolkit directory
   pip3 install toolkit/rknn-toolkit2/rknn-toolkit2/packages/rknn_toolkit2-2.3.2-*.whl
   ```

2. **Convert model to RKNN**:
   ```python
   from rknn.api import RKNN

   rknn = RKNN()

   # Configure for RK3588
   rknn.config(target_platform='rk3588')

   # Load ONNX model
   ret = rknn.load_onnx(model='model.onnx')

   # Build RKNN model
   ret = rknn.build(do_quantization=True,
                    dataset='calibration_data.txt',
                    rknn_batch_size=1)

   # Export
   ret = rknn.export_rknn('model_rk3588.rknn')

   rknn.release()
   ```

3. **Transfer to player**:
   ```bash
   scp model_rk3588.rknn brightsign@${PLAYER_IP}:/usr/local/models/
   ```

4. **Run on player**:
   ```python
   # On player
   from rknnlite.api import RKNNLite

   rknn = RKNNLite()
   rknn.load_rknn('/usr/local/models/model_rk3588.rknn')
   rknn.init_runtime()

   # Run inference
   outputs = rknn.inference(inputs=[preprocessed_image])
   ```

**See**: RKNN Toolkit 2 documentation for detailed conversion guide.

### Quantization

**Why quantize**: Reduces model size and improves NPU performance.

**Types**:
- **Dynamic quantization**: Automatic, no calibration data needed
- **Static quantization**: Requires calibration dataset, better accuracy

**Example with calibration**:
```python
# Create calibration dataset list
# calibration_data.txt contains paths to sample images
with open('calibration_data.txt', 'w') as f:
    for img_path in calibration_images:
        f.write(f'{img_path}\n')

# Build with quantization
rknn.build(do_quantization=True,
           dataset='calibration_data.txt')
```

---

## Performance Optimization

### NPU Core Allocation

RK3588 has 3 NPU cores (2 TOPS + 2 TOPS + 2 TOPS = 6 TOPS total).

**Use specific cores**:
```python
from rknnlite.api import RKNNLite

# Use all cores (default)
rknn = RKNNLite()
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_AUTO)

# Use specific core
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_0)  # Core 0 only
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_1)  # Core 1 only
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_2)  # Core 2 only

# Use multiple cores
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_0_1)  # Cores 0+1
rknn.init_runtime(core_mask=RKNNLite.NPU_CORE_0_1_2)  # All cores
```

**When to use specific cores**:
- Running multiple models simultaneously
- Balancing load across cores
- Testing core-specific performance

### Batch Processing

**Single image** (typical):
```python
# Add batch dimension
img = np.expand_dims(img, axis=0)  # (H,W,C) -> (1,H,W,C)
outputs = rknn.inference(inputs=[img])
```

**Batch processing** (multiple images):
```python
# Stack images
batch = np.stack([img1, img2, img3, img4])  # (4,H,W,C)
outputs = rknn.inference(inputs=[batch])
```

**Note**: NPU batch processing may not always be faster than sequential. Benchmark both approaches.

### Pre-processing Optimization

**Use OpenCV NPU acceleration**:
```python
import cv2

# Resize with hardware acceleration
img = cv2.resize(img, (640, 640))

# Convert color space
img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

# Normalize (if model expects 0-1 range)
img = img.astype(np.float32) / 255.0
```

**Minimize data copies**:
```python
# Bad: Multiple copies
img = cv2.imread('image.jpg')
img = cv2.resize(img, (640, 640))
img = img.astype(np.float32)
img = img / 255.0

# Better: In-place operations where possible
img = cv2.imread('image.jpg')
img = cv2.resize(img, (640, 640))
img = img.astype(np.float32, copy=False) / 255.0
```

### Memory Management

**Release resources**:
```python
# After inference done
rknn.release()

# Free memory explicitly
del large_array
import gc
gc.collect()
```

**Reuse RKNN object**:
```python
# Good: Load model once, run multiple inferences
rknn = RKNNLite()
rknn.load_rknn('model.rknn')
rknn.init_runtime()

for img in image_stream:
    outputs = rknn.inference(inputs=[img])
    process_results(outputs)

rknn.release()

# Bad: Reload model every time (slow)
for img in image_stream:
    rknn = RKNNLite()
    rknn.load_rknn('model.rknn')  # Slow!
    rknn.init_runtime()
    outputs = rknn.inference(inputs=[img])
    rknn.release()
```

---

## Custom Models

### Using Custom YOLOX Model

**Example**: Use different YOLOX variant (nano, tiny, medium, large):

1. **Download or convert** your model to RKNN format

2. **Modify detection parameters** in YOLOX example:
   ```python
   # In yolox.py

   # Change input size if model expects different dimensions
   IMG_SIZE = 416  # Or 640, 320, etc.

   # Adjust confidence threshold
   CONF_THRESH = 0.5  # Default, adjust as needed

   # Adjust NMS threshold
   NMS_THRESH = 0.45  # Default, adjust as needed
   ```

3. **Run with custom model**:
   ```bash
   python3 yolox.py --model_path /path/to/custom_model.rknn \
                    --target rk3588 \
                    --img_folder /path/to/images/ \
                    --img_save
   ```

### Creating Custom Inference Script

**Template**:
```python
#!/usr/bin/env python3
import numpy as np
from rknnlite.api import RKNNLite
import cv2

def preprocess(image_path):
    """Preprocess image for model"""
    img = cv2.imread(image_path)
    img = cv2.resize(img, (640, 640))
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = img.astype(np.float32) / 255.0
    img = np.expand_dims(img, axis=0)  # Add batch dimension
    return img

def postprocess(outputs):
    """Post-process model outputs"""
    # Your custom post-processing logic
    return results

def main():
    # Initialize RKNN
    rknn = RKNNLite()

    # Load model
    ret = rknn.load_rknn('/path/to/model.rknn')
    if ret != 0:
        print('Load model failed!')
        return

    # Initialize runtime
    ret = rknn.init_runtime()
    if ret != 0:
        print('Init runtime failed!')
        return

    # Preprocess
    inputs = preprocess('test_image.jpg')

    # Run inference
    outputs = rknn.inference(inputs=[inputs])

    # Postprocess
    results = postprocess(outputs)
    print(f'Results: {results}')

    # Cleanup
    rknn.release()

if __name__ == '__main__':
    main()
```

---

## Troubleshooting

### Model Won't Load

**Symptoms**: `load_rknn()` fails

**Causes**:
1. **Wrong model platform**:
   - Model must be compiled for RK3588
   - Check model filename: should contain `rk3588`

2. **Corrupted model file**:
   ```bash
   # Verify file integrity
   ls -lh model.rknn
   # Should be reasonable size (not 0 bytes)

   # Re-download or re-transfer
   ```

3. **Path incorrect**:
   ```python
   import os
   print(os.path.exists('/path/to/model.rknn'))  # Should print True
   ```

### Runtime Init Fails

**Symptoms**: `init_runtime()` returns non-zero

**Causes**:
1. **OS version too old**:
   ```bash
   cat /etc/version  # Must be 9.1.79.3+
   ```

2. **NPU library missing**:
   ```bash
   ls -la /usr/lib/librknnrt.so  # Should exist
   ```

3. **Already initialized**:
   ```python
   # Don't call init_runtime() twice
   # Call release() first if reinitializing
   ```

### Inference Results Wrong

**Symptoms**: Incorrect detections or poor accuracy

**Debug steps**:

1. **Check input preprocessing**:
   ```python
   # Print input shape and range
   print(f'Input shape: {inputs[0].shape}')
   print(f'Input range: {inputs[0].min()} to {inputs[0].max()}')

   # Should match model expectations
   # Common: (1, 640, 640, 3) with range 0-1 or 0-255
   ```

2. **Verify model**:
   ```bash
   # Test with known-good image
   # Should produce expected detections
   ```

3. **Check post-processing**:
   ```python
   # Print raw outputs
   for i, out in enumerate(outputs):
       print(f'Output {i} shape: {out.shape}')
       print(f'Output {i} range: {out.min()} to {out.max()}')
   ```

4. **Compare with CPU inference** (if possible):
   - Run same model on x86_64 host with full RKNN toolkit
   - Compare outputs

### Performance Issues

**Symptoms**: Inference slower than expected

**Optimization checklist**:
- [ ] Using NPU (not CPU fallback)
- [ ] Model compiled for RK3588
- [ ] Quantization enabled
- [ ] Batch dimension added
- [ ] Pre-processing optimized
- [ ] Reusing RKNN object (not recreating)
- [ ] Appropriate NPU core allocation

**Benchmark**:
```python
import time

start = time.time()
for i in range(100):
    outputs = rknn.inference(inputs=[img])
end = time.time()

avg_time = (end - start) / 100
print(f'Average inference time: {avg_time*1000:.2f}ms')
print(f'FPS: {1/avg_time:.1f}')
```

**Expected performance** (YOLOX-s):
- Inference only: ~10ms (~100 FPS)
- With pre/post-processing: ~30ms (~30 FPS)

---

## Examples Reference

### YOLOX Variants

```bash
# Download different YOLOX models
cd /usr/local/models

# YOLOX-nano (smallest, fastest)
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_n_rk3588.rknn

# YOLOX-small (balanced)
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn

# YOLOX-medium (more accurate)
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_m_rk3588.rknn
```

### Other Model Zoo Examples

**Face detection (RetinaFace)**:
```bash
cd /usr/local/rknn_model_zoo/examples/retinaface/python
cp -r /usr/local/pydev/examples/py_utils ./
python3 retinaface.py --model_path /path/to/retinaface_rk3588.rknn
```

**Segmentation (SegFormer)**:
```bash
cd /usr/local/rknn_model_zoo/examples/segformer/python
cp -r /usr/local/pydev/examples/py_utils ./
python3 segformer.py --model_path /path/to/segformer_rk3588.rknn
```

**Pattern**: Always copy `py_utils` before running model_zoo examples.

---

## Resources

- **RKNN Model Zoo**: https://github.com/airockchip/rknn_model_zoo
- **RKNN Toolkit 2**: https://github.com/rockchip-linux/rknn-toolkit2
- **RK3588 NPU Documentation**: https://www.rock-chips.com/
- **Model Zoo Releases**: https://github.com/airockchip/rknn_model_zoo/releases

---

## Summary

**Key Points**:
- ✅ Model zoo provides 50+ pre-trained NPU models
- ✅ Use patched `py_utils` for BrightSign compatibility
- ✅ YOLOX achieves ~10ms inference (60+ FPS)
- ✅ Models available for detection, segmentation, pose, etc.
- ✅ Can convert custom PyTorch/ONNX models to RKNN
- ✅ Optimize with core allocation, batching, preprocessing

**Next Steps**:
- Try different model zoo examples
- Convert your own models to RKNN
- Build custom CV application
- Deploy to production

**Need Help?**:
- [FAQ.md](../FAQ.md) - Common questions
- [troubleshooting.md](troubleshooting.md) - Issue resolution
- [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues)
