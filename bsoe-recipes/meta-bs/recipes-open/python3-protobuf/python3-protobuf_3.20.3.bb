SUMMARY = "Protocol Buffers - Google's data interchange format"
DESCRIPTION = "Protocol buffers are Google's language-neutral, platform-neutral, extensible mechanism for serializing structured data"
HOMEPAGE = "https://developers.google.com/protocol-buffers/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

PYPI_PACKAGE = "protobuf"

SRC_URI[sha256sum] = "2e3427429c9cffebf259491be0af70189607f365c2f41c7c3764af6f337105f2"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"