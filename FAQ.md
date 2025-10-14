# Frequently Asked Questions (FAQ)

Quick answers to common questions about the BrightSign Python CV Extension.

---

## Architecture & Compatibility

### Why x86_64 only? Can I use Apple Silicon (M1/M2/M3)?

**Short answer**: No, Apple Silicon (ARM64) is not compatible.

**Why**: The RKNN toolkit (required for NPU support) only provides x86_64 model compilation tools. Cross-compiling from ARM64 to ARM64 is not supported by Rockchip.

**Solutions**:
- Use an Intel/AMD Mac or PC
- Use x86_64 cloud VM (AWS EC2, Google Cloud, Azure)
- Use Docker Desktop with x86_64 emulation (slow but works)

**Cloud VM recommendation**: AWS EC2 t3.xlarge (4 vCPU, 16GB RAM, ~$0.16/hour)

### Can I build on Raspberry Pi or ARM Linux?

No, same limitation as Apple Silicon. The RKNN toolkit requires x86_64 architecture for model compilation.

### What if I only have an ARM64 system?

Use a cloud VM:

```bash
# Example: AWS EC2 instance
aws ec2 run-instances \
  --image-id ami-xxxxxx \  # Ubuntu 22.04 x86_64
  --instance-type t3.xlarge \
  --key-name your-key

# SSH in and build normally
ssh -i your-key.pem ubuntu@<instance-ip>
git clone ... && cd python-cv-dev-extension
./check-prerequisites  # Should pass on x86_64 instance
```

---

## Build Process

### Why is the build so slow (30-60 minutes)?

**Reasons**:
1. **Cross-compilation**: Building ARM64 binaries on x86_64 host
2. **Large source tree**: BrightSign OS is ~20GB
3. **Complex dependencies**: Python + OpenCV + PyTorch + RKNN
4. **First build compiles everything**: ~200+ packages

**Subsequent builds are faster** (5-15 min) because BitBake caches compiled packages.

**Tips to speed up**:
- Build incrementally: `./build python3-opencv` instead of full SDK
- Use `--clean` only when necessary
- Don't run `--distclean` unless absolutely needed
- Use SSD instead of HDD
- Allocate more CPU cores to Docker

### Can I speed up Docker builds?

Yes, allocate more resources to Docker:

**Docker Desktop** (Mac/Windows):
- Settings → Resources
- Increase CPUs: 4-8 cores recommended
- Increase Memory: 8-16GB recommended
- Enable "Use the new Virtualization framework" (Mac)

**Linux**:
- Docker uses all available resources by default
- Ensure host has adequate resources (16GB+ RAM)

### Why 50GB+ disk space?

**Breakdown**:
- Docker image: ~20GB (includes BrightSign OS source)
- Build artifacts: ~20GB (BitBake work directories)
- SDK: ~10GB (extracted cross-compiler + libraries)
- Packages: ~1GB (output zip files)
- **Total**: ~51GB

**Space-saving tip**: Delete old build artifacts after successful builds:
```bash
./build --distclean  # Cleans tmp directories
rm -f *.zip          # Delete old packages
```

### What is BitBake and why is it needed?

**BitBake** is the build tool used by OpenEmbedded/Yocto to cross-compile embedded Linux systems.

**Why needed**: BrightSign OS is built with OpenEmbedded, so we use the same build system to create compatible packages.

**You don't need to learn BitBake** for basic usage - the `./build` script handles it for you.

---

## Player Deployment

### Development vs Production packages - which one?

**Development Package (`pydev-*.zip`)**:
- Installed to `/usr/local/pydev/`
- **Volatile** - lost on reboot
- Quick to deploy (no reboot needed)
- **Use for**: Testing, development, iteration

**Production Package (`ext_pydev-*.zip`)**:
- Installed as system extension to `/var/volatile/bsext/ext_pydev/`
- **Persistent** - survives reboots
- Auto-starts on boot
- Requires reboot to activate
- **Use for**: Production deployments, final release

**Recommendation**: Use development packages during development, switch to production when stable.

### How do I update the player firmware to 9.1.79.3?

**Method 1: Via DWS** (Diagnostic Web Server):
1. Download firmware: [OS 9.1.79.3](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip)
2. Extract the `.bsfw` file from zip
3. Navigate to `http://<player-ip>:8080`
4. Go to "Software Updates" tab
5. Upload `.bsfw` file
6. Reboot player

**Method 2: Via Serial Console**:
1. Connect serial cable (115200 bps, n-8-1)
2. Interrupt boot (Ctrl-C within 3 seconds)
3. Follow on-screen firmware update instructions

**Verify version after update**:
```bash
ssh brightsign@<player-ip>
cat /etc/version  # Should show 9.1.79.3 or later
```

