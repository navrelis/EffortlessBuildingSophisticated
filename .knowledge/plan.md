# Plan (4.2.0)

| ID | Task | Loader | Model | Depends | Parallel | Status |
|----|------|--------|-------|---------|----------|--------|
| T1 | Fabric JSON config files (common/server/client) + server->client sync + new `survivalReplace.enabled` (default false) | Fabric | Opus | - | with T3 | done |
| T3 | #4 place with the real stack's data components (contents, name) and consume that exact stack; #1 hardening (third-party getStateForPlacement exceptions mark the entry invalid instead of crashing) | Fabric | Opus (worktree) | - | with T1 | done |
| T5 | #3 audit: trace Building Upgrade item flow (client count -> packet -> server extraction) on both loaders, report concrete bugs | both | Sonnet (read-only) | - | with T1, T3 | done |
| T2 | Survival replace: gate via config, mine replaced blocks with survival rules, delay + countdown, client preview marks unbreakable targets, safe survival undo/redo of replacements | Fabric | Opus | T1, T3 | - | done |
| T5b | Fix T5 bugs: Fabric anchor only when backpack has the item; widen tick-sync catches; NeoForge Curios double scan; NeoForge tooltip | both | Sonnet | T5 | - | done |
| T6 | NeoForge parity for T2, T3 + SERVER config entry (T5b ported separately) | NeoForge | Sonnet | T2, T3 | with T5 | done |
| T7 | Release 4.2.0: version bump, patch notes, export jars, .gitignore negation for .knowledge | both | Sonnet | T6 | - | done |
| T8 | Final: e2e smoke test (T8a Sonnet), graphify refresh, report, push, issue replies | - | lead | T7 | - | done |

## Definition of done (per task)
- T1: config files are created with defaults on first start, missing keys filled, invalid JSON logged and defaults kept, values clamped to the NeoForge ranges; all existing call sites unchanged; client gets the server's ServerConfig values on join and restores its own on disconnect; Fabric build + tests green.
- T3: placing a filled/renamed shulker (or any block item with data components) with any build mode keeps contents and name; survival consumes that exact stack; creative copies the template stack; plain blocks behave as before; an exception in a third-party getStateForPlacement marks that entry invalid and is logged once, no crash; Fabric build + tests green.
- T5: written report with file:line evidence per step, verdict per step (works / broken), list of bugs.
- T2: with config off, behaviour identical to 4.1.1 (replace creative-only); with config on, survival replace works with mining rules, unbreakable/protected targets are skipped and not consumed, delay+countdown shown; survival undo/redo of replacements never places blocks for free; Fabric build + tests green.
- T6: NeoForge builds, same behaviour as Fabric; new SERVER config entry default false.
- T7: both jars exported as 4.2.0, patch notes written.
