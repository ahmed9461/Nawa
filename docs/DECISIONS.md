# Decisions

## D-001 — Native Android instead of Flutter
Status: accepted
Reason: direct control of JNI/inference lifecycle, lower architectural overhead for a local-only app, and easier diagnosis of native model failures. Nexora remains the UI reference.

## D-002 — Own the JNI binding
Status: accepted
Reason: the official llama.cpp Android sample is an excellent base, but its example defaults (including a large context) are not tuned for Nawa's conservative mobile memory policy. Nawa keeps a small binding it can tune.

## D-003 — CPU first
Status: accepted
Reason: the current goal is correctness and stability. GPU acceleration will be measured later rather than enabled by default.

## D-004 — arm64-v8a only
Status: accepted for personal MVP
Reason: avoids shipping x86_64 native libraries and reduces build/release size. Revisit if emulator or broader distribution becomes necessary.

## D-005 — No network permission
Status: accepted
Reason: MVP imports existing GGUF files and runs them locally. A future downloader would be a separate explicit feature.

## D-006 — Nexora is a UX donor, not an architecture donor
Status: accepted
Reuse: Material 3 direction, color language, chat list, rounded composer, RTL-aware messages, code presentation.
Exclude: cloud provider clients, API key storage, provider routing, usage accounting, media/audio stack.

## D-007 — Conservative defaults
Status: accepted
Default context 2048, threads 4, batch 256, temperature 0.7, max generation 512. Settings can expand later with validation.
