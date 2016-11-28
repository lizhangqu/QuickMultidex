#ifndef NDK_LOG_H
#define NDK_LOG_H

#include <android/log.h>

#define NDK_TAG "VMultidex"

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,  NDK_TAG, __VA_ARGS__)
#define LOGVT(TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE,  TAG, __VA_ARGS__)

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  NDK_TAG, __VA_ARGS__)
#define LOGDT(TAG, ...) __android_log_print(ANDROID_LOG_DEBUG,  TAG, __VA_ARGS__)


#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  NDK_TAG, __VA_ARGS__)
#define LOGIT(TAG, ...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)


#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  NDK_TAG, __VA_ARGS__)
#define LOGWT(TAG, ...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)


#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,  NDK_TAG, __VA_ARGS__)
#define LOGET(TAG, ...) __android_log_print(ANDROID_LOG_ERROR,  TAG, __VA_ARGS__)

#endif //NDK_LOG_H