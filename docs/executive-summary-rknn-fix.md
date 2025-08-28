# RKNN Library Loading Issue - Executive Summary
**Date:** August 28, 2025  
**Prepared for:** VP of Engineering  
**Prepared by:** Senior Software Development Manager  
**Classification:** Internal - Technical Analysis  

## Executive Summary

### Critical Issue
The BrightSign Python CV Extension encounters a blocking runtime failure when initializing the RKNN (Rockchip Neural Processing Unit) toolkit, preventing AI/ML model inference capabilities. Despite multiple engineering attempts over several development cycles, **no working solution has been confirmed on actual hardware**.

### Business Impact
- **Feature Blocked**: AI/ML computer vision capabilities unusable on production players
- **Customer Impact**: Extension deploys successfully but core NPU acceleration fails at runtime  
- **Development Velocity**: Significant engineering time invested in multiple failed approaches
- **Technical Risk**: Increasing complexity of attempted solutions with no confirmed success

### Current Status
**Implementation Status**: UNRESOLVED - Latest binary patching approach untested on hardware  
**Risk Level**: HIGH - No working solution confirmed after multiple failed attempts  
**Timeline**: Unknown - Success probability uncertain, may require fundamental approach change  

---

## Technical Root Cause Analysis

### The Problem
The RKNN toolkit is a closed-source Python package that performs explicit path validation using `os.path.exists("/usr/lib/librknnrt.so")` before attempting dynamic library loading. This hardcoded path check occurs **before** any standard Linux library loading mechanisms (LD_LIBRARY_PATH, RPATH, symlinks) are consulted.

**Technical Flow:**
```
1. Python imports rknn-toolkit-lite2 ✅ (Package properly installed)
2. User calls rknn.init_runtime() 
3. RKNN checks os.path.exists("/usr/lib/librknnrt.so") ❌ (File doesn't exist)
4. RKNN throws exception before attempting dlopen() ❌ (Never reaches library loading)
```

### BrightSign Platform Constraints
- **Read-only `/usr/lib/`**: Cannot create files in system library directory
- **Embedded Linux**: No package manager (apt/yum) for system library installation  
- **Security Model**: System directories protected against modification
- **Closed Source**: Cannot modify RKNN source code to change hardcoded paths

### Architecture Mismatch
The RKNN toolkit was designed for traditional Linux distributions where:
- System administrators have write access to `/usr/lib/`
- Package managers handle system library installation
- Applications assume writable system directories

BrightSign's embedded security model prevents these assumptions, creating an impedance mismatch between the library's design and our platform constraints.

---

## History of Failed Attempts

### Failed Approach #1: Environment Variables
**Method**: Set LD_LIBRARY_PATH, LD_PRELOAD to point to extension library  
**Result**: FAILED - RKNN hardcoded path check bypasses all environment variables  
**Testing**: Confirmed on player hardware - no improvement in error messages  

### Failed Approach #2: Symlinks in Writable Locations  
**Method**: Created symlinks in /usr/local/lib/, /usr/local/lib64/  
**Result**: FAILED - RKNN only checks exact path /usr/lib/librknnrt.so  
**Testing**: Confirmed on player hardware - identical error messages  

### Failed Approach #3: Filesystem Bind Mounts
**Method**: Attempt to bind mount extension library to system location  
**Result**: FAILED - /usr/lib/ read-only, cannot create mount target  
**Testing**: Player filesystem constraints prevent implementation  

### Failed Approach #4: RPATH-Only Modification
**Method**: Use patchelf to modify library search paths  
**Result**: FAILED - Hardcoded os.path.exists() check occurs before library loading  
**Testing**: Build environment only - no hardware testing performed  

### Failed Approach #5: String Replacement (Initial)
**Method**: Replace /usr/lib/ paths with longer paths  
**Result**: FAILED - Binary corruption due to length mismatch  
**Testing**: Build environment - caused segmentation faults  

### Current Untested Approach: Complex Binary Patching
**Method**: Hybrid RPATH + same-length string replacement + symlinks  
**Status**: UNTESTED ON HARDWARE - no confirmation this will work  
**Risk**: Increasingly complex solution with no validated success

**Why This Approach May Also Fail:**
- No hardware validation - all previous "working" build-environment solutions failed on actual hardware
- Complexity increases failure probability - more components that can break
- String replacement in binaries is high-risk - may cause subtle corruption
- RKNN library may have additional undiscovered hardcoded assumptions
- /tmp/lib symlink approach is unproven and may not satisfy RKNN's validation logic

**Uncertainty Factors:**
- We don't fully understand RKNN's internal library loading sequence
- Hardware testing has consistently revealed issues missed in build environment
- The closed-source nature makes debugging extremely difficult

### Build Environment Evidence (Unconfirmed on Hardware)
```bash
# Build-time modifications appear successful (Host x86_64)
strings rknn_runtime.so | grep lib
# Shows: "/tmp/lib/librknnrt.so" (patched from "/usr/lib/librknnrt.so")

patchelf --print-rpath rknn_runtime.so  
# Shows: "$ORIGIN/../../../../" (points to extension directory)

# NOTE: Previous approaches also showed "success" in build environment
# but failed completely when tested on actual BrightSign hardware
```

---

## Risk Assessment and Mitigation

### Technical Risks

| Risk | Probability | Impact | Reality Check |
|------|-------------|---------|---------------|
| Current approach fails like all previous attempts | **HIGH** | High | Pattern of repeated failure suggests fundamental issue |
| Binary corruption from string replacement | **HIGH** | High | Modifying binaries with sed is inherently risky |
| RKNN has additional undiscovered hardcoded assumptions | **HIGH** | High | Closed source makes complete analysis impossible |
| Hardware behavior differs from build environment | **HIGH** | High | Consistent pattern in all previous attempts |

