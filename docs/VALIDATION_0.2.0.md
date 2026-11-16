# Nawa 0.2.0 — Validation and delivery evidence

Date: 2026-11-16

## Exact source and build
- Executable source commit: `c60ebc5677ad560d081d1be7189f15700591aa65`
- Strict CI: https://github.com/ahmed9461/Nawa/actions/runs/43777791988
- Android job: `133007948687`; native job: `133007948989`; both completed successfully.
- Public unsigned build artifact: `14186793450` (`Nawa-0.2.0-build`). Native evidence artifact: `14187142433`.
- The documentation-only delivery record does not alter the executable code above.

## Delivered signed APK
- Filename: `Nawa-0.2.0-arm64.apk`
- Version: `0.2.0` / code `2`
- Application ID: `com.ahmed9461.nawa.stable`
- Minimum Android SDK: 33 (Android 13); target SDK: 36.
- Architecture: `arm64-v8a` only.
- Size: `12325183` bytes, model weights not included.
- APK SHA-256: `3edb8f706b588df694642356d8e2112d09472c58e3b7378a06276378e177a0b3`
- Signing certificate SHA-256: `f9125cb7e93ae07b39c12d10b0d1adafe7eb1b22405f33cfabd25dcdd30f36f0`
- Signature: Android APK Signature Scheme v3, verified with official SDK apksigner; RSA 3072-bit key. The fingerprint is public, not the private key.

## Checks actually performed
1. 11 Robolectric storage/backup/import tests and 3 JVM engine/UTF-8 tests: 14 passed; zero failures, errors or skipped tests.
2. Eight C++ UTF-16/UTF-8 checks ran under address/undefined-behavior sanitizers.
3. A real small GGUF completion model generated visible text. Native checks cover three load/unload cycles, long-history trimming, overlong prompt failure, malformed file/template handling, cancellation and recovery.
4. Fixture SHA-256 is enforced: `270cba1bd5109f42d03350f60406024560464db173c0e387d91f0426d3bd256d`. Fixture is not shipped with the app.
5. Release assembly succeeded. Android lint has zero errors and 22 warnings (dependency/API/style notices and deliberate ARM64 restriction).
6. Binary APK inspection verified package/version/minSDK/targetSDK, no Internet permission, non-debuggable mode, disabled OS backup, only arm64 libraries, and ELF plus uncompressed ZIP 16KB alignment.
7. The verifier was intentionally given an incorrect minimum SDK and correctly rejected it. Shell pipelines use fail-closed behavior.
8. Offline signing verified the expected certificate. All signed ZIP entry contents were byte-identical to the CI-verified unsigned APK. Post-signing alignment was independently checked for every uncompressed entry.

## Initial verifier issue, resolved
The first run's human-readable aapt check expected `sdkVersion`, while the tool reported `minSdkVersion`; a shell pipeline masked that assertion. Reading the logs caught it. The replacement reads typed binary-manifest attributes, uses strict pipelines, and includes a negative test. The final strict run above passed; do not use the initial green status as packaging evidence.

## Boundaries
No physical Android phone or Android-runtime JNI/inference test was available. This record does not certify every GGUF architecture/template, Gemma compatibility, performance, thermals, memory pressure, or same-key in-place upgrade. These remain in docs/ACCEPTANCE.md. The host fixture is a tiny completion model, not a benchmark for phone chat quality.

## Private material
The release key and password were not committed or uploaded to public CI artifacts. An owner-only private backup accompanies the signed app delivery outside GitHub. Future stable updates must reuse that key. CI signing secrets still require deliberate private provisioning.
