/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>

#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/mman.h>

#include <binder/IMemory.h>
#include <surfaceflinger/SurfaceComposerClient.h>
#include <SkCanvas.h>
#include <SkImageEncoder.h>
#include <SkImageDecoder.h>
#include <SkBitmap.h>
#include <SkStream.h>
#include <SkPaint.h>
#include "jni.h"
#include <utils/Log.h>

using namespace android;

static SkBitmap::Config flinger2skia(PixelFormat f)
{
    switch (f) {
        case PIXEL_FORMAT_A_8:
        case PIXEL_FORMAT_L_8:
            return SkBitmap::kA8_Config;
        case PIXEL_FORMAT_RGB_565:
            return SkBitmap::kRGB_565_Config;
        case PIXEL_FORMAT_RGBA_4444:
            return SkBitmap::kARGB_4444_Config;
        default:
            return SkBitmap::kARGB_8888_Config;
    }
}

static status_t vinfoToPixelFormat(const fb_var_screeninfo& vinfo,
        uint32_t* bytespp, uint32_t* f)
{

    switch (vinfo.bits_per_pixel) {
        case 16:
            *f = PIXEL_FORMAT_RGB_565;
            *bytespp = 2;
            break;
        case 24:
            *f = PIXEL_FORMAT_RGB_888;
            *bytespp = 3;
            break;
        case 32:
            // TODO: do better decoding of vinfo here
            *f = PIXEL_FORMAT_RGBX_8888;
            *bytespp = 4;
            break;
        default:
            return BAD_VALUE;
    }
    return NO_ERROR;
}

static void take_screenshot(JNIEnv *env, jobject obj, jstring file,int width,int height)
{
    bool png = true;
	int fd = -1;
	
	const char *fn = env->GetStringUTFChars(file, NULL);
    fd = open(fn, O_WRONLY | O_CREAT | O_TRUNC, 0664);
    if (fd == -1) {
        fprintf(stderr, "Error opening file: %s (%s)\n", fn, strerror(errno));
        return;
    }

    void const* mapbase = MAP_FAILED;
    ssize_t mapsize = -1;

    void const* base = 0;
    uint32_t w, h, f;
    size_t size = 0;

    ScreenshotClient screenshot;
    if (screenshot.update() == NO_ERROR) {
        base = screenshot.getPixels();
        w = screenshot.getWidth();
        h = screenshot.getHeight();
        f = screenshot.getFormat();
        size = screenshot.getSize();
    } else {
		printf("oh,i'm reading fb0\n");
        const char* fbpath = "/dev/graphics/fb0";
        int fb = open(fbpath, O_RDONLY);
        if (fb >= 0) {
            struct fb_var_screeninfo vinfo;
            if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
                uint32_t bytespp;
                if (vinfoToPixelFormat(vinfo, &bytespp, &f) == NO_ERROR) {
                    size_t offset = (vinfo.xoffset + vinfo.yoffset*vinfo.xres) * bytespp;
                    w = vinfo.xres;
                    h = vinfo.yres;
                    size = w*h*bytespp;
                    mapsize = offset + size;
                    mapbase = mmap(0, mapsize, PROT_READ, MAP_PRIVATE, fb, 0);
                    if (mapbase != MAP_FAILED) {
                        base = (void const *)((char const *)mapbase + offset);
                    }
                }
            }
            close(fb);
        }
    }

    if (base) {
        if (png) {
			printf("yes,your file type is PNG.\n");
            SkBitmap b;
            b.setConfig(flinger2skia(f), w, h);
            b.setPixels((void*)base);
			// scale bitmap
			SkBitmap newbm;
			newbm.setConfig(b.config(), width , height);
			newbm.allocPixels(); 
			newbm.eraseColor(0); 
			SkCanvas canvas(newbm); 
			canvas.scale((float)width/w, (float)height/h); 
			canvas.drawBitmap(b, 0, 0, NULL); 
			
            SkDynamicMemoryWStream stream;
            SkImageEncoder::EncodeStream(&stream, newbm,
                    SkImageEncoder::kPNG_Type, SkImageEncoder::kDefaultQuality);
            write(fd, stream.getStream(), stream.getOffset());
        } else {
            write(fd, &w, 4);
            write(fd, &h, 4);
            write(fd, &f, 4);
            write(fd, base, size);
        }
    }
    close(fd);
    if (mapbase != MAP_FAILED) {
        munmap((void *)mapbase, mapsize);
    }
    return;
}

static const char *classPathName = "com/aisino/server/Screenshot";

static JNINativeMethod methods[] = {
    /* name, signature, funcPtr */
    { "native_take_screenshot", "(Ljava/lang/String;II)V",
            (void*) take_screenshot },       
};
	
/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;
	LOGI("==========register native methods...");
    clazz = (env)->FindClass(className);
    if (clazz == NULL) {
        LOGE("=======Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("=======RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
	// if(AndroidRuntime::registerNativeMethods(env,className,gMethods,numMethods) < 0) {
		// LOGE("=======RegisterNatives failed for '%s'", className);
        // return JNI_FALSE;
	// }

    return JNI_TRUE;
}	

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  LOGI("==========register natives...");
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}
	
/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    LOGI("==========JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("========ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("========ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
