#ifndef _POSTPROCESS_H_
#define _POSTPROCESS_H_

#include "rknn_api.h"
#include "common.h"
#include "yolo.h"
#include "image_utils.h"

void post_process(rknn_app_context_t* app_ctx, rknn_output* outputs, letterbox_t* letter_box, 
                  float conf_threshold, float nms_threshold, object_detect_result_list* od_results);

#endif // _POSTPROCESS_H_