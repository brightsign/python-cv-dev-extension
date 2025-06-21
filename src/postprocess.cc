#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <vector>
#include <algorithm>
#include "postprocess.h"

// COCO class names
static const char* class_names[] = {
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
    "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
    "hair drier", "toothbrush"
};

struct Detection {
    float bbox[4];  // x1, y1, x2, y2
    float conf;     // confidence score
    int class_id;   // class id
};

// Compare function for sorting detections by confidence
static bool compare_by_conf(const Detection& a, const Detection& b) {
    return a.conf > b.conf;
}

// Calculate IoU between two bounding boxes
static float calculate_iou(float* box1, float* box2) {
    float x1 = std::max(box1[0], box2[0]);
    float y1 = std::max(box1[1], box2[1]);
    float x2 = std::min(box1[2], box2[2]);
    float y2 = std::min(box1[3], box2[3]);
    
    float intersection = std::max(0.0f, x2 - x1) * std::max(0.0f, y2 - y1);
    float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
    float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);
    
    return intersection / (area1 + area2 - intersection);
}

// Non-maximum suppression
static void nms(std::vector<Detection>& detections, float nms_threshold) {
    for (int i = 0; i < detections.size(); ++i) {
        if (detections[i].conf == 0) continue;
        
        for (int j = i + 1; j < detections.size(); ++j) {
            if (detections[i].class_id == detections[j].class_id) {
                float iou = calculate_iou(detections[i].bbox, detections[j].bbox);
                if (iou > nms_threshold) {
                    detections[j].conf = 0;  // Suppress this detection
                }
            }
        }
    }
    
    // Remove suppressed detections
    detections.erase(
        std::remove_if(detections.begin(), detections.end(), 
                      [](const Detection& d) { return d.conf == 0; }),
        detections.end()
    );
}

void post_process(rknn_app_context_t* app_ctx, rknn_output* outputs, letterbox_t* letter_box, 
                  float conf_threshold, float nms_threshold, object_detect_result_list* od_results) {
    // The structure of YOLOv8 output depends on the specific model
    // This is a simplified example for YOLOv8 detection output format
    
    // Assuming the output is in the format of [batch, num_boxes, 85] where 85 = 4 (box) + 1 (obj conf) + 80 (class scores)
    // Or for YOLOv8 it could be [batch, 84, num_boxes] format
    
    int model_width = app_ctx->model_width;
    int model_height = app_ctx->model_height;
    float scale = letter_box->scale;
    float x_pad = letter_box->x_pad;
    float y_pad = letter_box->y_pad;
    
    // Get YOLOv8 output - exact structure depends on the model
    float* output_data = (float*)outputs[0].buf;
    
    // For YOLOv8, the output format might be different from previous YOLO versions
    // You need to adapt based on your specific model output
    
    std::vector<Detection> detections;
    
    // Parse the output tensor - this is model-specific
    // The following is a placeholder - adjust based on your model's output format
    
    int num_classes = 80;  // COCO dataset has 80 classes
    int boxes_per_grid = app_ctx->output_attrs[0].dims[1];  // Number of boxes
    
    // Parse detections from output
    for (int i = 0; i < boxes_per_grid; ++i) {
        float confidence = output_data[i * (num_classes + 5) + 4];
        
        if (confidence >= conf_threshold) {
            // Find class with highest score
            int class_id = 0;
            float max_class_score = 0;
            
            for (int j = 0; j < num_classes; ++j) {
                float class_score = output_data[i * (num_classes + 5) + 5 + j];
                if (class_score > max_class_score) {
                    max_class_score = class_score;
                    class_id = j;
                }
            }
            
            float score = confidence * max_class_score;
            
            if (score >= conf_threshold) {
                // YOLOv8 outputs are typically centerX, centerY, width, height
                float cx = output_data[i * (num_classes + 5) + 0];
                float cy = output_data[i * (num_classes + 5) + 1];
                float width = output_data[i * (num_classes + 5) + 2];
                float height = output_data[i * (num_classes + 5) + 3];
                
                // Convert to top-left, bottom-right format
                Detection det;
                det.bbox[0] = (cx - width/2) * model_width - x_pad;   // x1
                det.bbox[1] = (cy - height/2) * model_height - y_pad; // y1
                det.bbox[2] = (cx + width/2) * model_width - x_pad;   // x2
                det.bbox[3] = (cy + height/2) * model_height - y_pad; // y2
                det.conf = score;
                det.class_id = class_id;
                
                detections.push_back(det);
            }
        }
    }
    
    // Apply NMS
    std::sort(detections.begin(), detections.end(), compare_by_conf);
    nms(detections, nms_threshold);
    
    // Fill results
    od_results->count = std::min((int)detections.size(), 128);  // Limit to max 128 detections
    
    for (int i = 0; i < od_results->count; ++i) {
        od_results->results[i].box.left = (int)((detections[i].bbox[0] / scale));
        od_results->results[i].box.top = (int)((detections[i].bbox[1] / scale));
        od_results->results[i].box.right = (int)((detections[i].bbox[2] / scale));
        od_results->results[i].box.bottom = (int)((detections[i].bbox[3] / scale));
        od_results->results[i].score = detections[i].conf;
        od_results->results[i].class_id = detections[i].class_id;
        strncpy(od_results->results[i].name, class_names[detections[i].class_id], 256);
    }
}