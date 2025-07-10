SUMMARY = "Filesystem Spec"
DESCRIPTION = "A specification for pythonic filesystems"
HOMEPAGE = "https://github.com/fsspec/filesystem_spec/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

PYPI_PACKAGE = "fsspec"

SRC_URI[md5sum] = "7e7383e3be5127dbbc01caf30d8850c8"
SRC_URI[sha256sum] = "fad7d7e209dd4c1208e3bbfda706620e0da5142bebbd9c384afb95b07e798e49"

inherit pypi setuptools3

# fsspec has setup.cfg, should work with setuptools3 but add setup.py for safety
do_compile:prepend() {
    if [ ! -f ${S}/setup.py ]; then
        cat > ${S}/setup.py << 'EOF'
from setuptools import setup, find_packages

setup(
    name="fsspec",
    version="${PV}",
    packages=find_packages(),
    python_requires=">=3.8",
)
EOF
    fi
}

RDEPENDS:${PN} += " \
    python3-core \
"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths"

BBCLASSEXTEND = "native nativesdk"