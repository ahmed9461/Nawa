# Acceptance checklist

## Automated
- Storage Unicode round trip, legacy-array compatibility, corrupt-file preservation, concurrent writes, failed atomic replacement, legacy .tmp recovery.
- Backup merge conflicts and interrupted-response recovery.
- GGUF header validation and Unicode/path-safe filenames.
- Safe settings and every UTF-8 chunk boundary for Arabic, emoji, NUL, and malformed byte sequences.
- UTF-16 conversion under ASan/UBSan.
- Real GGUF CPU generation on host: three load/unload cycles, context trimming, long-prompt failure, cancellation, retry, malformed file/template errors.
- Release unit tests, Android lint, arm64-only APK, no INTERNET permission, native ELF/ZIP 16KB alignment.

## Physical Android device (record results; do not infer from CI)
1. Install the signed stable package alongside an old package; retain old data.
2. Import valid, corrupt, truncated, Arabic-named and too-large GGUF files. Cancel an import and verify partial cleanup.
3. Load/unload 20 times. Try memory pressure and low storage; check that errors preserve chats.
4. Arabic/English/emoji/code chat; rotate the screen and verify destination/draft, IME, and scroll-follow behavior.
5. Send long multi-turn chats, cancel during prompt processing and generation, switch/delete active conversations, reload a different model.
6. Confirm system prompt/temperature changes affect the next response; context/thread/batch changes require reload.
7. Force-stop during generation/write; verify the last persisted checkpoint and an interrupted-answer notice.
8. Export/import, merge duplicate/conflicting backups, corrupt the store in a test build, and recover without losing the original.
9. Verify a same-key next-version update retains all data.
10. Record model revision/quantization, device, OS, available RAM, TTFT, tokens/s and thermal behavior. No performance guarantees before measurement.
