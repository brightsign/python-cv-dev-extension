# BrightSign Python Extension Documentation

This directory contains comprehensive documentation for developing and deploying Python extensions with NPU acceleration for BrightSign players.

## Documentation Structure

### Getting Started (Start Here!)

1. **[../QUICKSTART.md](../QUICKSTART.md)** - Fast-track guide (60-90 minutes to first build)
   - Three-command quick start
   - Prerequisites with time estimates
   - Common issues and solutions
   - Model Zoo quick validation

2. **[getting-started.md](getting-started.md)** - Complete first-time setup guide
   - Detailed system requirements
   - Environment preparation
   - Step-by-step build process with explanations
   - Packaging and verification
   - Troubleshooting at each stage

3. **[../FAQ.md](../FAQ.md)** - 25+ frequently asked questions
   - Architecture & compatibility
   - Build process questions
   - Deployment and testing
   - NPU and Model Zoo

### Core Workflow Guides

4. **[deployment.md](deployment.md)** - Deploying packages to BrightSign players
   - Package types (development vs production)
   - Player preparation and prerequisites
   - Deployment workflows and commands
   - User init scripts for auto-start
   - Verification and testing
   - Updates and maintenance

5. **[model-zoo-guide.md](model-zoo-guide.md)** - Using NPU-accelerated inference
   - RKNN Model Zoo overview (50+ models)
   - RKNNLite compatibility setup
   - YOLOX quick start example
   - Available model categories
   - Model conversion and optimization
   - Performance tuning

6. **[../WORKFLOWS.md](../WORKFLOWS.md)** - Copy-paste command reference
   - First-time setup commands
   - Rebuild procedures
   - Deployment workflows
   - Adding Python packages
   - NPU testing examples
   - Common troubleshooting commands

### Advanced Topics

7. **[build-process.md](build-process.md)** - Deep dive into Yocto/BitBake workflow
   - Prerequisites and setup
   - Recipe development workflow
   - Build optimization strategies
   - Artifact management

8. **[troubleshooting.md](troubleshooting.md)** - Build issue resolution
   - Quick diagnosis flowchart
   - Error message reference
   - Advanced debugging techniques
   - Recovery procedures

9. **[troubleshooting-user-init.md](troubleshooting-user-init.md)** - User script initialization troubleshooting
   - Complete initialization flow diagram
   - Systematic diagnostic checks
   - 21+ failure point analysis
   - Copy-paste diagnostic commands
   - Common scenarios and solutions

### Quick Start Path

**For new developers**, follow this sequence:

1. Start with [../QUICKSTART.md](../QUICKSTART.md) for immediate hands-on experience
2. Read [getting-started.md](getting-started.md) for detailed understanding
3. Use [../WORKFLOWS.md](../WORKFLOWS.md) as your command reference
4. Deploy with [deployment.md](deployment.md)
5. Test NPU inference with [model-zoo-guide.md](model-zoo-guide.md)
6. Reference [../FAQ.md](../FAQ.md) when questions arise
7. Use [troubleshooting.md](troubleshooting.md) when issues occur

### Tools and Scripts

Located in the project root directory:

- **[../setup](../setup)** - Initial environment setup (downloads sources, builds Docker)
- **[../build](../build)** - BitBake build wrapper (SDK and individual packages)
- **[../package](../package)** - Package extension artifacts into deployable zips
- **[../check-prerequisites](../check-prerequisites)** - Validate system before building
- **[../check-recipe-syntax.py](../check-recipe-syntax.py)** - Validate BitBake recipes
- **[../validate](../validate)** - Comprehensive recipe validation suite

### Development Workflow

#### First-Time Setup
```
1. Check prerequisites (./check-prerequisites)
   ↓
2. Setup environment (./setup -y)
   ↓
3. Build SDK (./build --extract-sdk)
   ↓
4. Package extension (./package)
   ↓
5. Deploy to player (see deployment.md)
   ↓
6. Test with Model Zoo (see model-zoo-guide.md)
```

#### Adding Python Packages
```
1. Create recipe from template (bsoe-recipes/meta-bs/recipes-devtools/python/)
   ↓
2. Validate syntax (./check-recipe-syntax.py recipe.bb)
   ↓
3. Build individual package (./build python3-packagename)
   ↓
4. Fix issues (see troubleshooting.md)
   ↓
5. Rebuild SDK (./build --extract-sdk)
   ↓
6. Package and deploy
```

### Essential Commands Reference

See [../WORKFLOWS.md](../WORKFLOWS.md) for comprehensive command reference. Quick examples:

```bash
# Prerequisites check
./check-prerequisites

# Initial setup
./setup -y

# Build SDK (30-60 minutes)
./build --extract-sdk

# Build individual package (5-15 minutes)
./build python3-opencv

# Clean build
./build --clean python3-opencv

# Package development extension
./package

# Package production extension
./package --production

# Validate recipes
./validate
```

### Best Practices

1. **Check prerequisites first** - Use `./check-prerequisites` to fail fast on incompatible systems
2. **Test incrementally** - Individual packages before full SDK (saves 30+ minutes per iteration)
3. **Use WORKFLOWS.md** - Copy-paste commands instead of memorizing
4. **Follow the quick start path** - Reduces time-to-first-build from 2-4 hours to 60-90 minutes
5. **Read FAQ.md first** - 80% of questions already answered
6. **Deploy development packages for testing** - Faster iteration than production packages

### Documentation By Task

**I want to...**

- **Build my first extension** → Start with [../QUICKSTART.md](../QUICKSTART.md)
- **Understand the system deeply** → Read [getting-started.md](getting-started.md)
- **Deploy to a player** → Follow [deployment.md](deployment.md)
- **Use NPU for inference** → See [model-zoo-guide.md](model-zoo-guide.md)
- **Find a specific command** → Check [../WORKFLOWS.md](../WORKFLOWS.md)
- **Solve a build problem** → Try [../FAQ.md](../FAQ.md) first, then [troubleshooting.md](troubleshooting.md)
- **Fix user script issues** → See [troubleshooting-user-init.md](troubleshooting-user-init.md)
- **Add a Python package** → See [build-process.md](build-process.md)
- **Understand BitBake** → Deep dive in [build-process.md](build-process.md)

### Getting Help

1. **Check [../FAQ.md](../FAQ.md)** - Most common questions answered
2. **Search troubleshooting guides**:
   - Build issues → [troubleshooting.md](troubleshooting.md)
   - User script issues → [troubleshooting-user-init.md](troubleshooting-user-init.md)
3. **Review [../WORKFLOWS.md](../WORKFLOWS.md)** - Ensure you're using correct commands
4. **Check prerequisites** - Many issues stem from incompatible systems
5. **Read error logs** - BitBake and extension provide detailed failure information

### Contributing

When adding documentation:
- Follow progressive disclosure pattern (overview → details)
- Include time estimates for long operations
- Provide copy-paste commands where possible
- Add troubleshooting sections
- Update this README to link to new content