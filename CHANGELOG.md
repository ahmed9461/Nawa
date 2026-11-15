# Changelog

## 0.2.0 — Reliability release candidate (2026-11-16)
### Safety and correctness
Atomic and serialized chat storage; corruption preservation and explicit recovery; startup read gate; periodic stream checkpoints; validated JSON export and merge restore; native exception containment; correct UTF-16/UTF-8 boundaries; separate decode errors and EOS; process-owned backend; cooperative native cancellation; complete recent-prompt context budgeting.

### Experience
Import validation/progress/cancel and Unicode names; memory/model diagnostics; first-token and token-rate metrics; saved screen/draft state; keyboard/RTL/code presentation; follow-latest control; model attribution; confirmed deletion; conversation rename and regenerate.

### Build and distribution
ARM64-only APK; minimum Android 13; verified Gradle Wrapper; test/lint/native/packaging checks; pinned workflow actions and model fixture checksum; no implicit debug signing. The new stable package installs alongside legacy builds rather than requiring deletion of old private data.

### Limitations
No physical-device qualification in this pass, no universal GGUF compatibility/performance guarantee, no GPU/images/audio/downloader, no automatic legacy-package data transfer, no CI private signing secrets provisioned. Retain the owner-only signing backup for subsequent updates.
