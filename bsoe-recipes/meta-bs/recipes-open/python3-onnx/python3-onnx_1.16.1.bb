SUMMARY = "Open Neural Network Exchange (ONNX) - Python API"
DESCRIPTION = "ONNX is an open standard for machine learning interoperability"
HOMEPAGE = "https://onnx.ai"
LICENSE = "Apache-2.0 & MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
SECTION = "devel/python"

PYPI_PACKAGE = "onnx"

SRC_URI[md5sum] = "07fe47dc50e802e4a9051de9b3cdf928"
SRC_URI[sha256sum] = "8299193f0f2a3849bfc069641aa8e4f93696602da8d165632af8ee48ec7556b6"

inherit pypi setuptools3

DEPENDS += " \
    python3-protobuf-native \
    python3-numpy-native \
    protobuf-native \
    cmake-native \
    python3-pybind11-native \
"

RDEPENDS:${PN} += " \
    python3-protobuf \
    python3-numpy \
    python3-typing-extensions \
    python3-core \
    python3-ctypes \
"

# Environment variables for the build
ONNX_ML = "1"
CMAKE_ARGS = "-DONNX_ML=1 -DONNX_BUILD_TESTS=OFF -DONNX_USE_PROTOBUF_SHARED_LIBS=ON"

# Ensure proper Python build environment
do_configure:prepend() {
    export CMAKE_ARGS="-DONNX_ML=1 -DONNX_BUILD_TESTS=OFF -DONNX_USE_PROTOBUF_SHARED_LIBS=ON"
    export ONNX_ML=1
}

do_compile:prepend() {
    export CMAKE_ARGS="-DONNX_ML=1 -DONNX_BUILD_TESTS=OFF -DONNX_USE_PROTOBUF_SHARED_LIBS=ON"
    export ONNX_ML=1
}

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# ONNX uses C++ extensions that may need special handling
INSANE_SKIP:${PN} += "buildpaths dev-so already-stripped file-rdeps arch installed-vs-shipped"

BBCLASSEXTEND = "native nativesdk"