### Why does the extension require OS 9.1.79.3 or later?

**Short answer**: Older OS versions don't include the NPU runtime library.

**Technical details**:
- RKNN toolkit requires `librknnrt.so` at `/usr/lib/librknnrt.so`
- OS 9.1.79.3+ includes this library in the system image
- Older versions would need complex binary patching workarounds

**If you can't upgrade**: Extension will install but NPU features won't work. CPU-only Python/OpenCV will still function.

### How do I unsecure a BrightSign player?

**Warning**: Only unsecure development/test players, never production units.

**Via serial console**:
```bash
# Connect serial (115200 bps, n-8-1)
# Press Ctrl-C during boot to interrupt

=> console on
=> reboot

# On next boot, interrupt again
=> setenv SECURE_CHECKS 0
=> envsave
=> reboot
```

**Verify**:
```bash
ssh brightsign@<player-ip>  # Should connect without issues
```

---

## User Initialization Scripts

### Why are my user scripts not running?

**Most common reason**: User scripts are disabled by default for security.

**Fix**:
1. Enable via DWS registry at `http://<player-ip>:8080`
2. Go to Registry tab
3. Enter: `registry write extension bsext-pydev-enable-user-scripts true`
4. Reboot player

**Other reasons**:
- Scripts not executable: `chmod +x /storage/sd/python-init/*.sh`
- Scripts in wrong location: Must be in `/storage/sd/python-init/`
- Script errors: Check `/var/log/bsext-pydev.log` for errors

### Can user scripts run Python code?

**Directly**: No, only shell scripts (`.sh` files) can execute from `/storage/sd/` (noexec mount).

**Workaround**: Shell script calls Python:
```bash
#!/bin/bash
# 02_my_python_script.sh

# Source Python environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Run Python script (store .py files elsewhere)
python3 /usr/local/my_script.py

# Or inline Python
python3 << 'EOF'
import torch
print(f"PyTorch version: {torch.__version__}")
EOF
```

See `user-init/templates/python_wrapper.sh` for a complete template.

### How do requirements.txt auto-install work?

**Process**:
1. Extension reads `/storage/sd/python-init/requirements.txt` on startup
2. Runs `pip3 install --break-system-packages -r requirements.txt`
3. Packages install to `/usr/local/lib/python3.8/site-packages` (volatile)
4. Installation log saved to `/storage/sd/python-init/requirements-install.log`

**Limitations**:
- Only PyPI packages with **pre-compiled ARM64 wheels** will work
- Player has no build tools (no cmake, gcc, etc.)
- Packages requiring compilation will fail

**Check if package has ARM64 wheel**:
```bash
# On your dev machine
pip index versions <package-name> | grep -i arm
# Or check PyPI page → "Download files" → look for aarch64/arm64 wheels
```

---

## NPU & Model Zoo

### What is NPU and why do I need it?

**NPU**: Neural Processing Unit - hardware accelerator for AI/ML inference.

**Benefits**:
- **10x faster** inference vs CPU
- **Lower power** consumption
- **Real-time** object detection/segmentation
- Offloads CPU for other tasks

**Example**: YOLOX object detection
- CPU: ~100ms per frame
- NPU: ~10ms per frame

### Can I use the extension without NPU?

Yes! The extension provides full Python 3.8 + OpenCV + NumPy + PyTorch even without NPU.

**What works without NPU**:
- OpenCV image processing
- NumPy numerical computing
- PyTorch CPU inference
- All standard Python libraries

**What requires NPU**:
- RKNN model inference
- Model zoo examples
- NPU-accelerated models (`.rknn` files)

### Why do model_zoo examples need a compatibility wrapper?

**Problem**: Official model_zoo examples use full `rknn-toolkit2` which has:
- Hardcoded `/usr/lib64/` paths (BrightSign uses `/usr/lib/`)
- Designed for x86_64 hosts, not ARM64 embedded targets

**Solution**: We provide `py_utils` compatibility wrapper that:
- Uses `RKNNLite` instead (designed for embedded ARM64)
- Handles API differences automatically
- Makes model_zoo examples "just work"

**Usage**:
```bash
# Just copy the wrapper before running examples
cp -r /usr/local/pydev/examples/py_utils \
      /usr/local/rknn_model_zoo/examples/yolox/python/

# Then run normally
python3 yolox.py --model_path model.rknn ...
```

### Where can I find pre-trained RKNN models?

**Official sources**:
1. **RKNN Model Zoo**: https://github.com/airockchip/rknn_model_zoo
   - 50+ pre-trained models
   - YOLOX, YOLOv5, YOLOv8, RetinaFace, etc.
   - Download from Releases page

