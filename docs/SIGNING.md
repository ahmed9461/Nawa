# Stable personal signing

## Migration
Old 0.1 CI APKs used per-run debug keystores. Their private signing keys are not available in this repository. A replacement key cannot sign an in-place update for those certificates. The new durable application ID is `com.ahmed9461.nawa.stable`; keep this ID and its signing certificate unchanged for future updates. This preserves the ability to leave old installs/data untouched.

## Build policy
Release builds are unsigned unless all signing values are explicitly supplied. Debug builds use a separate `.debug` suffix. Never commit keys/passwords or upload them as public GitHub Actions artifacts.

For a trusted local build, supply:
- `NAWA_SIGNING_STORE`: absolute path to the private keystore
- `NAWA_SIGNING_PASSWORD`: store password
- `NAWA_SIGNING_ALIAS`: key alias
- `NAWA_SIGNING_KEY_PASSWORD`: private key password

Then run `./gradlew :app:assembleRelease`. Alternatively use Android SDK `apksigner sign` on the CI unsigned APK and `apksigner verify --verbose --print-certs` afterward. Use environment/file password inputs rather than putting passwords in shell history. The pipeline verifies ZIP alignment before offline signing; do not rewrite the ZIP after signing.

A personal delivery may be signed offline with a separately retained owner-only keystore backup. That backup must be stored privately and made available to future builds. Merely setting `signingConfigs.debug` is not persistent release signing.

## Future automated releases
An owner can provision the same private key through encrypted GitHub Actions secrets or a private signing service. The current CI deliberately produces unsigned APKs and does not create/upload a new key per run. Do not claim automated stable signing is configured until these secrets/service exist and an update test passes.

## Upgrade acceptance
Build versions N and N+1 with the same key, compare certificate SHA-256 fingerprints, and install N+1 without uninstalling N. Confirm conversations/models remain. Keep a protected offline copy of the key and its recovery password. A signing certificate fingerprint is public; the key and password are not.
