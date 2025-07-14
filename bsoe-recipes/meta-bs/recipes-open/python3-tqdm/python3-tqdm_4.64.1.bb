SUMMARY = "Fast, extensible progress meter for Python"
DESCRIPTION = "tqdm is a library for creating smart progress meters"
HOMEPAGE = "https://tqdm.github.io/"
LICENSE = "MIT & MPL-2.0"
LIC_FILES_CHKSUM = "file://LICENCE;md5=1672e2674934fd93a31c09cf17f34100"
SECTION = "devel/python"

PYPI_PACKAGE = "tqdm"

SRC_URI[md5sum] = "5822af464d77ea156ad1167c23e1bdac"
SRC_URI[sha256sum] = "5f4f682a004951c1b450bc753c710e9280c5746ce6ffedee253ddbcbf54cf1e4"

inherit pypi setuptools3

DEPENDS += " \
    python3-setuptools-scm-native \
"

# tqdm setup.py has setup_requires issue, create a simpler version
do_compile:prepend() {
    # Remove pyproject.toml which contains setup_requires
    rm -f ${S}/pyproject.toml || true
    
    # Remove setup_requires from setup.cfg
    if [ -f ${S}/setup.cfg ]; then
        sed -i '/setup_requires/d' ${S}/setup.cfg
        sed -i '/setuptools_scm/d' ${S}/setup.cfg
    fi
    
    # Backup original setup.py
    mv ${S}/setup.py ${S}/setup.py.orig || true
    
    # Create new setup.py with proper version
    cat > ${S}/setup.py << EOF
from setuptools import setup, find_packages

setup(
    name="tqdm",
    version="4.64.1",
    packages=find_packages(exclude=['tests', 'examples']),
    install_requires=[],
    python_requires=">=3.7",
    entry_points={
        'console_scripts': [
            'tqdm=tqdm.cli:main',
        ],
    },
)
EOF
}

RDEPENDS:${PN} += " \
    python3-core \
    python3-threading \
    python3-io \
"

# Standard FILES definition for Python packages
FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Skip QA warnings that may occur during cross-compilation
INSANE_SKIP:${PN} += "buildpaths already-stripped file-rdeps arch installed-vs-shipped"

BBCLASSEXTEND = "native nativesdk"