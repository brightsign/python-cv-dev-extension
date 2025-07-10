SUMMARY = "RKNN Runtime Library for Rockchip NPU"
DESCRIPTION = "Runtime library for executing RKNN models on Rockchip NPU hardware"
HOMEPAGE = "https://github.com/airockchip/rknn-toolkit2"
LICENSE = "CLOSED"
SECTION = "libs"

# This is a closed-source binary library from Rockchip
LIC_FILES_CHKSUM = ""

# Download the runtime library from the official repository
# This matches the README instructions for downloading librknnrt.so
SRC_URI = "https://github.com/airockchip/rknn-toolkit2/raw/v2.3.2/rknpu2/runtime/Linux/librknn_api/aarch64/librknnrt.so;downloadfilename=librknnrt.so.${PV};name=librknnrt"

SRC_URI[librknnrt.sha256sum] = "d31fc19c85b85f6091b2bd0f6af9d962d5264a4e410bfb536402ec92bac738e8"

# Only for ARM64 targets
COMPATIBLE_MACHINE = "cobra|pantera"


S = "${WORKDIR}"

do_install() {
    install -d ${D}${libdir}
    
    # Install the main library
    install -m 0755 ${WORKDIR}/librknnrt.so.${PV} ${D}${libdir}/librknnrt.so.${PV}
    
    # Create symlinks for compatibility
    ln -sf librknnrt.so.${PV} ${D}${libdir}/librknnrt.so.2
    
    # Create the dev symlink during install to avoid postinstall issues
    ln -sf librknnrt.so.${PV} ${D}${libdir}/librknnrt.so
}

# Package the library files properly
FILES:${PN} = "${libdir}/librknnrt.so.${PV} ${libdir}/librknnrt.so.2"
FILES:${PN}-dev = "${libdir}/librknnrt.so"

# Skip QA checks for pre-compiled binary
INSANE_SKIP:${PN} += "already-stripped ldflags"
INSANE_SKIP:${PN}-dev += "dev-so"