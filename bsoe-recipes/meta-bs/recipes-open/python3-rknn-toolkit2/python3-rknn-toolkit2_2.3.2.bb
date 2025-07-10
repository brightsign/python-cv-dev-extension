SUMMARY = "RKNN Toolkit2 - Full development toolkit for ARM64 targets"
DESCRIPTION = "Full RKNN Toolkit2 package including model conversion, inference and performance evaluation for Rockchip NPU devices"
HOMEPAGE = "https://github.com/airockchip/rknn-toolkit2"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
SECTION = "devel/python"

# Look for files in the downloads directory relative to the brightsign-oe root
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../../downloads:"

# ARM64 wheel for target devices - use pre-downloaded file from downloads directory
# This file must be manually downloaded from Rockchip's repository
SRC_URI = "file://rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl;unpack=0"

# Skip checksum validation since this is a manually downloaded file
BB_STRICT_CHECKSUM = "0"

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
    python3-onnx \
    python3-onnxruntime \
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
        
        # Store the original __init__.py if it exists
        if [ -f "${D}${PYTHON_SITEPACKAGES_DIR}/rknn/__init__.py" ]; then
            mv "${D}${PYTHON_SITEPACKAGES_DIR}/rknn/__init__.py" "${D}${PYTHON_SITEPACKAGES_DIR}/rknn/__init__.py.orig"
        fi
        
        # Create wrapper script for library path management
        cat > ${D}${PYTHON_SITEPACKAGES_DIR}/rknn/__init__.py << 'EOF'
"""
RKNN Toolkit with library path management for BrightSign extensions.
"""
import os
import sys
import ctypes

# Set up library path before importing RKNN modules
def setup_rknn_library_path():
    """Setup library paths for RKNN runtime library."""
    # Try to find the extension home directory
    extension_home = os.environ.get('EXTENSION_HOME')
    
    if not extension_home:
        # Fallback: try to detect from current Python path
        rknn_module_path = os.path.dirname(os.path.abspath(__file__))
        # Navigate up to find the extension root
        potential_home = os.path.dirname(os.path.dirname(os.path.dirname(rknn_module_path)))
        if os.path.exists(os.path.join(potential_home, 'usr', 'lib', 'librknnrt.so')):
            extension_home = potential_home
    
    if extension_home:
        # Set up multiple library paths that RKNN might check
        lib_paths = [
            os.path.join(extension_home, 'usr', 'lib'),
            os.path.join(extension_home, 'lib64'),
            '/usr/local/lib64'
        ]
        
        # Check for the library in all potential paths
        librknnrt_found = False
        for lib_path in lib_paths:
            if os.path.exists(os.path.join(lib_path, 'librknnrt.so')):
                # Add to LD_LIBRARY_PATH if not already there
                ld_library_path = os.environ.get('LD_LIBRARY_PATH', '')
                if lib_path not in ld_library_path:
                    os.environ['LD_LIBRARY_PATH'] = f"{lib_path}:{ld_library_path}"
                librknnrt_found = True
                
                # Try to preload the library to help Python find it
                try:
                    lib_file = os.path.join(lib_path, 'librknnrt.so')
                    ctypes.CDLL(lib_file, mode=ctypes.RTLD_GLOBAL)
                    print(f"Successfully preloaded RKNN runtime library from {lib_file}")
                    break
                except OSError as e:
                    print(f"Warning: Could not preload RKNN library from {lib_file}: {e}")
                    continue
        
        if not librknnrt_found:
            print(f"Warning: RKNN runtime library not found in any expected location")
            print(f"Searched paths: {lib_paths}")
    else:
        print("Warning: Could not determine extension home directory for RKNN library setup")

# Setup library path before any imports
setup_rknn_library_path()

# Import the real RKNN modules
try:
    from rknn.api.rknn import RKNN
    from rknn.api import *
    __version__ = "2.3.2"
    __all__ = ['RKNN']
except ImportError as e:
    print(f"Error importing RKNN modules: {e}")
    print("Make sure the RKNN runtime library (librknnrt.so) is available")
    raise
EOF
    else
        bbwarn "RKNN wheel file not found, creating stub package"
        bbwarn "To enable full RKNN functionality, download rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
        bbwarn "from https://github.com/airockchip/rknn-toolkit2 and place in downloads directory"
        
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