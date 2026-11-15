# Plan 0001 — Reliable Local Chat

Status: active

## Goal
Ship a buildable, data-safe, CPU-first Android text client. A successful APK build alone does not close the on-device acceptance gate.

## Hardening checkpoint — 2026-11-16
1. Serialize chat writes, use atomic replacement, preserve corrupt data, gate edits until startup completes, and checkpoint streamed answers.
2. Add explicit JSON backup/export and validated merge import without deleting existing conversations.
3. Contain native exceptions, fix UTF-8/UTF-16 at JNI boundaries, report native failures, and support cancellation.
4. Give the native backend process lifetime and serialize model operations; avoid runBlocking and destroyed-singleton reuse.
5. Rebuild templated prompts with a bounded recent-history window; retain the full history on disk.
6. Improve imports (GGUF header, storage checks, progress, cancel, Unicode filenames), UI state restoration, confirmation, and diagnostics.
7. Restrict the APK to arm64; use an independent stable package to preserve old debug-signed installs. Release builds must never silently fall back to ephemeral debug signing.
8. Add storage/config/native tests, lint, native smoke tests, Gradle Wrapper, signing/backup/build documentation, and a verified APK artifact.

## Acceptance
- Automated tests, lint, and release assembly must pass on the exact delivery commit.
- Inspect the produced APK's permissions, ABI, version, and signature.
- Test native text generation using a small GGUF fixture on CI; distinguish this from Android/device testing.
- Physical-phone tests remain explicit: sustained load/unload, Arabic/emoji, memory pressure, rotation, stop/retry, backup recovery, and upgrade using the same signing certificate.
- No model weights, signing keys, passwords, APKs, or generated build outputs may enter Git.
- GPU, vision, audio, and network/model download features remain outside this text-reliability phase.
