SUMMARY = "FlatBuffers - cross platform serialization library"
DESCRIPTION = "FlatBuffers is a cross platform serialization library architected for maximum memory efficiency"
HOMEPAGE = "https://flatbuffers.dev/"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://PKG-INFO;beginline=8;endline=8;md5=9a6ea5b6c346a830f54cc95f6a2a9245"
SECTION = "devel/python"

PYPI_PACKAGE = "flatbuffers"

SRC_URI[md5sum] = "8bdc5a43c748531d4d08a5e47916ca2e"
SRC_URI[sha256sum] = "de2ec5b203f21441716617f38443e0a8ebf3d25bf0d9c0bb0ce68fa00ad546a4"

inherit pypi setuptools3

# flatbuffers setup.py has absolute path issue, create fixed version
do_compile:prepend() {
    cat > ${S}/setup.py << 'EOF'
from setuptools import setup, find_packages

setup(
    name="flatbuffers",
    version="${PV}",
    packages=find_packages(),
    python_requires=">=3.6",
    license="Apache-2.0",
    description="FlatBuffers - cross platform serialization library",
    long_description="FlatBuffers is a cross platform serialization library architected for maximum memory efficiency",
    url="https://flatbuffers.dev/",
)
EOF
}

RDEPENDS:${PN} += " \
    python3-core \
"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths"

BBCLASSEXTEND = "native nativesdk"