SUMMARY = "PyTorch - an optimized tensor library for deep learning"
DESCRIPTION = "PyTorch is a Python package that provides tensor computation with strong GPU acceleration and deep neural networks built on tape-based autograd system"
HOMEPAGE = "https://pytorch.org"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"

# Download ARM64 wheel file for target architecture (Python 3.8 compatible)
# Using PyTorch 2.0.1 per RKNN Model Zoo v2.3.2 requirements (torch>=1.10.1,<=2.1.0)
SRC_URI = "https://download.pytorch.org/whl/torch-2.0.1-cp38-cp38-manylinux2014_aarch64.whl;downloadfilename=torch-${PV}-aarch64.whl"

SRC_URI[sha256sum] = "0882243755ff28895e8e6dc6bc26ebcf5aa0911ed81b2a12f241fc4b09075b13"

# Use proper BitBake Python inheritance
inherit python3native

# Minimal dependencies for Python cross-compilation
DEPENDS = "python3-native"

# Runtime dependencies for PyTorch
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

# No compile step needed for a pre-built wheel
do_compile() {
    :
}

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    
    # Unpack the wheel
    ${PYTHON} -m wheel unpack ${WORKDIR}/torch-${PV}-aarch64.whl --dest ${S}
    
    UNPACKED_WHEEL_DIR=$(find ${S} -name "torch-${PV}")

    if [ -d "$UNPACKED_WHEEL_DIR" ]; then
        # Copy the contents of the unpacked directories, not the directories themselves
        cp -r $UNPACKED_WHEEL_DIR/torch ${D}${PYTHON_SITEPACKAGES_DIR}/
        cp -r $UNPACKED_WHEEL_DIR/torch-*.dist-info ${D}${PYTHON_SITEPACKAGES_DIR}/
    else
        bbfatal "Unpacked wheel directory not found in ${S}"
    fi
}

# Explicitly package the contents of the site-packages directory
FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings for pre-compiled files from wheel
INSANE_SKIP:${PN} += "already-stripped file-rdeps dev-so arch ldflags installed-vs-shipped"

# Disable debug package generation and stripping for binary files
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"