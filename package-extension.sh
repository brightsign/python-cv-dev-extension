#!/bin/bash

# BrightSign Python CV Extension - Package Creation Script
# Automates Step 3: Extension packaging and deployment preparation

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[PACKAGE] $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Configuration
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DEVELOPMENT_PACKAGE="pydev-${TIMESTAMP}.zip"
EXTENSION_PACKAGE="ext_pydev-${TIMESTAMP}.zip"

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Package BrightSign Python CV Extension for deployment"
    echo ""
    echo "Options:"
    echo "  -d, --dev-only     Create development package only"
    echo "  -e, --ext-only     Create extension package only"
    echo "  -c, --clean        Clean install directory before packaging"
    echo "  -v, --verify       Run validation after packaging"
    echo "  -h, --help         Show this help message"
    echo ""
    echo "Package types:"
    echo "  Development: For /usr/local deployment (volatile, testing)"
    echo "  Extension:   For permanent installation (production)"
}

# Parse command line arguments
DEV_ONLY=false
EXT_ONLY=false
CLEAN=false
VERIFY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dev-only)
            DEV_ONLY=true
            shift
            ;;
        -e|--ext-only)
            EXT_ONLY=true
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -v|--verify)
            VERIFY=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if SDK exists
    if [[ ! -d "sdk" ]]; then
        error "SDK directory not found. Please extract the SDK first."
    fi
    
    if [[ ! -d "sdk/sysroots/aarch64-oe-linux" ]]; then
        error "SDK sysroot not found. Please check SDK installation."
    fi
    
    # Check for required scripts
    if [[ ! -f "sh/make-extension-lvm" ]]; then
        error "make-extension-lvm script not found"
    fi
    
    if [[ ! -f "test_cv_packages.py" ]]; then
        error "test_cv_packages.py not found"
    fi
    
    success "Prerequisites check passed"
}

# Create install directory structure
create_install_structure() {
    log "Creating extension package structure..."
    
    if [[ "$CLEAN" == "true" ]]; then
        log "Cleaning existing install directory..."
        rm -rf install
    fi
    
    # Create directory structure
    mkdir -p install/{usr/{bin,lib},python-apps}
    
    success "Directory structure created"
}

# Copy SDK components
copy_sdk_components() {
    log "Copying Python runtime and libraries from SDK..."
    
    local sdk_sysroot="sdk/sysroots/aarch64-oe-linux"
    
    # Copy Python binaries
    log "Copying Python binaries..."
    cp "$sdk_sysroot/usr/bin/python3"* install/usr/bin/ 2>/dev/null || warn "Some Python binaries not found"
    cp "$sdk_sysroot/usr/bin/pip3"* install/usr/bin/ 2>/dev/null || warn "Some pip binaries not found"
    
    # Copy libraries
    log "Copying libraries (this may take a few minutes)..."
    cp -r "$sdk_sysroot/usr/lib" install/usr/
    
    # Copy test script and setup files
    log "Copying support files..."
    cp test_cv_packages.py install/python-apps/
    cp sh/setup_python_env install/
    
    success "SDK components copied"
}

# Add extension scripts
add_extension_scripts() {
    log "Adding extension management scripts..."
    
    # Copy and make executable
    cp bsext_init install/ && chmod +x install/bsext_init
    cp sh/setup_python_env install/ && chmod +x install/setup_python_env  
    cp sh/uninstall.sh install/ && chmod +x install/uninstall.sh
    
    success "Extension scripts added"
}

# Verify installation structure
verify_structure() {
    log "Verifying installation structure..."
    
    local errors=0
    
    # Check essential directories
    for dir in "usr/bin" "usr/lib" "python-apps"; do
        if [[ ! -d "install/$dir" ]]; then
            error "Missing directory: install/$dir"
            ((errors++))
        fi
    done
    
    # Check essential files
    local essential_files=(
        "bsext_init"
        "setup_python_env" 
        "uninstall.sh"
        "python-apps/test_cv_packages.py"
        "usr/bin/python3"
    )
    
    for file in "${essential_files[@]}"; do
        if [[ ! -f "install/$file" ]]; then
            warn "Missing file: install/$file"
            ((errors++))
        fi
    done
    
    if [[ $errors -eq 0 ]]; then
        success "Structure verification passed"
    else
        warn "Structure verification found $errors issues"
    fi
    
    # Show package size
    local install_size=$(du -sh install/ 2>/dev/null | cut -f1)
    log "Extension package size: $install_size"
}

