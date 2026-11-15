#pragma once
#include <cstdint>
#include <string>
#include <string_view>

namespace nawa {
// JNI exposes UTF-16, NOT standard UTF-8. Preserve NUL and supplementary characters.
inline std::string utf16_to_utf8(std::u16string_view input) {
    std::string result;
    result.reserve(input.size());
    for (size_t i = 0; i < input.size(); ++i) {
        uint32_t cp = input[i];
        if (cp >= 0xD800 && cp <= 0xDBFF) {
            if (i + 1 < input.size() && input[i + 1] >= 0xDC00 && input[i + 1] <= 0xDFFF) {
                cp = 0x10000 + ((cp - 0xD800) << 10) + (input[++i] - 0xDC00);
            } else cp = 0xFFFD;
        } else if (cp >= 0xDC00 && cp <= 0xDFFF) cp = 0xFFFD;
        if (cp < 0x80) result.push_back(static_cast<char>(cp));
        else if (cp < 0x800) {
            result.push_back(static_cast<char>(0xC0 | (cp >> 6)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            result.push_back(static_cast<char>(0xE0 | (cp >> 12)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else {
            result.push_back(static_cast<char>(0xF0 | (cp >> 18)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        }
    }
    return result;
}
}
