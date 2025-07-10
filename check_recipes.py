#!/usr/bin/env python3
"""
Quick recipe validation script to check for common issues before running slow BitBake builds.
"""

import os
import re
import glob
import sys

def check_recipe_syntax(recipe_path):
    """Check a recipe file for common syntax issues."""
    issues = []
    
    with open(recipe_path, 'r') as f:
        content = f.read()
        lines = content.splitlines()
    
    # Check for required fields
    required_fields = ['SUMMARY', 'LICENSE', 'LIC_FILES_CHKSUM']
    for field in required_fields:
        if not re.search(f'^{field}\\s*=', content, re.MULTILINE):
            issues.append(f"Missing required field: {field}")
    
    # Check for pip usage (should be avoided)
    if 'pip install' in content:
        issues.append("WARNING: Contains 'pip install' - may not work in cross-compilation")
    
    # Check for proper inheritance
    if 'inherit' not in content:
        issues.append("No 'inherit' statement found")
    
    # Check for setuptools3 or similar (unless using wheel files)
    is_wheel_based = '.whl' in content and 'unpack=0' in content
    if 'setuptools3' not in content and 'pypi' not in content and not is_wheel_based:
        issues.append("No Python build system inheritance (setuptools3/pypi)")
    
    # Check for SRC_URI (unless using pypi inheritance which auto-generates it)
    has_pypi = 'inherit pypi' in content or 'inherit.*pypi' in content
    has_src_uri = re.search(r'SRC_URI\s*=', content, re.MULTILINE)
    
    if not has_src_uri and not has_pypi:
        issues.append("No SRC_URI defined and not using pypi inheritance")
    
    # Check for checksums when SRC_URI is present (unless checksum is explicitly disabled)
    if has_src_uri and 'SRC_URI[' not in content and 'BB_STRICT_CHECKSUM = "0"' not in content:
        issues.append("SRC_URI present but no checksums defined")
    
    # Check for RDEPENDS
    if 'RDEPENDS' not in content:
        issues.append("No RDEPENDS defined")
    
    return issues

def main():
    """Check all Python recipes for common issues."""
    
    recipe_dir = "bsoe-recipes/meta-bs/recipes-open"
    
    if not os.path.exists(recipe_dir):
        print(f"Recipe directory not found: {recipe_dir}")
        return 1
    
    # Find all Python recipes
    python_recipes = []
    for root, dirs, files in os.walk(recipe_dir):
        for file in files:
            if file.startswith('python3-') and file.endswith('.bb'):
                python_recipes.append(os.path.join(root, file))
    
    if not python_recipes:
        print("No Python recipes found")
        return 0
    
    print(f"=== Checking {len(python_recipes)} Python recipes ===\n")
    
    total_issues = 0
    for recipe_path in sorted(python_recipes):
        recipe_name = os.path.basename(recipe_path)
        issues = check_recipe_syntax(recipe_path)
        
        if issues:
            print(f"❌ {recipe_name}:")
            for issue in issues:
                print(f"   - {issue}")
            total_issues += len(issues)
        else:
            print(f"✅ {recipe_name}: OK")
        print()
    
    print(f"=== Summary ===")
    print(f"Total recipes checked: {len(python_recipes)}")
    print(f"Total issues found: {total_issues}")
    
    if total_issues > 0:
        print(f"\n⚠️  Fix these issues before running BitBake builds")
        return 1
    else:
        print(f"\n✅ All recipes look good for testing")
        return 0

if __name__ == '__main__':
    sys.exit(main())