SUMMARY = "A modern and designer friendly templating language for Python"
DESCRIPTION = "Jinja2 is a template engine written in pure Python. It provides a Django inspired non-XML syntax but supports inline expressions and an optional sandboxed environment"
HOMEPAGE = "https://palletsprojects.com/p/jinja/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"
SECTION = "devel/python"

PYPI_PACKAGE = "jinja2"

SRC_URI[md5sum] = "66d4c25ff43d1deaf9637ccda523dec8"
SRC_URI[sha256sum] = "0137fb05990d35f1275a587e9aee6d56da821fc83491a0fb838183be43f66d6d"

inherit pypi setuptools3

# jinja2 uses pyproject.toml, create setup.py for compatibility
do_compile:prepend() {
    cat > ${S}/setup.py << 'EOF'
from setuptools import setup, find_packages

setup(
    name="Jinja2",
    version="${PV}",
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    python_requires=">=3.7",
    install_requires=["MarkupSafe>=2.0"],
)
EOF
}

RDEPENDS:${PN} += " \
    python3-core \
    python3-markupsafe \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

BBCLASSEXTEND = "native nativesdk"