SUMMARY = "RKNN Toolkit 2 - Rockchip Neural Network Toolkit"
DESCRIPTION = "RKNN Toolkit 2 is a Python library for model conversion and inference on Rockchip NPU"
HOMEPAGE = "https://github.com/airockchip/rknn_model_zoo"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Proprietary;md5=0557f9d92cf58f2ccdd50f62f8ac0b28"
SECTION = "devel/python"

# Version matches RKNN Toolkit 2 v2.3.0 for Python 3.8 ARM64
PV = "2.3.0"

# Pre-built wheel for ARM64 from RKNN Toolkit 2
SRC_URI = "file://rknn_toolkit2-2.3.0-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"

# Inherit python3native for cross-compilation support
inherit python3native

# Since we're using a pre-built wheel, we need to handle it specially
S = "${WORKDIR}"

# Don't extract the wheel, we'll install it directly
do_unpack() {
    # Copy the wheel to the work directory
    cp ${WORKDIR}/rknn_toolkit2-2.3.0-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl ${S}/
}

# Extract and install the wheel manually (cross-compilation safe)
do_install() {
    # Extract the wheel
    cd ${S}
    ${PYTHON} -m zipfile -e rknn_toolkit2-2.3.0-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl extracted/
    
    # Install Python modules
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r extracted/rknn ${D}${PYTHON_SITEPACKAGES_DIR}/
    
    # Install metadata
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}/rknn_toolkit2-2.3.0.dist-info
    cp -r extracted/rknn_toolkit2-2.3.0.dist-info/* ${D}${PYTHON_SITEPACKAGES_DIR}/rknn_toolkit2-2.3.0.dist-info/
}

DEPENDS = "python3-pip-native python3-setuptools-native"

RDEPENDS:${PN} += " \
    python3-core \
    python3-protobuf \
    python3-ruamel-yaml \
    python3-ctypes \
    python3-threading \
    python3-io \
    python3-json \
    python3-logging \
    librknnrt \
"

# Dependencies to add later:
# RDEPENDS: python3-numpy \
#

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# This is a pre-built binary wheel, skip QA checks
INSANE_SKIP:${PN} += "already-stripped file-rdeps arch installed-vs-shipped ldflags"

# Inhibit stripping for binary components
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"

# This is architecture-specific
PACKAGE_ARCH = "${MACHINE_ARCH}"