#include <chrono>
#include <memory>
#include <sys/time.h>
#include <iostream>
#include <filesystem>
#include <fstream>
#include <stdio.h>
#include <string>
#include <thread>

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
    if (argc != 3) {
        printf("Usage: %s <rknn model> <source> \n", argv[0]);
        return -1;
    }

    // The path where the model is located
    model_name = (char *)argv[1];
    char *source_name = argv[2];

    // Create frame writer for decorated output
    auto frameWriter = std::make_shared<DecoratedFrameWriter>("/tmp/output.jpg");
    
    MLInferenceThread mlThread(
        model_name,
        source_name,
        resultQueue, 
        running,
        30,
        frameWriter);

    // Create formatters
    auto json_formatter = std::make_shared<JsonMessageFormatter>();
    
    // Create UDP publisher (backward compatibility)
    // UDPPublisher json_udp_publisher(
    //     "127.0.0.1",
    //     5002,
    //     resultQueue, 
    //     running,
    //     json_formatter,
    //     10);

    // Create file publisher using transport injection
    auto file_transport = std::make_shared<FileTransport>("/tmp/results.json");
    Publisher file_publisher(
        file_transport,
        resultQueue,
        running,
        json_formatter,
        1); // Write to file once per second

    std::thread inferenceThread(std::ref(mlThread));
    // std::thread udp_publisherThread(std::ref(json_udp_publisher));
    std::thread file_publisherThread(std::ref(file_publisher));

    while (running) {
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }

    // Cleanup and shutdown
    running = false;
    resultQueue.signalShutdown();

    inferenceThread.join();
    // udp_publisherThread.join();
    file_publisherThread.join();

    return 0;
}