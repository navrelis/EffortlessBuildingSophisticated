# Handoff: Sophisticated Building multi-version work (state after the 2026-09-25 continuation session)

## State: done
All 16 branches `mc/<ver>` (1.16.3 ... 26.2) are finished, pushed and CI green; the final all-versions run passed
(196 pass, 0 fail; `.knowledge/test-all-versions-final.md`, summary and branch table in `.knowledge/report.md`).
README support matrix, CHANGELOG, docs (PORTING, TESTING, RELEASING, ARCHITECTURE) on main are current. Nothing is
running; no `local/game-window.lock`, no `local/test-locks/*`. Worktrees in `versions/` are clean.

## Open (needs the user or a future session)
1. Findings from the original code, user decision pending (asked 2026-09-25): PlayerSettingsGui stub (fix, remove or
   leave); randomizer bag titles overflow the texture; 6 unused widget classes in gui/elements (1.21.10+ already
   deleted 3). Any fix touches all 16 branches: do it on mc/1.21.1 first, then port per branch (smoke harness checks
   client.player_settings_gui / client.randomizer_bag_screens must stay green), rebuild release jars, CI.
2. Fabric GameTest servers hung twice under 3 parallel test instances (1.21.8 mid-batch, 1.18.2 at startup); both
   pass alone and in CI. Next time: jstack the hung server before the 20-min timeout.
3. Graph refresh for the hub layout: agent g1 (see log.md) - check that graphify-out was committed and pushed.

## How to run everything again
`pwsh scripts/test-all-versions.ps1` with the parallel recipe in docs/TESTING.md ("Parallel runs"): the script now
picks the Gradle JDK per loader folder from `ci_gradle_jdk` (JDK 25 for 26.x from ~/.gradle/jdks or `SB_JDK_25`).
Merge with `-MergeReports`. Rules: every client muted on the second monitor, one at a time (window lock); agents
`--no-daemon`, never `gradlew --stop`, max 10 agents, no sub-agents; commit + push each accepted task.

## Key facts for future work
- Layout: main = hub; code on `mc/<ver>` worktrees under `versions/<ver>`; extra jars of one loader for an older MC
  version live in `<loader>-<mc>/` folders that copy `../<loader>` and override files at COPY time (configuration
  cache safe): forge-1.16.4 (mc/1.16.5), forge-1.18 (mc/1.18.1), forge-1.19 (mc/1.19.2), forge-1.21 (mc/1.21.1).
- Toolchains: docs/PORTING.md (Architectury Loom for 1.16.x Forge, MDG Legacy 1.17.1-1.20.1, FG6 for Forge 1.20.4,
  FG7 from 1.21.1 with the merge-source-sets processResources fix, JDK 25 Gradle for 26.x).
- Real Forge 1.16.3/1.16.4 servers need Java 8 < 8u321; local real-server folders in `local/realserver-*`.
