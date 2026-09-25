# Roadmap

## Phase 1 — Text-only local MVP
- Native Android project and CI.
- Import a GGUF model using the system file picker.
- Copy model once into app-private storage.
- Load/unload model on CPU.
- Stream generated text.
- Home/chat/models/settings screens inspired by Nexora.
- Local conversation history.
- Arabic RTL + English/LTR handling.
- Conservative memory defaults and useful load/generation errors.

## Phase 2 — Reliability and diagnostics
- Token/s and TTFT metrics.
- Raw inference diagnostics for reserved-token/template failures.
- Cancel generation.
- Model metadata display.
- Context/thread/max-token settings with validated safe ranges.
- Stress tests for repeated load/unload and long chats.

## Phase 3 — Rich chat
- Better Markdown coverage.
- Copyable fenced code blocks.
- Edit/resend and regenerate.
- Search/rename/delete chats.
- Export/import conversation backups.

## Phase 4 — Multimodal
- mmproj management.
- Image selection and vision prompts.
- Image token budget controls.
- Compatibility checks between model and projection file.

## Phase 5 — Acceleration
- Evaluate current llama.cpp Android GPU backends.
- Add acceleration only when it is measurably faster and as stable as CPU.
- Keep a CPU fallback.

## Phase 6 — Distribution polish
- Nawa launcher icon and visual identity.
- Signed personal release workflow.
- Small release APK/AAB, R8/resource shrinking, changelog.
