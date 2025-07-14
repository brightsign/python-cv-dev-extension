#!/usr/bin/env python3
"""
BitBake Recipe Syntax Checker for Python Packages
Validates recipe files against common patterns and requirements
"""

import os
import re
import sys
import argparse
from pathlib import Path

class RecipeChecker:
    def __init__(self):
        self.errors = []
        self.warnings = []
        
    def check_recipe(self, filepath):
        """Check a single recipe file for common issues"""
        self.errors = []
        self.warnings = []
        
        with open(filepath, 'r') as f:
            content = f.read()
            lines = content.splitlines()
        
        recipe_name = os.path.basename(filepath)
        
        # Check filename format
        if not re.match(r'^python3?-[a-z0-9-]+_[\d.]+\.bb$', recipe_name):
            self.warnings.append(f"Non-standard filename format: {recipe_name}")
        
        # Required variables
        required_vars = ['SUMMARY', 'LICENSE', 'LIC_FILES_CHKSUM']
        for var in required_vars:
            if not re.search(f'^{var}\\s*=', content, re.MULTILINE):
                self.errors.append(f"Missing required variable: {var}")
        
        # Check for proper FILES definition
        if 'FILES:${PN}' not in content and 'FILES_${PN}' not in content:
            self.errors.append("Missing FILES:${PN} definition - packages may not be created properly")
        
        # Check for inherit statements
        if 'inherit' not in content:
            self.errors.append("No inherit statement found - should inherit setuptools3 or similar")
        elif 'setuptools3' in content and 'pypi' not in content:
            self.warnings.append("Using setuptools3 without pypi inherit - consider using both")
        
        # Check for SRC_URI
        if 'SRC_URI' not in content and 'inherit pypi' not in content:
            self.errors.append("No SRC_URI defined and not inheriting pypi")
        
        # Check for checksum
        if 'SRC_URI[' in content and not re.search(r'SRC_URI\[(md5sum|sha256sum)\]', content):
            self.errors.append("SRC_URI defined but no checksum provided")
        
        # Check for QA skip flags for binary packages
        if re.search(r'\.whl|wheel|binary', content, re.IGNORECASE):
            if 'INSANE_SKIP' not in content:
                self.warnings.append("Binary package detected but no INSANE_SKIP flags")
        
        # Check for cross-compilation issues
        if 'pip install' in content:
            self.errors.append("Using 'pip install' in recipe - this will install for host architecture, not target!")
        
        # Check for proper Python paths
        if '${PYTHON_SITEPACKAGES_DIR}' not in content and 'FILES:${PN}' in content:
            self.warnings.append("FILES definition doesn't use ${PYTHON_SITEPACKAGES_DIR}")
        
        # Check for version in filename vs PV
        match = re.search(r'python3?-[a-z0-9-]+_([\d.]+)\.bb$', recipe_name)
        if match:
            file_version = match.group(1)
            if f'PV = "{file_version}"' not in content and '${PV}' in content:
                self.warnings.append(f"Filename version {file_version} may not match PV variable")
        
        # Check indentation
        for i, line in enumerate(lines):
            if line and not line.startswith('#'):
                if line.startswith(' ') and not line.startswith('    '):
                    self.warnings.append(f"Line {i+1}: Non-standard indentation (use 4 spaces)")
        
        return len(self.errors) == 0
    
    def print_results(self, filepath):
        """Print check results"""
        print(f"\nChecking: {filepath}")
        print("-" * 60)
        
        if self.errors:
            print("ERRORS:")
            for error in self.errors:
                print(f"  ❌ {error}")
        
        if self.warnings:
            print("WARNINGS:")
            for warning in self.warnings:
                print(f"  ⚠️  {warning}")
        
        if not self.errors and not self.warnings:
            print("  ✅ Recipe looks good!")
        
        return len(self.errors) == 0

def main():
    parser = argparse.ArgumentParser(description='Check BitBake recipe syntax')
    parser.add_argument('recipes', nargs='+', help='Recipe files to check')
    parser.add_argument('--strict', action='store_true', help='Treat warnings as errors')
    args = parser.parse_args()
    
    checker = RecipeChecker()
    all_passed = True
    
    for recipe_path in args.recipes:
        if os.path.isdir(recipe_path):
            # Check all .bb files in directory
            for bb_file in Path(recipe_path).rglob('*.bb'):
                passed = checker.check_recipe(bb_file)
                checker.print_results(bb_file)
                if not passed or (args.strict and checker.warnings):
                    all_passed = False
        else:
            passed = checker.check_recipe(recipe_path)
            checker.print_results(recipe_path)
            if not passed or (args.strict and checker.warnings):
                all_passed = False
    
    sys.exit(0 if all_passed else 1)

if __name__ == '__main__':
    main()