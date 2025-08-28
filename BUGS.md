# BUGS in current pydev environment

## librknnrt.so loading issue

**Status**: 🔧 **IN PROGRESS** - Binary patching solution implemented, awaiting hardware validation

**Last Updated**: 2025-08-28

### Problem Description

RKNN toolkit runtime initialization fails with hardcoded library path error:

```
Exception: Can not find dynamic library on RK3588!
Please download the librknnrt.so from [...] and move it to directory /usr/lib/
```

**Root Cause**: The `rknn-toolkit-lite2` package contains hardcoded path checking (`os.path.exists("/usr/lib/librknnrt.so")`) that occurs before dynamic library loading, bypassing all standard Linux library resolution mechanisms.

**Platform Constraint**: BrightSign's `/usr/lib/` directory is read-only, preventing direct library installation.

### Current Test Case

Test script demonstrating the issue:
```python
# /storage/sd/test-load.py                                   
from rknnlite.api import RKNNLite  # ✅ Import works              
                                                                
rknn_lite = RKNNLite()              # ✅ Object creation works                     
model_path = "/storage/sd/yolox_s.rknn"                          
ret = rknn_lite.load_rknn(model_path)    # ✅ Model loading works                    
ret = rknn_lite.init_runtime()           # ❌ FAILS HERE                            
```

### Evidence

Library exists in multiple locations but is ignored:
```bash
# find / -name "librknnrt.so"                       
/storage/sd/brightvision/lib/librknnrt.so              
/storage/sd/__unsafe__/lib/librknnrt.so                
/usr/local/lib64/librknnrt.so                          
/usr/local/pydev/lib64/librknnrt.so                    
/usr/local/pydev/usr/lib/librknnrt.so     # ← Extension location             
/var/volatile/bsext/ext_npu_obj/RK3568/lib/librknnrt.so
/var/volatile/bsext/ext_npu_obj/RK3576/lib/librknnrt.so
/var/volatile/bsext/ext_npu_obj/RK3588/lib/librknnrt.so
```

### Solution Implemented

**Hybrid Binary Patching + Runtime Symlink Approach:**

1. **Build-time Binary Modification** (✅ COMPLETED):
   - Extract RKNN wheel contents during packaging
   - Use `patchelf` to set `$ORIGIN`-relative RPATH on all .so files
   - Replace hardcoded `/usr/lib/` strings with `/tmp/lib/` (same length)
   - Verify binary integrity after modifications

2. **Runtime Symlink Creation** (✅ COMPLETED):
   - Extension init creates `/tmp/lib/librknnrt.so` symlink
   - Points to extension's copy of the library
   - `/tmp/lib/` is always writable on BrightSign

3. **Enhanced Diagnostics** (🆕 NEW):
   - Added debug script: `sh/debug_rknn_fix.sh`
   - Comprehensive validation of all fix components
   - Detailed error classification for debugging

### Testing Status

**Build Testing** (✅ COMPLETED):
- Binary patching working: all 8 .so files successfully modified
- String replacement verified: no `/usr/lib/` references remaining
- ELF integrity preserved: binaries still valid ARM64 objects
- Package creation successful: `pydev-TIMESTAMP.zip` ready

**Hardware Testing** (⏳ PENDING):
- Deploy to actual BrightSign player required
- Run validation script to confirm end-to-end functionality
- Test RKNN runtime initialization with patched binaries

### Next Steps

1. **Hardware Validation** (IMMEDIATE):
   ```bash
   # Deploy latest package to player
   scp pydev-TIMESTAMP.zip player:/storage/sd/
   
   # Install and test
   cd /usr/local && unzip /storage/sd/pydev-TIMESTAMP.zip
   source pydev/sh/setup_python_env
   
   # Run comprehensive diagnostics
   ./pydev/sh/debug_rknn_fix.sh
   
   # Test original failing case
   python3 /storage/sd/test-load.py
   ```

2. **If Hardware Testing Succeeds**:
   - Mark issue as RESOLVED ✅
   - Update documentation with final solution
   - Create production deployment process

3. **If Hardware Testing Fails**:
   - Analyze debug script output
   - Identify remaining gaps in solution
   - Implement additional fixes as needed

### Technical Details

**Files Modified**:
- `package` script: Enhanced binary patching with backup/rollback
- `sh/init-extension`: Symlink creation for `/tmp/lib/librknnrt.so`
- `sh/debug_rknn_fix.sh`: New comprehensive diagnostic tool

**Key Implementation**:
```bash
# String replacement (exact length preservation)
sed -i 's|/usr/lib/|/tmp/lib/|g' rknn_runtime.so

# RPATH modification (relative path resolution)  
patchelf --set-rpath '$ORIGIN/../../../../' rknn_runtime.so

# Runtime symlink (writable location)
ln -sf /var/volatile/bsext/ext_pydev/usr/lib/librknnrt.so /tmp/lib/librknnrt.so
```

### Confidence Level

**Technical Confidence**: HIGH - Solution addresses both hardcoded path check and dynamic loading  
**Implementation Confidence**: MEDIUM-HIGH - Comprehensive testing completed in build environment  
**Production Confidence**: PENDING - Requires hardware validation to confirm end-to-end functionality

---

## Other Issues

(No other active bugs reported)