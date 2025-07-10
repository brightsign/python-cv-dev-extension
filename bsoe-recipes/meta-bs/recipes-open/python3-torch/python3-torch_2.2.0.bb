SUMMARY = "PyTorch - an optimized tensor library for deep learning"
DESCRIPTION = "PyTorch is a Python package that provides tensor computation with strong GPU acceleration and deep neural networks built on tape-based autograd system"
HOMEPAGE = "https://pytorch.org"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

# Download ARM64 wheel file for target architecture
SRC_URI = "https://files.pythonhosted.org/packages/2a/2a/b6064e03a71d2dc4936975c667703f333ce663977ce489b50090daee332f/torch-2.2.0-cp310-cp310-manylinux2014_aarch64.whl;downloadfilename=torch-${PV}-aarch64.whl;unpack=0"

SRC_URI[sha256sum] = "707f2f80402981e9f90d0038d7d481678586251e6642a7a6ef67fc93511cb446"

inherit python3-dir

DEPENDS += "unzip-native"

RDEPENDS:${PN} += " \
    python3-filelock \
    python3-fsspec \
    python3-typing-extensions \
    python3-jinja2 \
    python3-sympy \
    python3-networkx \
    python3-core \
    python3-ctypes \
    python3-threading \
"

# Only for ARM64 targets
COMPATIBLE_MACHINE = "cobra|pantera"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    
    # Extract ARM64 wheel file
    WHEEL_FILE="${WORKDIR}/torch-${PV}-aarch64.whl"
    
    if [ -f "${WHEEL_FILE}" ]; then
        # Extract wheel to temporary directory
        TEMP_DIR=${WORKDIR}/wheel_extract
        rm -rf ${TEMP_DIR}
        mkdir -p ${TEMP_DIR}
        cd ${TEMP_DIR}
        
        unzip -o -q "${WHEEL_FILE}"
        
        # Copy the torch package directory
        if [ -d "torch" ]; then
            cp -r torch ${D}${PYTHON_SITEPACKAGES_DIR}/
        else
            bbfatal "torch directory not found in wheel"
        fi
        
        # Copy the package metadata (.dist-info directory)
        if [ -d "torch-${PV}.dist-info" ]; then
            cp -r torch-${PV}.dist-info ${D}${PYTHON_SITEPACKAGES_DIR}/
        fi
        
        # Verify installation succeeded
        if [ ! -d "${D}${PYTHON_SITEPACKAGES_DIR}/torch" ]; then
            bbfatal "PyTorch installation failed - torch directory not found"
        fi
    else
        bbfatal "PyTorch ARM64 wheel file not found: ${WHEEL_FILE}"
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