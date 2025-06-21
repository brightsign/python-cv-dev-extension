# FindTurboJPEG.cmake - Find TurboJPEG library
#
# This module defines:
#  TURBOJPEG_FOUND - True if TurboJPEG is found
#  TURBOJPEG_INCLUDE_DIRS - The TurboJPEG include directories
#  TURBOJPEG_LIBRARIES - The TurboJPEG libraries
#  TURBOJPEG_VERSION - The TurboJPEG version

find_path(TURBOJPEG_INCLUDE_DIR
  NAMES turbojpeg.h
  PATHS
    /usr/include
    /usr/local/include
    ${CMAKE_SOURCE_DIR}/include
)

find_library(TURBOJPEG_LIBRARY
  NAMES turbojpeg libturbojpeg
  PATHS
    /usr/lib
    /usr/lib/aarch64-linux-gnu
    /usr/local/lib
    /usr/local/lib/aarch64-linux-gnu
    ${CMAKE_SOURCE_DIR}/include
)

# Try to extract version information
if(TURBOJPEG_INCLUDE_DIR AND EXISTS "${TURBOJPEG_INCLUDE_DIR}/turbojpeg.h")
  file(STRINGS "${TURBOJPEG_INCLUDE_DIR}/turbojpeg.h" TURBOJPEG_VERSION_PARTS REGEX "^#define LIBJPEG_TURBO_VERSION_[A-Z]+ [0-9]+$")
  
  string(REGEX REPLACE ".*#define LIBJPEG_TURBO_VERSION_MAJOR ([0-9]+).*" "\\1" TURBOJPEG_VERSION_MAJOR "${TURBOJPEG_VERSION_PARTS}")
  string(REGEX REPLACE ".*#define LIBJPEG_TURBO_VERSION_MINOR ([0-9]+).*" "\\1" TURBOJPEG_VERSION_MINOR "${TURBOJPEG_VERSION_PARTS}")
  string(REGEX REPLACE ".*#define LIBJPEG_TURBO_VERSION_PATCH ([0-9]+).*" "\\1" TURBOJPEG_VERSION_PATCH "${TURBOJPEG_VERSION_PARTS}")
  
  set(TURBOJPEG_VERSION "${TURBOJPEG_VERSION_MAJOR}.${TURBOJPEG_VERSION_MINOR}.${TURBOJPEG_VERSION_PATCH}")
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(TurboJPEG
  REQUIRED_VARS TURBOJPEG_LIBRARY TURBOJPEG_INCLUDE_DIR
  VERSION_VAR TURBOJPEG_VERSION
)

if(TURBOJPEG_FOUND)
  set(TURBOJPEG_LIBRARIES ${TURBOJPEG_LIBRARY})
  set(TURBOJPEG_INCLUDE_DIRS ${TURBOJPEG_INCLUDE_DIR})
endif()

mark_as_advanced(TURBOJPEG_INCLUDE_DIR TURBOJPEG_LIBRARY)
