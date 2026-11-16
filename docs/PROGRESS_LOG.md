# Progress Log

## 2026-11-16 — Validation and signed delivery
- Executable source c60ebc5677ad560d081d1be7189f15700591aa65 passed strict CI run 43777791988: Android and native jobs succeeded.
- Confirmed 14 passing JVM tests, 8 native Unicode checks, and real tiny-GGUF host inference/failure/recovery checks. Lint has no errors and 22 warnings.
- Caught and repaired a false-green initial APK-verifier pipeline; binary manifest parsing and a deliberate negative test now enforce the gate.
- Inspected actual ARM64 APK, permissions, SDK levels and 16KB alignment; signed offline with a private persistent owner key and verified the v3 signature and unchanged ZIP content/alignment.
- Recorded exact artifact/source/certificate hashes in docs/VALIDATION_0.2.0.md. No private signing data in Git or public CI artifacts.
- Physical-phone acceptance remains open; do not mark the active plan complete based on host tests.

## 2026-11-16 — Reliability hardening
- Added atomic serialized chat persistence, startup read guards, recoverable corruption handling and streaming checkpoints.
- Added versioned JSON export/validated merge import without overwriting conflicting local data.
- Refactored native resources into a host-testable RAII session; guarded JNI operations; separated native errors from EOS.
- Replaced Modified-UTF-8 misuse with UTF-16 input conversion and byte-array output with incremental UTF-8 decoding.
- Process-owned lazy backend, full-operation serialization, abort/progress cancellation and bounded recent prompt construction.
- Added import progress, cancellation, header/storage checks, Unicode names and split-file rejection.
- Improved saved UI state, drafts, RTL arrows, keyboard handling, scrolling, model attribution, confirmation, rename/regenerate, metrics and backup UI.
- Restricted APK ABIs at the application layer; introduced an independent stable app ID so old debug-signed data need not be deleted.
- Release signing is explicit and fail-closed (unsigned without credentials), not per-run debug signing.
- Added verified Gradle Wrapper, pinned action revisions, fixture checksum and unit/native/packaging CI gates.

## 2026-09-25 — Original MVP
Initialized repository and pinned llama.cpp; implemented native GGUF import/inference, Compose screens, local persistence and CI. First successful debug APK: 2e67486b7c7bea05c87554971b99846171b0d568. Static native linkage and resource shrinking later reduced packaging size. Real-device model acceptance remained open.
