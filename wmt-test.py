from rknnlite.api import RKNNLite
rknn_lite = RKNNLite()

 ret = rknn_lite.load_rknn(model_path)
 ret = rknn_lite.init_runtime(core_mask=RKNNLite.NPU_CORE_AUTO)
 ret = rknn_lite.init_runtime() ppyoloe_person_face_fix.rknn