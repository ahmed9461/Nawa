#include <jni.h>
#include <atomic>
#include <memory>
#include <mutex>
#include <string>
#include "logging.h"
#include "session.h"
#include "utf.h"

namespace {
std::atomic<bool> cancelled{false};
std::mutex native_mutex;
std::once_flag initialized;
std::unique_ptr<nawa::Session> session;
std::string last_error;

std::string from_java(JNIEnv * env, jstring value) {
    if (!value) throw std::runtime_error("A required string is null.");
    const auto count = env->GetStringLength(value);
    const jchar * chars = env->GetStringChars(value, nullptr);
    if (!chars) throw std::bad_alloc();
    std::string result;
    try { result = nawa::utf16_to_utf8(std::u16string_view(reinterpret_cast<const char16_t *>(chars), count)); }
    catch (...) { env->ReleaseStringChars(value, chars); throw; }
    env->ReleaseStringChars(value, chars);
    return result;
}
jbyteArray to_bytes(JNIEnv * env, const std::string & text) {
    auto result = env->NewByteArray(static_cast<jsize>(text.size()));
    if (result && !text.empty()) env->SetByteArrayRegion(result, 0, static_cast<jsize>(text.size()), reinterpret_cast<const jbyte *>(text.data()));
    return result;
}
template<class F> int guarded(F && function) noexcept {
    try { last_error.clear(); function(); return 0; }
    catch (const std::bad_alloc &) { last_error = "Not enough memory. Use a smaller model/context."; }
    catch (const std::exception & e) { last_error = e.what(); }
    catch (...) { last_error = "Unexpected native engine failure."; }
    LOGE("%s", last_error.c_str());
    return -1;
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeInit(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    return guarded([] { std::call_once(initialized, [] { llama_log_set(nawa_llama_log_callback, nullptr); llama_backend_init(); }); });
}
extern "C" JNIEXPORT void JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeCancel(JNIEnv *, jobject) { cancelled.store(true); }
extern "C" JNIEXPORT void JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeClearCancel(JNIEnv *, jobject) { cancelled.store(false); }

extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeLoad(JNIEnv * env, jobject, jstring path, jint context, jint threads, jint batch) {
    std::lock_guard<std::mutex> lock(native_mutex);
    session.reset();
    return guarded([&] {
        auto candidate = std::make_unique<nawa::Session>(cancelled);
        candidate->load(from_java(env, path), context, threads, batch);
        session = std::move(candidate);
    });
}
extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeBegin(JNIEnv * env, jobject, jstring system, jobjectArray roles, jobjectArray contents, jstring prompt, jint max_tokens, jfloat temperature) {
    std::lock_guard<std::mutex> lock(native_mutex);
    return guarded([&] {
        if (!session) throw std::runtime_error("No model is loaded.");
        const auto count = env->GetArrayLength(roles);
        if (count != env->GetArrayLength(contents)) throw std::runtime_error("Invalid history arrays.");
        std::vector<common_chat_msg> history;
        history.reserve(count);
        for (jsize i = 0; i < count; ++i) {
            auto role = static_cast<jstring>(env->GetObjectArrayElement(roles, i));
            auto content = static_cast<jstring>(env->GetObjectArrayElement(contents, i));
            try {
                common_chat_msg message; message.role = from_java(env, role); message.content = from_java(env, content);
                if (message.role != "user" && message.role != "assistant") throw std::runtime_error("Invalid history role.");
                history.push_back(std::move(message));
            } catch (...) {
                env->DeleteLocalRef(role); env->DeleteLocalRef(content); throw;
            }
            env->DeleteLocalRef(role); env->DeleteLocalRef(content);
        }
        session->begin(from_java(env, system), std::move(history), from_java(env, prompt), max_tokens, temperature);
    });
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeNext(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    jbyteArray result = nullptr;
    guarded([&] {
        if (!session) throw std::runtime_error("No model is loaded.");
        auto value = session->next();
        if (value) result = to_bytes(env, *value);
    });
    return result;
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeError(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    return to_bytes(env, last_error);
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeDescription(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    jbyteArray result = nullptr;
    guarded([&] { if (session) result = to_bytes(env, session->description()); });
    return result;
}
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeStats(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    auto result = env->NewDoubleArray(6);
    if (!result) return nullptr;
    nawa::Stats stats;
    if (session) stats = session->stats();
    const jdouble values[] = {static_cast<double>(stats.prompt_tokens), static_cast<double>(stats.output_tokens),
        static_cast<double>(stats.dropped_messages), stats.ttft_ms, stats.tokens_per_second, static_cast<double>(stats.finish_reason)};
    env->SetDoubleArrayRegion(result, 0, 6, values);
    return result;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_ahmed9461_nawa_engine_NawaInferenceEngine_nativeUnload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(native_mutex);
    return guarded([] { session.reset(); });
}
