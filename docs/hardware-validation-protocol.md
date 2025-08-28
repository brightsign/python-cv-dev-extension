# Hardware Validation Protocol - RKNN Library Loading Fix

**Date**: 2025-08-28  
**Purpose**: Systematic testing procedure for validating RKNN library loading fix on BrightSign hardware  
**Prerequisites**: BrightSign player (XT-5 or compatible) with SSH access, latest pydev package

## Validation Overview

### Objectives
1. Confirm binary patching solution works on actual ARM64 hardware
2. Validate RKNN runtime initialization succeeds end-to-end  
3. Identify any hardware-specific issues requiring refinement
4. Document production-ready deployment procedure

### Expected Outcome
- **Success**: `rknn.init_runtime()` completes without "Can not find dynamic library" error
- **Partial Success**: Error messages change, indicating progress toward solution
- **Failure**: Same original error, requiring solution refinement

---

## Phase 1: Environment Setup (5 minutes)

### Step 1.1: Deploy Package to Player
```bash
# On development host - transfer latest package
export PLAYER_IP=192.168.1.100  # Replace with your player IP
export PACKAGE_NAME="pydev-$(date +%Y%m%d)"  # Use actual timestamp from build

# Find latest package
ls -la pydev-*.zip | tail -1

# Transfer to player
scp pydev-20250828-*.zip brightsign@${PLAYER_IP}:/storage/sd/

# Confirm transfer
ssh brightsign@${PLAYER_IP} "ls -la /storage/sd/pydev-*.zip"
```

### Step 1.2: Connect to Player and Install
```bash
# SSH to player
ssh brightsign@${PLAYER_IP}

# Install development package
cd /usr/local
sudo rm -rf pydev  # Remove any previous installation
sudo unzip /storage/sd/pydev-*.zip
sudo chown -R brightsign:brightsign pydev/
```

### Step 1.3: Initialize Python Environment  
```bash
# Source the environment setup
cd /usr/local/pydev
source sh/setup_python_env

# Expected output should include:
# "RKNN Runtime library setup complete."
# "Python development environment is set up."
```

**Validation Checkpoint**: Environment setup completes without errors

---

## Phase 2: Diagnostic Analysis (10 minutes)

### Step 2.1: Run Comprehensive Debug Script
```bash
# Execute the diagnostic script
./sh/debug_rknn_fix.sh > /tmp/rknn_debug_report.txt 2>&1

# Review results
cat /tmp/rknn_debug_report.txt
```

### Step 2.2: Critical Validation Points

**Check 1: Extension Library**
- ✅ Extension library exists at: `/usr/local/pydev/usr/lib/librknnrt.so`
- ✅ Library has correct permissions and is ARM64 architecture

**Check 2: Runtime Symlink**
- ✅ Symlink exists: `/tmp/lib/librknnrt.so` → `/usr/local/pydev/usr/lib/librknnrt.so`
- ✅ Symlink target is accessible and not broken

**Check 3: RKNN Package**
- ✅ Package directory exists: `/usr/local/pydev/usr/lib/python3.8/site-packages/rknnlite/`
- ✅ All 8 binary .so files present and ARM64 architecture

**Check 4: Binary Patching Verification**
- ✅ RPATH shows: `$ORIGIN/../../../../` (relative path to extension)
- ✅ Hardcoded `/usr/lib/` references: 0 (should be zero)
- ✅ Hardcoded `/tmp/lib/` references: >0 (should be positive)

**Check 5: Python Environment**
- ✅ `import rknnlite` succeeds
- ✅ `from rknnlite.api import RKNNLite` succeeds

**Check 6: Runtime Initialization**
- **CRITICAL TEST**: This is where success/failure is determined

### Step 2.3: Analyze Debug Results

**Success Indicators**:
- All checks pass with ✅ markers
- Runtime initialization test shows: "Different runtime error (may be expected without model file)"
- NO "Can not find dynamic library" error message

**Failure Indicators**:  
- "Library loading failed - string replacement worked but symlink/library issue"
- "Library loading failed - string replacement didn't work"  
- Original "Can not find dynamic library on RK3588" error persists

---

## Phase 3: Functional Testing (10 minutes)

### Step 3.1: Test Original Failing Case
```bash
# Create or use existing test script
cat > /tmp/test-rknn-init.py << 'EOF'
#!/usr/bin/env python3
"""Test RKNN initialization - the exact scenario that was failing"""

print("=== RKNN Initialization Test ===")

try:
    print("1. Importing RKNN...")
    from rknnlite.api import RKNNLite
    print("   ✅ Import successful")
    
    print("2. Creating RKNN object...")
    rknn_lite = RKNNLite()
    print("   ✅ Object creation successful")
    
    print("3. Testing runtime initialization...")
    # This is the critical test - where the original failure occurred
    try:
        ret = rknn_lite.init_runtime()
        print("   ✅ SUCCESS: Runtime initialization completed!")
        print(f"   Return code: {ret}")
        print("   🎉 RKNN LIBRARY LOADING FIX IS WORKING!")
        
    except Exception as e:
        error_msg = str(e)
        print(f"   ❌ Runtime initialization failed: {error_msg}")
        
        # Classify the error to determine progress
        if "Can not find dynamic library" in error_msg:
            if "/tmp/lib/" in error_msg:
                print("   📊 PROGRESS: String replacement worked, but library access issue")
            else:
                print("   📊 NO PROGRESS: String replacement failed")
        else:
            print("   📊 POSSIBLE PROGRESS: Different error than original hardcoded path issue")
            print("   📋 This may be expected without a valid model file")
        
        return False
        
except ImportError as e:
    print(f"   ❌ Import failed: {e}")
    return False
except Exception as e:
    print(f"   ❌ Unexpected error: {e}")
    return False

print("\n=== Test Complete ===")
return True
EOF

# Run the test
python3 /tmp/test-rknn-init.py
```

