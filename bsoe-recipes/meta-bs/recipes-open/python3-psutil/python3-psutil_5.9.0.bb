SUMMARY = "Cross-platform lib for process and system monitoring in Python"
DESCRIPTION = "psutil (process and system utilities) is a cross-platform library for retrieving information on running processes and system utilization"
HOMEPAGE = "https://github.com/giampaolo/psutil"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e35fd9f271d19d5f742f20a9d1f8bb8b"
SECTION = "devel/python"

PYPI_PACKAGE = "psutil"

SRC_URI[sha256sum] = "869842dbd66bb80c3217158e629d6fceaecc3a3166d3d1faee515b05dd26ca25"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
    python3-ctypes \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"