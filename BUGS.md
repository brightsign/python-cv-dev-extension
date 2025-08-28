# BUGS in current pydev environment

[ ] librknnrt.so not found

when trying to load a library, this error is given:

```
W rknn-toolkit-lite2 version: 2.3.2                                                                                                     
E Catch exception when init runtime!                                                                                                    
E Traceback (most recent call last):                                                                                                    
  File "/usr/local/pydev/usr/lib/python3.8/site-packages/rknnlite/api/rknn_lite.py", line 148, in init_runtime                          
    self.rknn_runtime = RKNNRuntime(root_dir=self.root_dir, target=target, device_id=device_id,                                         
  File "rknnlite/api/rknn_runtime.py", line 363, in rknnlite.api.rknn_runtime.RKNNRuntime.__init__                                      
  File "rknnlite/api/rknn_runtime.py", line 607, in rknnlite.api.rknn_runtime.RKNNRuntime._load_library                                 
  File "rknnlite/api/rknn_runtime.py", line 583, in rknnlite.api.rknn_runtime.RKNNRuntime._get_rknn_api_lib_path                        
Exception: Can not find dynamic library on RK3588!                                                                                      
Please download the librknnrt.so from https://github.com/airockchip/rknn-toolkit2/tree/master/rknpu2/runtime/Linux/librknn_api/aarch64 and move it to directory /usr/lib/                                                                                                       
                                                                                                                                        
                                                                                                                                        
E Catch exception when init runtime!                                                                                                    
E Traceback (most recent call last):                                                                                                    
  File "/usr/local/pydev/usr/lib/python3.8/site-packages/rknnlite/api/rknn_lite.py", line 148, in init_runtime                          
    self.rknn_runtime = RKNNRuntime(root_dir=self.root_dir, target=target, device_id=device_id,                                         
  File "rknnlite/api/rknn_runtime.py", line 363, in rknnlite.api.rknn_runtime.RKNNRuntime.__init__                                      
  File "rknnlite/api/rknn_runtime.py", line 607, in rknnlite.api.rknn_runtime.RKNNRuntime._load_library                                 
  File "rknnlite/api/rknn_runtime.py", line 583, in rknnlite.api.rknn_runtime.RKNNRuntime._get_rknn_api_lib_path                        
Exception: Can not find dynamic library on RK3588!                                                                                      
Please download the librknnrt.so from https://github.com/airockchip/rknn-toolkit2/tree/master/rknpu2/runtime/Linux/librknn_api/aarch64 and move it to directory /usr/lib/   
```

the test script is:

```
# cat /storage/sd/test-load.py                                   
from rknnlite.api import RKNNLite                                
                                                                
rknn_lite = RKNNLite()                                           
                                                                
model_path = "/storage/sd/yolox_s.rknn"                          
                                                                
ret = rknn_lite.load_rknn(model_path)                            
ret = rknn_lite.init_runtime(core_mask=RKNNLite.NPU_CORE_AUTO)   
ret = rknn_lite.init_runtime()                                   
#ppyoloe_person_face_fix.rknn                                    
                                                                
```

the enviroment was properly sourced and the model path exists. -- note that python was able to successfully import the package.

The lib IS ALREADY ON the OS:

```# find / -name "librknnrt.so"                       
/storage/sd/brightvision/lib/librknnrt.so              
/storage/sd/__unsafe__/lib/librknnrt.so                
/storage/sd/.Trashes/501/__unsafe__/lib/librknnrt.so   
/usr/local/lib64/librknnrt.so                          
/usr/local/pydev/lib64/librknnrt.so                    
/usr/local/pydev/usr/lib/librknnrt.so                  
/var/volatile/bsext/ext_npu_obj/RK3568/lib/librknnrt.so
/var/volatile/bsext/ext_npu_obj/RK3576/lib/librknnrt.so
/var/volatile/bsext/ext_npu_obj/RK3588/lib/librknnrt.s0
```

Seems like the package is looking for a different directory..