#include <jni.h>
#include <algorithm>
#include <sstream>
#include <string>
#include <vector>

#include "chat.h"
#include "common.h"
#include "llama.h"
#include "logging.h"
#include "sampling.h"

static llama_model * g_model = nullptr;
static llama_context * g_context = nullptr;
static llama_batch g_batch;
static common_chat_templates_ptr g_chat_templates;
static common_sampler * g_sampler = nullptr;

static int g_context_size = 2048;
static int g_threads = 4;
static int g_batch_size = 256;

static std::vector<common_chat_msg> g_messages;
static llama_pos g_position = 0;
static llama_pos g_stop_position = 0;
static std::string g_utf8_cache;
static std::ostringstream g_assistant;

static bool valid_utf8(const std::string & value) {
    const auto * bytes = reinterpret_cast<const unsigned char *>(value.c_str());
    while (*bytes != 0) {
        int count = 0;
        if ((*bytes & 0x80) == 0x00) count = 1;
        else if ((*bytes & 0xE0) == 0xC0) count = 2;
        else if ((*bytes & 0xF0) == 0xE0) count = 3;
        else if ((*bytes & 0xF8) == 0xF0) count = 4;
        else return false;
        ++bytes;
        for (int i = 1; i < count; ++i, ++bytes) {
            if ((*bytes & 0xC0) != 0x80) return false;
        }
    }
    return true;
}

static void clear_short_state() {
    g_stop_position = 0;
    g_utf8_cache.clear();
    g_assistant.str("");
    g_assistant.clear();
}

static void clear_conversation(bool clear_memory) {
    g_messages.clear();
    g_position = 0;
    clear_short_state();
    if (clear_memory && g_context != nullptr) {
        llama_memory_clear(llama_get_memory(g_context), false);
    }
}

static std::string format_message(
    const std::string & role,
    const std::string & content,
    bool add_generation_prompt
) {
    common_chat_msg message;
    message.role = role;
    message.content = content;
    const std::string formatted = common_chat_format_single(
        g_chat_templates.get(),
        g_messages,
        message,
        add_generation_prompt,
        true
    );
    g_messages.push_back(message);
    return formatted;
}

