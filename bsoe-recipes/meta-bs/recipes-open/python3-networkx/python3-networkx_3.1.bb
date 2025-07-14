SUMMARY = "Python package for creating and manipulating graphs and networks"
DESCRIPTION = "NetworkX is a Python package for the creation, manipulation, and study of the structure, dynamics, and functions of complex networks"
HOMEPAGE = "https://networkx.org/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=4266362445d56549f7b8973d02e5f22a"
SECTION = "devel/python"

PYPI_PACKAGE = "networkx"

SRC_URI[sha256sum] = "de346335408f84de0eada6ff9fafafff9bcda11f0a0dfaa931133debb146ab61"

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