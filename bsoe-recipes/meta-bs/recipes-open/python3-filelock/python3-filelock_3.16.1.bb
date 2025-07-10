SUMMARY = "A platform independent file lock"
DESCRIPTION = "This package contains a single module, which implements a platform independent file lock in Python"
HOMEPAGE = "https://github.com/tox-dev/py-filelock"
LICENSE = "Unlicense"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Unlicense;md5=7246f848faa4e9c9fc0ea91122d6e680"
SECTION = "devel/python"

PYPI_PACKAGE = "filelock"

SRC_URI[md5sum] = "4c66a5abfc4004cbb0cc30d22e472031"
SRC_URI[sha256sum] = "c249fbfcd5db47e5e2d6d62198e565475ee65e4831e2561c8e313fa7eb961435"

inherit pypi setuptools3

# filelock uses pyproject.toml, create setup.py for compatibility
do_compile:prepend() {
    cat > ${S}/setup.py << 'EOF'
from setuptools import setup, find_packages

setup(
    name="filelock",
    version="${PV}",
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    python_requires=">=3.8",
)
EOF
}

RDEPENDS:${PN} += " \
    python3-core \
    python3-threading \
    python3-io \
"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths"

BBCLASSEXTEND = "native nativesdk"