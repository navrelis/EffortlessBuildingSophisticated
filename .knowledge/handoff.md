# Handoff: Sophisticated Building multi-version work (session 2026-09-24/25, stopped by the usage limit)

All subagents died on the Anthropic session limit (resets 04:00 Europe/Vienna). Nothing is running: no Java process of
this repo, no `local/game-window.lock`, no `local/test-locks/*`.

## Goal (details in requirements.md)
Sophisticated Building 4.3.0 for every Minecraft version with a Sophisticated Backpacks (SB) release, on Forge,
NeoForge and Fabric wherever the loader exists; SB integration where SB exists for that loader+version. Branch
`mc/<ver>` per version (worktrees in `versions/<ver>`), `main` = hub. User rules: prove it WORKS in-game incl. SB;
game windows muted, second monitor, one at a time (window lock); agents `--no-daemon`, never `gradlew --stop`,
no child agents, max 10 agents; commit + push every accepted task.

## Finished, pushed, CI green (in-game smoke harness incl. sb.* where SB exists)
| Branch (HEAD) | Jars / MC coverage | SB |
|---|---|---|
| mc/1.21.1 (eae592e) | fabric + neoforge [1.21,1.21.1]; forge [1.21.1]; forge-1.21/ [1.21] | fabric, neoforge |
| mc/1.21.4 (fa41a23) | fabric, neoforge, forge [1.21.4] | neoforge |
| mc/1.21.5 (3f53370) | fabric, neoforge, forge [1.21.5] | neoforge |
| mc/1.21.8 (9a76f47) | fabric, neoforge, forge [1.21.8] | neoforge |
| mc/1.21.10 (5474c38) | fabric, neoforge, forge [1.21.10] | neoforge |
| mc/1.21.11 (73911ad) | fabric, neoforge, forge [1.21.11] | neoforge |
| mc/26.1.2 (6e01840) | fabric + forge [26.1,26.1.2]; neoforge [26.1.2] (26.1/26.1.1 NeoForge only betas) | neoforge |
| mc/26.2 (0b20bdc) | fabric, neoforge, forge [26.2] (rendering rewrite: VertexRecorder -> SubmitNodeCollector) | neoforge |
| mc/1.20.4 (e0dc62e) | fabric, neoforge, forge [1.20.4] | fabric, neoforge |
| mc/1.20.1 (05e3161) | fabric [1.20.1]; forge [1.20.1] (also runs on NeoForge 1.20.1-47.1.106, smoke-tested) | fabric, forge |
| mc/1.19.2 (8bc476a) | fabric [1.19,1.19.2]; forge [1.19.2]; forge-1.19/ [1.19,1.19.1] | fabric (1.19.2 only), forge, forge-1.19 |
| mc/1.18.2 (13cd256) | fabric, forge [1.18.2] | forge |
| mc/1.18.1 (e4b4551) | fabric [1.18,1.18.1]; forge [1.18.1]; forge-1.18/ [1.18] | forge, forge-1.18 |
| mc/1.17.1 (72bb507) | fabric, forge [1.17.1] | forge |
Every finished branch: build + unit tests (fabric 77, others 65), Fabric GameTests, runSmokeServer + runSmokeClient
per loader folder, release jars in `<loader>/release/`, README/changelog/TESTING.md, CI template (runSmokeServer in CI),
dependency floors = tested versions (B5), build-all.ps1 --no-daemon. Details and evidence: log.md.
Main: docs (PORTING, TESTING incl. "Parallel runs", RELEASING, ARCHITECTURE), templates/branch, scripts
(test-all-versions.ps1 parallel-safe with -SmokeTasks / -MergeReports, 37 offline tests), research in
.knowledge/r4-backports.md + r5-forward.md.

## Paused (work exists, not finished)
1. **mc/1.16.5** (LOCAL ONLY, not pushed): commits up to aebcac3 - Java 8 downgrade (3a1f48d), 1.17.1 merged, main code
   compiles, server test runner for Forge 36 + Fabric (no GameTest API), smoke servers pass (fabric 3/3, forge 9/9 incl.
   sb.*), 17/17 fabric server tests, H5 GUI checks ported. Plus 6 uncommitted files (README, TESTING, changelog,
   fabric/build.gradle, fabric.mod.json, deleted structures/smoketest_empty.nbt) - backup:
   `local/handoff-2026-09-25/1.16.5-uncommitted.patch`. Missing: runSmokeClient both loaders (incl. H5 checks),
   screenshots, 1.16.4 verdict (SB 1.16.4 = 3.0.0.289; runtime test of the release jars on 1.16.4; forge-1.16.4/ if
   needed), real Forge 36 server with SB, CI=true build, jar metadata, release jars, fresh copy, sync-branch-infra
   (-GradleJdk 17 if FG6/Gradle 8.4), push.
2. **mc/1.16.3** (LOCAL ONLY): commits up to 2162936 (main compiles, SB fixture without Tool Swapper, modlauncher
   8.1.3 for dev runs, smoke servers pass: forge 9 incl. 6 sb.* (tool swapper skipped), fabric 3; GameTests 17/17;
   docs). Uncommitted: rebuilt 1.16.3 release jars (1.17.1 jars removed) - backup patch in local/handoff-2026-09-25/.
   Missing: merge final mc/1.16.5, runSmokeClient both loaders, real Forge 34 server with SB, CI=true, metadata,
   fresh copy, sync-branch-infra, push.
