# Project Status

Last updated: 2026-11-16

## Current state
Nawa 0.2.0 has been built and signed for personal installation. Automated validation and actual-APK inspection passed on code commit `c60ebc5677ad560d081d1be7189f15700591aa65`. The final documentation commit records those results without changing executable code. Physical Android-device qualification remains open.

## Active plan
plans/0001-bootstrap-local-chat.md — keep active until a real target phone passes the acceptance checklist.

## Implemented
Atomic serialized storage and corruption recovery guard; versioned JSON export/merge; periodic streaming checkpoints; native RAII/error containment; incremental UTF-8 byte streaming and UTF-16 input; process-owned serialized engine; cancellation callbacks; bounded recent-history prompts; model import progress/cancellation/header/storage checks; diagnostics and metrics; saved UI state/RTL/IME/delete confirmation/rename/regenerate; arm64-only stable application identity; explicit private signing; verified Gradle Wrapper; unit/native/packaging CI checks.

## Verified results
Final strict run: https://github.com/ahmed9461/Nawa/actions/runs/43777791988
- 14 JVM tests passed (11 storage/backup/import and 3 engine/Unicode), zero failures/errors/skips.
- 8 native UTF-16/UTF-8 checks under ASan/UBSan passed.
- Real small-GGUF CPU generation, three load/unload cycles, context trimming, cancellation/retry, oversized prompts and malformed file/template checks passed on Linux host.
- Android release assembly and lint passed; lint reports 22 warnings and zero errors, not a warning-free build.
- Binary manifest, ARM64-only native libraries, no INTERNET permission, non-debuggable release, disabled OS backup, native ELF/ZIP 16KB alignment and an intentional negative verification test passed.
- Delivered APK was signed offline with the owner-only stable key. Official apksigner verified its v3 signature; every ZIP entry's content was compared to the verified unsigned build, and alignment was independently rechecked after signing.
- See docs/VALIDATION_0.2.0.md for immutable source/run/hash evidence.

## Signing and migration
Stable ID `com.ahmed9461.nawa.stable` coexists with legacy debug-signed installs. Do not require uninstalling an older package. Its private data are not automatically migrated. CI deliberately outputs unsigned releases; no signing secret is in Git or public artifacts. The private signing backup must be retained by the owner for future updates; encrypted CI signing secrets are not configured automatically.

## Not yet verified / deferred
No Android runtime/JNI-on-device or physical-phone test was performed in this pass. Instruction-model compatibility (including the originally planned Gemma models), sustained memory/thermals and same-key in-place upgrade require device acceptance. Host tests do not certify those behaviors.

GPU, vision/mmproj, voice, model downloads, full Markdown and wider-device distribution remain deferred. The owner still needs to choose a top-level Nawa source license; bundled third-party notices do not license Nawa itself.
