# BrightSign Python Extension Documentation

This directory contains detailed documentation for developing Python extensions for BrightSign players using Yocto/BitBake.

## Documentation Structure

### Core Documents

1. **[build-process.md](build-process.md)** - Step-by-step guide to the build workflow
   - Prerequisites and setup
   - Recipe development workflow
   - Build optimization strategies
   - Artifact management

2. **[troubleshooting.md](troubleshooting.md)** - Common issues and solutions
   - Quick diagnosis flowchart
   - Error message reference
   - Advanced debugging techniques
   - Recovery procedures

### Quick Start

For new developers:
1. Read the main [README.md](../README.md) for project overview
2. Review [CLAUDE.md](../CLAUDE.md) for development constraints
3. Follow [build-process.md](build-process.md) for your first build
4. Reference [troubleshooting.md](troubleshooting.md) when issues arise

### Tools and Scripts

- **check-recipe-syntax.py** - Validate BitBake recipes before building
- **validate-recipes.sh** - Comprehensive recipe validation suite
- **patch-n-build.sh** - Main build automation script
- **recipe-template.bb** - Template for new Python package recipes

### Development Workflow

```
1. Create recipe from template
   ↓
2. Validate with check-recipe-syntax.py
   ↓
3. Build individual package
   ↓
4. Fix any issues (see troubleshooting.md)
   ↓
5. Run full validation suite
   ↓
6. Build full SDK
   ↓
7. Create extension package
```

### Key Commands Reference

```bash
# Validate single recipe
./check-recipe-syntax.py recipe.bb

# Validate all recipes
./validate-recipes.sh

# Build individual package
./patch-n-build.sh python3-package

# Clean build
./patch-n-build.sh --clean python3-package

# Full clean (permissions fix)
./patch-n-build.sh --distclean python3-package

# Build SDK
./patch-n-build.sh brightsign-sdk
```

### Best Practices

1. **Always validate before building** - Saves 5-15 minutes per iteration
2. **Test incrementally** - Individual packages before full SDK
3. **Use the template** - Ensures consistent recipe structure
4. **Monitor build output** - Catch errors early
5. **Document changes** - Track what worked and what didn't

### Getting Help

- Check error messages against troubleshooting.md
- Review similar recipes for patterns
- Validate syntax before asking for help
- Include logs and recipe content in bug reports