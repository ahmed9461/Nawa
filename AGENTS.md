# AGENTS.md

## Required startup order
Before changing code, read:
1. PROJECT_MEMORY.md
2. PROJECT_STATUS.md
3. docs/PRODUCT_SPEC.md
4. docs/ARCHITECTURE.md
5. ROADMAP.md
6. docs/DECISIONS.md
7. the active plan named in PROJECT_STATUS.md
8. the latest entries in docs/PROGRESS_LOG.md

The repository is the source of truth.

## Working rules
- Keep Nawa local-first, lightweight, smooth, and Android-focused.
- Prefer the smallest reliable implementation. Do not add frameworks, services, permissions, or dependencies without a concrete need.
- Preserve Arabic RTL and English/LTR behavior.
- Treat model files as user-owned local data. Never upload prompts, chats, models, or telemetry.
- CPU stability comes before GPU acceleration.
- Do not hide inference failures. Surface actionable errors and preserve useful diagnostics.
- Before each non-trivial phase, update the active plan. Do not create a new plan when the current one still covers the work.
- After each meaningful change, update PROJECT_STATUS.md and docs/PROGRESS_LOG.md. Update PROJECT_MEMORY.md and docs/DECISIONS.md when durable facts or decisions change.
- Review every phase for unnecessary code, duplicated state, avoidable allocations, memory pressure, and Android lifecycle issues.
- Never copy Nexora provider/API-key code into Nawa. Nexora is a UI/UX reference only unless a specific local-only utility is clearly useful.
- Never commit model binaries, API keys, signing keys, local paths, generated APKs, or build outputs.

## Quality gate
A phase is not complete until:
- the project builds or the exact blocker is recorded;
- primary flows are manually reasoned through for lifecycle and failure cases;
- no known silent crash path is left undocumented;
- memory/planning/status files match reality.
