#pragma once

#include <string>
#include <memory>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

// Abstract transport interface for sending data
class Transport {
public:
    virtual ~Transport() = default;
    virtual bool send(const std::string& data) = 0;
    virtual bool isConnected() const = 0;
};

// UDP transport implementation
class UDPTransport : public Transport {
public:
    UDPTransport(const std::string& ip, int port);
    ~UDPTransport();
    
    bool send(const std::string& data) override;
    bool isConnected() const override;

private:
    void setupSocket(const std::string& ip, int port);
    
    int sockfd;
    struct sockaddr_in servaddr;
    bool connected;
};

// File transport implementation - writes to specified file path
class FileTransport : public Transport {
public:
    explicit FileTransport(const std::string& filepath);
    ~FileTransport() = default;
    
    bool send(const std::string& data) override;
    bool isConnected() const override;

private:
    std::string filepath;
    bool enabled;
};