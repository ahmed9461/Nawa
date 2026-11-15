#pragma once
#include "chat.h"
#include "common.h"
#include "sampling.h"
#include "llama.h"
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <memory>
#include <optional>
#include <stdexcept>
#include <string>
#include <vector>

namespace nawa {
struct Stats {
    int prompt_tokens = 0, output_tokens = 0, dropped_messages = 0, finish_reason = 0;
    double ttft_ms = 0, tokens_per_second = 0;
};

// Owned once per loaded model. No JNI or Android dependency: exercised directly by host tests.
class Session {
    llama_model * model_ = nullptr;
    llama_context * context_ = nullptr;
    llama_batch batch_{};
    bool batch_allocated_ = false;
    common_chat_templates_ptr templates_;
    common_sampler * sampler_ = nullptr;
    std::atomic<bool> & cancelled_;
    int context_size_ = 2048, batch_size_ = 256, position_ = 0, limit_ = 0, reserved_streak_ = 0;
    bool visible_ = false, finished_ = true;
    Stats stats_;
    std::chrono::steady_clock::time_point start_, first_;

    void check_cancelled() const {
        if (cancelled_.load(std::memory_order_relaxed)) throw std::runtime_error("Operation cancelled.");
    }
    void decode(const llama_tokens & tokens, bool final_logits) {
        for (int offset = 0; offset < static_cast<int>(tokens.size()); offset += batch_size_) {
            check_cancelled();
            const int count = std::min(batch_size_, static_cast<int>(tokens.size()) - offset);
            common_batch_clear(batch_);
            for (int i = 0; i < count; ++i) {
                common_batch_add(batch_, tokens[offset + i], position_ + i, {0},
                    final_logits && offset + i == static_cast<int>(tokens.size()) - 1);
            }
            const int status = llama_decode(context_, batch_);
            check_cancelled();
            if (status != 0) throw std::runtime_error("Native decode failed (code " + std::to_string(status) + "). Reduce context/batch size.");
            position_ += count;
        }
    }
    llama_tokens tokenize(const std::string & system, const std::vector<common_chat_msg> & history,
                          const std::string & prompt) {
        std::string text;
        if (common_chat_templates_was_explicit(templates_.get())) {
            common_chat_templates_inputs input;
            input.use_jinja = true;
            input.add_generation_prompt = true;
            input.enable_thinking = false;
            if (!system.empty()) { common_chat_msg m; m.role = "system"; m.content = system; input.messages.push_back(m); }
            input.messages.insert(input.messages.end(), history.begin(), history.end());
            common_chat_msg user; user.role = "user"; user.content = prompt;
            input.messages.push_back(user);
            text = common_chat_templates_apply(templates_.get(), input).prompt;
        } else {
            // Base/completion models have no chat template. Do not invent control tokens.
            if (!system.empty()) text = system + "\n";
            for (const auto & m : history) text += m.content + "\n";
            text += prompt;
        }
        // The whole prompt is tokenized once, never per-message with repeated BOS insertion.
        return common_tokenize(context_, text, true, true);
    }
    void finish(int reason) {
        finished_ = true;
        stats_.finish_reason = reason;
        if (!visible_) throw std::runtime_error("The model ended without visible text. Check its chat template and compatibility.");
    }
public:
    explicit Session(std::atomic<bool> & cancelled) : cancelled_(cancelled) {}
    Session(const Session &) = delete;
    Session & operator=(const Session &) = delete;
    ~Session() {
        if (sampler_) common_sampler_free(sampler_);
        templates_.reset();
        if (batch_allocated_) llama_batch_free(batch_);
        if (context_) llama_free(context_);
        if (model_) llama_model_free(model_);
    }
    void load(const std::string & path, int context_size, int threads, int batch_size) {
        check_cancelled();
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        mp.progress_callback = [](float, void * p) { return !static_cast<std::atomic<bool> *>(p)->load(); };
        mp.progress_callback_user_data = &cancelled_;
        model_ = llama_model_load_from_file(path.c_str(), mp);
        check_cancelled();
        if (!model_) throw std::runtime_error("Cannot load GGUF model. Check file integrity, architecture support, and available RAM.");
        auto cp = llama_context_default_params();
        cp.n_ctx = std::clamp(context_size, 512, 8192);
        cp.n_threads = cp.n_threads_batch = std::clamp(threads, 1, 8);
        cp.n_batch = cp.n_ubatch = batch_size_ = std::clamp(batch_size, 64, 512);
        cp.abort_callback = [](void * p) { return static_cast<std::atomic<bool> *>(p)->load(); };
        cp.abort_callback_data = &cancelled_;
        context_ = llama_init_from_model(model_, cp);
        if (!context_) throw std::runtime_error("Cannot allocate context. Use a smaller model/context.");
        context_size_ = static_cast<int>(llama_n_ctx(context_));
        batch_ = llama_batch_init(batch_size_, 0, 1);
        batch_allocated_ = true;
        if (!batch_.token || !batch_.pos || !batch_.n_seq_id || !batch_.seq_id || !batch_.logits) throw std::bad_alloc();
        templates_ = common_chat_templates_init(model_, "");
        check_cancelled();
    }
    std::string description() const {
        char text[512]{};
        llama_model_desc(model_, text, sizeof(text));
        return std::string(text) + " | context " + std::to_string(context_size_) +
            (common_chat_templates_was_explicit(templates_.get()) ? " | Jinja" : " | completion (no embedded template)");
    }
    void begin(const std::string & system, std::vector<common_chat_msg> history,
               const std::string & prompt, int max_tokens, float temperature) {
        if (!context_) throw std::runtime_error("No model is loaded.");
        check_cancelled();
        if (prompt.empty()) throw std::runtime_error("Prompt is empty.");
        if (!std::isfinite(temperature)) throw std::runtime_error("Temperature must be finite.");
        start_ = std::chrono::steady_clock::now();
        stats_ = {}; position_ = 0; reserved_streak_ = 0; visible_ = false; finished_ = false;
        llama_memory_clear(llama_get_memory(context_), false);
        if (sampler_) { common_sampler_free(sampler_); sampler_ = nullptr; }
        common_params_sampling sampling;
        sampling.temp = std::clamp(temperature, 0.0f, 2.0f);
        sampler_ = common_sampler_init(model_, sampling);
        if (!sampler_) throw std::runtime_error("Cannot allocate sampler.");
        max_tokens = std::clamp(max_tokens, 16, 4096);
        // Bound formatting work even when importing very large archives.
        if (history.size() > 128) {
            size_t remove = history.size() - 128;
            if (remove % 2) ++remove;
            stats_.dropped_messages += static_cast<int>(remove);
            history.erase(history.begin(), history.begin() + remove);
        }
        const int budget = context_size_ - std::min(max_tokens, context_size_ / 2) - 1;
        llama_tokens tokens;
        while (true) {
            check_cancelled();
            tokens = tokenize(system, history, prompt);
            if (static_cast<int>(tokens.size()) <= budget || history.empty()) break;
            const auto count = std::min<size_t>(2, history.size());
            history.erase(history.begin(), history.begin() + count);
            stats_.dropped_messages += static_cast<int>(count);
        }
        if (tokens.empty() || tokens.size() >= static_cast<size_t>(context_size_ - 16)) {
            throw std::runtime_error("The current message/system prompt is too long for the context. Shorten it or reload with a larger context.");
        }
        stats_.prompt_tokens = static_cast<int>(tokens.size());
        limit_ = std::min(max_tokens, context_size_ - stats_.prompt_tokens - 1);
        decode(tokens, true);
    }
    std::optional<std::string> next() {
        check_cancelled();
        if (finished_) return std::nullopt;
        if (stats_.output_tokens >= limit_) {
            finish(position_ >= context_size_ - 1 ? 3 : 2);
            return std::nullopt;
        }
        const auto vocab = llama_model_get_vocab(model_);
        const auto token = common_sampler_sample(sampler_, context_, -1);
        common_sampler_accept(sampler_, token, true);
        if (llama_vocab_is_eog(vocab, token)) { finish(1); return std::nullopt; }
        decode(llama_tokens{token}, true);
        ++stats_.output_tokens;
        const auto attr = llama_vocab_get_attr(vocab, token);
        if (attr & (LLAMA_TOKEN_ATTR_CONTROL | LLAMA_TOKEN_ATTR_UNUSED)) {
            if (++reserved_streak_ >= 16) throw std::runtime_error("Repeated reserved/control tokens. The model/template is not compatible.");
            return std::string();
        }
        reserved_streak_ = 0;
        auto piece = common_token_to_piece(context_, token, false);
        if (!piece.empty() && !visible_) {
            visible_ = true;
            first_ = std::chrono::steady_clock::now();
            stats_.ttft_ms = std::chrono::duration<double, std::milli>(first_ - start_).count();
        }
        if (visible_) {
            const double seconds = std::chrono::duration<double>(std::chrono::steady_clock::now() - first_).count();
            stats_.tokens_per_second = seconds > 0 && stats_.output_tokens > 1 ? (stats_.output_tokens - 1) / seconds : 0;
        }
        return piece;
    }
    const Stats & stats() const { return stats_; }
};
}
