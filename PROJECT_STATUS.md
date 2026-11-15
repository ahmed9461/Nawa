# Project Status

Last updated: 2026-11-16

## Current state
0.2.0 reliability hardening implemented on a review branch; automated validation and APK inspection are in progress. Do not describe this as a physically device-validated release.

## Active plan
plans/0001-bootstrap-local-chat.md

## Implemented in this phase
Atomic serialized storage and recovery guard; versioned JSON backup/merge; periodic streaming checkpoints; native RAII/error containment; standard UTF-8 byte streaming and safe UTF-16 input; process-owned serialized engine; abort callbacks; bounded recent-history prompts; model import progress/cancellation/header/storage checks; diagnostics and metrics; state restoration/RTL/IME/delete confirmation/rename/regenerate; stable application identity and explicit signing policy; unit/native/packaging CI checks.

## Pending verification
Run all checks on the exact candidate commit, inspect the produced APK, and record actual results. Provision/retain a private permanent signing key outside Git for the delivery. Automated CI signing secrets are not configured by source changes alone.

## Physical-device gate
No physical Android-device validation has been performed in this hardening pass. Real-model phone tests, sustained memory/thermal behavior and same-key upgrade acceptance remain required.

## Still intentionally deferred
Vision/mmproj, voice, GPU acceleration, network model downloads, full Markdown renderer, and broad-device distribution.
