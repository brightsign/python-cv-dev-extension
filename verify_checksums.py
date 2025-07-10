#!/usr/bin/env python3
"""
Verify checksums for Python package recipes by downloading files and computing actual checksums.
"""

import os
import re
import sys
import hashlib
import urllib.request
import urllib.parse
from pathlib import Path

def compute_checksums(file_path):
    """Compute MD5 and SHA256 checksums for a file."""
    md5_hash = hashlib.md5()
    sha256_hash = hashlib.sha256()
    
    with open(file_path, 'rb') as f:
        for chunk in iter(lambda: f.read(4096), b""):
            md5_hash.update(chunk)
            sha256_hash.update(chunk)
    
    return md5_hash.hexdigest(), sha256_hash.hexdigest()

def download_file(url, local_path):
    """Download a file from URL to local path."""
    print(f"Downloading {url}...")
    try:
        urllib.request.urlretrieve(url, local_path)
        return True
    except Exception as e:
        print(f"Error downloading {url}: {e}")
        return False

def extract_recipe_info(recipe_path):
    """Extract package info and checksums from a recipe file."""
    with open(recipe_path, 'r') as f:
        content = f.read()
    
    # Extract PYPI_PACKAGE if present
    pypi_match = re.search(r'PYPI_PACKAGE\s*=\s*"([^"]+)"', content)
    pypi_package = pypi_match.group(1) if pypi_match else None
    
    # Extract version from filename
    recipe_name = os.path.basename(recipe_path)
    version_match = re.search(r'_([^_]+)\.bb$', recipe_name)
    version = version_match.group(1) if version_match else None
    
    # Extract SRC_URI
    src_uri_match = re.search(r'SRC_URI\s*=\s*"([^"]+)"', content)
    src_uri = src_uri_match.group(1) if src_uri_match else None
    
    # Extract current checksums
    md5_match = re.search(r'SRC_URI\[md5sum\]\s*=\s*"([^"]+)"', content)
    sha256_match = re.search(r'SRC_URI\[sha256sum\]\s*=\s*"([^"]+)"', content)
    
    current_md5 = md5_match.group(1) if md5_match else None
    current_sha256 = sha256_match.group(1) if sha256_match else None
    
    # Generate URL if using pypi inheritance
    if pypi_package and version and not src_uri:
        src_uri = f"https://files.pythonhosted.org/packages/source/{pypi_package[0]}/{pypi_package}/{pypi_package}-{version}.tar.gz"
    
    return {
        'recipe_path': recipe_path,
        'package': pypi_package,
        'version': version,
        'src_uri': src_uri,
        'current_md5': current_md5,
        'current_sha256': current_sha256
    }

def verify_recipe_checksums(recipe_info, temp_dir):
    """Verify checksums for a single recipe."""
    if not recipe_info['src_uri']:
        return True, "No SRC_URI found"
    
    # Skip wheel files and files with disabled checksums
    if '.whl' in recipe_info['src_uri'] or 'BB_STRICT_CHECKSUM = "0"' in open(recipe_info['recipe_path']).read():
        return True, "Wheel file or checksum disabled - skipping"
    
    # Parse the URL (handle pypi inheritance URLs)
    url = recipe_info['src_uri']
    if ';' in url:
        url = url.split(';')[0]  # Remove download parameters
    
    # Create local filename
    filename = os.path.basename(urllib.parse.urlparse(url).path)
    if not filename:
        filename = f"{recipe_info['package']}-{recipe_info['version']}.tar.gz"
    
    local_path = os.path.join(temp_dir, filename)
    
    # Download file
    if not download_file(url, local_path):
        return False, f"Failed to download {url}"
    
    # Compute checksums
    actual_md5, actual_sha256 = compute_checksums(local_path)
    
    # Compare checksums
    md5_ok = not recipe_info['current_md5'] or recipe_info['current_md5'] == actual_md5
    sha256_ok = not recipe_info['current_sha256'] or recipe_info['current_sha256'] == actual_sha256
    
    result = {
        'md5_ok': md5_ok,
        'sha256_ok': sha256_ok,
        'actual_md5': actual_md5,
        'actual_sha256': actual_sha256,
        'expected_md5': recipe_info['current_md5'],
        'expected_sha256': recipe_info['current_sha256']
    }
    
    # Clean up downloaded file
    os.remove(local_path)
    
    if md5_ok and sha256_ok:
        return True, "Checksums match"
    else:
        return False, result

def main():
    """Main function to verify all Python recipe checksums."""
    recipe_dir = "bsoe-recipes/meta-bs/recipes-open"
    temp_dir = "/tmp/checksum_verify"
    
    if not os.path.exists(recipe_dir):
        print(f"Recipe directory not found: {recipe_dir}")
        return 1
    
    # Create temp directory
    os.makedirs(temp_dir, exist_ok=True)
    
    # Find all Python recipes
    python_recipes = []
    for root, dirs, files in os.walk(recipe_dir):
        for file in files:
            if file.startswith('python3-') and file.endswith('.bb'):
                python_recipes.append(os.path.join(root, file))
    
    if not python_recipes:
        print("No Python recipes found")
        return 0
    
    print(f"=== Verifying checksums for {len(python_recipes)} Python recipes ===\n")
    
    total_checked = 0
    total_failed = 0
    fixes_needed = []
    
    for recipe_path in sorted(python_recipes):
        recipe_name = os.path.basename(recipe_path)
        print(f"Checking {recipe_name}...")
        
        try:
            recipe_info = extract_recipe_info(recipe_path)
            
            if not recipe_info['src_uri']:
                print(f"  ⚠️  No SRC_URI found - skipping")
                continue
            
            total_checked += 1
            success, result = verify_recipe_checksums(recipe_info, temp_dir)
            
            if success:
                print(f"  ✅ {result}")
            else:
                print(f"  ❌ Checksum mismatch!")
                total_failed += 1
                
                if isinstance(result, dict):
                    print(f"     Expected MD5: {result['expected_md5']}")
                    print(f"     Actual MD5:   {result['actual_md5']}")
                    print(f"     Expected SHA256: {result['expected_sha256']}")
                    print(f"     Actual SHA256:   {result['actual_sha256']}")
                    
                    fixes_needed.append({
                        'recipe': recipe_path,
                        'md5': result['actual_md5'],
                        'sha256': result['actual_sha256']
                    })
                else:
                    print(f"     Error: {result}")
        
        except Exception as e:
            print(f"  ❌ Error processing recipe: {e}")
            total_failed += 1
        
        print()
    
    # Summary
    print(f"=== Summary ===")
    print(f"Total recipes checked: {total_checked}")
    print(f"Failed checksum verification: {total_failed}")
    
    if fixes_needed:
        print(f"\n=== Fixes needed ===")
        for fix in fixes_needed:
            recipe_name = os.path.basename(fix['recipe'])
            print(f"\n{recipe_name}:")
            print(f'  SRC_URI[md5sum] = "{fix["md5"]}"')
            print(f'  SRC_URI[sha256sum] = "{fix["sha256"]}"')
    
    # Clean up temp directory
    try:
        os.rmdir(temp_dir)
    except:
        pass
    
    return 1 if total_failed > 0 else 0

if __name__ == '__main__':
    sys.exit(main())