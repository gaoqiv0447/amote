LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	$(call all-java-files-under, ../gmotecommon/src) \
	src/com/aisino/server/IAisinoService.aidl \

LOCAL_PACKAGE_NAME := AisinoRmote
LOCAL_CERTIFICATE := platform
LOCAL_AAPT_FLAGS += -c hdpi -c ldpi -c mdpi

#LOCAL_REQUIRED_MODULES := libnative-udp-socket
#LOCAL_JNI_SHARED_LIBRARIES := libnative-udp-socket

# the followings are used for screenshot
LOCAL_REQUIRED_MODULES := libscreenshotjni \
			libnative-udp-socket
LOCAL_JNI_SHARED_LIBRARIES := libscreenshotjni \
			libnative-udp-socket

# Any libraries that this library depends on
include $(BUILD_PACKAGE)
MY_PATH := $(LOCAL_PATH)

include $(MY_PATH)/jni/Android.mk
