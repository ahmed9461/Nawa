# Progress Log

## 2026-11-16 — Reliability hardening
- Added atomic serialized chat persistence, startup read guards, recoverable corruption handling, and streaming checkpoints.
- Added versioned JSON export/validated merge import without overwriting conflicting local data.
- Refactored native resources into a host-testable RAII session; guarded JNI operations; separated native errors from EOS.
- Replaced Modified-UTF-8 misuse with UTF-16 input conversion and byte-array output with incremental UTF-8 decoding.
- Process-owned lazy backend, full-operation serialization, abort/progress cancellation, bounded recent prompt construction.
- Added import progress, cancellation, header/storage checks, Unicode names and split-file rejection.
- Improved saved UI state, drafts, RTL arrows, keyboard handling, scrolling, model attribution, confirmation, rename/regenerate, metrics and backup UI.
- Restricted APK ABIs at the application layer; introduced an independent stable app ID so old debug-signed data need not be deleted.
- Release signing is explicit and fail-closed (unsigned without credentials), not per-run debug signing.
- Added unit, native sanitizer/smoke, lint and actual-APK verification jobs; validation results pending.
- Physical-phone acceptance is still pending and is not inferred from CI.

## 2026-09-25 — Original MVP
Initialized repository and pinned llama.cpp; implemented native GGUF import/inference, Compose screens, local persistence and CI. First successful debug APK: 2e67486b7c7bea05c87554971b99846171b0d568. Static native linkage and resource shrinking later reduced packaging size. Real-device model acceptance remained open.
