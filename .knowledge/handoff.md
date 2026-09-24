# Handoff: Sophisticated Building multi-version work (session 2026-09-24, stopped early at the user's request)

## Goal (see requirements.md for every user answer)
Sophisticated Building 4.3.0 for every Minecraft version that has a Sophisticated Backpacks (SB) release, on Forge,
NeoForge and Fabric wherever the loader exists; SB integration only where SB exists for that loader+version.
One git branch per MC version (`mc/<version>`), each with `common/` + one standalone Gradle build per loader and the
built jar in `<loader>/release/`. `main` is the hub. The user also requires: an all-versions test run at the end that
proves the mod WORKS in-game, including with Sophisticated Backpacks; every test client muted and on the second
monitor; as few game windows as possible (the user watches them and got annoyed by idle/looping windows).

## How the repo works now
- `main` (hub): README with support matrix, `docs/` (ARCHITECTURE, PORTING = toolchain matrix + API breaks,
  RELEASING, TESTING, history/), `CHANGELOG.md`, `upstream/` (SB/Core manifest + fetch/update scripts, jars
  git-ignored), `templates/branch/` (canonical per-branch CI workflow, release.ps1, build-all.ps1),
  `scripts/` (setup-worktrees, build-all-versions, sync-branch-infra, test-all-versions, move-game-window, lib/),
  `.github/workflows/upstream-watch.yml` (weekly SB release watch), `.knowledge/` (this board).
- Version branches are git worktrees under `versions/<mc>/` (git-ignored on main). `pwsh scripts/setup-worktrees.ps1`
  recreates them from origin. Local-only stuff lives in `local/` (dev instances, old 1.21.11 draft in
  `local/reference/`, test reports in `local/test-reports/`).
- Port rules for agents: `.knowledge/port-brief.md` (layout, rules, definition of done). Always `--no-daemon`, never
  `gradlew --stop`; quick play only into EXISTING saves; clients muted (`soundCategory_master:0.0`,
  `pauseOnLostFocus:false`) and moved with `scripts/move-game-window.ps1`.

