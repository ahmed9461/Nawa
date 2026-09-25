# Project Status

Last updated: 2026-09-25

## Current state
Bootstrap in progress. Repository initialized and architecture selected.

## Active plan
plans/0001-bootstrap-local-chat.md

## Implemented
- Repository created.
- Nexora UI structure reviewed.
- Official llama.cpp Android binding reviewed.
- Native Android + Compose architecture selected.
- llama.cpp pin selected.

## In progress
- Android project scaffold.
- Nawa-owned llama.cpp JNI binding with safe mobile defaults.
- Local model import/load flow.
- Local chat UI and persistence.
- Basic CI build.

## Not yet implemented
- Verified APK build.
- On-device test with Gemma 3 1B.
- On-device test with Gemma 4 E2B.
- Multimodal/mmproj.
- GPU/OpenCL/Vulkan acceleration.
- In-app model downloads.
- Export/import backups.

## Known risks
- Large GGUF files can exceed available RAM during load; defaults must remain conservative.
- Some Gemma builds/templates can emit reserved tokens when chat formatting is wrong. Nawa must use current llama.cpp templating and surface diagnostics instead of hiding the output.
- Android app sandboxing prevents directly reading another app's private model files.
