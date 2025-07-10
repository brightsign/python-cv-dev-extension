SUMMARY = "Seamless operability between C++11 and Python"
DESCRIPTION = "pybind11 is a lightweight header-only library that exposes C++ types in Python and vice versa"
HOMEPAGE = "https://github.com/pybind/pybind11"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

PYPI_PACKAGE = "pybind11"

SRC_URI[md5sum] = "67c58224e41c442e47fa84e7789c2c39"
SRC_URI[sha256sum] = "00cd59116a6e8155aecd9174f37ba299d1d397ed4a6b86ac1dfe01b3e40f2cc4"

DEPENDS += "python3-native"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
"

FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Enable native build support
BBCLASSEXTEND = "native"