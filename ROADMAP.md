# Roadmap

## Phase 1 — Local text MVP
Implemented: Android/Compose app, local GGUF import, CPU load/unload, streaming, private conversation persistence, Arabic/English/code direction, settings and CI. Physical-device text-generation acceptance remains open.

## Phase 2 — Reliability and diagnostics (0.2.0)
Implemented: atomic storage/recovery guards; streaming checkpoints; backup export/merge; native exception and Unicode boundaries; whole-operation serialization; cancellation; context budgeting; import header/storage validation and progress; TTFT/token-rate metrics; model description; unit/native smoke tests; actual APK checks; verified Gradle Wrapper.
Remaining acceptance: Android JNI/runtime and sustained phone tests, realistic instruction models/templates, thermal/memory pressure, same-certificate upgrade test.

## Phase 3 — Rich chat
Implemented: copyable code/message, rename/search/delete confirmation, regenerate last response, saved drafts and screens, intentional scroll following.
Later: full Markdown renderer, editing arbitrary messages/branching responses, per-conversation model/settings profiles.

## Phase 4 — Multimodal (deferred)
mmproj lifecycle, image inputs, image token budgets, model/projector compatibility. Do not add before text reliability gate is passed.

## Phase 5 — Acceleration (deferred)
Benchmark GPU against CPU on target devices; keep CPU fallback. No current GPU-speed claims.

## Phase 6 — Distribution
Implemented: arm64 packaging, minification, vector icon, versioned stable application ID, explicit no-debug-fallback release signing.
Remaining: provision the SAME private key to a trusted signing environment, physical same-key upgrade verification, owner decision on Nawa license, complete distribution notice review and wider-device QA. Never commit private signing material.
