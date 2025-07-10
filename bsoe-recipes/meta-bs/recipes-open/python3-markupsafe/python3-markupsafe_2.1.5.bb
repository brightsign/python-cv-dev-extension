SUMMARY = "Safely add untrusted strings to HTML/XML markup"
DESCRIPTION = "MarkupSafe implements a text object that escapes characters so it is safe to use in HTML and XML"
HOMEPAGE = "https://palletsprojects.com/p/markupsafe/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.rst;md5=ffeffa59c90c9c4a033c7574f8f3fb75"
SECTION = "devel/python"

PYPI_PACKAGE = "MarkupSafe"

SRC_URI[md5sum] = "8fe7227653f2fb9b1ffe7f9f2058998a"
SRC_URI[sha256sum] = "d283d37a890ba4c1ae73ffadf8046435c76e7bc2247bbb63c00bd1a709c6544b"

inherit pypi setuptools3

RDEPENDS:${PN} += " \
    python3-core \
"

# Allow wheels
BBCLASSEXTEND = "native nativesdk"