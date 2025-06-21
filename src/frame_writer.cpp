#include "frame_writer.h"
#include "inference.h"
#include <cstdio>
#include <cstring>

void DecoratedFrameWriter::writeFrame(cv::Mat& frame, const InferenceResult& result) {
    // Draw boxes on the image for detected objects
    for (int i = 0; i < result.detections.count; i++) {
        try {
            auto color = cv::Scalar(0, 255, 0);  // green for detected objects
            auto& box = result.detections.results[i].box;
            
            // Validate box coordinates
            if (box.left < 0 || box.top < 0 || box.right >= frame.cols || box.bottom >= frame.rows ||
                box.left >= box.right || box.top >= box.bottom) {
                printf("Warning: Invalid bounding box coordinates: left=%d, top=%d, right=%d, bottom=%d\n",
                       box.left, box.top, box.right, box.bottom);
                continue;  // Skip this detection
            }
            
            // Draw bounding box
            cv::rectangle(frame, cv::Point(box.left, box.top), cv::Point(box.right, box.bottom), color, 2);
            
            // Validate name pointer before using
            const char* name_ptr = result.detections.results[i].name;
            std::string obj_name = (name_ptr && strlen(name_ptr) < 100) ? name_ptr : "unknown";
            
            // Draw label with confidence score
            char text[256];
            snprintf(text, sizeof(text), "%.2f", result.detections.results[i].score);
            std::string label_text = obj_name + ": " + text;
            
            // Make sure label is drawn inside the image
            int baseline = 0;
            cv::Size text_size = cv::getTextSize(label_text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 2, &baseline);
            int y_pos = std::max(box.top - 10, text_size.height);
            
            cv::putText(frame, label_text, cv::Point(box.left, y_pos), 
                        cv::FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
        } catch (const std::exception& e) {
            printf("Exception while drawing box %d: %s\n", i, e.what());
        }
    }

    // Convert back to BGR for OpenCV image writing
    cv::cvtColor(frame, frame, cv::COLOR_RGB2BGR);
    
    // Write processed image to temporary file then rename atomically
    // Preserve extension for OpenCV codec detection by inserting .tmp before extension
    size_t last_dot = output_path.find_last_of('.');
    std::string temp_path;
    if (last_dot != std::string::npos) {
        temp_path = output_path.substr(0, last_dot) + ".tmp" + output_path.substr(last_dot);
    } else {
        // No extension found, assume .jpg
        temp_path = output_path + ".tmp.jpg";
    }
    cv::imwrite(temp_path, frame);
    std::rename(temp_path.c_str(), output_path.c_str());
}