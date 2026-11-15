#include "../../engine/src/main/cpp/utf.h"
#include <iostream>
#include <stdexcept>

static void check(bool condition) { if (!condition) throw std::runtime_error("UTF conversion regression"); }
int main() {
    check(nawa::utf16_to_utf8(u"Hello") == "Hello");
    check(nawa::utf16_to_utf8(u"مرحبا") == "مرحبا");
    check(nawa::utf16_to_utf8(u"😀🧠") == "😀🧠");
    check(nawa::utf16_to_utf8(std::u16string{u'a', 0, u'b'}) == std::string("a\0b", 3));
    check(nawa::utf16_to_utf8(std::u16string{char16_t(0xD800)}) == "\xEF\xBF\xBD");
    check(nawa::utf16_to_utf8(std::u16string{char16_t(0xDC00)}) == "\xEF\xBF\xBD");
    check(nawa::utf16_to_utf8(std::u16string{char16_t(0xD800), u'x'}) == "\xEF\xBF\xBDx");
    check(nawa::utf16_to_utf8(u"العربية / English 😀") == "العربية / English 😀");
    std::cout << "8 UTF-16/UTF-8 checks passed (ASan/UBSan).\n";
}
