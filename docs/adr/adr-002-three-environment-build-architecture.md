# ADR-002: Three-Environment Build Architecture

## Status
Accepted

## Context

The BrightSign Python CV Extension project involves complex cross-compilation with multiple distinct environments that were initially misunderstood, leading to debugging difficulties and incorrect architectural assumptions.

The original mental model treated the build process as a simple two-stage operation (build → deploy), but investigation revealed a more complex three-environment architecture that must be clearly understood for effective development and debugging.

## Decision

**Explicitly model and document the build process as three distinct, separate environments with specific roles and constraints.**

The three environments are:

1. **Build Machine (x86_64 Host)** - Developer's local workspace
2. **SDK (Cross-Compilation Toolchain)** - Extracted target libraries and tools  
3. **Target Player (ARM64 BrightSign Device)** - Runtime deployment environment

Each environment has distinct characteristics, file paths, and capabilities that must be respected throughout the development process.

## Alternatives Considered

### Alternative 1: Two-Environment Model (Build → Deploy)
**Approach**: Treat SDK as part of build machine, ignore distinctions
**Rejected because**:
- Led to incorrect debugging assumptions about library locations
- Caused confusion about where files exist and when
- Made cross-compilation requirements unclear

### Alternative 2: Four-Environment Model (Build → SDK → Package → Deploy)
**Approach**: Treat packaging as separate fourth environment
**Rejected because**:
- Packaging is a process, not an environment
- Adds unnecessary complexity without clarifying capabilities
- Staging directory is part of build machine environment

### Alternative 3: Docker-Centric Model
**Approach**: Focus on Docker container as primary environment
**Rejected because**:
- Docker is implementation detail, not architectural boundary
- Obscures the cross-compilation nature of the problem
- Makes SDK extraction and usage less clear

## Architecture Details

### Environment 1: Build Machine (x86_64 Host)
**Location**: Developer's machine (`/home/scott/workspace/Brightsign/python-cv-dev-extension/`)  
**Purpose**: Cross-compilation and packaging  
**Capabilities**: 
- File manipulation and extraction
- Docker container execution
- ZIP/archive operations
- Cross-architecture file copying (safe for ARM64 binaries)

**Key Directories**:
```
├── sdk/                    # Extracted cross-compilation toolchain
├── install/                # Staging directory for packaging  
├── bsoe-recipes/          # BitBake recipe overlays
├── toolkit/               # Downloaded RKNN wheels and tools
└── build                  # Build script using Docker
```

### Environment 2: SDK (Cross-Compilation Toolchain)  
**Location**: `./sdk/` on build machine (extracted from Docker build)  
**Purpose**: Contains target libraries and cross-compiler tools  
**Capabilities**: ARM64 libraries, Python site-packages, cross-compilation toolchain

**Key Structure**:
```
sdk/sysroots/
├── aarch64-oe-linux/       # Target sysroot (ARM64)
│   └── usr/
│       ├── lib/
│       │   ├── librknnrt.so    # ✅ Library IS here
│       │   └── python3.8/site-packages/
│       └── bin/
└── x86_64-oesdk-linux/     # Build tools (x86_64)
```

### Environment 3: Target Player (ARM64 BrightSign Device)
**Purpose**: Runtime execution environment  
**Capabilities**: ARM64 code execution, limited filesystem write access  
**Deployment Modes**: Development (volatile) and Production (persistent extensions)

**Development Mode** (`/usr/local/` - volatile):
```
/usr/local/                 # Read-write, executable, volatile
├── pydev/                  # Development extraction point
│   ├── usr/lib/librknnrt.so
│   └── usr/lib/python3.8/site-packages/
└── lib64/librknnrt.so     # Symlink created by setup script
```

**Production Mode** (`/var/volatile/bsext/` - persistent):
```
/var/volatile/bsext/ext_pydev/    # Extension mount point
├── usr/lib/librknnrt.so
├── usr/lib/python3.8/site-packages/
├── lib64/librknnrt.so           # Runtime symlink
└── bsext_init                   # Service startup script
```

