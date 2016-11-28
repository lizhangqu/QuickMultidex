#include <jni.h>
#include "include/log.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <jni.h>
#include <assert.h>
#include <dlfcn.h>

//大小端定义
#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
#define HAVE_LITTLE_ENDIAN

int getEndian() {
    return 1;
}

#else
#define HAVE_BIG_ENDIAN
int getEndian(){
    return 0;
}
#endif

#if defined(HAVE_ENDIAN_H)
# include <endian.h>

#else /*not HAVE_ENDIAN_H*/
# define __BIG_ENDIAN 4321
# define __LITTLE_ENDIAN 1234
# if defined(HAVE_LITTLE_ENDIAN)
#  define __BYTE_ORDER __LITTLE_ENDIAN
# else
#  define __BYTE_ORDER __BIG_ENDIAN
# endif
#endif /*not HAVE_ENDIAN_H*/


//数据结构构造定义
typedef uint8_t u1;
typedef uint16_t u2;
typedef uint32_t u4;
typedef uint64_t u8;
typedef int8_t s1;
typedef int16_t s2;
typedef int32_t s4;
typedef int64_t s8;

union JValue {
#if defined(HAVE_LITTLE_ENDIAN)
    u1 z;
    s1 b;
    u2 c;
    s2 s;
    s4 i;
    s8 j;
    float f;
    double d;
    void *l;
#endif
#if defined(HAVE_BIG_ENDIAN)
    struct {
        u1 _z[3];
        u1 z;
    };
    struct {
        s1 _b[3];
        s1 b;
    };
    struct {
        u2 _c;
        u2 c;
    };
    struct {
        s2 _s;
        s2 s;
    };
    s4 i;
    s8 j;
    float f;
    double d;
    void *l;
#endif
};

typedef struct {
    void *clazz;
    u4 lock;
    u4 length;
    u1 *contents;
} ArrayObject;


//定义
JNINativeMethod *dvm_dalvik_system_DexFile;

void (*openDexFile)(const u4 *args, union JValue *pResult);

#if defined(__i386__)
#define array_object_contents_offset 12
#else
#define array_object_contents_offset 16
#endif


JNIEXPORT jint JNICALL Multidex_openDexFile(
        JNIEnv *env, jclass jv, jbyteArray dexArray, jlong dexLen) {
    LOGD("array_object_contents_offset: %d", array_object_contents_offset);
    u1 *dexData = (u1 *) (*env)->GetByteArrayElements(env, dexArray, NULL);
    char *arr;
    arr = (char *) malloc((size_t) (array_object_contents_offset + dexLen));
    ArrayObject *ao = (ArrayObject *) arr;
    ao->length = (u4) dexLen;
    memcpy(arr + array_object_contents_offset, dexData, dexLen);
    u4 args[] = {(u4) ao};
    union JValue pResult;
    jint result = -1;
    if (openDexFile != NULL) {
        openDexFile(args, &pResult);
        result = (jint) pResult.l;
    }
    return result;
}


//动态注册jni函数开始
static JNINativeMethod methods[] = {
        {"openDexFile", "([BJ)I", (void *) Multidex_openDexFile}
};
static const char *classPathName = "com/android/quickmultidex/Multidex";

static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;

    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, classPathName,
                               methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
//动态注册jni函数结束


//查找dalvik函数开始
int lookup(JNINativeMethod *table, const char *name, const char *sig,
           void (**fnPtrout)(u4 const *, union JValue *)) {
    int i = 0;
    while (table[i].name != NULL) {
        LOGD("lookup %d %s", i, table[i].name);
        if ((strcmp(name, table[i].name) == 0)
            && (strcmp(sig, table[i].signature) == 0)) {
            *fnPtrout = table[i].fnPtr;
            return 1;
        }
        i++;
    }
    return 0;
}
//查找dalvik函数结束

//JNI_OnLoad开始
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {

    int endian = getEndian();
    if (endian == 1) {
        LOGD("endian: %s", "HAVE_LITTLE_ENDIAN");
    } else {
        LOGD("endian: %s", "HAVE_BIG_ENDIAN");
    }

    JNIEnv *env = NULL;
    jint result = -1;

    void *ldvm = (void *) dlopen("libdvm.so", RTLD_LAZY);
    dvm_dalvik_system_DexFile = (JNINativeMethod *) dlsym(ldvm, "dvm_dalvik_system_DexFile");
    if (0 == lookup(dvm_dalvik_system_DexFile, "openDexFile", "([B)I",
                    &openDexFile)) {
        openDexFile = NULL;
        return result;
    }
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return result;
    }
    if (registerNatives(env) != JNI_TRUE) {
        return result;
    }
    return JNI_VERSION_1_6;
}
//JNI_OnLoad结束