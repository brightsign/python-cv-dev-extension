#include "publisher.h"

#include <iostream>
#include <thread>

// Implementation of the JsonMessageFormatter
std::string JsonMessageFormatter::formatMessage(const InferenceResult& result) {
    json j;
    
    // Add timestamp
    j["timestamp"] = std::chrono::system_clock::to_time_t(result.timestamp);
    
    // Serialize the detection results
    json detection_results;
    json results_array = json::array();
    int valid_count = 0;
    
    // Filter and serialize only valid detections if suppress_empty is enabled
    for (int i = 0; i < result.detections.count; ++i) {
        const auto& detection = result.detections.results[i];
        
        // If suppress_empty is enabled, filter out detections with score 0 or class_id 0
        if (suppress_empty && (detection.score == 0.0f || detection.class_id == 0)) {
            continue;
        }
        
        json detection_obj;
        
        // Box coordinates
        detection_obj["box"] = {
            {"left", detection.box.left},
            {"top", detection.box.top}, 
            {"right", detection.box.right},
            {"bottom", detection.box.bottom}
        };
        
        // Detection properties
        detection_obj["score"] = detection.score;
        detection_obj["class_id"] = detection.class_id;
        detection_obj["name"] = std::string(detection.name);  // Convert char array to string
        
        results_array.push_back(detection_obj);
        valid_count++;
    }
    
    // Set the count to the number of valid detections
    detection_results["count"] = valid_count;
    detection_results["results"] = results_array;
    
    // Set the complete detection results as the main object
    j["object_detect_result_list"] = detection_results;
    
    return j.dump();
}

// Implementation of the BSVariableMessageFormatter
std::string BSVariableMessageFormatter::formatMessage(const InferenceResult& result) {
    // format the message as a string like detection_count:2!!timestamp:1746732409
    std::string message = 
        "detection_count:" + std::to_string(result.detections.count) + "!!" +
        "timestamp:" + std::to_string(std::chrono::system_clock::to_time_t(result.timestamp));
    return message;
}

// Generic Publisher implementation
Publisher::Publisher(
        std::shared_ptr<Transport> transport,
        ThreadSafeQueue<InferenceResult>& queue, 
        std::atomic<bool>& isRunning,
        std::shared_ptr<MessageFormatter> formatter,
        int messages_per_second)
    : transport(transport),
      resultQueue(queue), 
      running(isRunning), 
      target_mps(messages_per_second),
      formatter(formatter) {
}

void Publisher::operator()() {
    InferenceResult result;
    while (resultQueue.pop(result)) {
        if (!transport->isConnected()) {
            std::cerr << "Transport not connected, skipping message" << std::endl;
            continue;
        }
        
        std::string message = formatter->formatMessage(result);
        
        if (!transport->send(message)) {
            std::cerr << "Failed to send message via transport" << std::endl;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(1000 / target_mps));
    }
}

// UDPPublisher backward compatibility wrapper
UDPPublisher::UDPPublisher(
        const std::string& ip,
        const int port,
        ThreadSafeQueue<InferenceResult>& queue, 
        std::atomic<bool>& isRunning,
        std::shared_ptr<MessageFormatter> formatter,
        int messages_per_second)
    : Publisher(std::make_shared<UDPTransport>(ip, port), queue, isRunning, formatter, messages_per_second) {
}