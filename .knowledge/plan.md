# Plan (multi-version, 4.3.0)

Status: open / in progress / in review / done. Chains: forward (1.21.1 -> 26.2) and backward (1.21.1 -> 1.16.x) can run in parallel once F1 is done.

| ID | Task | Model | Depends | Parallel | Status |
|----|------|-------|---------|----------|--------|
| R1 | Read-only map: Fabric vs NeoForge 1.21.1 diff, loader touchpoints, Create stack, common/ split proposal | Sonnet | - | R2, R3 | done |
| R2 | Download SB + Core jars per loader/MC into `upstream/`, manifest + fetch script | Sonnet | - | R1, R3 | done |
| R3 | Toolchain matrix per MC/loader + merge analysis + proof builds (scratchpad) | Opus | - | R1, R2 | in progress |
| F1 | Branch `mc/1.21.1`: restructure into `common/` + `fabric/` + `neoforge/`, tests green | Opus | R1, R3 | - | open |
| F2 | Forge 1.21.1 loader folder (no SB for Forge 1.21.1) | tbd | F1 | - | open |
| F3 | Vanilla preview renderer in common for versions without the Create stack | tbd | R1, F1 | - | open |
| F4 | 1.21.1 dependency updates, version 4.3.0, jar naming, `release/` folders | Sonnet | F1 | - | open |
| F5 | Per-branch CI (GitHub Actions) + build/export script | Sonnet | F1 | - | open |
| F6 | Hub `main`: README + matrix, porting guide, worktree setup script, cleanup, changelog | Sonnet | F1-F5 | - | open |
| P* | Ports (one task per version branch, details after R3): forward 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.x, 26.2; backward 1.20.4, 1.20.1, 1.19.x, 1.18.x, 1.17.1, 1.16.x | Opus | F1-F5 | 2 chains | open |
| Z  | Final: e2e check all branches, graphify refresh, report, push | lead | all | - | open |

## Definition of done
- R1/R3: written report with paths/evidence; R3 proof builds actually run.
- R2: jars verified (size, zip, sha1), manifest + fetch script idempotent, only manifest/script/README/.gitignore tracked.
- F1: same behaviour as 4.2.1; Fabric 77 unit + 17/17 GameTests, NeoForge build green; no logic drift between loaders.
- Each port: all loader builds green, unit tests green, dedicated server starts (with SB where it exists), jar in `<loader>/release/`, CI file present, manual checklist entry.
