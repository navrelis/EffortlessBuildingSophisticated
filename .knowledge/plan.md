# Plan (multi-version, 4.3.0)

Status: open / in progress / in review / done. Chains: forward (1.21.1 -> 26.2) and backward (1.21.1 -> 1.16.x) can run in parallel once F1 is done.

| ID | Task | Model | Depends | Parallel | Status |
|----|------|-------|---------|----------|--------|
| R1 | Read-only map: Fabric vs NeoForge 1.21.1 diff, loader touchpoints, Create stack, common/ split proposal | Sonnet | - | R2, R3 | done |
| R2 | Download SB + Core jars per loader/MC into `upstream/`, manifest + fetch script | Sonnet | - | R1, R3 | done |
| R3 | Toolchain matrix per MC/loader + merge analysis + proof builds (scratchpad) | Opus | - | R1, R2 | done |
| F1 | Branch `mc/1.21.1`: restructure into `common/` + `fabric/` + `neoforge/`, tests green | Opus | R1, R3 | - | done |
| F2 | Forge 1.21.1 loader folder (no SB for Forge 1.21.1) | Opus | F1 | - | done |
| F3 | (merged into F1) vanilla preview renderer, Create stack dropped | Opus | - | - | done |
| F4 | 1.21.1 dependency updates, version 4.3.0, jar naming, `release/` folders | Sonnet | F1 | - | done |
| F5 | Per-branch CI (GitHub Actions) + build/export script into <loader>/release/ | Sonnet | F2, F4 | - | done |
| F6a | Hub `main`: README + matrix, docs (architecture, porting, releasing), worktree + build-all-versions scripts, upstream refresh script + weekly watch workflow, cleanup/local/ | Sonnet | F1 | with F2, P-B1 | done |
| F6b | Hub docs: CI + release sections after F5 | Sonnet | F5, F6a | - | open |
| P-B1 | Port mc/1.20.4 (fabric + neoforge; forge later) | Opus | F4 | with F2, F6a | done |
| P-F1 | Port mc/1.21.4 (fabric, neoforge SB, forge) | Opus | F2 | with P-B1, F5 | done |
| P* | Ports, one task per branch (see matrix below); forward chain and backward chain | Opus | F1, F2 | 2 chains | open |
| T1 | In-game smoke-test harness incl. Sophisticated Backpacks functional scenarios (sb.* checks), dev-only, runSmokeClient/runSmokeServer, result JSON + screenshots; mc/1.21.1 first, then every port | Opus | F5 | - | done |
| T2 | scripts/test-all-versions.ps1 on main: runs build/unit/GameTest/smoke per branch+loader, report | Sonnet | - | with ports | done |
| F7 | templates/branch on main (canonical CI/release/build-all, actions v5-era, ubuntu-24.04) + sync-branch-infra.ps1; applied to mc/1.20.4 + release jars | Sonnet | F5 | with T1, T2, P-F1 | done |
| B1 | Fix OmegaRandomizerBagScreen native BufferBuilder leak (1.21.1, 1.20.4, then ports) | Sonnet | - | 1.20.4 first | done |
| P-B1f | Forge 1.20.4 build | Opus | P-B1 | with H2 (own worktree) | done (42d4100); H4 harness in progress |
| B2 | Forge 1.21.1 client crash (LootModifierManager; data pack flagged incompatible) | Opus (inside T1) | - | - | done (Forge upstream bug; our pack.mcmeta fixed) |
| P-F2 | Port mc/1.21.5 (RenderPipeline rewrite, GameTest rework, B1 folded in) | Opus+Sonnet | P-F1 | with T1 | done (7a462c3, CI green) |
| B1b | Leak fix on 1.21.4 | Sonnet | - | with P-F2 | done |
| P-B2 | Port mc/1.20.1 (forge incl. NeoForge 1.20.1 compat, fabric SB port) | Opus | P-B1 | with P-F2 | done (cfa2acb, CI green) |
| H1 | Adopt smoke harness on 1.21.4 (+ the 2 T1 prod fixes) | Opus | T1 | with P-F2, P-B2 | done (13e138d) |
| B1c+CI-S | Leak fix on 1.21.1; CI template runs runSmokeServer; docs; stale-log fix | Sonnet | T1 | with H1 | done (main + mc/1.21.1 5d90c3b); sync other branches as each gets the harness |
| H* | Adopt the smoke harness: 1.20.4 done (H2, e7dd5ce), 1.21.5 (H3) in progress, then every port | Opus | H1 | - | in progress |
| Z  | Final: e2e check all branches, graphify refresh, report, push | lead | all | - | open |