2. **Rockchip GitHub**: https://github.com/rockchip-linux/rknn-toolkit2
   - Example models in `examples/` directory

3. **Convert your own**:
   - Use `rknn-toolkit2` on x86_64 host to convert ONNX/PyTorch models
   - See RKNN documentation for conversion process

---

## Troubleshooting

### Build fails with "Nothing PROVIDES python3-XXX"

**Problem**: Recipe for package doesn't exist in BrightSign OS or your recipes.

**Solution**:
1. Check if package is in BrightSign OS:
   ```bash
   ./build --interactive
   # Inside container:
   bitbake-layers show-recipes | grep python3-XXX
   ```

2. If missing, create recipe in `bsoe-recipes/meta-bs/recipes-devtools/python/`
3. See [docs/building.md](docs/building.md) for recipe development

### Extension won't start - "Mount failed"

**Problem**: Player is secured (SECURE_CHECKS=1)

**Solution**: Unsecure player via serial console (see above)

**Verify**:
```bash
ssh brightsign@<player-ip>
# Should connect and show BrightSign prompt
```

### Python import error: "No module named 'XXX'"

**Diagnosis**:
```bash
# On player
source /var/volatile/bsext/ext_pydev/sh/setup_python_env

# Check Python path
python3 -c "import sys; print('\n'.join(sys.path))"

# Try importing
python3 -c "import XXX"
```

**Common causes**:
1. **Package not in SDK**: Add to build (see [docs/building.md](docs/building.md))
2. **Environment not sourced**: Always `source sh/setup_python_env` first
3. **Wrong Python**: Make sure using `/usr/bin/python3.8` from extension
4. **Package failed to install**: Check requirements install log

### How do I completely remove the extension?

**Quick method**:
```bash
# Copy uninstall script to safe location first
cp /var/volatile/bsext/ext_pydev/uninstall.sh /tmp/
chmod +x /tmp/uninstall.sh

# Run uninstall
/tmp/uninstall.sh

# Reboot
reboot
```

**Manual method**:
```bash
# Stop extension
/var/volatile/bsext/ext_pydev/bsext_init stop

# Unmount and remove
umount /var/volatile/bsext/ext_pydev
rm -rf /var/volatile/bsext/ext_pydev
lvremove --yes /dev/mapper/bsos-ext_pydev

# Reboot
reboot
```

---

## Development & Customization

### How do I add a new Python package to the build?

See [docs/building.md](docs/building.md) for complete guide.

**Quick summary**:
1. Create BitBake recipe in `bsoe-recipes/meta-bs/recipes-devtools/python/`
2. Use existing recipes as templates
3. Validate recipe: `./check-recipe-syntax.py recipe.bb`
4. Build: `./build python3-<package>`
5. Rebuild SDK and repackage

### Can I modify the extension after deployment?

**Development package**: Yes, files in `/usr/local/pydev/` are writable

**Production extension**: No, mounted read-only from LVM volume

**To update production**: Deploy new version and reinstall

### How do I contribute?

1. Fork the repository
2. Make changes
3. Test thoroughly
4. Submit pull request

See [CONTRIBUTING.md](CONTRIBUTING.md) (if it exists) for detailed guidelines.

---

## Performance & Optimization

### How can I make builds faster?

1. **Incremental builds**: Build only changed packages
2. **Don't clean unnecessarily**: Avoid `--distclean` unless needed
3. **Allocate more resources**: More CPU/RAM to Docker
4. **Use SSD**: Much faster than HDD for builds
5. **Cache Docker image**: Reuse `bsoe-build` image across projects

### How much RAM does the player need?

**Minimum**: 2GB (built into XT-5 hardware)
**No configuration needed**: BrightSign manages memory automatically

**Extension uses**:
- Python runtime: ~50MB
- Libraries: ~200MB
- Models in memory: Varies (YOLOX ~100MB)

### Can I run multiple Python scripts simultaneously?

Yes, but:
- Be mindful of memory usage
- NPU can only run one model at a time
- Use multiprocessing carefully (limited CPU cores)

**Example**: Run background model inference + HTTP server
```python
from multiprocessing import Process

def run_inference():
    # NPU inference loop
    pass

def run_server():
    # HTTP server
    pass

if __name__ == '__main__':
    p1 = Process(target=run_inference)
    p2 = Process(target=run_server)
    p1.start()
    p2.start()
```

---

## Still Have Questions?

- **Documentation**: [README.md](README.md), [QUICKSTART.md](QUICKSTART.md), [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues)
- **Workflows**: [WORKFLOWS.md](WORKFLOWS.md)
- **Troubleshooting**: [docs/troubleshooting.md](docs/troubleshooting.md)
