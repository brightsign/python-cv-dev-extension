SUMMARY = "Image processing in Python"
DESCRIPTION = "scikit-image is a collection of algorithms for image processing. \
It is available free of charge and free of restriction. We pride ourselves on \
high-quality, peer-reviewed code, written by an active community of volunteers."
HOMEPAGE = "https://github.com/scikit-image/scikit-image"
SECTION = "devel/python"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=850ce197dcedf0246c6db98622c1a2c9"

PV = "0.17.2"

SRC_URI = "https://files.pythonhosted.org/packages/82/58/a4b01da2a8e1477620e131b9c7b4920dd0f435f2b93463eaa28bc9e52e95/scikit-image-${PV}.tar.gz"
SRC_URI[sha256sum] = "bd954c0588f0f7e81d9763dc95e06950e68247d540476e06cb77bcbcd8c2d8b3"

S = "${WORKDIR}/scikit-image-${PV}"

inherit setuptools3

DEPENDS += " \
    python3-numpy-native \
    python3-cython-native \
    python3-wheel-native \
"

RDEPENDS:${PN} += " \
    python3-numpy \
    python3-networkx \
    python3-pillow \
    python3-imageio \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "already-stripped file-rdeps arch installed-vs-shipped"

# Inhibit stripping for binary components
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_PACKAGE_STRIP = "1"

# This package requires compilation
PACKAGE_ARCH = "${MACHINE_ARCH}"