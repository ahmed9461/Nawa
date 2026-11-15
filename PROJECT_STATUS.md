# Project Status

Last updated: 2026-11-16

## Current state
0.2.0 reliability candidate is implemented. The first validation run compiled the release APK, passed 14 JVM tests and lint with no errors, and passed native host checks with actual GGUF generation. A packaging-verifier formatting bug was found by reading the logs despite a green pipeline; verification now reads the binary manifest and shell pipelines fail closed. The final strict run is the delivery gate; its exact results are linked from the release/PR record.

## Active plan
plans/0001-bootstrap-local-chat.md

## Implemented
Atomic serialized storage and recovery guard; versioned JSON backup/merge; periodic streaming checkpoints; native RAII/error containment; incremental UTF-8 byte streaming and UTF-16 input; process-owned serialized engine; abort callbacks; bounded recent-history prompts; model import progress/cancellation/header/storage checks; diagnostics and metrics; state restoration/RTL/IME/delete confirmation/rename/regenerate; arm64-only stable application identity; explicit private signing; verified Gradle Wrapper; unit/native/packaging CI checks.

## Validation evidence and limits
Initial run: https://github.com/ahmed9461/Nawa/actions/runs/43776587905
- 11 storage/backup/import JVM tests + 3 engine/Unicode JVM tests passed; no failures or skips.
- 8 native Unicode assertions under ASan/UBSan passed.
- A checksum-authenticated tiny GGUF completion model generated visible text; three load/unload cycles, context trimming, cancellation/retry, oversized prompts, and malformed file/template checks passed on Linux host.
- Android lint had no errors; remaining warnings include deliberately restricted ABIs, dependency version notices and API/style suggestions. No claim of zero warnings.
- Initial binary APK manifest independently inspected: stable package, minSdk 33, targetSdk 36, no Internet permission, non-debuggable, backups disabled, only arm64 libraries. The final verifier additionally enforces ZIP/ELF 16KB alignment and tests a deliberate failure.

## Signing
The stable package coexists with legacy debug-signed installs. CI outputs unsigned releases; no secret has been placed in Git. Owner-only private signing material must be retained for all future stable updates. Automated CI signing secrets are not configured by these source changes.

## Physical-device gate
No physical Android-device test or on-device JNI/inference run has been performed in this pass. Real-model phone compatibility, sustained memory/thermal behavior and same-key in-place upgrade remain explicit acceptance steps. Host smoke checks are not substitutes.

## Intentionally deferred
Vision/mmproj, voice, GPU acceleration, network model downloads, full Markdown renderer and broad-device distribution. The repository owner's top-level source license still requires an explicit choice; third-party notices do not license Nawa itself.