## Definition of done
- R1/R3: written report with paths/evidence; R3 proof builds actually run.
- R2: jars verified (size, zip, sha1), manifest + fetch script idempotent, only manifest/script/README/.gitignore tracked.
- F1: same behaviour as 4.2.1; Fabric 77 unit + 17/17 GameTests, NeoForge build green; no logic drift between loaders.
- Each port: all loader builds green, unit tests green, dedicated server starts (with SB where it exists), jar in `<loader>/release/`, CI file present, manual checklist entry.

## Branch matrix (SB = Sophisticated Backpacks integration available; others build without it)
| Branch | Covers MC | Forge | NeoForge | Fabric | Chain |
|---|---|---|---|---|---|
| mc/1.21.1 | 1.21, 1.21.1 | yes (F2) | SB | SB (port) | base |
| mc/1.21.4 | 1.21.4 | yes | SB | yes | fwd 1 |
| mc/1.21.5 | 1.21.5 | yes | SB | yes | fwd 2 |
| mc/1.21.8 | 1.21.8 | yes | SB | yes | fwd 3 (done 6c19f36) |
| mc/1.21.10 | 1.21.10 | yes | SB | yes | fwd 4 |
| mc/1.21.11 | 1.21.11 | yes | SB | yes | fwd 5 |
| mc/26.1.2 | 26.1-26.1.2 (NeoForge 26.1.2 only) | yes | SB | yes | fwd 6 |
| mc/26.2 | 26.2 | yes | SB | yes | fwd 7 |
| mc/1.20.4 | 1.20.4 | yes | SB | SB (port) | back 1 |
| mc/1.20.1 | 1.20.1 | SB (also NeoForge 1.20.1) | (Forge jar) | SB (port) | back 2 |
| mc/1.19.2 | 1.19-1.19.2 (split 1.19 if runtime test fails) | SB | - | SB (port) | back 3 |
| mc/1.18.2 | 1.18.2 | SB | - | yes (no SB port) | back 4 |
| mc/1.18.1 | 1.18, 1.18.1 | SB | - | yes (no SB port) | back 5 |
| mc/1.17.1 | 1.17.1 | SB | - | yes (no SB port) | back 6 |
| mc/1.16.5 | 1.16.4, 1.16.5 | SB | - | yes (no SB port) | back 7 |
| mc/1.16.3 | 1.16.3 | SB (no Tool Swapper) | - | yes (no SB port) | back 8 |
Toolchains per cell: scratchpad TOOLCHAINS.md (R3); copied into the hub porting guide in F6.


Session stopped early by the user on 2026-09-24; continue from handoff.md.
| V1 | 1.21.1 jars on MC 1.21 (Fabric, NeoForge yes; Forge no) | Opus | - | - | done (f0dbd72) |
| V2 | Forge 1.21 jar (forge-1.21/ on mc/1.21.1) | Opus | V1 | - | done (58ea037) |
| B4 | 1.20.1 Forge unit tests fail on CI only (SecurityException signed IForgePlayer) | Opus | - | - | done |
| B5 | fabric-api floor = tested version on every branch (was "*") | tbd | ports | - | in progress (finished branches) |
