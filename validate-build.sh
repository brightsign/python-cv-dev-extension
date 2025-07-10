#!/bin/bash

# BrightSign Python CV Extension - Build Validation Script
# Validates the build outputs and SDK contents

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[VALIDATE] $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Validation counters
CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0

# Helper function to check file existence
check_file() {
    local file_path="$1"
    local description="$2"
    
    if [[ -f "$file_path" ]]; then
        success "$description: Found"
        ((CHECKS_PASSED++))
        return 0
    else
        error "$description: Missing ($file_path)"
        ((CHECKS_FAILED++))
        return 1
    fi
}

# Helper function to check directory existence
check_directory() {
    local dir_path="$1"
    local description="$2"
    
    if [[ -d "$dir_path" ]]; then
        success "$description: Found"
        ((CHECKS_PASSED++))
        return 0
    else
        error "$description: Missing ($dir_path)"
        ((CHECKS_FAILED++))
        return 1
    fi
}

# Helper function to check file with warning
check_file_warn() {
    local file_path="$1"
    local description="$2"
    
    if [[ -f "$file_path" ]]; then
        success "$description: Found"
        ((CHECKS_PASSED++))
        return 0
    else
        warn "$description: Missing ($file_path)"
        ((WARNINGS++))
        return 1
    fi
}

# Validate build environment
validate_environment() {
    log "Validating build environment..."
    
    check_directory "brightsign-oe" "BrightSign OE source directory"
    check_directory "bsoe-recipes" "Recipe patches directory"
    check_file "patch-n-build.sh" "Build script"
    check_file "Dockerfile" "Docker build file"
    
    # Check Docker image
    if docker image inspect bsoe-build >/dev/null 2>&1; then
        success "Docker image 'bsoe-build': Found"
        ((CHECKS_PASSED++))
    else
        error "Docker image 'bsoe-build': Missing"
        ((CHECKS_FAILED++))
    fi
}

# Validate build outputs
validate_build_outputs() {
    log "Validating build outputs..."
    
    # Check build directory structure
    check_directory "brightsign-oe/build" "Build directory"
    check_directory "brightsign-oe/build/tmp-glibc" "Build temp directory"
    check_directory "brightsign-oe/build/tmp-glibc/deploy" "Deploy directory"
    
    # Check for SDK installer
    if ls brightsign-oe/build/tmp-glibc/deploy/sdk/brightsign-x86_64-cobra-toolchain-*.sh 1> /dev/null 2>&1; then
        success "SDK installer: Found"
        ((CHECKS_PASSED++))
    else
        error "SDK installer: Missing"
        ((CHECKS_FAILED++))
    fi
    
    # Check for Python packages in deploy
    check_directory "brightsign-oe/build/tmp-glibc/deploy/ipk" "IPK packages directory"
    
    # Look for Python-related IPKs
    if ls brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/python3-*.ipk 1> /dev/null 2>&1; then
        success "Python IPK packages: Found"
        ((CHECKS_PASSED++))
        
        # Count Python packages
        local python_packages=$(ls brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/python3-*.ipk 2>/dev/null | wc -l)
        log "Found $python_packages Python packages"
    else
        error "Python IPK packages: Missing"
        ((CHECKS_FAILED++))
    fi
    
    # Check for RKNN packages specifically
    check_file_warn "brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/python3-rknn-toolkit2_*.ipk" "RKNN Toolkit2 package"
    check_file_warn "brightsign-oe/build/tmp-glibc/deploy/ipk/aarch64/librknnrt_*.ipk" "RKNN Runtime library package"
}

