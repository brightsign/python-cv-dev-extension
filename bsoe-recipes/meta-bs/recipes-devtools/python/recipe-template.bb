# Recipe template for Python packages
# Copy this file and rename to python3-PACKAGENAME_VERSION.bb
# Replace all instances of TEMPLATE with your package details

SUMMARY = "TEMPLATE_SUMMARY"
HOMEPAGE = "TEMPLATE_HOMEPAGE"
LICENSE = "TEMPLATE_LICENSE"
LIC_FILES_CHKSUM = "file://LICENSE;md5=TEMPLATE_MD5"

# For PyPI packages, use this pattern:
PYPI_PACKAGE = "TEMPLATE_PACKAGE_NAME"
inherit pypi setuptools3

# For source-based packages:
SRC_URI = "https://files.pythonhosted.org/packages/source/${PYPI_PACKAGE_EXT}/${PYPI_PACKAGE}/${PYPI_PACKAGE}-${PV}.tar.gz"
SRC_URI[sha256sum] = "TEMPLATE_SHA256"

# Dependencies
DEPENDS = "python3-setuptools-native"
RDEPENDS:${PN} = "python3-core"

# Standard FILES definition for Python packages
FILES:${PN} = " \
    ${PYTHON_SITEPACKAGES_DIR}/${PYPI_PACKAGE} \
    ${PYTHON_SITEPACKAGES_DIR}/${PYPI_PACKAGE}-${PV}.dist-info \
    ${PYTHON_SITEPACKAGES_DIR}/${PYPI_PACKAGE}/* \
"

# QA skip flags for cross-compilation
INSANE_SKIP:${PN} += "already-stripped file-rdeps arch installed-vs-shipped"

# For packages with binary components:
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"

# Additional notes:
# - For packages with C extensions, add: DEPENDS += "python3-numpy-native"
# - For packages needing specific build tools: DEPENDS += "cmake-native"
# - For runtime dependencies: RDEPENDS:${PN} += "python3-numpy python3-pillow"
# - For packages with data files: FILES:${PN} += "${datadir}/${BPN}"