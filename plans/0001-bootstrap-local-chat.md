# Plan 0001 — Bootstrap Local Chat

Status: active

## Goal
Produce the first buildable Nawa Android MVP that can import/load a GGUF model and stream a text response through a polished local chat UI.

## Steps
1. Scaffold Gradle/Android/Compose project.
2. Add pinned llama.cpp submodule and Nawa engine module.
3. Implement safe configurable CPU context, model lifecycle, Jinja chat formatting, and token streaming.
4. Implement app stores for imported models and local chats.
5. Implement Home, Models, Chat, Settings, and the Nexora-inspired theme/composer/message UI.
6. Add CI that clones submodules and builds a debug APK.
7. Review build failures and fix until CI is green.
8. Test on-device in this order:
   - Gemma 3 1B
   - Gemma 4 E2B
9. Record token output, TTFT/tokens-per-second observations, crashes, and reserved-token behavior.
10. Close this plan only after a real model produces visible normal text on-device.

## Self-review checklist
- Is any dependency unnecessary?
- Is any expensive work on the main thread?
- Can a model load twice or generation overlap?
- Does cancellation free the UI immediately?
- Can a failed model leave the engine in a poisoned state?
- Do Arabic and English messages choose sensible direction?
- Are reserved/special-token failures visible rather than silently blank?
