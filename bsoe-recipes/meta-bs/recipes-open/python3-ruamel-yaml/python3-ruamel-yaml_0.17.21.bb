SUMMARY = "YAML parser/emitter that supports roundtrip preservation of comments, seq/map flow style, and map key order"
DESCRIPTION = "ruamel.yaml is a YAML 1.2 parser/emitter that supports roundtrip preservation of comments, seq/map flow style, and map key order"
HOMEPAGE = "https://sourceforge.net/projects/ruamel-yaml/"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=034154b7344d15438bc5ed5ee9cc075f"
SECTION = "devel/python"

PYPI_PACKAGE = "ruamel.yaml"

SRC_URI[sha256sum] = "8b7ce697a2f212752a35c1ac414471dc16c424c9573be4926b56ff3f5d23b7af"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
    python3-setuptools \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"