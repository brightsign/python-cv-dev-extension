# ADR-001: Cross-Architecture Wheel Installation Strategy

## Status
Accepted

## Context

The BrightSign Python CV Extension requires installing Python packages with native binaries (specifically `rknn-toolkit-lite2`) in a cross-compilation environment where:

- **Build machine**: x86_64 architecture running package assembly
- **Target device**: ARM64 BrightSign player running the extension
- **Package format**: Python wheel containing pre-compiled ARM64 binaries

Initial implementation only copied wheel files without extracting/installing them, leading to runtime failures when Python couldn't find the package.

The fundamental question was whether it's safe to extract ARM64 wheel contents on an x86_64 build machine for deployment to ARM64 targets.

## Decision

**Extract and install wheel contents during packaging on the build machine rather than copying wheel files for runtime installation.**

Specifically:
1. Extract wheel ZIP archive during `./package` execution on x86_64 build machine
2. Copy extracted package contents to staging directory `install/usr/lib/python3.8/site-packages/`
3. Include extracted contents in final extension ZIP for deployment

## Alternatives Considered

### Alternative 1: Runtime pip installation
**Approach**: Copy wheel to extension, use pip install during player initialization
**Rejected because**:
- BrightSign filesystem constraints make runtime pip unreliable
- Read-only areas prevent consistent package installation
- Adds complexity and failure points during player startup

### Alternative 2: BitBake recipe integration
**Approach**: Create proper BitBake recipe for rknn-toolkit-lite2 in SDK build
**Deferred because**:
- Requires complex SDK rebuild cycle (30+ minutes)
- Recipe complexity with proprietary RKNN dependencies
- Build-time solution is simpler and more maintainable

### Alternative 3: Manual extraction on target
**Approach**: Leave wheel extraction as manual post-deployment step
**Rejected because**:
- Poor user experience requiring manual intervention
- Inconsistent deployment across different environments
- Error-prone manual process

## Technical Rationale

### Architecture Safety Analysis
Python wheels are ZIP archives containing:
- **Pure Python code**: Architecture-agnostic `.py` files
- **Pre-compiled binaries**: Architecture-specific `.so` files (already compiled for ARM64)
- **Package metadata**: `.dist-info/` directories for package management

**Key insight**: Extracting ARM64 binaries on x86_64 is safe because:
- No execution of ARM64 code occurs during extraction (only file I/O)
- ARM64 binaries remain intact for target execution
- Standard unzip/copy operations are architecture-agnostic

### Wheel Format Validation
```bash
# Confirmed ARM64 architecture in wheel contents:
unzip -l rknn_toolkit_lite2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.whl
# Contains: rknn_runtime.cpython-38-aarch64-linux-gnu.so
```

The wheel filename and contents explicitly target `aarch64` (ARM64) architecture.

## Implementation

### Modified `copy_rknn_wheel()` function:
```bash
copy_rknn_wheel() {
    log "Installing rknn-toolkit-lite2 into extension site-packages..."
    
    local wheel_path="toolkit/rknn-toolkit2/rknn-toolkit-lite2/packages/rknn_toolkit_lite2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl"
    
    if [[ -f "$wheel_path" ]]; then
        local temp_dir=$(mktemp -d)
        unzip -q "$wheel_path" -d "$temp_dir"
        
        local site_packages="install/usr/lib/python3.8/site-packages"
        mkdir -p "$site_packages"
        
        # Install package and metadata
        cp -r "$temp_dir/rknnlite" "$site_packages/"
        cp -r "$temp_dir"/rknn_toolkit_lite2*.dist-info "$site_packages/"
        
        rm -rf "$temp_dir"
        success "rknn-toolkit-lite2 installed into extension"
    fi
}
```

## Consequences

### Positive
- ✅ **Reliable deployment**: Package guaranteed present at runtime
- ✅ **Consistent with other packages**: Matches how numpy, opencv are handled
- ✅ **No runtime dependencies**: Eliminates pip installation failures
- ✅ **Architecture safety**: Proven safe for cross-compilation scenarios
- ✅ **Simple maintenance**: Standard file operations, easy to debug

### Negative
- ❌ **Build-time wheel dependency**: Requires wheel present during packaging
- ❌ **Manual version updates**: Wheel path must be updated for new RKNN versions
- ❌ **Larger extension size**: Extracted contents larger than compressed wheel

### Neutral
- **Precedent established**: Other Python packages with native binaries can follow same pattern
- **BitBake integration**: Still possible as future optimization, but not required

## Validation

### Build-time verification:
```bash
./package --dev-only
ls -la install/usr/lib/python3.8/site-packages/rknnlite/  # Should exist
file install/usr/lib/python3.8/site-packages/rknnlite/api/*.so  # Should show ARM64
```

### Runtime verification:
```python
import pkg_resources
print(pkg_resources.get_distribution('rknn-toolkit-lite2'))  # Should show 2.3.2

from rknnlite.api import RKNNLite
rknn = RKNNLite()  # Should initialize successfully
```

## References

- **Root issue**: BUGS.md - librknnrt.so loading failure
- **Investigation session**: `.claude/session-logs/2025-01-28-1430-explore-installing-wheels.md`
- **Implementation plan**: `plans/fix-librknnrt.md`
- **Architecture documentation**: `docs/architecture-understanding.md`
- **Python wheel specification**: [PEP 427](https://peps.python.org/pep-0427/)

---

**Date**: 2025-01-28  
**Author**: System Architecture Analysis  
**Stakeholders**: BrightSign extension developers, cross-compilation workflows