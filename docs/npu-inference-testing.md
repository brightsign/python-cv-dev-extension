# NPU Inference Testing Protocol

**Date**: 2025-01-31
**Purpose**: Test end-to-end YOLOX object detection using RKNN NPU acceleration on BrightSign player
**Prerequisites**:
- Extension installed and initialized on player
- BrightSign OS 9.1.79.3+ (librknnrt.so available)
- SSH/SCP access to player

---

## Overview

This protocol validates that the NPU can successfully load and run object detection models using the rknnlite API. It tests the complete inference pipeline:
1. RKNN model loading
2. Image preprocessing
3. NPU inference execution
4. Post-processing and detection output

---

## Test Files

### 1. Test Script
- **Source**: [user-init/examples/test_yolox_npu.py](../user-init/examples/test_yolox_npu.py)
- **Description**: Self-contained YOLOX inference test using RKNNLite API
- **Features**:
  - Loads RKNN model
  - Preprocesses image with letterbox padding
  - Runs NPU inference
  - Post-processes detections (NMS, thresholding)
  - Outputs detected objects with bounding boxes and confidence scores

### 2. YOLOX Model
- **Source**: `../cv-npu-yolo-object-detect/install/RK3588/model/yolox_s.rknn`
- **Description**: Pre-compiled YOLOX-S model for RK3588
- **Size**: ~10MB
- **Input**: 640x640 RGB image
- **Detects**: 80 COCO classes

### 3. Test Image
- **Source**: `toolkit/rknn_model_zoo/examples/yolox/model/bus.jpg`
- **Description**: Standard test image with multiple objects (person, bus, etc.)
- **Expected detections**: Bus, people, traffic elements

---

## Testing Procedure

### Step 1: Copy Files to Player

**IMPORTANT**: BrightSign uses busybox/dropbear SSH - use atomic SCP commands only.

```bash
# Set your player IP
export PLAYER_IP=192.168.1.100

# Copy test script
scp user-init/examples/test_yolox_npu.py brightsign@${PLAYER_IP}:/storage/sd/

# Copy YOLOX model from companion project
scp ../cv-npu-yolo-object-detect/install/RK3588/model/yolox_s.rknn brightsign@${PLAYER_IP}:/storage/sd/

# Copy test image
scp toolkit/rknn_model_zoo/examples/yolox/model/bus.jpg brightsign@${PLAYER_IP}:/storage/sd/
```

**Verify files transferred:**
```bash
ssh brightsign@${PLAYER_IP}
ls -lh /storage/sd/test_yolox_npu.py /storage/sd/yolox_s.rknn /storage/sd/bus.jpg
```

### Step 2: Run NPU Inference Test

**SSH to player and run test:**
```bash
ssh brightsign@${PLAYER_IP}
python3 /storage/sd/test_yolox_npu.py /storage/sd/yolox_s.rknn /storage/sd/bus.jpg
```

---

## Expected Output

### Success Output
```
============================================================
YOLOX NPU Inference Test
============================================================
Loading image: /storage/sd/bus.jpg
  Image shape: (1080, 810, 3)
Preprocessing image to (640, 640)
Loading RKNN model: /storage/sd/yolox_s.rknn
  Model loaded successfully
Initializing RKNN runtime...
W rknn-toolkit-lite2 version: 2.3.2
  Runtime initialized successfully
Running NPU inference...
  Inference complete - 3 outputs
Post-processing detections...
============================================================
Detection Results: 5 objects found
============================================================
1. person          @ ( 210,  240,  285,  505) confidence: 0.887
2. person          @ ( 110,  235,  225,  536) confidence: 0.869
3. bus             @ (  95,  133,  556,  438) confidence: 0.861
4. person          @ (  80,  324,  123,  516) confidence: 0.552
5. person          @ ( 258,  237,  305,  510) confidence: 0.518
============================================================
NPU inference test completed successfully!
============================================================
```

### What This Proves

✅ **Model Loading**: RKNN model successfully loaded
✅ **Runtime Initialization**: NPU runtime initialized with librknnrt.so
✅ **NPU Inference**: Model executed on NPU hardware
✅ **Post-Processing**: Detections filtered and processed correctly
✅ **End-to-End Pipeline**: Complete inference pipeline working

---

## Troubleshooting

### Error: "Could not load image"
- **Cause**: Image file not found or corrupted
- **Fix**: Verify SCP transfer succeeded, check file path

### Error: "Failed to load RKNN model"
- **Cause**: Model file not found or incompatible
- **Fix**: Verify model is RK3588-compatible RKNN format

### Error: "Failed to initialize runtime"
- **Cause**: Missing librknnrt.so or wrong OS version
- **Fix**: Verify BrightSign OS 9.1.79.3+, check `/usr/lib/librknnrt.so` exists

### Error: "ModuleNotFoundError: No module named 'rknnlite'"
- **Cause**: Extension not installed or Python environment not initialized
- **Fix**: Verify extension installed, source environment setup:
  ```bash
  source /usr/local/pydev/sh/setup_python_env
  ```

### No objects detected
- **Cause**: Model output format mismatch or threshold too high
- **Fix**: Lower OBJ_THRESH in script, verify model is YOLOX format

