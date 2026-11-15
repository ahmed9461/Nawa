# Architecture

`app/` contains Compose screens, ViewModel state, atomic chat persistence and local model imports. `engine/` contains the coroutine API, JNI byte bridge, and an Android-independent RAII inference session. `third_party/llama.cpp` is a pinned submodule.

SAF model URI → validated buffered private copy → serialized native load → templated bounded prompt → CPU decode → UTF-8 byte pieces → incremental Kotlin decoder → batched UI updates → periodic atomic chat checkpoints.

## Lifetimes and concurrency
The backend has process lifetime and initializes lazily off the UI thread. ViewModel destruction cancels work but never destroys a singleton still returned by get(). Load/unload/generation share one coroutine Mutex for entire operations; JNI calls also guard native state. Cancellation is a separate atomic flag so it can interrupt decode without waiting for that lock. No runBlocking is used in the app engine.

Writes go through one ViewModel writer queue; all file reads/writes are protected by the store Mutex and AtomicFile. New ViewModels wait for a closing writer before reading. UI edits remain disabled until storage initialization succeeds. A read failure blocks replacement until explicit validated recovery. Streamed content is checkpointed approximately once per second plus before/after generation.

## Prompt policy
Each generation constructs one full prompt using the model's Jinja template (or documented completion fallback), then tokenizes once. Old complete exchanges are removed until the context budget fits. The system instruction and current prompt remain; an oversized current prompt fails clearly. Full persisted transcripts are not truncated. This correctness-first approach trades additional prompt processing for predictable recovery/model switching.

## Ownership and privacy
A loaded model remains process-owned until explicitly unloaded or process death. Backgrounding cancels generation but does not promise persistent background execution. No network/account/telemetry services. User exports are intentional plaintext files; OS backup/transfer are excluded. Model and signing files are never committed.

## Validation boundaries
Core native inference is exercised on a host fixture; JNI, Android lifecycle, device RAM/thermals and model-specific phone compatibility require separate physical-device tests. Automated successes must not be presented as those tests.