## Done and verified
| Branch | State | Verification |
|---|---|---|
| main | hub complete, pushed | scripts parse/tested; upstream manifest 28 pairs / 50 jars |
| mc/1.21.1 | fabric + neoforge (SB) + forge (no SB), pushed (8c3c34b) | build 77/65/65; Fabric GameTests 17/17; `scripts/test-all-versions.ps1 -Mc 1.21.1` 13 pass 0 fail (servers with SB verified; in-game smoke harness 17/17 client checks incl. 7 sb.* on fabric+neoforge, 10/10 forge; server smoke 9/9 incl. 6 sb.*); CI green (before 8c3c34b; the leak-fix commit only built on fabric locally, CI checks the rest) |
| mc/1.20.4 | fabric (SB port) + neoforge (SB), pushed (74281db) | build 77/65, GameTests 18/18 (17 + Porting-Lib self test), CI green; NO forge yet, NO smoke harness yet |
| mc/1.21.4 | fabric + neoforge (SB) + forge, pushed (1a9012c) | build 77/65/65, GameTests 17/17, CI green; NO smoke harness yet (see below) |
- 1.21.1 smoke harness = `common/src/smoketest*`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`, tasks
  `runSmokeClient` / `runSmokeServer -PsmoketestOut=<dir>`; contract in `docs/TESTING.md` and branch `TESTING.md`.
- Bugs fixed this session: Omega bag screen native-buffer leak (B1, 1.20.4/1.21.4/1.21.1, folded into 1.21.5);
  Fabric held-slot resend when a build click cancels vanilla placement (1.21.1 only so far); Forge pack.mcmeta
  supported_formats (1.21.1 only so far); ForgeNetworking logs handler exceptions.

## Partially done (committed as WIP)
- **mc/1.21.5** (pushed, 39a3b1d, WIP): compiles on fabric/neoforge/forge, unit tests 77/65/65, Fabric GameTests
  18/18 (17 + minecraft:always_pass), Fabric server + in-game render check OK. Missing: NeoForge/Forge runServer
  (SB check on NeoForge) and one client each; verify the Forge world-render mixin
  (`forge/.../mixin/LevelRendererMixin.java`, `sophisticatedbuilding.forge.mixins.json`, MixinConfigs manifest entry)
  loads in dev and in the jar; README + changelog still 1.21.4 texts; jar metadata check; `release.ps1 -NoBuild`
  (release/ still holds 1.21.4 jars); fresh-copy build. Remove unused NBTHelper import in
  neoforge BuildingUpgradeContainer. Forge floor 55.0.24.
- **mc/1.20.1** (LOCAL ONLY, NOT pushed, 7da383f, does NOT compile): fabric partly ported (deps, ModPayload replaces
  CustomPacketPayload in common, 1.20.1 GUI fixes, GameTest fake player). Open fabric errors: SB port 1.20.1 API
  (`handleMessage(CompoundTag)` instead of `handlePacket` in BuildingUpgradeContainer; no
  `BackpackWrapper.fromData` in ToolSwapperIntegration:49 / BuildingUpgradeHelper:89 -> use the 1.20.1 lookup,
  javap the jar). forge/ not created yet (ModDevGradle Legacy 2.0.147 + Gradle 8.14.5, Forge 47.1.3, Parchment
  2023.09.03, SimpleChannel, capabilities; SB Forge 1.20.1: wrapper via
  `stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())`, runOnBackpacks returns void; Curios
  5.14.1+1.20.1). Delete neoforge/ (Forge jar covers NeoForge 1.20.1). Many files show as modified only because of
  LF conversion.
- **mc/1.21.4 harness (H1)**: only untracked copies of the 1.21.1 harness folders exist in `versions/1.21.4`
  (not wired, not compiled). Facts found: 1.21.4 DataVersion 4189, resource pack 46, data pack 61. Still to do:
  wire into the three build.gradle, adapt ClientScenarios/RadialMenuDriver, no sb.* on fabric/forge (no SB),
  apply the two T1 production fixes (Fabric held-slot resend; Forge pack.mcmeta supported_formats [46, 61]).
- **CI smoke-server step (CI-S)**: `templates/branch/.github/workflows/build.yml` on main has the new
  `has_smoke` detection + `runSmokeServer` step + artifact upload (committed, NOT yet synced to branches, not yet
  run on GitHub). Next: update docs/RELEASING.md + docs/TESTING.md, `scripts/sync-branch-infra.ps1 -Mc 1.21.1,...`,
  prove `runSmokeServer` locally, push and watch CI.

## Missing (not started)
1. Forge 1.20.4 (FG6/Gradle 8.12.1 or MDG Legacy, Forge 49.x; no SB for Forge 1.20.4).
2. Smoke harness adoption on 1.20.4, 1.21.4, 1.21.5 and every later port (each branch needs runSmokeClient/Server;
   sb.* where SB exists). The two T1 production fixes must be ported to every branch.
3. Forward ports: 1.21.8, 1.21.10, 1.21.11 (1.21.6 GUI rewrite GuiRenderState; 1.21.9 KeyMapping.Category;
   1.21.11 ResourceLocation -> Identifier), 26.1.2 (covers 26.1-26.1.2 on fabric/forge; NeoForge 26.1.2 only;
   unobfuscated, Java 25, Loom `fabric-loom` non-remap), 26.2 (MultiBufferSource/Tesselator removed). NeoForge has SB
   on all of these; Fabric/Forge do not.
4. Backward ports: finish 1.20.1, then 1.19.2 (1.19-1.19.2), 1.18.2, 1.18.1 (1.18-1.18.1), 1.17.1, 1.16.5
   (1.16.4-1.16.5), 1.16.3. Forge has SB on all; Fabric SB port only on 1.19.2 (and 1.20.1/1.20.4/1.21.1).
   FG6 needs a JDK 17 Gradle JVM (set `ci_gradle_jdk=17`); pre-1.19.3 static Registry, pre-1.20 no GuiGraphics,
   pre-1.20.2 no CustomPacketPayload, Java 8/16 for 1.16/1.17.
5. Version-range merges (need a runtime test each): 1.21 into the 1.21.1 jars ([1.21,1.21.1]); 26.1.x.
6. Final: README support-matrix statuses + link docs/TESTING.md; run `pwsh scripts/test-all-versions.ps1` over ALL
   branches (report in the final report); graphify incremental refresh (see memory: never plain `graphify update .`);
   `.knowledge/report.md` for this multi-version work; memory update; push everything.

## Known issues / findings
- Forge upstream bug: quick play into an EXISTING world crashes ("Can not retrieve LootModifierManager until
  resources have loaded once") because Forge runs quick play before its event bus starts (1.21.1 and 1.21.4, still
  on the latest 1.21.1 branch). The smoke harness creates worlds in-game and is unaffected; test-all-versions
  classifies that crash as known-upstream only when no mod frame is in the trace.
- `--quickPlaySingleplayer <name>` only opens existing saves; a missing name shows "Failed to Quick Play".
- test-all-versions client-only stage can match a stale "logged in" line from a previous log (reported by the 1.21.5
  agent); fix: clear/rotate the log or record the log position before launch.
- Fabric SB port rescans Trinkets slots only every 100 ticks (tests wait for it).
- Forge 55 (1.21.5) has no world render event -> client mixin; check again for Forge 58+.
- The Fabric SB port for 1.20.4/1.20.1 nests Porting-Lib builds that are on no public maven: the fabric build unpacks
  them from the Core jar.

## Prompt for the next session
```
You are Opus 5.5, lead developer (same role rules as last session: plan, delegate to subagents, review, never write
code yourself; you may edit .knowledge/, run builds/tests/git/graphify). Repo:
C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated (public GitHub navrelis/EffortlessBuildingSophisticated).
First read .knowledge/handoff.md, requirements.md, decisions.md, plan.md and port-brief.md; then docs/PORTING.md and
docs/TESTING.md on main. Run `pwsh scripts/setup-worktrees.ps1` if versions/ worktrees are missing (mc/1.20.1 exists
only locally). Do not ask the user the scope questions again - they are answered in requirements.md.
Continue in this order, committing and pushing each accepted task:
1. Finish mc/1.21.5 (NeoForge/Forge server + client checks, Forge mixin runtime check, README/changelog, release jars).
2. CI smoke-server template: docs, sync to all finished branches, prove on GitHub.
3. Harness adoption + the two T1 production fixes on 1.21.4, 1.20.4, 1.21.5.
4. Finish mc/1.20.1 (fabric compile fixes, create forge/, delete neoforge/), then Forge 1.20.4.
5. Remaining forward ports (1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2) and backward ports (1.19.2, 1.18.2, 1.18.1,
   1.17.1, 1.16.5, 1.16.3), each with harness and sb.* checks where SB exists.
6. Final all-versions test run with scripts/test-all-versions.ps1 (clients muted, second monitor, minimal windows),
   README matrix, graphify refresh, report, push.
Rules the user insisted on: test that the mod WORKS in-game incl. Sophisticated Backpacks, not just builds; every
game window muted and on the second monitor; launch as few game windows as possible; agents use --no-daemon and
never `gradlew --stop`.
```
