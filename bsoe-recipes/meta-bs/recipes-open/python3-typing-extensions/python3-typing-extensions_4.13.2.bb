SUMMARY = "Backported and Experimental Type Hints for Python"
DESCRIPTION = "The typing_extensions module contains both backports of these changes as well as experimental types that will eventually be added to the typing module"
HOMEPAGE = "https://github.com/python/typing_extensions"
LICENSE = "Python-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Python-2.0;md5=a5c8025e305fb49e6d405769358851f6"
SECTION = "devel/python"

PYPI_PACKAGE = "typing_extensions"

SRC_URI[md5sum] = "58226788d248cee8d6283ee616543975"
SRC_URI[sha256sum] = "e6c81219bd689f51865d9e372991c540bda33a0379d5573cddb9a3a23f7caaef"

inherit pypi setuptools3

# typing-extensions uses flit build system, create a setup.py for compatibility
do_compile:prepend() {
    cat > ${S}/setup.py << 'EOF'
from setuptools import setup
import os

# Default version
version = "${PV}"

# Try to read version from src/typing_extensions.py
version_file = "src/typing_extensions.py"
if os.path.exists(version_file):
    with open(version_file) as f:
        for line in f:
            if line.startswith("__version__"):
                version = line.split('"')[1]
                break

setup(
    name="typing_extensions",
    version=version,
    py_modules=["typing_extensions"],
    package_dir={"": "src"},
    python_requires=">=3.7",
)
EOF
}

RDEPENDS:${PN} += " \
    python3-core \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

# Allow this recipe to override base system typing-extensions
DEFAULT_PREFERENCE = "10"

BBCLASSEXTEND = "native nativesdk"