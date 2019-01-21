LOCAL_PATH := $(call my-dir)
MAIN_LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_MODULE := v++_64
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++_64.so\"
else
LOCAL_MODULE := v++
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++.so\"
endif

LOCAL_CFLAGS += -Wno-error=format-security -fpermissive -O2
LOCAL_CFLAGS += -DLOG_TAG=\"VA++\"
LOCAL_CFLAGS += -fno-rtti -fno-exceptions
LOCAL_CPPFLAGS += -std=c++11

LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Foundation
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Jni
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Syscall

LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Jni/Helper.cpp \
				   Foundation/syscall/BinarySyscallFinder.cpp \
				   Foundation/fake_dlfcn.cpp \
				   Foundation/canonicalize_md.c \
				   Foundation/MapsRedirector.cpp \
				   Foundation/IORelocator.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/Symbol.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   Substrate/And64InlineHook.cpp \
                   syscall/sysnum.cpp \
                   syscall/tracer/tracer.cpp \
                   syscall/tracer/reg.cpp \
                   syscall/tracer/event.cpp \
                   syscall/tracer/syscall.cpp \
                   syscall/tracer/mem.cpp \
                   syscall/tracer/enter.cpp \
                   syscall/tracer/exit.cpp \
                   syscall/tracer/path.cpp \

LOCAL_LDLIBS := -llog -latomic

include $(BUILD_SHARED_LIBRARY)