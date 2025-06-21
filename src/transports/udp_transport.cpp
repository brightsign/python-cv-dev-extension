#include "transport.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstring>
#include <iostream>

UDPTransport::UDPTransport(const std::string& ip, int port) 
    : sockfd(-1), connected(false) {
    setupSocket(ip, port);
}

UDPTransport::~UDPTransport() {
    if (sockfd >= 0) {
        close(sockfd);
    }
}

void UDPTransport::setupSocket(const std::string& ip, int port) {
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        std::cerr << "UDP Socket creation failed" << std::endl;
        return;
    }

    memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(port);  
    servaddr.sin_addr.s_addr = inet_addr(ip.c_str());
    
    connected = true;
}

bool UDPTransport::send(const std::string& data) {
    if (!connected || sockfd < 0) {
        return false;
    }
    
    ssize_t sent = sendto(sockfd, data.c_str(), data.length(), 0,
                         (struct sockaddr*)&servaddr, sizeof(servaddr));
    
    return sent == static_cast<ssize_t>(data.length());
}

bool UDPTransport::isConnected() const {
    return connected && sockfd >= 0;
}