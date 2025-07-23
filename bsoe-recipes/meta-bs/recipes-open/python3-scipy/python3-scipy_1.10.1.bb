SUMMARY = "Scientific computation library for Python"
DESCRIPTION = "SciPy is open-source software for mathematics, science, and engineering"
HOMEPAGE = "https://scipy.org/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

# Version matches what pip installs (1.10.1)
PV = "1.10.1"

# Use pre-built wheel for ARM64 - this is what pip uses
SRC_URI = "https://files.pythonhosted.org/packages/5b/bd/c9e711b2a5cc1daa7dc7db3bb12ba25a7ae38e112c76f70eb9ab0c142dac/scipy-1.10.1-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
SRC_URI[sha256sum] = "2cf9dfb80a7b4589ba4c40ce7588986d6d5cebc5457cad2c2880f6bc2d42f3a5"

# Inherit python3native for cross-compilation support
inherit python3native

# Since we're using a pre-built wheel, we need to handle it specially
S = "${WORKDIR}"

# Don't extract the wheel, we'll install it directly
do_unpack() {
    # Copy the wheel to the work directory
    cp ${WORKDIR}/scipy-1.10.1-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl ${S}/
}

# Extract and install the wheel manually (cross-compilation safe)
do_install() {
    # Extract the wheel
    cd ${S}
    ${PYTHON} -m zipfile -e scipy-1.10.1-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl extracted/
    
    # Install Python modules
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r extracted/scipy ${D}${PYTHON_SITEPACKAGES_DIR}/
    
    # Install metadata if it exists
    if [ -d extracted/scipy-1.10.1.dist-info ]; then
        install -d ${D}${PYTHON_SITEPACKAGES_DIR}/scipy-1.10.1.dist-info
        cp -r extracted/scipy-1.10.1.dist-info/* ${D}${PYTHON_SITEPACKAGES_DIR}/scipy-1.10.1.dist-info/
    fi
}

DEPENDS = "python3-pip-native python3-setuptools-native"

RDEPENDS:${PN} += " \
    python3-core \
    python3-math \
    python3-ctypes \
    python3-threading \
    python3-pickle \
    python3-json \
    python3-compression \
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