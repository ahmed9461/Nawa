#pragma once

#include <android/log.h>
#include "ggml.h"

#define NAWA_TAG "NawaEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, NAWA_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, NAWA_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, NAWA_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, NAWA_TAG, __VA_ARGS__)

inline void nawa_llama_log_callback(enum ggml_log_level level, const char * text, void *) {
    int priority = ANDROID_LOG_DEBUG;
    if (level == GGML_LOG_LEVEL_ERROR) priority = ANDROID_LOG_ERROR;
    else if (level == GGML_LOG_LEVEL_WARN) priority = ANDROID_LOG_WARN;
    else if (level == GGML_LOG_LEVEL_INFO) priority = ANDROID_LOG_INFO;
    __android_log_print(priority, "llama.cpp", "%s", text);
}
