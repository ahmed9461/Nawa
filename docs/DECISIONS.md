# Decisions

## D-001 — Native Android instead of Flutter
Accepted. Kotlin/Compose avoids a second runtime around native inference. Nexora remains a visual reference only.

## D-002 — Nawa-owned JNI binding
Accepted. Nawa controls resource lifetime, conservative settings, errors, cancellation, UTF conversion and metrics.

## D-003 — CPU first
Accepted. GPU is deferred until measurable device stability/performance justify it.

## D-004 — arm64-v8a only
Accepted for personal use; enforced in both app and engine. Android 13+ remains the minimum.

## D-005 — No network permission
Accepted. Import existing GGUF files; no telemetry, accounts, backend, or hidden network fallback.

## D-006 — Nexora donates UX, not cloud architecture
Accepted. Material 3, purple/navy surfaces, rounded composer, RTL text and LTR code; no provider/API-key code.

## D-007 — Conservative defaults
Accepted. Context 2048, CPU threads 4, batch 256, temperature 0.7, output 512, with validated bounds.

## D-008 — Single CPU backend
Accepted. Single portable arm64 CPU path and KleidiAI; real-device speed remains to be measured.

## D-009 — Temporary debug-signed release APKs
SUPERSEDED by D-011. Historical 0.1 builds used changing runner-local debug keys, preventing dependable updates. Do not restore this fallback.

## D-010 — Static native linkage
Accepted. llama.cpp/common/ggml are linked into one stripped JNI library; dependent OpenMP remains packaged. No claim that only one .so exists.

## D-011 — Stable identity and private signing
Accepted for 0.2.0. New durable ID com.ahmed9461.nawa.stable coexists with old installs; old private data are not automatically accessible. Release builds are unsigned unless explicit private signing credentials are supplied; debug builds have a separate suffix. Preserve the stable signing key privately for later updates. No keys/passwords in Git/public artifacts. CI signing secrets require separate provisioning.

## D-012 — Process-owned backend and deterministic prompts
Accepted. Lazy process lifetime, explicit model unload, no destroyed-singleton reuse or runBlocking. Serialize full operations, not only individual native calls. Rebuild the whole templated recent prompt each turn; trim oldest complete exchanges within inference only, retain full disk history. Extra prompt work is accepted in favor of correctness/recovery.

## D-013 — Atomic storage and deliberate backup
Accepted. AtomicFile plus one write queue; read failure blocks overwrite. Periodic streamed checkpoints; versioned plaintext JSON export/validated merge via user intent. OS cloud/device transfer disabled. Backup does not include model weights/settings.

## D-014 — Foreground generation
Accepted for this version. Backgrounding stops generation; no background service/wake lock is promised. Partial answers remain recoverable from the last checkpoint.

## D-015 — Evidence-based validation
Accepted. Build/JVM/native host tests and binary APK inspection are distinct from Android runtime, physical-device tests and compatibility across instruction templates. Green workflow status alone is not evidence when logs show a failed gate; use strict shell pipelines and a negative verifier test.