## Key Insights from Architecture

### Cross-Environment File Flow
```
Build Machine (x86_64) → SDK (ARM64 libs) → Target Player (ARM64 execution)
     ↓                        ↓                      ↓
File extraction         Library staging        Runtime execution
ZIP operations         Cross-compilation      ARM64 native code
Packaging assembly     Target preparation     User applications
```

### Environment Boundaries and Constraints

1. **Build ↔ SDK**: File copying, extraction safe; no ARM64 code execution
2. **SDK → Target**: Architecture transition; ARM64 binaries must be deployment-ready  
3. **Target constraints**: Read-only filesystem areas, extension mount points, ARM64 execution only

### Library Location Clarity
The three-environment model resolved critical debugging confusion:

- **❌ Initial assumption**: librknnrt.so missing from system
- **✅ Reality**: Library present in SDK and deployed to target
- **🔍 Actual issue**: Python package rknn-toolkit-lite2 not installed in any environment

## Implementation Guidelines

### Development Process
1. **Build Phase**: Use Docker + BitBake on build machine (x86_64)
2. **SDK Extraction**: Extract ARM64 libraries and tools to `./sdk/`
3. **Packaging Phase**: Assemble from SDK + additional sources to `./install/`  
4. **Deployment**: Transfer to target player (ARM64) for runtime execution

### Cross-Environment Operations
- **Safe**: File copying, ZIP extraction, directory operations across architectures
- **Unsafe**: Attempting to execute ARM64 binaries on x86_64 build machine
- **Required**: Proper staging in `install/` directory before packaging

### Debugging Strategy
When investigating issues:
1. **Identify environment**: Which of the three environments has the issue?
2. **Check file presence**: Verify files exist in correct environment locations
3. **Validate architecture**: Confirm ARM64 binaries are properly staged
4. **Test environment transitions**: Ensure proper file flow between environments

## Consequences

### Positive
- ✅ **Clear mental model**: Eliminates confusion about where files exist
- ✅ **Better debugging**: Focused investigation within correct environment  
- ✅ **Architectural clarity**: Makes cross-compilation requirements explicit
- ✅ **Development efficiency**: Prevents wrong-environment assumptions
- ✅ **Documentation foundation**: Provides structure for explaining complex flows

### Negative  
- ❌ **Increased complexity**: Developers must understand three distinct environments
- ❌ **Documentation overhead**: More concepts to explain and maintain
- ❌ **Setup complexity**: Multiple environment setup and validation required

### Neutral
- **Training requirement**: New developers need three-environment education
- **Tooling implications**: Scripts and tools must respect environment boundaries

## Validation

### Environment Verification Checklist
**Build Machine**:
- [ ] Docker container can build and execute BitBake
- [ ] Staging directory `install/` properly assembles target files
- [ ] Cross-architecture file operations work correctly

**SDK**:  
- [ ] ARM64 libraries present in `sdk/sysroots/aarch64-oe-linux/usr/lib/`
- [ ] Python site-packages extracted properly
- [ ] Cross-compilation toolchain functional

**Target Player**:
- [ ] Extension mounts at correct location (`/var/volatile/bsext/ext_pydev/`)
- [ ] ARM64 binaries execute correctly
- [ ] Environment setup script creates proper symlinks

## References

- **Architecture documentation**: `docs/architecture-understanding.md`
- **Debugging session**: `.claude/session-logs/2025-01-28-1430-explore-installing-wheels.md`
- **Build system docs**: `README.md` sections on build process
- **Environment setup**: `sh/setup_python_env` script
- **BitBake documentation**: OpenEmbedded/Yocto project references

---

**Date**: 2025-01-28  
**Author**: System Architecture Analysis  
**Stakeholders**: Build system developers, cross-compilation workflows, debugging processes