3. **H5 GUI smoke checks** (reference done on mc/1.21.1 eae592e: client.randomizer_bag_screens,
   client.player_settings_gui, client.modifier_entry_widgets, sb.upgrade_settings_tab). Port started on 13 branches, all
   UNCOMMITTED and UNVERIFIED in their worktrees (backups `local/handoff-2026-09-25/<ver>-uncommitted.patch`):
   1.21.4, 1.21.5, 1.21.8 (H5b: was waiting for the window lock), 1.21.10, 1.21.11, 26.1.2, 26.2 (H5c: was compiling
   26.2), 1.20.4, 1.20.1, 1.19.2 (H5d: was editing the 1.19.2 fixture), 1.18.2, 1.18.1 incl. forge-1.18, 1.17.1 (H5e:
   was editing the 1.17.1 fixture). Per branch still needed: compile smoketest source sets, runSmokeClient per loader
   folder (window lock), inspect screenshots, TESTING.md table, commit, push, CI. On old branches these checks are the
   first runtime test of the GUI shims (fix real src/main bugs minimally, report them).

## Missing (not started)
- Final all-versions run: `pwsh scripts/test-all-versions.ps1` with the parallel recipe in docs/TESTING.md ("Parallel
  runs": 3 headless instances on disjoint -Mc lists + one client instance + -MergeReports); results into report.md.
- README support matrix on main (all ranges above, incl. forge-1.21 / forge-1.19 / forge-1.18 folders, 26.1-26.1.2,
  Fabric 1.19-1.19.2 and 1.18-1.18.1, 1.20.1 Forge jar on NeoForge) + main CHANGELOG.md; PORTING.md toolchain rows from
  the ports (MDG Legacy 2.0.147 works for Forge 1.17.1-1.20.1 incl. 1.18/1.18.2/1.19; FG6 for 1.16.x; Forge 38 has no
  gametest; Forge 1.20.4 needs FG6, MDG Legacy fails; Fabric API < ~0.77 uses mod id "fabric").
- Check forge-1.21/ and forge-1.19/ override-skip: forge-1.18 found a configure-time skip compiles duplicates when the
  Gradle configuration cache is on (forge-1.18 skips at copy time).
- graphify incremental refresh (memory: never plain `graphify update .`; a separate "Graphify rebuild" session ran
  earlier - check graphify-out state first), `.knowledge/report.md`, memory update, push everything.

## Findings for the user (not changed; ask before fixing)
- PlayerSettingsGui is an unfinished stub from the original code (buttons/slider no-op, save TODO, no key opens it).
- Randomizer bag titles are wider than their GUI texture (e.g. "...Golden Randomizer Bag" overflows) - original code.
- 6 dead widget classes in gui/elements (GuiCheckBoxFixed, GuiNumberField, GuiIconButton, GuiScrollPane,
  GuiCollapsibleScrollEntry, SlotGui); 1.21.10+ already deleted 3.
- 1.17.1/1.18: upgrade items listed in our creative tab via an override - not checked in-game.
- NeoForge smoke server skips sb.worn_backpack on Curios 12+ (fake player has no Curios slot); client run covers it.

## Bugs fixed this session (all pushed)
NeoForge 1.20.4 singleplayer builds placed nothing (in-memory payload objects); Fabric held-slot resend + Forge
pack.mcmeta on all branches; Forge 1.21.5 pack formats; Fabric 1.19.2/1.18.x client without SB crashed (access
widener); Forge 26.1/26.1.1 AbstractMethodError; Fabric 1.21.1 minecraft range allowed 1.21.2+, fabric-api floor
0.108.0; MDG Legacy CI signature failure (B4); BackpackScanCompat logs the LinkageError cause (B3); build-all.ps1
without --no-daemon; dependency floors (B5).

## Prompt for the next session
```
You are Opus 5.5, lead developer (plan, delegate to subagents, review, never write code yourself; you may edit
.knowledge/, run builds/tests/git/graphify). Repo: C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated
(public GitHub navrelis/EffortlessBuildingSophisticated). First read .knowledge/handoff.md, requirements.md,
decisions.md, plan.md, port-brief.md (all its rules sections), then docs/TESTING.md on main. Worktrees are in
versions/ (run `pwsh scripts/setup-worktrees.ps1` only if missing; mc/1.16.5 and mc/1.16.3 are LOCAL ONLY - never
recreate those worktrees from origin). Uncommitted H5 work sits in 13 worktrees (backups in
local/handoff-2026-09-25/). Do not ask the user the scope questions again. Up to 10 parallel agents, none may spawn
its own agents. Continue in this order, committing and pushing each accepted task:
1. Finish mc/1.16.5 (client smoke, 1.16.4 verdict, real server, release, push) and then mc/1.16.3 (merge 1.16.5,
   client smoke, real server, release, push) - two agents, in parallel where possible.
2. Finish the H5 GUI checks on the 13 branches listed in the handoff (verify, commit, push, CI) - 3-4 agents.
3. Check the forge-1.21/forge-1.19 override skip vs configuration cache; update README matrix, CHANGELOG, PORTING.md.
4. Final all-versions run with scripts/test-all-versions.ps1 (parallel recipe in docs/TESTING.md; clients one at a
   time, muted, second monitor), report in .knowledge/report.md, graphify incremental refresh, memory, push.
Also show the user the "Findings for the user" list and ask which to fix.
Rules the user insisted on: test that the mod WORKS in-game incl. Sophisticated Backpacks, not just builds; every game
window muted and on the second monitor; as few game windows as possible (one at a time); agents use --no-daemon and
never `gradlew --stop`.
```
