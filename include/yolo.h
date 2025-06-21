#ifndef _RKNN_DEMO_MOBILENET_H_
#define _RKNN_DEMO_MOBILENET_H_

#include "rknn_api.h"
#include "common.h"

#define BOX_THRESH 0.5    // Default box confidence threshold
#define NMS_THRESH 0.45   // Default NMS threshold

typedef struct {
    rknn_context rknn_ctx;
    rknn_input_output_num io_num;
    rknn_tensor_attr *input_attrs;
    rknn_tensor_attr *output_attrs;
    int model_channel;
    int model_width;
    int model_height;
    bool is_quant;
} rknn_app_context_t;

typedef struct box_rect_t {
    int left;    ///< Most left coordinate
    int top;     ///< Most top coordinate
    int right;   ///< Most right coordinate
    int bottom;  ///< Most bottom coordinate
} box_rect_t;

typedef struct object_detect_result {
    box_rect_t box;
    float score;
    int class_id;
    char name[256];
} object_detect_result_t;

typedef struct object_detect_result_list {
    int count;
    object_detect_result_t results[128];
} object_detect_result_list;

int init_yolo_model(const char *model_path, rknn_app_context_t *app_ctx);
int release_yolo_model(rknn_app_context_t *app_ctx);
int inference_yolo_model(rknn_app_context_t *app_ctx, image_buffer_t *img, object_detect_result_list *od_results);

#endif //_RKNN_DEMO_MOBILENET_H_