### Business Risks

| Risk | Probability | Impact | Mitigation Options |
|------|-------------|---------|-------------------|
| **Complete failure to resolve issue** | **HIGH** | **HIGH** | Consider alternative NPU solutions or architectural changes |
| Continued resource drain on failed approaches | **MEDIUM** | Medium | Set clear success/failure criteria and timeline |
| Customer expectations not managed properly | Medium | High | Clear communication about uncertain timeline |

### Reality Assessment
**After 5+ failed approaches, we have no evidence that ANY solution will work.** The closed-source nature of RKNN combined with BrightSign's security constraints may represent an unsolvable compatibility problem.

---

## Recommended Action Plan

### Phase 1: Final Hardware Validation Attempt (1-2 days)
**Immediate Actions:**
1. Deploy latest binary-patched package to test player
2. Execute comprehensive testing protocol 
3. **If this fails like all previous attempts**: Proceed to Phase 2

**Success Criteria (Unlikely):**
- `rknn_lite.init_runtime()` completes without exception
- Must work consistently, not just once

### Phase 2: Escalation and Alternative Assessment (2-3 days)  
**Failure Response:**
- Document complete failure history and root cause analysis
- **Investigate alternative NPU solutions** (other vendors, different approaches)
- **Assess feasibility of CPU-only ML** as fallback
- **Consider architectural changes** to avoid RKNN dependency entirely

### Phase 3: Business Decision Point
**Options if all approaches fail:**
1. **Abandon RKNN-based NPU acceleration** - proceed with CPU-only ML
2. **Investigate alternative ML acceleration** - different NPU vendors or approaches  
3. **Defer feature** - await potential RKNN toolkit updates or BrightSign OS changes
4. **Architectural redesign** - consider external ML processing or different deployment model

---

## Resource Requirements

### Engineering Time
- **Senior Engineer**: 4-5 days for validation, refinement, and documentation
- **QA Engineer**: 2-3 days for comprehensive testing protocol
- **Hardware Access**: BrightSign test player for validation testing

### Dependencies
- Access to test BrightSign hardware (XT-5 or compatible)
- Network connectivity for test model deployment
- Ability to deploy and test extension packages

### Success Metrics
- **Technical**: RKNN initialization success rate: 100%
- **Functional**: NPU model inference working on test hardware
- **Operational**: Clear deployment and troubleshooting procedures documented

---

## Conclusion and Recommendation

### Technical Reality Check
**We have no working solution after multiple engineering attempts.** The pattern of repeated failure suggests this may be an unsolvable compatibility issue between RKNN's closed-source assumptions and BrightSign's security architecture.

### Business Recommendation  
**Proceed with ONE FINAL hardware validation attempt, but prepare for failure.** Set a strict timeline and immediately pivot to alternative approaches if this fails.

### Honest Risk vs. Reward Assessment
- **LOW Probability of Success**: 5+ failed attempts indicate fundamental incompatibility
- **HIGH Technical Risk**: Each approach increases system complexity and potential failure points
- **UNCERTAIN Business Value**: May need to abandon NPU acceleration entirely
- **NO Clear Path Forward**: Running out of viable technical approaches

### Realistic Next Steps
1. **Immediate**: Test current approach on hardware (expect failure)
2. **If it fails**: Stop attempting RKNN fixes and investigate alternatives
3. **Business Decision**: Accept CPU-only ML or explore different architectures
4. **Long-term**: Monitor for RKNN toolkit updates but don't depend on them

---

## Appendix A: Technical Details

### Binary Modification Strategy
```bash
# String replacement (hardcoded path check)
sed -i 's|/usr/lib/|/tmp/lib/|g' rknn_runtime.so
# Result: RKNN checks /tmp/lib/librknnrt.so (symlinked location)

# RPATH modification (dynamic library loading)  
patchelf --set-rpath '$ORIGIN/../../../../' rknn_runtime.so
# Result: Library loader searches relative to extension directory
```

### Runtime Environment Setup
```bash
# Extension initialization creates writable symlink
mkdir -p /tmp/lib
ln -sf /var/volatile/bsext/ext_pydev/usr/lib/librknnrt.so /tmp/lib/librknnrt.so
# Result: Both hardcoded check and dynamic loading find library
```

### Verification Commands
```bash
# Confirm string replacement successful
strings rknn_runtime.so | grep -c "/tmp/lib/"  # Should be > 0
strings rknn_runtime.so | grep -c "/usr/lib/"  # Should be 0

# Confirm RPATH modification successful  
patchelf --print-rpath rknn_runtime.so         # Should show $ORIGIN path

# Confirm runtime symlink exists
ls -la /tmp/lib/librknnrt.so                   # Should show valid symlink
```

---

## Appendix B: Alternative Approaches Considered

### Approach 1: Source Code Modification
**Status**: Not feasible - RKNN toolkit is closed source
**Research**: Confirmed through community forums and repository analysis

### Approach 2: System Library Installation
**Status**: Blocked by read-only filesystem constraints  
**Limitation**: Cannot modify `/usr/lib/` on BrightSign platforms

### Approach 3: Python-level Wrapper
**Status**: Complex implementation with limited benefit
**Assessment**: Would require reimplementing significant RKNN functionality

### Approach 4: Version Downgrade
**Status**: Not recommended - potential loss of NPU features
**Risk**: Compatibility issues with newer model formats

**Conclusion**: Binary modification approach is the most practical solution given platform constraints and closed-source limitations.