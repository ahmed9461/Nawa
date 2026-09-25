# Progress Log

## 2026-09-25
- Initialized ahmed9461/Nawa.
- Audited Nexora's theme, home/chat screens, composer, message rendering, Markdown/code handling, chat model, and local chat store.
- Chose which Nexora ideas to carry into Nawa and explicitly excluded cloud-provider/API-key/media code.
- Reviewed current official llama.cpp Android documentation and binding.
- Pinned llama.cpp commit 4b1a27fa0eb875bbca4f6cfe936e3d65adc685c0.
- Chose native Kotlin + Compose with a Nawa-owned JNI binding.
- Defined conservative CPU-first inference defaults and arm64-only MVP packaging.
- Started plan 0001.

- Fixed Android CI SDK-manager discovery and aligned Compose/Lifecycle versions with AGP 8.13.
- Fixed lightweight Compose icon/layout compatibility without adding the large material-icons-extended dependency.
- First full debug APK build succeeded in GitHub Actions at commit `2e67486b7c7bea05c87554971b99846171b0d568`.
- The first artifact archive was about 57.5 MB, so size optimization is now active before on-device validation.
- Removed `GGML_CPU_ALL_VARIANTS` from the Nawa engine build; Nawa will keep one arm64 CPU backend and KleidiAI instead of packaging every CPU feature variant.

- The slimmed debug build remained green, but inspecting the APK showed native llama.cpp shared libraries still carried debug information in the debug variant.
- Switched CI to an optimized release-variant test APK with R8/resource shrinking, native symbol stripping, and temporary debug signing so the artifact remains directly installable for device testing.

- Disabled Android cloud backup to keep Nawa's chats/settings strictly local-first.
- Added a tiny vector adaptive Nawa launcher icon instead of shipping raster icon assets.

- Inspected the optimized APK directly: it was still about 134 MB because several llama.cpp shared libraries retained debug sections.
- Switched the native engine to static llama.cpp linkage with one stripped JNI library and linker garbage collection.
- Hardened model switching so a failed replacement load cannot leave the UI claiming the old model is still loaded.
- Added cleanup for interrupted `.part` model imports and clearer Arabic import errors.
- Reserved Gemma-style special-token loops are now withheld from the visible answer and converted into an explicit compatibility error after a short streak.
- Prevented sending into a second chat while another chat is generating.