# Create development package
create_development_package() {
    log "Creating development package..."
    
    cd install
    zip -r "../$DEVELOPMENT_PACKAGE" ./ >/dev/null
    cd ..
    
    local package_size=$(du -sh "$DEVELOPMENT_PACKAGE" 2>/dev/null | cut -f1)
    success "Development package created: $DEVELOPMENT_PACKAGE ($package_size)"
    
    echo ""
    echo "Development Package Usage:"
    echo "1. Transfer $DEVELOPMENT_PACKAGE to player via DWS"
    echo "2. On player: mkdir -p /usr/local/pydev && cd /usr/local/pydev"
    echo "3. On player: unzip /storage/sd/$DEVELOPMENT_PACKAGE"
    echo "4. On player: source ./setup_python_env"
    echo "Note: Development installation is volatile (lost on reboot)"
}

# Create extension package
create_extension_package() {
    log "Creating production extension package..."
    
    cd install
    
    # Run make-extension script
    ../sh/make-extension-lvm || error "Extension creation failed"
    
    # Package the extension
    zip "../$EXTENSION_PACKAGE" ext_pydev* >/dev/null
    
    # Clean up temporary files
    rm -rf ext_pydev*
    
    cd ..
    
    local package_size=$(du -sh "$EXTENSION_PACKAGE" 2>/dev/null | cut -f1)
    success "Extension package created: $EXTENSION_PACKAGE ($package_size)"
    
    echo ""
    echo "Extension Package Usage:"
    echo "1. Transfer $EXTENSION_PACKAGE to player via DWS"
    echo "2. On player: mkdir -p /usr/local/pydev && cd /usr/local/pydev"
    echo "3. On player: unzip /storage/sd/$EXTENSION_PACKAGE"
    echo "4. On player: bash ./ext_pydev_install-lvm.sh"
    echo "5. On player: reboot"
    echo "Note: Extension installation is permanent (persists across reboots)"
}

# Run validation if requested
run_validation() {
    if [[ "$VERIFY" == "true" ]]; then
        echo ""
        log "Running validation..."
        if [[ -f "validate-build.sh" ]]; then
            ./validate-build.sh
        else
            warn "validate-build.sh not found, skipping validation"
        fi
    fi
}

# Main packaging function
main() {
    echo "BrightSign Python CV Extension - Package Creation"
    echo "================================================"
    
    local start_time=$(date +%s)
    
    check_prerequisites
    echo ""
    
    create_install_structure
    copy_sdk_components
    add_extension_scripts
    echo ""
    
    verify_structure
    echo ""
    
    # Create packages based on options
    if [[ "$EXT_ONLY" == "true" ]]; then
        create_extension_package
    elif [[ "$DEV_ONLY" == "true" ]]; then
        create_development_package
    else
        create_development_package
        echo ""
        create_extension_package
    fi
    
    run_validation
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    success "Packaging completed in $(($duration / 60))m $(($duration % 60))s"
    
    # Show created packages
    echo ""
    echo "Created packages:"
    if [[ "$EXT_ONLY" != "true" ]]; then
        echo "  📦 $DEVELOPMENT_PACKAGE (development/testing)"
    fi
    if [[ "$DEV_ONLY" != "true" ]]; then
        echo "  📦 $EXTENSION_PACKAGE (production)"
    fi
    
    echo ""
    echo "Next steps:"
    echo "1. Transfer package(s) to BrightSign player"
    echo "2. Install using instructions shown above"
    echo "3. Test with: python3 /var/volatile/bsext/ext_pydev/python-apps/test_cv_packages.py"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi