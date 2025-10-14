#!/usr/bin/env python3
"""
YOLOX NPU Inference Test
Tests end-to-end object detection using RKNN NPU acceleration.

Usage:
    python3 test_yolox_npu.py <model_path> <image_path>

Example:
    python3 test_yolox_npu.py /storage/sd/yolox_s.rknn /storage/sd/bus.jpg
"""

import sys
import cv2
import numpy as np
from rknnlite.api import RKNNLite

# YOLOX parameters
OBJ_THRESH = 0.25
NMS_THRESH = 0.45
IMG_SIZE = (640, 640)

# COCO 80 class labels
CLASSES = (
    "person", "bicycle", "car", "motorbike", "aeroplane", "bus", "train", "truck", "boat",
    "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
    "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
    "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
    "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
    "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
    "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "sofa",
    "pottedplant", "bed", "diningtable", "toilet", "tvmonitor", "laptop", "mouse", "remote",
    "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
    "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
)


def letterbox(im, new_shape=(640, 640), color=(114, 114, 114)):
    """Resize and pad image to new_shape with letterbox."""
    shape = im.shape[:2]  # current shape [height, width]

    # Scale ratio (new / old)
    r = min(new_shape[0] / shape[0], new_shape[1] / shape[1])

    # Compute padding
    new_unpad = int(round(shape[1] * r)), int(round(shape[0] * r))
    dw, dh = new_shape[1] - new_unpad[0], new_shape[0] - new_unpad[1]  # wh padding
    dw /= 2  # divide padding into 2 sides
    dh /= 2

    if shape[::-1] != new_unpad:  # resize
        im = cv2.resize(im, new_unpad, interpolation=cv2.INTER_LINEAR)

    top, bottom = int(round(dh - 0.1)), int(round(dh + 0.1))
    left, right = int(round(dw - 0.1)), int(round(dw + 0.1))
    im = cv2.copyMakeBorder(im, top, bottom, left, right, cv2.BORDER_CONSTANT, value=color)

    return im, r, (dw, dh)