---

## Next Steps After Success

1. **Test with custom images**: Copy your own test images and verify detection
2. **Performance testing**: Measure inference time for different image sizes
3. **Model comparison**: Test other RKNN models (YOLOv8, YOLOv5, etc.)
4. **Integration testing**: Integrate NPU inference into actual application

---

## Reference Information

### RKNNLite API Methods Used
- `rknn.load_rknn(path)` - Load compiled RKNN model
- `rknn.init_runtime()` - Initialize NPU runtime
- `rknn.inference(inputs=[img])` - Run inference on NPU
- `rknn.release()` - Cleanup resources

### Model Zoo Resources
- **GitHub**: https://github.com/airockchip/rknn_model_zoo/tree/v2.3.2
- **YOLOX Documentation**: https://github.com/airockchip/rknn_model_zoo/tree/v2.3.2/examples/yolox
- **Companion Project**: https://github.com/brightsign/brightsign-npu-object-extension

### Detection Parameters
- **OBJ_THRESH**: 0.25 (minimum confidence for detection)
- **NMS_THRESH**: 0.45 (non-maximum suppression threshold)
- **IMG_SIZE**: (640, 640) (model input resolution)
- **CLASSES**: 80 COCO object categories

---

## Validation Checklist

- [x] Files copied to player successfully
- [x] Test script executes without Python errors
- [x] RKNN model loads successfully
- [x] NPU runtime initializes successfully
- [x] Inference completes and returns outputs
- [x] Detections are reasonable (correct objects, sensible positions)
- [x] Script completes with success message

---

## Actual Validation Results ✅

**Testing Sign-off**

**Tester**: Scott (User)
**Date**: 2025-01-31
**Result**: ☑ Success
**Platform**: BrightSign XT-5 (RK3588)
**OS Version**: 9.1.79.3

### Test Execution

**Command**:
```bash
python3 /storage/sd/test_yolox_npu.py /storage/sd/yolox_s.rknn /storage/sd/bus.jpg
```

### Complete Test Output

```
============================================================
YOLOX NPU Inference Test
============================================================
Loading image: /storage/sd/bus.jpg
  Image shape: (640, 640, 3)
Preprocessing image to (640, 640)
Loading RKNN model: /storage/sd/yolox_s.rknn
W rknn-toolkit-lite2 version: 2.3.2
  Model loaded successfully
Initializing RKNN runtime...
I RKNN: [17:22:10.878] RKNN Runtime Information, librknnrt version: 2.3.0 (c949ad889d@2024-11-07T11:35:33)
I RKNN: [17:22:10.878] RKNN Driver Information, version: 0.9.3
I RKNN: [17:22:10.878] RKNN Model Information, version: 6, toolkit version: 2.3.0(compiler version: 2.3.0 (c949ad889d@2024-11-07T11:39:30)), target: RKNPU v2, target platform: rk3588, framework name: ONNX, framework layout: NCHW, model inference type: static_shape
W RKNN: [17:22:10.891] query RKNN_QUERY_INPUT_DYNAMIC_RANGE error, rknn model is static shape type, please export rknn with dynamic_shapes
W Query dynamic range failed. Ret code: RKNN_ERR_MODEL_INVALID. (If it is a static shape RKNN model, please ignore the above warning message.)
  Runtime initialized successfully
Running NPU inference...
  Input shape: (1, 640, 640, 3)
  Inference complete - 3 outputs
Post-processing detections...
  Output shapes: [(1, 85, 80, 80), (1, 85, 40, 40), (1, 85, 20, 20)]
============================================================
Detection Results: 5 objects found
============================================================
1. bus             @ (  87,  137,  550,  428) confidence: 0.930
2. person          @ ( 106,  236,  218,  534) confidence: 0.896
3. person          @ ( 211,  239,  286,  510) confidence: 0.871
4. person          @ ( 474,  235,  559,  519) confidence: 0.831
5. person          @ (  80,  328,  118,  516) confidence: 0.499
============================================================
NPU inference test completed successfully!
============================================================
```

### Test Analysis

**Runtime Environment**:
- librknnrt version: 2.3.0 (system library from OS 9.1.79.3)
- RKNN Driver version: 0.9.3
- Model version: RKNN v6, toolkit 2.3.0
- Target platform: RK3588 (RKNPU v2)

**Detection Performance**:
- Primary object (bus): 93.0% confidence - EXCELLENT
- Secondary objects (people): 83.1-89.6% confidence - EXCELLENT
- Additional object (person): 49.9% confidence - above threshold
- Total detections: 5 objects
- False positives: 0 (all detections valid)

**Pipeline Validation**:
- ✅ Model loading: Successful
- ✅ Runtime initialization: Successful (no hardcoded path error)
- ✅ Preprocessing: Letterbox resize working correctly
- ✅ NPU inference: Completed without errors
- ✅ Post-processing: NMS and filtering working correctly
- ✅ Output quality: Excellent detection accuracy

**Conclusion**: Complete end-to-end NPU inference pipeline is **FULLY OPERATIONAL**. The 2-month blocking issue is **RESOLVED and VALIDATED** on actual hardware.

---
