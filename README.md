# BrightSign Python Extension for CV Development (ALPHA RELEASE)

> Python 3.8 + OpenCV + PyTorch + NPU acceleration for BrightSign digital signage players

**Quick Links**: [Quick Start](#quick-start) | [QUICKSTART.md](QUICKSTART.md) | [WORKFLOWS.md](WORKFLOWS.md) | [FAQ.md](FAQ.md)

---

## What Is This?

A complete Python development environment for BrightSign Series 5 players with:

- **Python 3.8** + standard library
- **Computer Vision**: OpenCV, scikit-image, PIL
- **Deep Learning**: PyTorch, ONNX
- **NPU Acceleration**: RKNN toolkit for 10x faster inference
- **Scientific Stack**: NumPy, pandas, SciPy, matplotlib

**Use Cases**: Audience analytics, interactive displays, retail analytics, smart signage, edge AI

---

## System Architecture

```
┌─────────────────────────────────┐         ┌──────────────────────────────────┐
│   Development Host (x86_64)     │         │  BrightSign Player (ARM64)       │
│                                 │         │                                  │
│  ┌──────────────────────────┐  │         │  ┌────────────────────────────┐  │
│  │  1. Setup (5-10 min)     │  │         │  │  BrightSign OS 9.1.79.3+   │  │
│  │  - Download sources      │  │         │  │  (Read-only Linux)         │  │
│  │  - Build Docker image    │  │         │  └────────────────────────────┘  │
│  └──────────────────────────┘  │         │                                  │
│            ↓                    │         │  ┌────────────────────────────┐  │
│  ┌──────────────────────────┐  │         │  │  Extension                 │  │
│  │  2. Build (30-60 min)    │  │         │  │  /var/volatile/bsext/      │  │
│  │  - Cross-compile         │  │         │  │  - Python 3.8 runtime      │  │
│  │  - Python + CV packages  │  │         │  │  - CV/ML libraries         │  │
│  │  - Extract SDK           │  │         │  │  - RKNN NPU toolkit        │  │
│  └──────────────────────────┘  │         │  │  - Auto-start on boot      │  │
│            ↓                    │         │  └────────────────────────────┘  │
│  ┌──────────────────────────┐  │         │                                  │
│  │  3. Package (5 min)      │  │         │  ┌────────────────────────────┐  │
│  │  - Create .zip files     │──┼────────▶│  │  User Scripts (Optional)   │  │
│  │  - Dev + Production      │  │ Deploy │  │  /storage/sd/python-init/  │  │
│  └──────────────────────────┘  │         │  │  - Custom initialization   │  │
│                                 │         │  │  - Auto-run on startup     │  │
└─────────────────────────────────┘         │  └────────────────────────────┘  │
                                            │                                  │
                                            │  ┌────────────────────────────┐  │
                                            │  │  NPU (Neural Processor)    │  │
                                            │  │  RK3588 - 6 TOPS           │  │
                                            │  │  ~10x faster inference     │  │
                                            │  └────────────────────────────┘  │
                                            └──────────────────────────────────┘
```

**Build Time**: 60-90 minutes first time, 5-15 minutes incremental
**Package Size**: 420MB development, 370MB production
**Target**: ARM64 BrightSign players with RK3588 NPU

---

## Prerequisites

### Critical Requirements

| Requirement | Details | Why |
|------------|---------|-----|
| **x86_64 architecture** | Intel/AMD CPU | RKNN toolchain limitation |
| **16GB+ RAM** | 32GB recommended | Cross-compilation memory needs |
| **50GB+ disk space** | SSD preferred | Build artifacts (~20GB) + source (~20GB) + SDK (~10GB) |
| **Docker** | Docker Desktop or docker-ce | Isolated build environment |
| **OS 9.1.79.3+** | BrightSign firmware | NPU runtime library required |

### ❌ NOT Compatible

- Apple Silicon (M1/M2/M3/M4) - use x86_64 cloud VM instead
- ARM64 / aarch64 Linux
- Raspberry Pi
- BrightSign OS < 9.1.79.3

**Validate your system**:
```bash
./check-prerequisites  # Checks architecture, Docker, disk space, etc.
```

See [FAQ.md](FAQ.md#architecture--compatibility) for Apple Silicon workarounds.

---

## Quick Start

### Three Commands to Build

```bash
# 1. Validate and setup (5-10 min)
./check-prerequisites && ./setup -y

# 2. Build SDK (30-60 min - safe to background)
./build --extract-sdk

# 3. Package extension (5 min)
./brightsign-x86_64-cobra-toolchain-*.sh -d ./sdk -y && ./package
```

**Output**: Two deployment packages
- `pydev-YYYYMMDD-HHMMSS.zip` - Development (volatile, fast iteration)
- `ext_pydev-YYYYMMDD-HHMMSS.zip` - Production (persistent, auto-start)

**Detailed walkthrough**: See [QUICKSTART.md](QUICKSTART.md) for step-by-step guide with troubleshooting.

### Deploy to Player

**Quick deploy** (development package):

```bash
# Transfer via DWS (http://<player-ip>:8080 → SD tab)
# Then install via SSH:

ssh brightsign@<player-ip>
# Exit to Linux shell (type 'exit' twice)

mkdir -p /usr/local/pydev && cd /usr/local/pydev
unzip /storage/sd/pydev-*.zip
source sh/setup_python_env

# Test it works
python3 -c "import cv2, torch, numpy; print('Ready!')"
```

**Production deploy** (persistent extension):

```bash
# Transfer ext_pydev-*.zip via DWS, then:
ssh brightsign@<player-ip>

mkdir -p /usr/local && cd /usr/local
unzip /storage/sd/ext_pydev-*.zip
bash ./ext_pydev_install-lvm.sh
reboot  # Extension auto-starts after reboot
```

**See**: [docs/deployment.md](docs/deployment.md) for complete deployment guide.

---

## Documentation

### Getting Started
- **[QUICKSTART.md](QUICKSTART.md)** - 60-90 minute walkthrough (START HERE)
- **[WORKFLOWS.md](WORKFLOWS.md)** - Copy-paste commands for common tasks
- **[FAQ.md](FAQ.md)** - Answers to 25+ common questions
- **[check-prerequisites](check-prerequisites)** - Validate system before building

### Detailed Guides
- **[docs/getting-started.md](docs/getting-started.md)** - First-time setup details
- **[docs/building.md](docs/building.md)** - Build system and recipe development
- **[docs/deployment.md](docs/deployment.md)** - Player setup and deployment
- **[docs/model-zoo-guide.md](docs/model-zoo-guide.md)** - NPU examples and usage
- **[docs/troubleshooting.md](docs/troubleshooting.md)** - Common issues and fixes

### Advanced Topics
- **[user-init/README.md](user-init/README.md)** - Custom initialization scripts
- **[docs/architecture-understanding.md](docs/architecture-understanding.md)** - System architecture
- **[docs/adr/](docs/adr/)** - Architecture Decision Records

---

## Features

### Python Environment

**Core Python** (built-in):
- Python 3.8.19 + standard library
- pip, setuptools, wheel
- Full cross-compilation for ARM64

**Pre-installed Packages**:
```python
# Computer Vision
import cv2              # OpenCV 4.x
import PIL              # Pillow image processing
from skimage import *   # scikit-image

# Deep Learning
import torch            # PyTorch 2.4.1
import onnx             # ONNX model format

# NPU Acceleration
from rknnlite.api import RKNNLite  # RKNN toolkit 2.3.2

# Scientific Computing
import numpy            # NumPy 1.24.4
import pandas           # pandas 2.0.3
import scipy            # SciPy 1.10.1
import matplotlib       # matplotlib 3.7.5

# And 50+ more packages...
```

### NPU Acceleration

**RK3588 Neural Processing Unit**:
- 6 TOPS performance
- ~10x faster than CPU inference
- Low power consumption
- Official RKNN model zoo support

**Example**: YOLOX object detection
- CPU: ~100ms per frame
- NPU: ~10ms per frame (6 FPS → 60+ FPS real-time)

**Model Zoo**: 50+ pre-trained models available (YOLO, RetinaFace, SegFormer, etc.)

### User Initialization System

**Automatic package installation**:
```bash
# Place requirements.txt in /storage/sd/python-init/
# Packages auto-install on boot

echo "opencv-contrib-python" >> /storage/sd/python-init/requirements.txt
# Installs automatically next reboot
```

**Custom init scripts**:
```bash
# Shell scripts in /storage/sd/python-init/*.sh
# Auto-run on extension startup
# Use for: model downloads, env setup, validation checks
```

**Security**: User scripts disabled by default (run as root), must enable via registry.

See [user-init/README.md](user-init/README.md) for details.

---

## Supported Players

| Player Model | Minimum OS Version | Status |
|-------------|-------------------|---------|
| **XT-5** (XT1145, XT2145) | [9.1.79.3](https://brightsignbiz.s3.amazonaws.com/firmware/xd5/9.1/9.1.79.3/brightsign-xd5-update-9.1.79.3.zip) | ✅ Tested |
| **Firebird** | 9.1.79.3+ | 🔄 In Process |
| **LS-5** (LS445) | 9.1.79.3+ | 🔄 In Process |

**Why 9.1.79.3+?** This OS version includes the NPU runtime library (`librknnrt.so`) at `/usr/lib/librknnrt.so`. Earlier versions will encounter RKNN initialization failures.

---

## Configuration

### Registry Settings (via DWS)

Access DWS at `http://<player-ip>:8080` → Registry tab:

| Registry Key | Default | Description |
|-------------|---------|-------------|
| `bsext-pydev-enable-user-scripts` | `false` | Enable user init scripts (security: runs as root) |
| `bsext-pydev-disable-auto-start` | `false` | Disable extension auto-start (for debugging) |

**Enable user scripts**:
```bash
registry write extension bsext-pydev-enable-user-scripts true
# Restart player for changes to take effect
```

**Disable auto-start** (for troubleshooting):
```bash
registry write extension bsext-pydev-disable-auto-start true
# Restart, then manually control extension
```

---

## Example: NPU Object Detection

Run YOLOX object detection with NPU acceleration in <10 minutes:

```bash
# 1. Download model and test image (on dev machine)
wget https://github.com/airockchip/rknn_model_zoo/releases/download/v2.3.2/yolox_s_rk3588.rknn
wget https://raw.githubusercontent.com/airockchip/rknn_model_zoo/v2.3.2/examples/yolox/model/bus.jpg
scp yolox_s_rk3588.rknn bus.jpg brightsign@<player-ip>:/usr/local/

# 2. On player: Setup model_zoo
ssh brightsign@<player-ip>
source /usr/local/pydev/sh/setup_python_env

cd /usr/local
wget https://github.com/airockchip/rknn_model_zoo/archive/refs/tags/v2.3.2.zip
unzip v2.3.2.zip && mv rknn_model_zoo-2.3.2 rknn_model_zoo

# Copy compatibility wrapper (enables RKNNLite API)
cp -r /usr/local/pydev/examples/py_utils \
      /usr/local/rknn_model_zoo/examples/yolox/python/

# 3. Run inference
cd /usr/local/rknn_model_zoo/examples/yolox/python
python3 yolox.py --model_path /usr/local/yolox_s_rk3588.rknn \
                 --target rk3588 \
                 --img_folder /usr/local/ \
                 --img_save

# Results: ./result/bus.jpg with bounding boxes
```

**Expected**: Detects bus (93% confidence), people (85-90%) using NPU in ~10ms

**See**: [docs/model-zoo-guide.md](docs/model-zoo-guide.md) for more examples and custom models.

---

## Troubleshooting

### Quick Diagnostics

```bash
# On dev host: Validate prerequisites
./check-prerequisites

# On player: Check extension status
ssh brightsign@<player-ip>
/var/volatile/bsext/ext_pydev/bsext_init status

# View logs
tail -f /var/log/bsext-pydev.log

# Test Python environment
source /var/volatile/bsext/ext_pydev/sh/setup_python_env
python3 -c "import cv2, torch, numpy; print('OK')"
```

### Common Issues

| Issue | Quick Fix |
|-------|-----------|
| Build fails: "Not x86_64" | Use x86_64 machine or cloud VM (see [FAQ](FAQ.md#why-x86_64-only)) |
| Build fails: "No space" | Free 50GB+ disk space |
| Player: RKNN init fails | Upgrade to OS 9.1.79.3+ |
| Extension won't install | Unsecure player (`SECURE_CHECKS=0`) |
| Scripts don't run | Enable user scripts via registry |

**Full troubleshooting**: [docs/troubleshooting.md](docs/troubleshooting.md)

---

## Project Structure

```
.
├── setup                    # Setup script (run once)
├── build                    # Build script (main build tool)
├── package                  # Package creation script
├── check-prerequisites      # System validation script
├── QUICKSTART.md            # 60-90 min quick start guide
├── WORKFLOWS.md             # Common workflow commands
├── FAQ.md                   # Frequently asked questions
│
├── bsoe-recipes/            # BitBake recipe overlays
│   └── meta-bs/recipes-devtools/python/  # Python package recipes
│
├── user-init/               # User initialization system
│   ├── examples/            # Ready-to-deploy scripts
│   ├── templates/           # Script templates
│   └── tools/               # Deployment helpers
│
├── docs/                    # Detailed documentation
│   ├── getting-started.md
│   ├── building.md
│   ├── deployment.md
│   ├── model-zoo-guide.md
│   ├── troubleshooting.md
│   └── adr/                 # Architecture decisions
│
└── sh/                      # Build and extension scripts
```

---

## Development Workflow

### First-Time Setup
```bash
./check-prerequisites  # Validate system
./setup -y             # Setup (5-10 min)
./build --extract-sdk  # Build (30-60 min)
./package              # Package (5 min)
```

### Incremental Rebuild
```bash
# After modifying a recipe
./build --clean python3-opencv
./build python3-opencv
./build --extract-sdk
./package
```

### Add Python Package
```bash
# Create recipe in bsoe-recipes/meta-bs/recipes-devtools/python/
./check-recipe-syntax.py python3-newpackage.bb
./build python3-newpackage
./build --extract-sdk
./package
```

See [WORKFLOWS.md](WORKFLOWS.md) for more common tasks.

---

## Contributing

Contributions welcome! Areas of interest:

- **Python packages**: Add more CV/ML libraries
- **Documentation**: Improve guides and examples
- **Testing**: Validate on different players
- **Examples**: Create demo applications

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## Licensing

- **Project**: Apache 2.0 License (see [LICENSE.txt](LICENSE.txt))
- **Models**: Check individual model licenses (see [model-licenses.md](model-licenses.md))
- **Dependencies**: Various open source licenses

---

## Getting Help

- **Quick start**: [QUICKSTART.md](QUICKSTART.md)
- **Common workflows**: [WORKFLOWS.md](WORKFLOWS.md)
- **FAQ**: [FAQ.md](FAQ.md)
- **Troubleshooting**: [docs/troubleshooting.md](docs/troubleshooting.md)
- **Issues**: [GitHub Issues](https://github.com/brightsign/python-cv-dev-extension/issues)

---

## Credits

Built with:
- BrightSign OS (OpenEmbedded/Yocto)
- Rockchip RKNN toolkit
- Python 3.8 + ecosystem
- Docker

Maintained by BrightSign development team.