static int decode_text(const std::string & text, bool logits_last) {
    const bool has_template = common_chat_templates_was_explicit(g_chat_templates.get());
    const llama_tokens tokens = common_tokenize(g_context, text, has_template, has_template);
    if (tokens.empty()) return 0;
    if (g_position + static_cast<llama_pos>(tokens.size()) >= g_context_size - 4) return 2;

    for (int offset = 0; offset < static_cast<int>(tokens.size()); offset += g_batch_size) {
        const int count = std::min(g_batch_size, static_cast<int>(tokens.size()) - offset);
        common_batch_clear(g_batch);
        for (int i = 0; i < count; ++i) {
            const bool want_logit = logits_last && (offset + i == static_cast<int>(tokens.size()) - 1);
            common_batch_add(g_batch, tokens[offset + i], g_position + offset + i, {0}, want_logit);
        }
        if (llama_decode(g_context, g_batch) != 0) return 3;
    }
    g_position += static_cast<llama_pos>(tokens.size());
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeInit(
    JNIEnv *, jobject, jstring
) {
    llama_log_set(nawa_llama_log_callback, nullptr);
    // CPU is statically linked into libnawa-engine.so.
    llama_backend_init();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeLoad(
    JNIEnv * env, jobject, jstring model_path
) {
    const char * path = env->GetStringUTFChars(model_path, nullptr);
    llama_model_params params = llama_model_default_params();
    g_model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(model_path, path);
    return g_model == nullptr ? 1 : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativePrepare(
    JNIEnv *, jobject, jint context_size, jint threads, jint batch_size, jfloat temperature
) {
    if (g_model == nullptr) return 1;
    g_context_size = std::max(512, std::min(8192, (int) context_size));
    g_threads = std::max(1, std::min(8, (int) threads));
    g_batch_size = std::max(64, std::min(512, (int) batch_size));
    g_batch_size = std::min(g_batch_size, g_context_size);

    llama_context_params params = llama_context_default_params();
    params.n_ctx = g_context_size;
    params.n_batch = g_batch_size;
    params.n_ubatch = g_batch_size;
    params.n_threads = g_threads;
    params.n_threads_batch = g_threads;

    g_context = llama_init_from_model(g_model, params);
    if (g_context == nullptr) return 2;

    g_batch = llama_batch_init(g_batch_size, 0, 1);
    g_chat_templates = common_chat_templates_init(g_model, "");

    common_params_sampling sampling;
    sampling.temp = std::max(0.0f, std::min(2.0f, (float) temperature));
    g_sampler = common_sampler_init(g_model, sampling);
    if (g_sampler == nullptr) return 3;

    clear_conversation(true);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeResetConversation(JNIEnv *, jobject) {
    if (g_context == nullptr) return 1;
    clear_conversation(true);
    if (g_sampler != nullptr) common_sampler_reset(g_sampler);
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeAppendHistory(
    JNIEnv * env, jobject, jstring jrole, jstring jcontent
) {
    if (g_context == nullptr) return 1;
    const char * role_chars = env->GetStringUTFChars(jrole, nullptr);
    const char * content_chars = env->GetStringUTFChars(jcontent, nullptr);
    const std::string role(role_chars);
    const std::string content(content_chars);
    env->ReleaseStringUTFChars(jrole, role_chars);
    env->ReleaseStringUTFChars(jcontent, content_chars);

    try {
        const bool has_template = common_chat_templates_was_explicit(g_chat_templates.get());
        const std::string formatted = has_template ? format_message(role, content, false) : content;
        return decode_text(formatted, false);
    } catch (const std::exception & e) {
        LOGE("History template error: %s", e.what());
        return 4;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeProcessUserPrompt(
    JNIEnv * env, jobject, jstring jprompt, jint max_tokens
) {
    if (g_context == nullptr || g_sampler == nullptr) return 1;
    clear_short_state();
    const char * chars = env->GetStringUTFChars(jprompt, nullptr);
    const std::string prompt(chars);
    env->ReleaseStringUTFChars(jprompt, chars);

    try {
        const bool has_template = common_chat_templates_was_explicit(g_chat_templates.get());
        const std::string formatted = has_template ? format_message("user", prompt, true) : prompt;
        const int result = decode_text(formatted, true);
        if (result != 0) return result;
        g_stop_position = std::min<llama_pos>(
            g_context_size - 1,
            g_position + std::max(1, (int) max_tokens)
        );
        return 0;
    } catch (const std::exception & e) {
        LOGE("Prompt template error: %s", e.what());
        return 4;
    }
}

static void finalize_assistant_message() {
    if (g_assistant.str().empty()) return;
    common_chat_msg assistant;
    assistant.role = "assistant";
    assistant.content = g_assistant.str();
    g_messages.push_back(assistant);
    g_assistant.str("");
    g_assistant.clear();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeGenerateNextToken(JNIEnv * env, jobject) {
    if (g_context == nullptr || g_sampler == nullptr) return nullptr;
    if (g_position >= g_stop_position || g_position >= g_context_size - 1) {
        finalize_assistant_message();
        return nullptr;
    }

    const llama_token token = common_sampler_sample(g_sampler, g_context, -1);
    common_sampler_accept(g_sampler, token, true);

    common_batch_clear(g_batch);
    common_batch_add(g_batch, token, g_position, {0}, true);
    if (llama_decode(g_context, g_batch) != 0) return nullptr;
    ++g_position;

    if (llama_vocab_is_eog(llama_model_get_vocab(g_model), token)) {
        finalize_assistant_message();
        return nullptr;
    }

    g_utf8_cache += common_token_to_piece(g_context, token);
    if (!valid_utf8(g_utf8_cache)) return env->NewStringUTF("");

    const std::string output = g_utf8_cache;
    g_assistant << output;
    g_utf8_cache.clear();
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeUnload(JNIEnv *, jobject) {
    clear_conversation(g_context != nullptr);
    if (g_sampler != nullptr) {
        common_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    g_chat_templates.reset();
    if (g_context != nullptr) {
        llama_batch_free(g_batch);
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeShutdown(JNIEnv *, jobject) {
    llama_backend_free();
}
