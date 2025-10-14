"""
Patched RKNN executor for BrightSign embedded use.

This version uses RKNNLite API instead of full RKNN toolkit because:
1. Full RKNN toolkit has hardcoded /usr/lib64/ paths (BrightSign uses /usr/lib/)
2. Full toolkit is designed for x86_64 development hosts, not ARM64 embedded targets
3. RKNNLite is optimized for on-device inference

API Adaptation:
- Uses RKNNLite instead of RKNN
- init_runtime() call simplified - no target/device_id parameters
  (RKNNLite always runs locally on the device's NPU)
- All other methods (load_rknn, inference, release) are API-compatible

Usage:
1. Copy this file to player
2. In model_zoo directory: cp /path/to/rknn_executor_patched.py py_utils/rknn_executor.py
3. Run model_zoo examples normally - they will use RKNNLite automatically
"""

import numpy as np
from rknnlite.api import RKNNLite


class RKNN_model_container():
    def __init__(self, model_path, target=None, device_id=None) -> None:
        # Use RKNNLite for on-device inference
        # Note: target and device_id parameters are accepted for compatibility
        # but ignored since RKNNLite always runs locally on the device
        rknn = RKNNLite()

        # Direct Load RKNN Model
        rknn.load_rknn(model_path)

        print('--> Init runtime environment')

        # RKNNLite API: init_runtime() has different signature than full RKNN
        # Full RKNN: init_runtime(target='rk3588', device_id=None)
        # RKNNLite:  init_runtime(core_mask=RKNNLite.NPU_CORE_AUTO)
        #
        # Since we're already running on the RK3588 device, we don't need
        # to specify a target. The target/device_id parameters from model_zoo
        # examples are safely ignored.
        ret = rknn.init_runtime()

        if ret != 0:
            print('Init runtime environment failed')
            exit(ret)
        print('done')

        self.rknn = rknn

    # def __del__(self):
    #     self.release()

    def run(self, inputs):
        if self.rknn is None:
            print("ERROR: rknn has been released")
            return []

        if isinstance(inputs, list) or isinstance(inputs, tuple):
            pass
        else:
            inputs = [inputs]

        # RKNNLite requires explicit batch dimension (full RKNN auto-adds it)
        # Add batch dimension to 3D inputs: (H,W,C) -> (1,H,W,C)
        processed_inputs = []
        for inp in inputs:
            if isinstance(inp, np.ndarray) and len(inp.shape) == 3:
                inp = np.expand_dims(inp, axis=0)
            processed_inputs.append(inp)

        result = self.rknn.inference(inputs=processed_inputs)

        return result

    def release(self):
        self.rknn.release()
        self.rknn = None
