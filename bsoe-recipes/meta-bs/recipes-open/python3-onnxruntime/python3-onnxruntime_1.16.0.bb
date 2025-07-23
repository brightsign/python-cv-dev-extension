SUMMARY = "ONNX Runtime - Cross-platform machine learning inferencing accelerator"
DESCRIPTION = "ONNX Runtime is a performance-focused scoring engine for ONNX models"
HOMEPAGE = "https://onnxruntime.ai"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SECTION = "devel/python"

# Download ARM64 wheel file for target architecture
SRC_URI = "https://files.pythonhosted.org/packages/51/d8/7dedff222ac4e4bdccc86bba5cbc7f972a01426af10a1b273328573730fb/onnxruntime-1.16.0-cp310-cp310-manylinux_2_17_aarch64.manylinux2014_aarch64.whl;downloadfilename=onnxruntime-${PV}-aarch64.whl;unpack=0"

SRC_URI[sha256sum] = "a40660516b382031279fb690fc3d068ad004173c2bd12bbdc0bd0fe01ef8b7c3"

inherit python3-dir

DEPENDS += "unzip-native"

RDEPENDS:${PN} += " \
    python3-protobuf \
    python3-flatbuffers \
    python3-packaging \
    python3-sympy \
    python3-core \
    python3-ctypes \
    python3-threading \
"

# Dependencies to add later:
# RDEPENDS: python3-numpy \
#

# Only for ARM64 targets
COMPATIBLE_MACHINE = "cobra|pantera"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    
    # Extract ARM64 wheel file
    WHEEL_FILE="${WORKDIR}/onnxruntime-${PV}-aarch64.whl"
    
    if [ -f "${WHEEL_FILE}" ]; then
        # Extract wheel to temporary directory
        TEMP_DIR=${WORKDIR}/wheel_extract
        rm -rf ${TEMP_DIR}
        mkdir -p ${TEMP_DIR}
        cd ${TEMP_DIR}
        
        unzip -o -q "${WHEEL_FILE}"
        
        # Copy the onnxruntime package directory
        if [ -d "onnxruntime" ]; then
            cp -r onnxruntime ${D}${PYTHON_SITEPACKAGES_DIR}/
        else
            bbfatal "onnxruntime directory not found in wheel"
        fi
        
        # Copy the package metadata (.dist-info directory)
        if [ -d "onnxruntime-${PV}.dist-info" ]; then
            cp -r onnxruntime-${PV}.dist-info ${D}${PYTHON_SITEPACKAGES_DIR}/
        fi
        
        # Verify installation succeeded
        if [ ! -d "${D}${PYTHON_SITEPACKAGES_DIR}/onnxruntime" ]; then
            bbfatal "ONNX Runtime installation failed - onnxruntime directory not found"
        fi
    else
        bbfatal "ONNX Runtime ARM64 wheel file not found: ${WHEEL_FILE}"
    fi
}

FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings for pre-compiled files
INSANE_SKIP:${PN} += "already-stripped file-rdeps dev-so arch ldflags installed-vs-shipped"

# Disable debug package generation and stripping for binary files
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"

# Enable native build support if needed
BBCLASSEXTEND = "native"