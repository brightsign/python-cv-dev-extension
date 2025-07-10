SUMMARY = "Optimize ONNX models"
DESCRIPTION = "A library for optimizing ONNX models"
HOMEPAGE = "https://github.com/onnx/optimizer"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"
SECTION = "devel/python"

PYPI_PACKAGE = "onnxoptimizer"

SRC_URI[md5sum] = "a198a7da32dc1d53aeb4a047da50f8ff"
SRC_URI[sha256sum] = "a9f972b2b68ceb82b1f268042879f807fceb9ad76e38def2a39f102e62216d21"

inherit pypi setuptools3

DEPENDS += " \
    python3-onnx-native \
    python3-protobuf-native \
    python3-pytest-runner-native \
"

# onnxoptimizer setup.py has setup_requires issue
do_configure:append() {
    # Remove setup_requires from setup.py
    if [ -f ${S}/setup.py ]; then
        sed -i '/setup_requires/d' ${S}/setup.py
        sed -i '/pytest-runner/d' ${S}/setup.py
    fi
}

RDEPENDS:${PN} += " \
    python3-onnx \
    python3-core \
    python3-protobuf \
"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths"

BBCLASSEXTEND = "native nativesdk"