### Step 3.2: Model Loading Test (Optional)
```bash
# Only if a test model file is available
if [ -f "/storage/sd/yolox_s.rknn" ]; then
    echo "Testing with actual model file..."
    python3 /storage/sd/test-load.py
else
    echo "No test model file available - runtime init test sufficient"
fi
```

---

## Phase 4: Results Analysis and Next Steps

### Success Criteria

**Complete Success** (Fix is working):
- Runtime initialization succeeds OR shows different error (not hardcoded path)
- Debug script shows all components correctly installed
- No "Can not find dynamic library on RK3588!" error

**Partial Success** (Progress made):
- Error messages reference `/tmp/lib/` instead of `/usr/lib/` (string replacement worked)
- Different error than original hardcoded path issue
- May indicate library access or dependency issues

**Failure** (Solution needs refinement):
- Same original error: "Can not find dynamic library on RK3588!"
- Error messages still reference `/usr/lib/` (string replacement failed)
- Debug script shows missing components

### Success Actions

If validation succeeds:

1. **Document Success**:
   ```bash
   echo "✅ RKNN FIX VALIDATED ON HARDWARE" >> /tmp/validation_results.txt
   date >> /tmp/validation_results.txt
   ```

2. **Update Project Status**:
   - Mark BUGS.md issue as RESOLVED ✅
   - Update executive summary with success confirmation
   - Create production deployment documentation

3. **Prepare for Production**:
   - Test extension package installation (`ext_pydev-*.zip`)
   - Validate automatic startup works correctly
   - Document deployment procedures for field teams

### Failure Actions

If validation fails:

1. **Collect Diagnostic Data**:
   ```bash
   # Save all debug information
   cp /tmp/rknn_debug_report.txt /storage/sd/
   cp /tmp/test-rknn-init.py /storage/sd/
   
   # Additional debugging commands
   ldd /usr/local/pydev/usr/lib/librknnrt.so > /storage/sd/lib_dependencies.txt
   file /usr/local/pydev/usr/lib/python3.8/site-packages/rknnlite/api/*.so > /storage/sd/binary_info.txt
   ```

2. **Analyze Failure Mode**:
   - Review debug script output for specific failure points
   - Determine if string replacement, RPATH, or symlink creation failed
   - Check for missing dependencies or permission issues

3. **Plan Refinement**:
   - Address specific issues identified in diagnostic data
   - Consider additional fallback mechanisms
   - Implement refined solution and repeat validation

### Partial Success Actions

If progress is made but not complete success:

1. **Analyze Progress**:
   - Identify which components of the fix are working
   - Determine remaining gaps in the solution

2. **Implement Refinements**:
   - Address library access issues if symlinks are working
   - Check for missing dependencies using `ldd`
   - Consider direct file copy instead of symlinks

3. **Iterate**: Repeat validation process with refined solution

---

## Expected Timeline

- **Phase 1** (Environment Setup): 5 minutes
- **Phase 2** (Diagnostic Analysis): 10 minutes  
- **Phase 3** (Functional Testing): 10 minutes
- **Phase 4** (Results Analysis): 5-15 minutes (depending on outcome)

**Total**: 30-40 minutes for complete validation cycle

---

## Troubleshooting Common Issues

### Issue: SSH Connection Problems
**Solution**: Use serial console or ensure player is on correct network

### Issue: Permission Denied
**Solution**: Use `sudo` for file operations, ensure correct user ownership

### Issue: Package Not Found
**Solution**: Verify file transfer completed, check file names and timestamps

### Issue: Python Environment Not Working
**Solution**: Ensure `source sh/setup_python_env` was run, check PYTHONPATH

### Issue: Debug Script Fails
**Solution**: Check script permissions: `chmod +x sh/debug_rknn_fix.sh`

---

## Success Confirmation Checklist

- [ ] Package deploys and installs without errors
- [ ] Environment setup completes successfully  
- [ ] Debug script shows all components working (✅ markers)
- [ ] RKNN import and object creation work
- [ ] Runtime initialization either succeeds OR shows different error
- [ ] No "Can not find dynamic library on RK3588!" error
- [ ] Documentation updated with results
- [ ] Next steps clearly defined based on results

## Validation Sign-off

**Tester**: ________________  **Date**: ________  
**Result**: ☐ Success  ☐ Partial Success  ☐ Failure  
**Notes**: ________________________________  
**Next Action**: ___________________________