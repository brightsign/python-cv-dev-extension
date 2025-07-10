SUMMARY = "RKNN Toolkit2 - Full development toolkit for ARM64 targets"
DESCRIPTION = "Full RKNN Toolkit2 package including model conversion, inference and performance evaluation for Rockchip NPU devices"
HOMEPAGE = "https://github.com/airockchip/rknn-toolkit2"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
SECTION = "devel/python"

# Look for files in the downloads directory relative to the brightsign-oe root
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../../downloads:"

# ARM64 wheel for target devices - auto-download from Rockchip's repository
# Falls back to local file if auto-download fails
SRC_URI = "https://raw.githubusercontent.com/airockchip/rknn-toolkit2/v2.3.2/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl;downloadfilename=rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl;name=rknn_toolkit \
           file://rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl;unpack=0;name=rknn_local"

# Checksum for the official wheel file
SRC_URI[rknn_toolkit.sha256sum] = "d78e2ecd77502988dc2dcd46d665102be8fb15f4d4d541ef272f6abaabca0eda"
# Skip checksum for local file fallback
SRC_URI[rknn_local.sha256sum] = ""

DEPENDS += "python3-native unzip-native"

# Disabled due to compilation issues:
# python3-onnxoptimizer (cmake cross-compilation issues)
# python3-fast-histogram (NumPy C API compatibility issues)
# python3-scipy (Cython build issues)

RDEPENDS:${PN} += " \
    librknnrt \
    python3-numpy \
    python3-core \
    python3-ctypes \
    python3-threading \
    python3-ruamel-yaml \
    python3-pyyaml \
    python3-psutil \
    python3-opencv \
    python3-pillow \
    python3-protobuf \
    python3-torch \
    python3-tqdm \
    bash \
"

# Target ARM64 only - compatible with RK3588 BrightSign players  
# COMPATIBLE_MACHINE = "cobra|pantera|impala"

inherit setuptools3

# Use the downloaded wheel file
S = "${WORKDIR}"

do_compile() {
    # No compilation needed for wheel
    :
}

do_install() {
    # Install the full RKNN toolkit package
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    
    # Find the specific RKNN wheel file in WORKDIR
    WHEEL_FILE="${WORKDIR}/rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
    
    if [ -f "${WHEEL_FILE}" ]; then
        bbnote "RKNN wheel file found, installing from wheel"
        # Extract wheel to temporary directory (clean it first to avoid conflicts)
        TEMP_DIR=${WORKDIR}/wheel_extract
        rm -rf ${TEMP_DIR}
        mkdir -p ${TEMP_DIR}
        cd ${TEMP_DIR}
        
        unzip -o -q "${WHEEL_FILE}"
        
        # Copy the rknn package directory (main Python module)
        if [ -d "rknn" ]; then
            cp -r rknn ${D}${PYTHON_SITEPACKAGES_DIR}/
        else
            bbfatal "rknn directory not found in wheel"
        fi
        
        # Copy any additional packages that might be included
        if [ -d "rknnlite" ]; then
            cp -r rknnlite ${D}${PYTHON_SITEPACKAGES_DIR}/
        fi
        
        # Copy the package metadata (.dist-info directory)
        if [ -d "rknn_toolkit2-2.3.2.dist-info" ]; then
            cp -r rknn_toolkit2-2.3.2.dist-info ${D}${PYTHON_SITEPACKAGES_DIR}/
        else
            bbwarn "Package metadata directory not found - package may not be properly recognized"
        fi
        
        # Verify installation succeeded
        if [ ! -d "${D}${PYTHON_SITEPACKAGES_DIR}/rknn" ]; then
            bbfatal "RKNN module installation failed - rknn directory not found in destination"
        fi
        
        bbnote "RKNN Toolkit2 installed successfully from wheel"
    else
        bbwarn "RKNN wheel file not found - creating stub package"
        bbwarn "For full RKNN functionality, ensure network connectivity for automatic download"
        bbwarn "or manually place rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
        bbwarn "in the downloads directory from https://github.com/airockchip/rknn-toolkit2"
        
        # Create minimal stub package
        mkdir -p ${D}${PYTHON_SITEPACKAGES_DIR}/rknn
        
        # Create stub __init__.py
        cat > ${D}${PYTHON_SITEPACKAGES_DIR}/rknn/__init__.py << 'EOF'
"""
RKNN Toolkit2 Stub Package

This is a placeholder package. To use RKNN functionality, you need to:
1. Download the rknn_toolkit2 wheel file from Rockchip's GitHub repository
2. Place it in the downloads directory
3. Rebuild this recipe

For more information, see: https://github.com/airockchip/rknn-toolkit2
"""

class RKNN:
    """Stub RKNN class"""
    def __init__(self):
        raise NotImplementedError(
            "RKNN Toolkit2 wheel file not found during build. "
            "Please download the wheel file and rebuild."
        )

__version__ = "2.3.2-stub"
__all__ = ['RKNN']
EOF
    fi
}

FILES:${PN} += "${PYTHON_SITEPACKAGES_DIR}/*"

# Suppress QA warnings for pre-compiled .so files that are already stripped and shell script dependencies
INSANE_SKIP:${PN} += "already-stripped file-rdeps"

# Only for target ARM64 - no native/nativesdk support needed