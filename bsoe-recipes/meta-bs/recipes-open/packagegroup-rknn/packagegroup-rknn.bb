SUMMARY = "RKNN Development Package Group"
DESCRIPTION = "Package group for RKNN toolkit development and runtime components"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit packagegroup

PACKAGES = " \
    ${PN} \
    ${PN}-dev \
    ${PN}-runtime \
"

# Runtime components for target devices (ARM64)
RDEPENDS:${PN}-runtime = " \
    python3-rknn-toolkit2 \
    python3-numpy \
    python3-scipy \
    python3-opencv \
    python3-pillow \
    python3-core \
    python3-ctypes \
    python3-threading \
    python3-ruamel-yaml \
    python3-pyyaml \
    python3-psutil \
    python3-protobuf \
    python3-tqdm \
    python3-typing-extensions \
    python3-flatbuffers \
    python3-jinja2 \
    python3-markupsafe \
    python3-filelock \
    python3-fsspec \
    python3-networkx \
    python3-mpmath \
    python3-fast-histogram \
"

# Development components (extended runtime for development)
RDEPENDS:${PN}-dev = " \
    ${PN}-runtime \
    python3-torch \
    python3-onnx \
    python3-onnxruntime \
    python3-onnxoptimizer \
    python3-sympy \
    python3-json \
    python3-logging \
    python3-setuptools \
    python3-pip \
"

# Default package includes runtime components
RDEPENDS:${PN} = " \
    ${PN}-runtime \
"

