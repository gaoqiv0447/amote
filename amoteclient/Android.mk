LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_AAPT_FLAGS += -c hdpi -c ldpi -c mdpi

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	$(call all-java-files-under, ../gmotecommon/src) \

LOCAL_PACKAGE_NAME := AmoteClient
# Any libraries that this library depends on
include $(BUILD_PACKAGE)
