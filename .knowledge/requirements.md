# Requirements

## Session 2026-10-19: 5.1.0 (reach-upgrade fix + balanced survival breaking)
- Reach upgrades must stay after consumption. Finding: server saves them (relog/death/dimension OK), but the client is
  not re-synced after death -> client acts as level 0 until relog. Fix on all branches.
- New option "balanced survival breaking", off by default.
  - Dedicated server: the server config decides for all players. Singleplayer: the client setting decides (server config also counts).
  - Covers survival build-mode breaking + survival replace mining; one-block operations count like vanilla; creative never affected; placing not affected.
  - Tool durability x5 per block (server config); stopBeforeToolBreaks keeps working.
  - Hunger: 0.075 exhaustion per block (user value); refused when food level <= 6.
  - Cooldown: base 15 s; uses in the last 30 real-time minutes; first 10 keep 15 s, +5 s per extra use, max 120 s; old uses expire. Server config.
  - One operation = one use (not weighted by block count).
  - Cooldown survives relog and offline time (stored with the player, real-time clock).
  - Feedback: action bar on refusal (cooldown seconds / too hungry) + HUD cooldown next to the break countdown.
- All 17 branches, version 5.1.0, push to GitHub (mc/* branches + main), upload to CurseForge (user approved upfront).
- Lead decides all other details. No new dependencies.

# Requirements (session 2026-09-24, multi-version)

## Goal
Sophisticated Building (only mod, only repo: navrelis/EffortlessBuildingSophisticated, public) for every Minecraft
version that has a Sophisticated Backpacks (SB) release, on Forge, NeoForge and Fabric wherever that loader exists
for the version. Backpacks integration only where SB exists for that loader+version; elsewhere the mod works without it.

## User answers
- Scope: every MC version with an SB release (release files, no alpha/beta). "Do whatever is needed, merge whatever you need"
  -> one jar may cover several MC versions (declared range) when verified compatible.
- Everything else: lead's defaults accepted, lead decides:
  - `main` = hub (README + support matrix, porting guide, shared scripts, CI templates, .knowledge, docs, upstream manifest).
  - One branch per MC version `mc/<version>` with `common/` + one folder per loader (`forge/`, `neoforge/`, `fabric/`),
    each loader folder holds its Gradle project and `release/` with the latest built jar
    `sophisticatedbuilding-<loader>-<mc>-<ver>.jar` (jars committed there only).
  - Local: root = main checkout, version branches as git worktrees under `versions/<mc>/` (ignored on main).
  - Upstream SB + Sophisticated Core jars: latest release per loader/version in `upstream/`, git-ignored (third-party,
    public repo), reproducible via committed manifest + fetch script. Builds resolve deps from Maven (CurseMaven/Modrinth).
  - Mod version 4.3.0 everywhere; update 1.21.1 deps to latest; no new gameplay features beyond porting; fix found bugs.
  - CI: GitHub Actions per version branch (build all loaders + unit tests; GameTests where available).
  - Every port: builds, unit tests green, dedicated server starts with SB (where it exists). In-game checks -> manual checklist.
  - Cleanup: remove empty `net/`, `temp-fabric-src/`, `.scratch_javap/`; old `Fabric-0.19.2-1.21.11/` only as reference,
    then removed; `DevInstance_*` -> ignored `local/`; patch notes into version branches, overall changelog on main.
  - Push: new `mc/*` branches directly; main converted to hub after `mc/1.21.1` is verified.

## Constraints
- Existing 1.21.1 code (4.2.1) is the source of truth: 77 Fabric unit tests + 17 GameTests must stay green.
- Sophisticated integration stays optional and guarded (Exception | LinkageError).
- No secrets, no third-party jars in git. Never force-push.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Never stage graphify-out/ in feature commits; refresh the graph at the end (incremental recipe, never plain `graphify update .`).

## User addition (2026-09-24, during the ports)
- "When finished, run a test script for all versions that checks everything works." -> automated all-versions test
  script on main (build, unit tests, GameTests, server + client in-game smoke scenarios per loader), run at the end
  over every branch; results in the final report. User observed a "very buggy" Forge window during the work
  (details not given; our agents' smoke-test clients run automated check hooks and unfinished ports).
- User addition: the tests must also prove the mod works WITH Sophisticated Backpacks (not only build/start):
  in-world checks where SB exists - Building Upgrade supplies blocks, disabled upgrade ignored, tier cap, HUD count
  sync, Tool Swapper tools for survival breaking, worn backpack (Curios/Trinkets/Accessories where present).
- User (2026-09-24): every test client must start MUTED and on the SECOND monitor (secondary = DISPLAY1 left of the primary). -> scripts/lib/GameWindow.ps1 + move-game-window.ps1 (T2), built into the smoke harness (T1); add to PORT_BRIEF.

## User addition (2026-09-25, round 2)
- Scope widened: every MC version with ANY Sophisticated Backpacks file (release, beta or alpha) gets our mod on Fabric,
  NeoForge and Forge wherever the loader exists - also on beta loader builds (NeoForge 26.1 / 26.1.1). CurseForge list
  (2026-09-25): Forge 1.16.3-1.20.1, NeoForge 1.20.1-26.2 (incl. 26.1, 26.1.1), Fabric port 1.19.2, 1.19.4 (beta only),
  1.20.1, 1.20.4, 1.21.1. New: mc/1.19.4 (Fabric with SB beta, Forge 45 without SB); NeoForge jar for 26.1/26.1.1.
- Always test with Sophisticated Backpacks + Building Upgrade where SB exists; test fast; fix bugs when encountered.
