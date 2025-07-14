SUMMARY = "Python library for arbitrary-precision floating-point arithmetic"
DESCRIPTION = "Mpmath is a pure-Python library for multiprecision floating-point arithmetic"
HOMEPAGE = "https://mpmath.org/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=bde3c575382996b75d85702949512751"
SECTION = "devel/python"

PYPI_PACKAGE = "mpmath"

SRC_URI[sha256sum] = "7a28eb2a9774d00c7bc92411c19a89209d5da7c4c9a9e227be8330a23a25b91f"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"