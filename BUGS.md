# BUGS in current pydev environment

## librknnrt.so loading issue

**Status**: ✅ **RESOLVED** - Fixed in BrightSign OS 9.1.79.3

**Resolution Date**: 2025-01-31
**Resolved By**: BrightSign OS update (OS 9.1.79.3 includes system library)

### Resolution Summary

**BrightSign OS 9.1.79.3 includes `librknnrt.so` at `/usr/lib/librknnrt.so`**, completely resolving the hardcoded path issue that blocked RKNN toolkit initialization.

**No code workarounds are required on OS 9.1.79.3 or later.**

### Test Results

**Test Date**: 2025-01-31
**Player**: BrightSign with OS 9.1.79.3
**Test Command**:
```bash
cd /usr/local/pydev
source sh/setup_python_env
python3 -c "from rknnlite.api import RKNNLite; r = RKNNLite(); print('Object created'); r.init_runtime(); print('SUCCESS!')"
```

**Output**:
```
W rknn-toolkit-lite2 version: 2.3.2
Object created
E Model is not loaded yet, this interface should be called after load_rknn!
SUCCESS!
```

**Result**: ✅ RKNN initialization succeeds. No "Can not find dynamic library" error.

### Minimum OS Requirement

**Requires**: BrightSign OS **9.1.79.3 or later**

Players with older OS versions (9.1.52, 9.1.53, etc.) will encounter the library loading issue. Upgrade to OS 9.1.79.3+ to resolve.

---

## Historical Context (OS 9.1.52 and Earlier)

### The Problem (Now Resolved)

On OS versions prior to 9.1.79.3, RKNN toolkit runtime initialization failed with:

```
Exception: Can not find dynamic library on RK3588!
Please download the librknnrt.so from [...] and move it to directory /usr/lib/
```

**Root Cause**: The `rknn-toolkit-lite2` package performed explicit path validation using `os.path.exists("/usr/lib/librknnrt.so")` before library loading. BrightSign's `/usr/lib/` was read-only, preventing library installation.

### Failed Workaround Attempts (Historical)

Multiple engineering approaches were attempted on older OS versions:

1. ❌ **Environment Variables** (LD_LIBRARY_PATH, LD_PRELOAD) - Bypassed by hardcoded check
2. ❌ **Symlinks in Writable Locations** (/usr/local/lib) - RKNN only checked exact path
3. ❌ **Filesystem Bind Mounts** - Blocked by read-only constraints
4. ❌ **RPATH Modification Only** - Hardcoded check occurred before dynamic loading
5. ❌ **Binary String Replacement** (length mismatch) - Caused binary corruption

### Final Workaround (No Longer Needed)

A complex binary patching solution was developed but became unnecessary with OS 9.1.79.3:
- RPATH modification with patchelf
- Same-length string replacement (/usr/lib/ → /tmp/lib/)
- Runtime symlink creation
- ~460 lines of workaround code

**This solution was removed from the codebase** after confirming OS 9.1.79.3 resolves the issue.

### Code Cleanup

**Commit**: f20fae6 (2025-01-31)
**Changes**:
- Removed `patch_rknn_binaries()` function (~290 lines)
- Removed `create_rknn_debug_script()` function (~170 lines)
- Simplified init-extension script (removed symlink logic)
- Package script now performs simple wheel extraction only

**Impact**: Much simpler codebase, easier maintenance, cleaner deployment process.

### Documentation

See [docs/os-9.1.79.3-testing-protocol.md](docs/os-9.1.79.3-testing-protocol.md) for complete testing procedure and results.

---

## Other Issues

(No other active bugs reported)
