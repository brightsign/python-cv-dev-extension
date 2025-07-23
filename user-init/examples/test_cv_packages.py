#!/usr/bin/env python3
"""
Test script to verify Python CV/ML package installation on BrightSign player.
"""

import sys
import os
import importlib

def test_import(module_name, display_name=None, version_attr='__version__', debug=False):
    """Test if a module can be imported and optionally display its version."""
    if display_name is None:
        display_name = module_name
    
    try:
        module = importlib.import_module(module_name)
        version = getattr(module, version_attr, 'N/A')
        print(f"✓ {display_name:<25} {version}")
        return True
    except ImportError as e:
        print(f"✗ {display_name:<25} FAILED: {str(e).split(':')[0]}")
        return False
    except Exception as e:
        print(f"✗ {display_name:<25} ERROR: {str(e).split(':')[0]}")
        return False

def test_library_load():
    """Test if native libraries can be loaded."""
    print("\n=== Testing Native Libraries ===")
    
    try:
        import ctypes
        # Test librknnrt
        try:
            lib = ctypes.CDLL('librknnrt.so')
            print("✓ librknnrt.so            Loaded successfully")
        except OSError as e:
            print(f"✗ librknnrt.so            FAILED: {e}")
    except Exception as e:
        print(f"✗ Native library test     ERROR: {e}")

def main():
    print("=== BrightSign Python CV/ML Package Test ===")
    print(f"Python version: {sys.version.split()[0]}")
    print()
    
    print("=== Testing Core CV/ML Packages ===")
    packages = [
        ('cv2', 'OpenCV'),
        ('torch', 'PyTorch'),
        ('onnx', 'ONNX'),
        ('onnxruntime', 'ONNX Runtime', '__version__'),
        ('onnxoptimizer', 'ONNX Optimizer'),
        ('rknn', 'RKNN Toolkit2'),
    ]
    
    core_success = 0
    for pkg in packages:
        if test_import(*pkg):
            core_success += 1
    
    print("\n=== Testing Scientific Computing ===")
    sci_packages = [
        ('numpy', 'NumPy'),
        ('scipy', 'SciPy'),
        ('fast_histogram', 'Fast Histogram'),
        ('PIL', 'Pillow (PIL)', '__version__'),
    ]
    
    sci_success = 0
    for pkg in sci_packages:
        if test_import(*pkg):
            sci_success += 1
    
    print("\n=== Testing Dependencies ===")
    dep_packages = [
        ('flatbuffers', 'FlatBuffers'),
        ('typing_extensions', 'Typing Extensions'),
        ('google.protobuf', 'Protocol Buffers', '__version__'),
        ('psutil', 'psutil'),
        ('tqdm', 'tqdm'),
        ('filelock', 'FileLock'),
        ('fsspec', 'fsspec'),
        ('networkx', 'NetworkX'),
        ('mpmath', 'mpmath'),
        ('jinja2', 'Jinja2'),
        ('markupsafe', 'MarkupSafe'),
        ('ruamel.yaml', 'ruamel.yaml', 'version_info'),
    ]
    
    dep_success = 0
    for pkg in dep_packages:
        if test_import(*pkg):
            dep_success += 1
    
    # Test native libraries
    test_library_load()
    
    # Summary
    print("\n=== Summary ===")
    print(f"Core CV/ML packages: {core_success}/{len(packages)}")
    print(f"Scientific packages: {sci_success}/{len(sci_packages)}")
    print(f"Dependencies: {dep_success}/{len(dep_packages)}")
    
    total_success = core_success + sci_success + dep_success
    total_packages = len(packages) + len(sci_packages) + len(dep_packages)
    
    if total_success == total_packages:
        print(f"\n✓ ALL TESTS PASSED ({total_success}/{total_packages})")
        return 0
    else:
        print(f"\n✗ Some tests failed ({total_success}/{total_packages})")
        return 1

if __name__ == '__main__':
    sys.exit(main())