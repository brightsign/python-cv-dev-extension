SUMMARY = "Fast simple 1D and 2D histograms"
DESCRIPTION = "A fast histogram calculation library for Python using NumPy"
HOMEPAGE = "https://github.com/astrofrog/fast-histogram"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-2-Clause;md5=8bbc9bc34a47c84f4f2b9a35c9dd1a12"
SECTION = "devel/python"

# Version matches what pip installs
PV = "0.11"

# Use pre-built wheel for ARM64 - this is what pip uses
SRC_URI = "https://files.pythonhosted.org/packages/aa/4c/e1de06a5de31b4e87a88d73b54f2b6b6c30e1da16e23bfb06a47c3dc0aef/fast_histogram-0.11-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
SRC_URI[sha256sum] = "9acb6fa5b6efd928663008965da186962bdeae20e6d5bbb3b1195dfbd1d906f0"

# Inherit python3native for cross-compilation support
inherit python3native

# Since we're using a pre-built wheel, we need to handle it specially
S = "${WORKDIR}"

# Don't extract the wheel, we'll install it directly
do_unpack() {
    # Copy the wheel to the work directory
    cp ${WORKDIR}/fast_histogram-0.11-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl ${S}/
}

# Extract and install the wheel manually (cross-compilation safe)
do_install() {
    # Extract the wheel
    cd ${S}
    ${PYTHON} -m zipfile -e fast_histogram-0.11-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl extracted/

    # Install Python modules
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r extracted/fast_histogram ${D}${PYTHON_SITEPACKAGES_DIR}/

    # Install metadata if it exists
    if [ -d extracted/fast_histogram-0.11.dist-info ]; then
        install -d ${D}${PYTHON_SITEPACKAGES_DIR}/fast_histogram-0.11.dist-info
        cp -r extracted/fast_histogram-0.11.dist-info/* ${D}${PYTHON_SITEPACKAGES_DIR}/fast_histogram-0.11.dist-info/
    fi
}

DEPENDS = "python3-pip-native python3-setuptools-native"

RDEPENDS:${PN} += " \
    python3-core \
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