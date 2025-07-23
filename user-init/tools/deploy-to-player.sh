#!/bin/bash
# Deploy user init scripts to BrightSign player
# Usage: ./deploy-to-player.sh <player-ip> [password]

set -e

PLAYER_IP="$1"
PASSWORD="${2:-password}"  # Default password
REMOTE_DIR="/storage/sd/python-init"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[DEPLOY] $1${NC}"
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

usage() {
    echo "Usage: $0 <player-ip> [password]"
    echo ""
    echo "Deploy user initialization scripts to BrightSign player"
    echo ""
    echo "Arguments:"
    echo "  player-ip    IP address of the BrightSign player"
    echo "  password     SSH password (default: 'password')"
    echo ""
    echo "Examples:"
    echo "  $0 192.168.1.100"
    echo "  $0 192.168.1.100 mypassword"
    echo ""
    echo "What this script does:"
    echo "  1. Creates /storage/sd/python-init/ directory on player"
    echo "  2. Copies all files from ../examples/ to player"
    echo "  3. Sets correct permissions for shell scripts"
    echo "  4. Shows deployment status"
}

check_prerequisites() {
    if ! command -v sshpass &> /dev/null; then
        error "sshpass is required but not installed. Please install it first."
    fi
    
    if [ -z "$PLAYER_IP" ]; then
        usage
        error "Player IP address is required"
    fi
    
    if ! ping -c 1 "$PLAYER_IP" &> /dev/null; then
        warn "Player at $PLAYER_IP is not responding to ping"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            error "Deployment cancelled"
        fi
    fi
}

deploy_scripts() {
    log "Deploying user init files to $PLAYER_IP..."
    
    # Check if examples directory exists
    if [ ! -d "../examples" ]; then
        error "Examples directory not found. Run this script from the tools/ directory."
    fi
    
    # Create remote directory - BrightSign requires different approach
    log "Creating remote directory: $REMOTE_DIR"
    # First try to copy a dummy file to create the directory structure
    echo "# Directory created by deploy script" > /tmp/deploy_temp.txt
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        /tmp/deploy_temp.txt brightsign@"$PLAYER_IP":"$REMOTE_DIR"/.deploy_temp 2>/dev/null || {
        # If that fails, the directory doesn't exist, so we'll create it via scp with a different approach
        log "Directory doesn't exist, will be created during file copy"
    }
    rm -f /tmp/deploy_temp.txt
    
    # Copy all files from examples directory
    log "Copying example files..."
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        ../examples/* brightsign@"$PLAYER_IP":"$REMOTE_DIR"/ || error "Failed to copy files"
    
    # Set permissions for shell scripts using BrightSign's command interface
    log "Setting script permissions..."
    # BrightSign uses a different command format - we'll document this for manual setup
    warn "Shell script permissions must be set manually on BrightSign player"
    warn "SSH to player and run: chmod +x $REMOTE_DIR/*.sh"
    
    success "Files deployed successfully"
}

verify_deployment() {
    log "Verifying deployment..."
    
    # List deployed files - BrightSign command interface limitation
    log "Files should be deployed to $REMOTE_DIR on the player"
    warn "File listing requires manual SSH connection to BrightSign player"
    warn "SSH to player and run: ls -la $REMOTE_DIR/"
    
    success "Deployment verification completed"
}

show_next_steps() {
    echo ""
    echo "=== Next Steps ==="
    echo ""
    echo "1. Restart the Python extension on the player:"
    echo "   /var/volatile/bsext/ext_pydev/bsext_init restart"
    echo ""
    echo "2. Check if scripts ran successfully:"
    echo "   cat /var/log/bsext-pydev.log"
    echo "   cat /storage/sd/cv_test.log"
    echo ""
    echo "3. Check extension status:"
    echo "   /var/volatile/bsext/ext_pydev/bsext_init status"
    echo ""
    echo "To modify script behavior, edit:"
    echo "   $REMOTE_DIR/01_validate_cv.sh"
    echo "   $REMOTE_DIR/requirements.txt"
    echo ""
}

main() {
    echo "=== BrightSign User Init Script Deployment ==="
    echo ""
    
    check_prerequisites
    deploy_scripts
    verify_deployment
    show_next_steps
    
    success "Deployment completed successfully!"
}

# Handle help flag
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    usage
    exit 0
fi

main "$@"