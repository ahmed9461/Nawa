# Architecture

## Stack
- Kotlin 2.3
- Jetpack Compose + Material 3
- Single Android app module
- Nawa engine Android library module
- llama.cpp pinned as a Git submodule
- Kotlin coroutines/Flow for streaming

## Modules
```
app/
  UI, view model, chat/model stores
engine/
  Kotlin inference API + JNI bridge + Nawa-owned C++
third_party/llama.cpp/
  pinned upstream source
```

## Data flow
```
SAF Uri -> ModelStore -> app-private .gguf
                         |
                         v
                  NawaInferenceEngine
                         |
                         v
                 token Flow<String>
                         |
                         v
                  NawaViewModel
                         |
                         v
                    Compose UI
```

## Why native Android
Nexora is Flutter and is a strong UI reference, but Nawa's core problem is local native inference. Native Android removes an extra runtime/bridge layer around the hot inference path and lets the project own JNI, model lifecycle, memory settings, and diagnostics.

## Memory policy
- Default context: 2048.
- Default threads: 4.
- Default batch: 256.
- Model load and generation run off the main thread.
- Only one model is loaded at a time.
- Only one generation is active at a time.
- UI must remain responsive during load and generation.

## Model import
The native engine expects a readable filesystem path. Android SAF returns a content Uri, so Nawa copies the selected model once into app-private storage using buffered streaming. This also avoids depending on fragile external-storage paths.

## Chat templating
The engine uses the model's embedded chat template through current llama.cpp common chat helpers and requests Jinja formatting. This is a deliberate response to reserved-token failures seen in other mobile clients.

## Privacy
The MVP requests no Internet permission. Prompts, responses, models, and chat history stay on-device.
