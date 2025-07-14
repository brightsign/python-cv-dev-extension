SUMMARY = "A platform independent file lock"
DESCRIPTION = "This package contains a single module, which implements a platform independent file lock in Python"
HOMEPAGE = "https://github.com/tox-dev/py-filelock"
LICENSE = "Unlicense"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Unlicense;md5=7246f848faa4e9c9fc0ea91122d6e680"
SECTION = "devel/python"

PYPI_PACKAGE = "filelock"

SRC_URI[md5sum] = "4c66a5abfc4004cbb0cc30d22e472031"
SRC_URI[sha256sum] = "c249fbfcd5db47e5e2d6d62198e565475ee65e4831e2561c8e313fa7eb961435"

inherit pypi setuptools3 python3native

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# No custom setup.py needed, setuptools3 handles pyproject.toml
# No custom RDEPENDS needed, they are determined automatically

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"