SUMMARY = "Image processing in Python"
DESCRIPTION = "scikit-image is a collection of algorithms for image processing. \
It is available free of charge and free of restriction. We pride ourselves on \
high-quality, peer-reviewed code, written by an active community of volunteers."
HOMEPAGE = "https://github.com/scikit-image/scikit-image"
SECTION = "devel/python"
LICENSE = "BSD-3-Clause"
# LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=PLACEHOLDER_WILL_UPDATE_AFTER_FETCH"

PYPI_PACKAGE = "scikit_image"
PV = "0.19.3"

SRC_URI[sha256sum] = "4eb877c98d1395769daef5bc2ba8a7efd3f736c87086aecb3775a9174593398b"

inherit pypi setuptools3

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