#pragma once

#include <string>
#include <atomic>
#include <memory>
#include <nlohmann/json.hpp>

#include "inference.h"
#include "transport.h"

using json = nlohmann::json;

// Abstract message formatter interface
class MessageFormatter {
public:
    virtual ~MessageFormatter() = default;
    virtual std::string formatMessage(const InferenceResult& result) = 0;
};

// Concrete implementation of MessageFormatter for JSON format
class JsonMessageFormatter : public MessageFormatter {
private:
    bool suppress_empty;
    
public:
    explicit JsonMessageFormatter(bool suppress_empty = false) : suppress_empty(suppress_empty) {}
    std::string formatMessage(const InferenceResult& result) override;
};

// Concrete implementation of MessageFormatter for BrightScript variable format
//  e.g. "faces_attending:0!!faces_in_frame_total:0!!timestamp:1746732409"
class BSVariableMessageFormatter : public MessageFormatter {
public:
    std::string formatMessage(const InferenceResult& result) override;
};


// Generic publisher class using transport injection
class Publisher {
public:
    Publisher(
        std::shared_ptr<Transport> transport,
        ThreadSafeQueue<InferenceResult>& queue,
        std::atomic<bool>& isRunning,
        std::shared_ptr<MessageFormatter> formatter,
        int messages_per_second = 1);
    
    ~Publisher() = default;
    
    void operator()();

private:
    std::shared_ptr<Transport> transport;
    ThreadSafeQueue<InferenceResult>& resultQueue;
    std::atomic<bool>& running;
    int target_mps;
    std::shared_ptr<MessageFormatter> formatter;
};

// Backward compatibility: UDPPublisher using transport injection
class UDPPublisher : public Publisher {
public:
    UDPPublisher(
        const std::string& ip,
        const int port,
        ThreadSafeQueue<InferenceResult>& queue,
        std::atomic<bool>& isRunning,
        std::shared_ptr<MessageFormatter> formatter,
        int messages_per_second = 1);
};