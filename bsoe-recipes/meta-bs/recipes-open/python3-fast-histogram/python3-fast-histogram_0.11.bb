SUMMARY = "Fast simple 1D and 2D histograms"
DESCRIPTION = "A fast histogram calculation library for Python using NumPy"
HOMEPAGE = "https://github.com/astrofrog/fast-histogram"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=7d8eb54c719b58c2a28fe60bf721e13c"
SECTION = "devel/python"

PYPI_PACKAGE = "fast-histogram"

SRC_URI[md5sum] = "92f8790c43ff5cecc99ce2885d90e155"
SRC_URI[sha256sum] = "9acb6fa5b6efd928663008965da186962bdeae20e6d5bbb3b1195dfbd1d906f0"

inherit pypi setuptools3 python3native

DEPENDS += " \
    python3-numpy-native \
    python3-setuptools-scm-native \
    python3-native \
"

# Ensure NumPy headers are available during build
export NUMPY_INCLUDE_PATH = "${STAGING_LIBDIR_NATIVE}/${PYTHON_DIR}/site-packages/numpy/core/include"

# Fix cross-compilation for C extensions
do_compile:prepend() {
    export NUMPY_INCLUDE_PATH="${STAGING_LIBDIR_NATIVE}/${PYTHON_DIR}/site-packages/numpy/core/include"
    export _PYTHON_SYSCONFIGDATA_NAME="_sysconfigdata"
}

RDEPENDS:${PN} += " \
    python3-numpy \
    python3-core \
"

# May need these for C extension compilation
INSANE_SKIP:${PN} += "buildpaths"

BBCLASSEXTEND = "native nativesdk"