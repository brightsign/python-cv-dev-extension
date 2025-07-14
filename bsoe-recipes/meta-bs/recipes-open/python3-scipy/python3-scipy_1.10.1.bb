SUMMARY = "Scientific computation library for Python"
DESCRIPTION = "SciPy is open-source software for mathematics, science, and engineering"
HOMEPAGE = "https://scipy.org/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

PYPI_PACKAGE = "scipy"

SRC_URI[md5sum] = "de3db61d840456634ba37f2b5816e049"
SRC_URI[sha256sum] = "2cf9dfb80a7b4589ba4c40ce7588986d6d5cebc5457cad2c2880f6bc2d42f3a5"

inherit pypi setuptools3

DEPENDS += " \
    python3-numpy-native \
    python3-pybind11-native \
    python3-cython-native \
    python3-pip-native \
"

# Ensure Cython is available in PATH and create cython symlink
do_compile:prepend() {
    export PATH="${STAGING_BINDIR_NATIVE}:${PATH}"
    # Create cython symlink if it doesn't exist
    if [ ! -f "${STAGING_BINDIR_NATIVE}/cython" ] && [ -f "${STAGING_BINDIR_NATIVE}/cython3" ]; then
        ln -sf cython3 "${STAGING_BINDIR_NATIVE}/cython"
    fi
}

RDEPENDS:${PN} += " \
    python3-numpy \
    python3-core \
    python3-math \
    python3-ctypes \
    python3-threading \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# SciPy uses compiled C/Fortran extensions
INSANE_SKIP:${PN} += "buildpaths dev-so already-stripped file-rdeps arch installed-vs-shipped"

BBCLASSEXTEND = "native nativesdk"