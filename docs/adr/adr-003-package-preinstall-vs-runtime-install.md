# ADR-003: Package Pre-Installation vs Runtime Installation

## Status
Accepted

## Context

Python packages in embedded Linux systems can be installed through different strategies with significantly different reliability and maintenance characteristics. The BrightSign Python CV Extension originally relied on runtime pip installation for the critical `rknn-toolkit-lite2` package, which led to deployment failures.

The core question was whether to install Python packages during the build/packaging phase (pre-installation) or during the runtime initialization phase on the target device.

Key constraints influencing this decision:
- **BrightSign filesystem**: Most areas read-only after extension installation
- **Cross-compilation**: Build machine (x86_64) targeting ARM64 device
- **Package complexity**: Native binaries requiring proper architecture matching
- **Reliability requirements**: Extension must work consistently across deployments

## Decision

**Pre-install Python packages during the packaging phase rather than relying on runtime pip installation.**

Specifically:
- Extract and install packages into extension's site-packages during `./package` execution
- Include all dependencies in the extension ZIP file  
- Eliminate runtime pip installation requirements
- Ensure packages are immediately available when extension loads

## Alternatives Considered

### Alternative 1: Runtime pip installation (original approach)
**Approach**: Ship wheels in extension, use pip install during startup
**Implementation**: 
```bash
# In post-init scripts:
pip install /path/to/extension/wheels/*.whl
```

**Rejected because**:
- **Filesystem constraints**: BrightSign read-only areas prevent reliable pip writes
- **Installation failures**: pip cannot consistently write to target directories
- **Startup complexity**: Adds failure points during critical initialization
- **Network dependencies**: May require network access for dependency resolution
- **Debugging difficulty**: Runtime failures harder to diagnose than build failures

### Alternative 2: Hybrid approach (partial pre-install)
**Approach**: Pre-install core packages, runtime install optional components  
**Rejected because**:
- **Complexity**: Two installation mechanisms to maintain and debug
- **Failure modes**: Runtime portion still subject to same pip limitations
- **Inconsistency**: Some packages available immediately, others delayed

### Alternative 3: System-level integration (BitBake recipes)
**Approach**: Install all packages through BitBake during SDK build
**Deferred (not rejected) because**:
- **Build time**: 30+ minute SDK rebuilds for package changes
- **Development velocity**: Slows iteration for package experimentation  
- **Complexity**: Requires maintaining BitBake recipes for proprietary packages
- **Future option**: Can be adopted later for stable package sets

## Technical Analysis

### Pre-Installation Advantages

**Reliability**:
- ✅ Packages guaranteed available at extension startup
- ✅ No runtime dependency on pip functionality
- ✅ Filesystem permissions resolved during packaging
- ✅ All dependencies bundled and validated

**Debugging**:
- ✅ Build-time failures easier to diagnose than runtime failures
- ✅ Package installation can be validated before deployment
- ✅ Consistent environment across all deployed players

**Performance**:
- ✅ No startup delays for package installation
- ✅ Extension ready immediately after mount
- ✅ Reduced I/O during player initialization

### Runtime Installation Disadvantages

**BrightSign Filesystem Issues**:
```bash
# Typical runtime pip failure:
pip install rknn-toolkit-lite2
# ERROR: Could not install packages due to OSError: 
# [Errno 30] Read-only file system: '/usr/lib/python3.8/site-packages'
```

**Dependency Resolution Problems**:
- pip may attempt to upgrade system packages
- Network connectivity required for dependency checking
- Version conflicts with pre-installed system packages

**Inconsistent State Management**:
- Extension may start before pip installation completes
- Partial installations leave system in undefined state
- Recovery from failed installations requires manual intervention

## Implementation Pattern

### Packaging Phase Integration
```bash
# In package script - install_python_packages() function:
install_python_packages() {
    local site_packages="install/usr/lib/python3.8/site-packages"
    mkdir -p "$site_packages"
    
    # Extract wheel contents to staging directory
    for wheel in toolkit/*.whl; do
        if [[ -f "$wheel" ]]; then
            local temp_dir=$(mktemp -d)
            unzip -q "$wheel" -d "$temp_dir"
            
            # Copy package and metadata
            cp -r "$temp_dir"/*/ "$site_packages/" 2>/dev/null || true
            cp -r "$temp_dir"/*.dist-info "$site_packages/" 2>/dev/null || true
            
            rm -rf "$temp_dir"
        fi
    done
}
```

