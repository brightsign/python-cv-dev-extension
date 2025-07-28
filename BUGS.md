# BUGS in current pydev environment

list of bugs obseved through manual testing.  Each bug is prefixed with **BUG**.

## general test setup

1. build and package lastest extension
2. copy to player, expand and install (`bsext_init run`)
3. open a shell and run commands

### Python package import errors

from the shell (busybox) on the player, open the Python interpreter with

```
python3
```

then import packages

**BUG** opencv

```
>>> import cv2
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
  File "/usr/local/usr/lib/python3.8/site-packages/cv2/__init__.py", line 181, in <module>
    bootstrap()
  File "/usr/local/usr/lib/python3.8/site-packages/cv2/__init__.py", line 175, in bootstrap
    if __load_extra_py_code_for_module("cv2", submodule, DEBUG):
  File "/usr/local/usr/lib/python3.8/site-packages/cv2/__init__.py", line 28, in __load_extra_py_code_for_module
    py_module = importlib.import_module(module_name)
  File "/usr/local/usr/lib/python3.8/importlib/__init__.py", line 127, in import_module
    return _bootstrap._gcd_import(name[level:], package, level)
  File "/usr/local/usr/lib/python3.8/site-packages/cv2/typing/__init__.py", line 162, in <module>
    LayerId = cv2.dnn.DictValue
AttributeError: module 'cv2.dnn' has no attribute 'DictValue'
>>>
```