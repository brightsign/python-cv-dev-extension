#include <chrono>
#include <memory>
#include <sys/time.h>
#include <iostream>
#include <filesystem>
#include <fstream>
#include <stdio.h>
#include <string>
#include <thread>
#include <cstring>
#include <signal.h>

#include "image_utils.h"
#include "inference.h"
#include "publisher.h"
#include "queue.h"
#include "transport.h"
#include "utils.h"
#include "yolo.h"

#include <opencv2/opencv.hpp>


std::atomic<bool> running{true};
ThreadSafeQueue<InferenceResult> resultQueue(1);

void signalHandler(int signum) {
    std::cout << "Interrupt signal (" << signum << ") received.\n";

    // Cleanup and shutdown
    running = false;
    resultQueue.signalShutdown();
}

int main(int argc, char **argv) {
    char *model_name = NULL;
    bool suppress_empty = false;
    bool is_file_input = false;
    
    if (argc < 3 || argc > 4) {
        printf("Usage: %s <rknn model> <source> [--suppress-empty]\n", argv[0]);
        printf("  <source>: V4L device (e.g. /dev/video0) or image file (e.g. /tmp/bus.jpg)\n");
        printf("  --suppress-empty: suppress output when no detections (optional)\n");
        return -1;
    }

    // The path where the model is located
    model_name = (char *)argv[1];
    char *source_name = argv[2];
    
    // Check for suppress-empty flag
    if (argc == 4 && strcmp(argv[3], "--suppress-empty") == 0) {
        suppress_empty = true;
        printf("Suppress-empty mode enabled\n");
    }
    
    // Set up signal handler
    signal(SIGINT, signalHandler);
    signal(SIGTERM, signalHandler);
    
    // Determine if source is a file or device
    if (strstr(source_name, "/dev/video") == source_name) {
        is_file_input = false;
        printf("Using V4L device: %s\n", source_name);
    } else if (std::filesystem::exists(source_name)) {
        is_file_input = true;
        printf("Using image file: %s\n", source_name);
    } else {
        printf("Error: Source '%s' is neither a valid V4L device nor an existing file\n", source_name);
        return -1;
    }

    // Create frame writer for decorated output
    auto frameWriter = std::make_shared<DecoratedFrameWriter>("/tmp/output.jpg", suppress_empty);
    
    if (is_file_input) {
        // Single-shot inference mode for file input
        MLInferenceThread mlThread(
            model_name,
            source_name,
            resultQueue, 
            running,
            1, // Single frame
            frameWriter);
        
        // Create formatters
        auto json_formatter = std::make_shared<JsonMessageFormatter>(suppress_empty);
        
        // Create file publisher using transport injection
        auto file_transport = std::make_shared<FileTransport>("/tmp/results.json");
        Publisher file_publisher(
            file_transport,
            resultQueue,
            running,
            json_formatter,
            1);
        
        // Run single inference and exit
        mlThread.runSingleInference();
        
        // Process result if any
        std::thread file_publisherThread(std::ref(file_publisher));
        std::this_thread::sleep_for(std::chrono::milliseconds(500)); // Give time for processing
        running = false;
        resultQueue.signalShutdown();
        file_publisherThread.join();
        
    } else {
        // Continuous inference mode for video device
        MLInferenceThread mlThread(
            model_name,
            source_name,
            resultQueue, 
            running,
            30,
            frameWriter);

        // Create formatters
        auto json_formatter = std::make_shared<JsonMessageFormatter>(suppress_empty);
        
        // Create file publisher using transport injection
        auto file_transport = std::make_shared<FileTransport>("/tmp/results.json");
        Publisher file_publisher(
            file_transport,
            resultQueue,
            running,
            json_formatter,
            1); // Write to file once per second

        std::thread inferenceThread(std::ref(mlThread));
        std::thread file_publisherThread(std::ref(file_publisher));

        while (running) {
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }

        // Cleanup and shutdown
        running = false;
        resultQueue.signalShutdown();

        inferenceThread.join();
        file_publisherThread.join();
    }

    return 0;
}