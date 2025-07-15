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
        # Special handling for PyTorch to debug C extension issues
        if module_name == 'torch' and debug:
            print(f"\nDEBUG: Testing PyTorch import...")
            print(f"  Python paths: {sys.path[:3]}...")
            print(f"  Working directory: {os.getcwd()}")
            
            # Check if torch directory exists
            for path in sys.path:
                torch_path = os.path.join(path, 'torch')
                if os.path.exists(torch_path):
                    print(f"  Found torch at: {torch_path}")
                    _c_path = os.path.join(torch_path, '_C')
                    if os.path.exists(_c_path):
                        if os.path.isdir(_c_path):
                            print(f"    WARNING: _C is a directory (should be .so file)")
                        else:
                            print(f"    _C exists as file")
                    else:
                        print(f"    _C not found")
        
        module = importlib.import_module(module_name)
        version = getattr(module, version_attr, 'N/A')
        print(f"✓ {display_name:<25} {version}")
        return True
    except ImportError as e:
        print(f"✗ {display_name:<25} FAILED: {e}")
        if module_name == 'torch' and 'torch._C' in str(e):
            test_import('torch', 'PyTorch', debug=True)
        return False
    except Exception as e:
        print(f"✗ {display_name:<25} ERROR: {e}")
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
    print(f"Python version: {sys.version}")
    print(f"Python executable: {sys.executable}")
    print(f"\n=== Environment Debug ===")
    print(f"PYTHONPATH: {os.environ.get('PYTHONPATH', 'Not set')}")
    print(f"LD_LIBRARY_PATH: {os.environ.get('LD_LIBRARY_PATH', 'Not set')}")
    print(f"Current directory: {os.getcwd()}")
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
    
    # Additional RKNN debug if it failed
    if 'rknn' in [pkg[0] for pkg in packages]:
        print("\n=== RKNN Import Debug ===")
        try:
            # Try to preload the RKNN runtime library
            import ctypes
            for lib_path in ['/usr/local/usr/lib/librknnrt.so', 
                           '/usr/local/lib64/librknnrt.so',
                           'librknnrt.so']:
                try:
                    lib = ctypes.CDLL(lib_path)
                    print(f"Successfully preloaded RKNN runtime library from {lib_path}")
                    break
                except:
                    continue
            
            # Now try importing with preloaded library
            try:
                import rknn
                print("RKNN import successful after preloading library")
            except ImportError as e:
                print(f"Error importing RKNN modules: {e}")
                print("Make sure the RKNN runtime library (librknnrt.so) is available")
        except Exception as e:
            print(f"Debug error: {e}")
    
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