### Environment Setup
```bash
# In setup_python_env - ensure site-packages in PYTHONPATH
export PYTHONPATH="/var/volatile/bsext/ext_pydev/usr/lib/python3.8/site-packages:$PYTHONPATH"
```

### Validation Strategy
```python
# Runtime validation (no installation, just verification)
import pkg_resources

required_packages = [
    'rknn-toolkit-lite2==2.3.2',
    'numpy>=1.17.4', 
    'opencv-python>=4.5.0'
]

for package in required_packages:
    try:
        pkg_resources.require(package)
        print(f"✅ {package} available")
    except pkg_resources.DistributionNotFound:
        print(f"❌ {package} missing")
```

## Consistency with Existing Architecture

This decision aligns with how other packages in the extension are handled:

**Pre-installed system packages**:
- `numpy` - Installed in SDK during BitBake build
- `opencv-python` - Built and installed through BitBake recipes  
- `pandas` - Available in system site-packages

**Extension-specific packages**:
- `rknn-toolkit-lite2` - Now pre-installed during packaging
- Custom CV utilities - Included directly in extension structure

The pattern establishes consistency: **all Python dependencies available at extension startup without runtime installation**.

## Migration Strategy

### Phase 1: Remove Runtime Installation (Immediate)
- Remove pip installation commands from post-init scripts
- Remove `post-init_requirements.txt` dependency file
- Update documentation to reflect pre-installation approach

### Phase 2: Implement Pre-Installation (Current)  
- Modify `package` script with wheel extraction
- Add package validation to packaging process
- Test pre-installed packages in development environment

### Phase 3: Extend to Other Packages (Future)
- Apply same pattern to other wheel-based dependencies
- Consider pre-installing development tools and utilities
- Document standard wheel pre-installation procedures

## Consequences

### Positive
- ✅ **Deployment reliability**: Eliminates most common extension startup failure
- ✅ **Consistent experience**: All deployments have identical package availability
- ✅ **Faster startup**: No runtime installation delays
- ✅ **Better debugging**: Build-time failures easier to diagnose and fix
- ✅ **Offline operation**: No network dependencies for package availability
- ✅ **Architectural consistency**: Matches system package management approach

### Negative
- ❌ **Larger extension size**: Pre-installed packages increase ZIP file size
- ❌ **Less flexibility**: Cannot install packages dynamically based on runtime conditions
- ❌ **Update complexity**: Package updates require rebuild and redeployment
- ❌ **Disk space usage**: Packages consume player storage space permanently

### Neutral
- **Build process dependency**: Requires wheel availability during packaging
- **Version management**: Need to track and update wheel versions in build system
- **Documentation updates**: Installation procedures need revision

## Metrics and Validation

### Success Criteria
- [ ] Extension startup time improved (no runtime pip delays)
- [ ] Zero pip-related failures in deployment logs
- [ ] All required packages available immediately after extension load
- [ ] Consistent package versions across all deployed players
- [ ] Simplified troubleshooting procedures (no runtime pip debugging)

### Monitoring
- **Build validation**: Verify all packages extracted during packaging
- **Deployment verification**: Check `pip list` shows expected packages  
- **Runtime health**: Monitor for import errors in extension logs
- **Size impact**: Track extension ZIP file size changes

## References

- **Original issue**: `BUGS.md` - rknn-toolkit-lite2 import failures
- **Implementation**: ADR-001 Cross-Architecture Wheel Installation Strategy  
- **Architecture context**: ADR-002 Three-Environment Build Architecture
- **Python packaging**: [PEP 427 - The Wheel Binary Package Format](https://peps.python.org/pep-0427/)
- **BrightSign constraints**: Extension deployment documentation
- **Investigation session**: `.claude/session-logs/2025-01-28-1430-explore-installing-wheels.md`

---

**Date**: 2025-01-28  
**Author**: System Architecture Analysis  
**Stakeholders**: Extension deployment, Python package management, embedded systems reliability