# Validate extracted SDK
validate_sdk() {
    log "Validating extracted SDK..."
    
    if [[ ! -d "sdk" ]]; then
        warn "SDK not extracted yet (run: ./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y)"
        ((WARNINGS++))
        return
    fi
    
    check_directory "sdk/sysroots" "SDK sysroots directory"
    check_directory "sdk/sysroots/aarch64-oe-linux" "Target sysroot"
    check_directory "sdk/sysroots/aarch64-oe-linux/usr" "Target usr directory"
    
    # Check Python installation in SDK
    check_file "sdk/sysroots/aarch64-oe-linux/usr/bin/python3" "Python3 binary"
    check_directory "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8" "Python3.8 library directory"
    check_directory "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages" "Python site-packages"
    
    # Check for key Python packages
    check_file_warn "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/numpy" "NumPy package"
    check_file_warn "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/cv2" "OpenCV package"
    check_file_warn "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/PIL" "Pillow package"
    check_file_warn "sdk/sysroots/aarch64-oe-linux/usr/lib/python3.8/site-packages/rknn" "RKNN package"
    
    # Check for RKNN runtime library
    check_file_warn "sdk/sysroots/aarch64-oe-linux/usr/lib/librknnrt.so" "RKNN runtime library"
    
    # Check library versions
    if [[ -f "sdk/sysroots/aarch64-oe-linux/usr/lib/librknnrt.so" ]]; then
        local rknn_libs=$(ls sdk/sysroots/aarch64-oe-linux/usr/lib/librknnrt.so* 2>/dev/null | wc -l)
        log "Found $rknn_libs RKNN runtime library files"
    fi
}

# Validate extension package
validate_extension() {
    log "Validating extension package..."
    
    if [[ ! -d "install" ]]; then
        warn "Extension package not created yet (run Step 3 packaging commands)"
        ((WARNINGS++))
        return
    fi
    
    check_directory "install/usr" "Extension usr directory"
    check_directory "install/usr/bin" "Extension bin directory"
    check_directory "install/usr/lib" "Extension lib directory"
    check_directory "install/python-apps" "Extension Python apps directory"
    
    # Check for essential files
    check_file "install/bsext_init" "Extension init script"
    check_file "install/setup_python_env" "Python environment setup script"
    check_file "install/uninstall.sh" "Uninstall script"
    check_file "install/python-apps/test_cv_packages.py" "CV packages test script"
    
    # Check extension package size
    if [[ -d "install" ]]; then
        local install_size=$(du -sh install/ 2>/dev/null | cut -f1)
        log "Extension package size: $install_size"
        
        # Warn if package is unusually small or large
        local size_mb=$(du -sm install/ 2>/dev/null | cut -f1)
        if [[ $size_mb -lt 100 ]]; then
            warn "Extension package seems small ($install_size) - may be incomplete"
            ((WARNINGS++))
        elif [[ $size_mb -gt 2000 ]]; then
            warn "Extension package is large ($install_size) - consider optimization"
            ((WARNINGS++))
        fi
    fi
}

# Check for deployment packages
validate_packages() {
    log "Checking for deployment packages..."
    
    # Look for development packages
    if ls pydev-*.zip 1> /dev/null 2>&1; then
        local dev_packages=$(ls pydev-*.zip 2>/dev/null | wc -l)
        success "Development packages: Found $dev_packages"
        ((CHECKS_PASSED++))
    else
        warn "Development packages: None found"
        ((WARNINGS++))
    fi
    
    # Look for extension packages  
    if ls ext_pydev-*.zip 1> /dev/null 2>&1; then
        local ext_packages=$(ls ext_pydev-*.zip 2>/dev/null | wc -l)
        success "Extension packages: Found $ext_packages"
        ((CHECKS_PASSED++))
    else
        warn "Extension packages: None found"
        ((WARNINGS++))
    fi
}

# Print validation summary
print_summary() {
    echo ""
    echo "Validation Summary"
    echo "================="
    success "Checks passed: $CHECKS_PASSED"
    
    if [[ $CHECKS_FAILED -gt 0 ]]; then
        error "Checks failed: $CHECKS_FAILED"
    fi
    
    if [[ $WARNINGS -gt 0 ]]; then
        warn "Warnings: $WARNINGS"
    fi
    
    echo ""
    
    if [[ $CHECKS_FAILED -eq 0 ]]; then
        success "Validation completed successfully!"
        echo "Your build appears to be ready for deployment."
        return 0
    else
        error "Validation failed with $CHECKS_FAILED errors."
        echo "Please review the failed checks and rebuild as necessary."
        return 1
    fi
}

# Main validation function
main() {
    echo "BrightSign Python CV Extension - Build Validation"
    echo "================================================="
    echo ""
    
    validate_environment
    echo ""
    
    validate_build_outputs
    echo ""
    
    validate_sdk
    echo ""
    
    validate_extension
    echo ""
    
    validate_packages
    echo ""
    
    print_summary
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi