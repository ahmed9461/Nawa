# Product Spec

## Problem
Existing local-model mobile apps can be convenient but may hide inference/template failures, crash under aggressive settings, or expose too many controls. Nawa is a personal Android client focused on predictable local inference and a clean chat interface.

## MVP user flow
1. Open Nawa.
2. Open Models and choose a local .gguf file.
3. Nawa imports the model to private storage and lists its name/size.
4. Tap Load.
5. Create a chat and send text.
6. Nawa streams the response visibly as it is generated.
7. Chats persist locally across app restarts.
8. The model can be unloaded to free memory.

## MVP requirements
- No Internet permission is required for local chat.
- No sign-in or API key.
- Supports GGUF text models supported by the pinned llama.cpp revision.
- Safe defaults designed for a modern arm64 Android phone.
- Clear loading/generating/error states.
- Arabic text must render naturally RTL; English/code must remain readable LTR.
- UI should feel like Nexora: modern Material 3, restrained surfaces, rounded composer, clean conversation list.
- App should avoid media/audio features until text inference is stable.

## Non-goals for MVP
- Cloud AI providers.
- Built-in Hugging Face downloader.
- Voice/audio.
- Image understanding.
- GPU acceleration.
- Cross-platform support.