def box_process(position):
    """Decode YOLOX box predictions from feature map."""
    grid_h, grid_w = position.shape[2:4]
    col, row = np.meshgrid(np.arange(0, grid_w), np.arange(0, grid_h))
    col = col.reshape(1, 1, grid_h, grid_w)
    row = row.reshape(1, 1, grid_h, grid_w)
    grid = np.concatenate((col, row), axis=1)
    stride = np.array([IMG_SIZE[1]//grid_h, IMG_SIZE[0]//grid_w]).reshape(1, 2, 1, 1)

    box_xy = position[:, :2, :, :]
    box_wh = np.exp(position[:, 2:4, :, :]) * stride

    box_xy += grid
    box_xy *= stride
    box = np.concatenate((box_xy, box_wh), axis=1)

    # Convert [c_x, c_y, w, h] to [x1, y1, x2, y2]
    xyxy = np.copy(box)
    xyxy[:, 0, :, :] = box[:, 0, :, :] - box[:, 2, :, :] / 2  # top left x
    xyxy[:, 1, :, :] = box[:, 1, :, :] - box[:, 3, :, :] / 2  # top left y
    xyxy[:, 2, :, :] = box[:, 0, :, :] + box[:, 2, :, :] / 2  # bottom right x
    xyxy[:, 3, :, :] = box[:, 1, :, :] + box[:, 3, :, :] / 2  # bottom right y

    return xyxy


def filter_boxes(boxes, box_confidences, box_class_probs):
    """Filter boxes with object threshold."""
    box_confidences = box_confidences.reshape(-1)
    candidate, class_num = box_class_probs.shape

    class_max_score = np.max(box_class_probs, axis=-1)
    classes = np.argmax(box_class_probs, axis=-1)

    _class_pos = np.where(class_max_score * box_confidences >= OBJ_THRESH)
    scores = (class_max_score * box_confidences)[_class_pos]

    boxes = boxes[_class_pos]
    classes = classes[_class_pos]

    return boxes, classes, scores


def nms_boxes(boxes, scores):
    """Apply non-maximum suppression."""
    x = boxes[:, 0]
    y = boxes[:, 1]
    w = boxes[:, 2] - boxes[:, 0]
    h = boxes[:, 3] - boxes[:, 1]

    areas = w * h
    order = scores.argsort()[::-1]

    keep = []
    while order.size > 0:
        i = order[0]
        keep.append(i)

        xx1 = np.maximum(x[i], x[order[1:]])
        yy1 = np.maximum(y[i], y[order[1:]])
        xx2 = np.minimum(x[i] + w[i], x[order[1:]] + w[order[1:]])
        yy2 = np.minimum(y[i] + h[i], y[order[1:]] + h[order[1:]])

        w1 = np.maximum(0.0, xx2 - xx1 + 0.00001)
        h1 = np.maximum(0.0, yy2 - yy1 + 0.00001)
        inter = w1 * h1

        ovr = inter / (areas[i] + areas[order[1:]] - inter)
        inds = np.where(ovr <= NMS_THRESH)[0]
        order = order[inds + 1]

    return np.array(keep)


def post_process(outputs, img_shape, letterbox_shape, ratio, pad):
    """Post-process YOLOX outputs to get final detections."""
    # Outputs are feature maps: [(1, 85, 80, 80), (1, 85, 40, 40), (1, 85, 20, 20)]
    # Channel 85 = 4 (box) + 1 (objectness) + 80 (classes)

    boxes_list, scores_list, classes_conf_list = [], [], []

    # Process each scale
    for output in outputs:
        # Split channels: boxes (0:4), objectness (4:5), classes (5:85)
        boxes_list.append(box_process(output[:, :4, :, :]))
        scores_list.append(output[:, 4:5, :, :])
        classes_conf_list.append(output[:, 5:, :, :])

    # Flatten spatial dimensions: (1, C, H, W) -> (H*W, C)
    def sp_flatten(_in):
        ch = _in.shape[1]
        _in = _in.transpose(0, 2, 3, 1)
        return _in.reshape(-1, ch)

    boxes = np.concatenate([sp_flatten(b) for b in boxes_list])
    scores = np.concatenate([sp_flatten(s) for s in scores_list])
    classes_conf = np.concatenate([sp_flatten(c) for c in classes_conf_list])

    # Filter boxes by threshold
    boxes, classes, scores = filter_boxes(boxes, scores, classes_conf)

    if len(boxes) == 0:
        return [], [], []

    # Apply NMS
    keep = nms_boxes(boxes, scores)

    if len(keep) == 0:
        return [], [], []

    boxes = boxes[keep]
    classes = classes[keep]
    scores = scores[keep]

    # Scale boxes back to original image coordinates
    boxes[:, 0] = (boxes[:, 0] - pad[0]) / ratio
    boxes[:, 1] = (boxes[:, 1] - pad[1]) / ratio
    boxes[:, 2] = (boxes[:, 2] - pad[0]) / ratio
    boxes[:, 3] = (boxes[:, 3] - pad[1]) / ratio

    # Clip to image boundaries
    boxes[:, 0] = np.clip(boxes[:, 0], 0, img_shape[1])
    boxes[:, 1] = np.clip(boxes[:, 1], 0, img_shape[0])
    boxes[:, 2] = np.clip(boxes[:, 2], 0, img_shape[1])
    boxes[:, 3] = np.clip(boxes[:, 3], 0, img_shape[0])

    return boxes, classes, scores


def main():
    if len(sys.argv) != 3:
        print("Usage: python3 test_yolox_npu.py <model_path> <image_path>")
        sys.exit(1)

    model_path = sys.argv[1]
    image_path = sys.argv[2]

    print("=" * 60)
    print("YOLOX NPU Inference Test")
    print("=" * 60)

    # Load image
    print(f"Loading image: {image_path}")
    img = cv2.imread(image_path)
    if img is None:
        print(f"ERROR: Could not load image: {image_path}")
        sys.exit(1)

    print(f"  Image shape: {img.shape}")
    orig_shape = img.shape[:2]

    # Prepare input
    print(f"Preprocessing image to {IMG_SIZE}")
    img_resized, ratio, pad = letterbox(img, IMG_SIZE)
    img_rgb = cv2.cvtColor(img_resized, cv2.COLOR_BGR2RGB)

    # Add batch dimension: (H, W, C) -> (1, H, W, C)
    img_input = np.expand_dims(img_rgb, axis=0)

    # Initialize RKNN
    print(f"Loading RKNN model: {model_path}")
    rknn = RKNNLite()

    ret = rknn.load_rknn(model_path)
    if ret != 0:
        print(f"ERROR: Failed to load RKNN model (ret={ret})")
        sys.exit(1)
    print("  Model loaded successfully")

    print("Initializing RKNN runtime...")
    ret = rknn.init_runtime()
    if ret != 0:
        print(f"ERROR: Failed to initialize runtime (ret={ret})")
        sys.exit(1)
    print("  Runtime initialized successfully")

    # Run inference
    print("Running NPU inference...")
    print(f"  Input shape: {img_input.shape}")
    outputs = rknn.inference(inputs=[img_input])
    if outputs is None:
        print("ERROR: Inference failed")
        sys.exit(1)
    print(f"  Inference complete - {len(outputs)} outputs")

    # Post-process results
    print("Post-processing detections...")
    print(f"  Output shapes: {[out.shape for out in outputs]}")
    boxes, classes, scores = post_process(outputs, orig_shape, IMG_SIZE, ratio, pad)

    # Print results
    print("=" * 60)
    print(f"Detection Results: {len(boxes)} objects found")
    print("=" * 60)

    if len(boxes) > 0:
        for i, (box, cls, score) in enumerate(zip(boxes, classes, scores)):
            x1, y1, x2, y2 = [int(b) for b in box]
            class_name = CLASSES[int(cls)]
            print(f"{i+1}. {class_name:15s} @ ({x1:4d}, {y1:4d}, {x2:4d}, {y2:4d}) confidence: {score:.3f}")
    else:
        print("No objects detected above threshold")

    print("=" * 60)
    print("NPU inference test completed successfully!")
    print("=" * 60)

    # Cleanup
    rknn.release()


if __name__ == '__main__':
    main()
