# Nawa Project Memory

## Identity and constraints
- Android-only local GGUF text chat; Arabic-first with correct mixed English/code direction.
- Kotlin + Compose, app/ and engine/ modules, pinned llama.cpp submodule.
- No accounts, API keys, backend, analytics, cloud sync, or Internet permission.
- CPU-first, arm64-v8a only, Android 13+. No model weights in Git/APK.
- Preserve Nexora-inspired Material 3 UI and restrained purple/navy theme without importing provider architecture.

## Inference
- llama.cpp pin: 4b1a27fa0eb875bbca4f6cfe936e3d65adc685c0.
- Defaults: context 2048, threads 4, batch 256, temperature 0.7, output 512.
- Process-owned lazy native backend; operations serialized for full generation, not merely per JNI call.
- Rebuild the full templated recent prompt each turn for deterministic history/system-prompt handling. Old exchanges are omitted from inference as needed, never deleted from storage.
- Native resources use RAII; no C++ exceptions intentionally cross JNI. Input is converted from UTF-16; output is UTF-8 byte arrays decoded incrementally in Kotlin.
- Cancellation uses native progress/abort callbacks and coroutine cancellation. Backgrounding stops generation; no foreground service is promised.
- Base models without embedded templates use completion behavior and are identified in diagnostics.

## Storage
- GGUF imports are buffered/cancellable into private files/models with header/storage checks and .part cleanup.
- Preserve Arabic filenames. Split GGUF collections are not supported in this phase.
- AtomicFile plus serialized writes, startup read guards, periodic checkpoints, and explicit corruption recovery.
- Backup JSON is versioned and plaintext, exported by user intent. No automatic cloud or device-transfer backup.
- Uninstall removes private models/chats; keep original models and exported conversations separately.

## Signing
- Stable application ID: com.ahmed9461.nawa.stable, to coexist with legacy nonpersistently debug-signed installs.
- Never silently use a new debug key for release updates. CI produces unsigned APKs unless a future trusted signing setup is added; private key material stays outside Git/public artifacts.

## Validation
- Native host smoke tests are not Android/physical-phone tests. Record actual CI results, package/signature checks and remaining device acceptance separately.
