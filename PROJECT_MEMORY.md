# Nawa Project Memory

## Identity
- Name: Nawa
- Repository: ahmed9461/Nawa
- Platform: Android only for now.
- Purpose: run local GGUF language models directly on the phone with a polished chat experience and no server/API dependency.
- Primary language UX: Arabic first, with correct mixed Arabic/English directionality.

## Product constraints
- Local-only inference. No account, API key, backend, analytics, or cloud sync.
- Lightweight UI and dependency set.
- Smooth streaming chat.
- User selects/imports GGUF files from Android storage.
- Model files are not stored in Git.
- CPU-first implementation; GPU work is deferred until CPU inference is stable.
- Release target is arm64-v8a to avoid shipping unnecessary ABIs for the personal-use build.
- Package a single arm64 CPU backend rather than `GGML_CPU_ALL_VARIANTS`; keep KleidiAI enabled and measure the result on-device.

## UI source
Nexora is the visual/interaction reference. Reuse the successful ideas, not its cloud-provider architecture:
- Material 3 visual language.
- Seed accent around #6F63FF.
- Dark background #0B1020 and dark surface #121A2D.
- Rounded cards and a rounded bottom composer.
- Conversation search/list, clear model status, streaming responses.
- Arabic RTL-aware rendering and LTR treatment for code.
- Copyable code blocks and message actions where practical.

Do not bring over DeepSeek/Gemini clients, API-key handling, cloud routing, usage billing, audio/media pipelines, or provider settings.

## Inference foundation
- Native Android: Kotlin + Jetpack Compose.
- llama.cpp is pinned as a Git submodule under third_party/llama.cpp.
- Pin: 4b1a27fa0eb875bbca4f6cfe936e3d65adc685c0.
- Native binding is owned by Nawa so context size, threads, sampling, chat templating, and diagnostics can be controlled.
- Safe defaults: context 2048, 4 CPU threads, batch 256, temperature 0.7, max generation 512.
- Prefer Jinja chat templates when the model exposes a chat template.
- The app must never silently display an empty answer when reserved/special tokens are being generated.

## Storage
- Imported models live in app-private files/models after selection through Android's Storage Access Framework.
- Chats are local app data.
- Deleting the app may remove imported private copies and chats; export/backup is a later feature.

## Current focus
Build a stable text-only local inference MVP before multimodal, voice, GPU, or model-download features.
