# Nawa

Arabic-first, private Android chat with local GGUF language models. Kotlin, Jetpack Compose, and a pinned llama.cpp CPU backend. No accounts, API keys, Internet permission, telemetry, or cloud synchronization.

## Version 0.2.0
- Atomic, serialized conversation persistence; startup corruption is reported instead of overwritten.
- Checkpointed streaming, local JSON export/merge restore, conversation rename/delete confirmation, and regenerate.
- Correct streaming Unicode and native exception/error handling; cancellable import/inference.
- Recent-history context budgeting while retaining the full transcript on disk.
- Import progress, GGUF header validation, Unicode filenames, model diagnostics, TTFT/token-rate metrics.
- Restored screen/draft state, keyboard handling, RTL arrows, copyable LTR code, and explicit model attribution.

## Device and install requirements
Android 13 (API 33) or newer, **arm64-v8a**. Model weights are NOT included. Import a compatible single-file GGUF through the system picker, load it, then start a chat. Start with a small text/instruction model and default context 2048; available RAM and model architecture determine practical limits.

The new durable channel uses **com.ahmed9461.nawa.stable**. Previous 0.1 builds used changing debug signing keys. This new package installs alongside them: **do not uninstall an older copy merely to install this one**. Android does not let the new package read the old app's private models/chats. Keep originals and export from an older app if it supports export. A new package cannot automatically recover private data from an unrelated signing identity.

## Backups and privacy
Settings → export conversations creates a plaintext JSON file via the system picker. Protect that file. Restore validates it, skips identical entries, and preserves conflicting local/imported copies. Backups contain conversations, not model weights or settings. Uninstalling an app removes its private data. OS cloud/device-transfer backup is explicitly disabled; use the intentional export feature.

Generation stops when the app goes to the background. An interrupted saved answer remains visible and can be regenerated. Full chat histories stay on disk; the prompt may omit the oldest exchanges to fit the selected context, with a visible notice. GPU, image/mmproj, voice, and in-app downloads remain out of scope.

## Build and verify
```sh
git clone --recurse-submodules https://github.com/ahmed9461/Nawa.git
cd Nawa
# JDK 17; SDK 36; NDK 29.0.13113456; CMake 3.31.6
./gradlew :engine:testReleaseUnitTest :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```
Use `gradlew.bat` on Windows. Configure the Android SDK using `ANDROID_HOME` or an untracked `local.properties`. Without explicit signing credentials, release outputs are **unsigned**, never silently debug-signed. See [signing](docs/SIGNING.md) before installation/distribution.

## Tests and limitations
CI runs Kotlin configuration/streaming UTF-8 checks, Robolectric storage/backup tests, lint, APK ABI/permission/16KB-alignment checks, C++ UTF conversion under ASan/UBSan, and real small-GGUF host inference including cancellation/recovery/context pressure. Host/native checks do not certify Android JNI/runtime behavior, real-device RAM/heat, or compatibility with every GGUF model. See [acceptance](docs/ACCEPTANCE.md) and [project status](PROJECT_STATUS.md).

Report failures with app version, device/Android version, model filename/size, context settings, and the displayed error. Do not publish private chats or model files in issues.
