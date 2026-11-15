#include "session.h"
#include <fstream>
#include <iostream>
#include <stdexcept>

static void check(bool condition, const char * message) { if (!condition) throw std::runtime_error(message); }
template<class F> static void must_fail(F function) {
    bool failed = false;
    try { function(); } catch (const std::exception &) { failed = true; }
    check(failed, "Expected a contained native error");
}
static std::string drain(nawa::Session & session) {
    std::string result;
    while (auto piece = session.next()) result += *piece;
    check(!result.empty(), "Model returned no visible output");
    return result;
}
int main(int argc, char ** argv) {
    try {
        check(argc == 2, "Pass the GGUF fixture path");
        llama_backend_init();
        std::atomic<bool> cancel{false};
        std::ofstream("invalid-test.gguf") << "not a model";
        must_fail([&] { nawa::Session s(cancel); s.load("invalid-test.gguf", 512, 2, 64); });
        must_fail([&] { auto templates = common_chat_templates_init(nullptr, "{% if %}"); });
        for (int cycle = 0; cycle < 3; ++cycle) {
            nawa::Session s(cancel);
            s.load(argv[1], 512, 2, 64);
            s.begin("", {}, "Once upon a time", 32, 0.0f);
            auto text = drain(s);
            check(s.stats().output_tokens <= 32, "Output token cap exceeded");
            std::cout << "cycle " << cycle << ": " << s.description() << "\n" << text << "\n";
            std::vector<common_chat_msg> history;
            for (int i = 0; i < 50; ++i) {
                common_chat_msg user; user.role = "user"; user.content = "Tell me a little bedtime story about a happy dog.";
                common_chat_msg assistant; assistant.role = "assistant"; assistant.content = "The little dog ran home to his family and fell asleep.";
                history.push_back(user); history.push_back(assistant);
            }
            s.begin("", history, "Once upon a time", 16, 0.0f);
            drain(s);
            check(s.stats().dropped_messages > 0, "Long history was not trimmed");
            cancel.store(true);
            must_fail([&] { s.begin("", {}, "hello", 16, 0.0f); });
            cancel.store(false);
            std::string huge;
            for (int i = 0; i < 5000; ++i) huge += "word ";
            must_fail([&] { s.begin("", {}, huge, 16, 0.0f); });
            s.begin("", {}, "Once upon a time", 16, 0.0f);
            s.next(); cancel.store(true);
            must_fail([&] { s.next(); });
            cancel.store(false);
            s.begin("", {}, "Once upon a time", 16, 0.0f);
            drain(s);
            check(s.stats().prompt_tokens > 0, "Missing prompt metrics");
        }
        std::remove("invalid-test.gguf");
        llama_backend_free();
        std::cout << "Native smoke checks passed: malformed file/template, 3 load/unload cycles, generation, context trimming, cancel, overflow, recovery.\n";
        return 0;
    } catch (const std::exception & e) {
        std::cerr << "NATIVE TEST FAILED: " << e.what() << "\n";
        return 1;
    }
}
