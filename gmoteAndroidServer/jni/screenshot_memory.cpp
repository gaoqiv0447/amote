#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>

#include <linux/fb.h>

#include <zlib.h>
#include <libpng/png.h>

#include "private/android_filesystem_config.h"
#include "jni.h"

#define LOG_TAG "screenshot"
#include <utils/Log.h>

struct mem_encode
{
	char *buffer;
	size_t size;
};

void png_write_data_my(png_structp png_ptr, png_bytep data, png_size_t length) {
	struct mem_encode *p = (struct mem_encode*)png_ptr->io_ptr;
	size_t nsize = p->size + length;
	
	/* allocate or grow buffer */
	if(p->buffer)
		p->buffer = (char *)realloc(p->buffer,nsize);
	else
		p->buffer = (char *)malloc(nsize);
	if(!p->buffer)
		png_error(png_ptr,"Write Error");
	/* copy new bytes to end of buffer */
	memcpy(p->buffer + p->size,data,length);
	p->size += length;
}


static jbyteArray take_screenshot(JNIEnv *env, jobject obj,int width,int height) {
	FILE *fb_in = NULL;
    int fb;
    char imgbuf[0x10000];
    struct fb_var_screeninfo vinfo;
    png_structp png;
    png_infop info;
    unsigned int r,c,rowlen;
    unsigned int bytespp,offset;
	
	struct mem_encode state;
	state.buffer = NULL;
	state.size = 0;
	
	fb_in = fopen("/dev/graphics/fb0", "r");
    if (!fb_in) {
        fprintf(stderr, "error: could not read framebuffer\n");
        exit(1);
    }
		
    fb = fileno(fb_in);
    if(fb < 0) {
        LOGE("failed to open framebuffer\n");
        return NULL;
    }
    fb_in = fdopen(fb, "r");

    if(ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) < 0) {
        LOGE("failed to get framebuffer info\n");
        return NULL;
    }
    fcntl(fb, F_SETFD, FD_CLOEXEC);

    png = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (png == NULL) {
        LOGE("failed png_create_write_struct\n");
        fclose(fb_in);
        return NULL;
    }

    //png_init_io(png, fb_out);
	png_set_write_fn(png,(png_voidp)&state,png_write_data_my,NULL);
	
    info = png_create_info_struct(png);
    if (info == NULL) {
        LOGE("failed png_create_info_struct\n");
        png_destroy_write_struct(&png, NULL);
        fclose(fb_in);
        return NULL;
    }
    if (setjmp(png_jmpbuf(png))) {
        LOGE("failed png setjmp\n");
        png_destroy_write_struct(&png, NULL);
        fclose(fb_in);
        return NULL;
    }

    bytespp = vinfo.bits_per_pixel / 8;
    png_set_IHDR(png, info,
        vinfo.xres, vinfo.yres, vinfo.bits_per_pixel / 4, 
        PNG_COLOR_TYPE_RGB_ALPHA, PNG_INTERLACE_NONE,
        PNG_COMPRESSION_TYPE_BASE, PNG_FILTER_TYPE_BASE);
    png_write_info(png, info);
	LOGE("###########X:%d-----Y:%d",vinfo.xres,vinfo.yres);
    rowlen=vinfo.xres * bytespp;
    if (rowlen > sizeof(imgbuf)) {
        LOGE("crazy rowlen: %d\n", rowlen);
        png_destroy_write_struct(&png, NULL);
        fclose(fb_in);
        return NULL;
    }

    offset = vinfo.xoffset * bytespp + vinfo.xres * vinfo.yoffset * bytespp;
    fseek(fb_in, offset, SEEK_SET);

    for(r=0; r<vinfo.yres; r++) {
        int len = fread(imgbuf, 1, rowlen, fb_in);
        if (len <= 0) break;
        png_write_row(png, (png_bytep)imgbuf);
    }

    png_write_end(png, info);
    fclose(fb_in);
    png_destroy_write_struct(&png, NULL);
	
	//
	jbyteArray bufferbyte = NULL;
    bufferbyte =env->NewByteArray(strlen(state.buffer));
    env->SetByteArrayRegion(bufferbyte, 0, strlen(state.buffer), (jbyte*)state.buffer );

    if(state.buffer) {
        free(state.buffer);
    }

    return bufferbyte;
}

static const char *classPathName = "com/aisino/server/ScreenshotNative";

static JNINativeMethod methods[] = {
    /* name, signature, funcPtr */
    { "nativeScreenshot", "(II)[B",
            (void*) take_screenshot },            
};
	
/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = (env)->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}	

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
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
    
    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
