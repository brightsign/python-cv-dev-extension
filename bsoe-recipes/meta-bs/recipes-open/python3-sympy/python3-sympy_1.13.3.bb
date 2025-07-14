SUMMARY = "Python library for symbolic mathematics"
DESCRIPTION = "SymPy is a Python library for symbolic mathematics. It aims to become a full-featured computer algebra system"
HOMEPAGE = "https://sympy.org/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=ea48085d7dff75b49271b25447e8cdca"
SECTION = "devel/python"

PYPI_PACKAGE = "sympy"

SRC_URI[sha256sum] = "b27fd2c6530e0ab39e275fc9b683895367e51d5da91baa8d3d64db2565fec4d9"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
    python3-mpmath \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"