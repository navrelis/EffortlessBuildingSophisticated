# Port brief (applies to every version port of Sophisticated Building 4.3.0)

## Project layout (every version branch `mc/<version>`, checked out as worktree `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\versions\<version>`)
- `common/`: loader-neutral code, assets, data, unit tests. Loader access ONLY through `sophisticated.building.platform.Services` (IPlatformHelper, IBlockEventHelper, INetworkHelper, IConfigHelper, optional IBackpackIntegration with NONE fallback) and `ClientServices` (IClientHelper). Implementations registered in `<loader>/src/main/resources/META-INF/services/`.
- `fabric/`, `neoforge/`, `forge/`: each a STANDALONE Gradle build (own settings.gradle, build.gradle, gradle.properties, wrapper) compiling `../common/src/main/{java,resources}` and `../common/src/test/java`, loading `../gradle/shared.properties` (mod_id, mod_version, minecraft_version, minecraft_version_range, java_version, ...). Each build has `checkCommonIsLoaderNeutral` in `check`.
- Sophisticated Backpacks (SB) integration lives in the loader folder (`integration/*`, `item/upgrade/*`, `gui/BuildingUpgradeContainer`, `client/gui/BuildingUpgradeSettingsTab`, NeoForge `compatibility/CuriosCompatHelper`) and registers `IBackpackIntegration`. Where SB does not exist for a loader+version: delete those classes and the service file from that loader folder; the mod runs with NONE (placeholder upgrade items, recipes guarded by load conditions). All SB calls stay guarded by `Exception | LinkageError`.
- Jar names `sophisticatedbuilding-<loader>-<mc>-<version>.jar`; `build-all.ps1` builds every loader folder; `changelog/PATCH_NOTES_4.3.0.md`; `README.md` describes the branch.
- Reference implementation: branch `mc/1.21.1` (worktree `versions\1.21.1`, read-only for you). Fabric GameTests: `fabric/src/gametest` (17 tests, every class listed in `src/gametest/resources/fabric.mod.json`; `gradlew runGametest`).

## Research and inputs (read-only)
- Toolchain matrix, API break list, gotchas: `docs/PORTING.md` on main (the old session's scratchpad templates are gone; recreate minimal builds from the MDKs when needed).
- Upstream SB/Core jars + CurseMaven coordinates: `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\upstream\manifest.json` and `upstream\<loader>\<mc>\*.jar` (inspect APIs with javap).
- Old experimental Fabric 1.21.11 port (mod version 2.0.0, useful as evidence of 1.21.2-1.21.11 API changes): `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\local\reference\Fabric-0.19.2-1.21.11`.
- Hub docs: `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\docs\PORTING.md`, `docs\ARCHITECTURE.md`.

## Rules
- Work only inside your worktree. Do not commit or push (you may `git mv`). The lead reviews and commits.
- Behaviour/features identical to mc/1.21.1 wherever the game allows; no new features; the closest equivalent where an API is gone (list each). Keep code in common/ where possible. Match the existing style; no TODOs/debug output left.
- Build from a clean clone: dependencies only from public Mavens (CurseMaven, Modrinth, loader mavens); no local jar trees.
- Declared minimum loader versions = the loader version you actually ran; minecraft range = only versions you actually ran (or state why a wider range is safe, e.g. vanilla-identical and tested).
- Update the branch README (versions, differences to 1.21.1) and changelog (header with this MC version; artifacts; drop statements that do not apply).
- Never run two ForgeGradle 7 builds in parallel on a cold cache. Other agents run Gradle in OTHER folders at the same time: ALWAYS pass `--no-daemon` to every gradlew call, and NEVER run `gradlew --stop` (it kills every daemon of that Gradle version, including other agents' builds). Kill only Java PIDs you started yourself.

- Every Minecraft client you start must be MUTED and on the SECOND monitor (user request): put `soundCategory_master:0.0` and `pauseOnLostFocus:false` into `<loader>/run/options.txt` before launch, note that `--quickPlaySingleplayer <name>` only opens an EXISTING save (a missing name shows "Failed to Quick Play" and hangs — never do that); on Forge, quick-playing into an existing world can crash with the upstream LootModifierManager bug, so for Forge create the world in-game or accept/rerun that known crash; launch as few clients as possible (the user watches every window), and after launch run `pwsh C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\scripts\move-game-window.ps1 -RunDir <loader>/run -GradlePid <pid>` in the background (moves only that game window, never others). Where the smoke harness exists, it does both itself.

## Definition of done (verify yourself, report exact output lines)
1. `<loader>/gradlew build` green for every loader; unit tests pass (report counts per loader; common tests 65, Fabric +12 ConfigSpecTest).
2. Fabric: `gradlew runGametest` -> "All N required tests passed" (N=17 unless a test is impossible on this version; explain).
3. `runServer` per loader: "Done (" reached; where SB exists for that loader the SB+Core dev runtime is present and "Registered Sophisticated Backpacks upgrade containers" is logged; no ERROR/exception from `sophisticatedbuilding`; stop.
4. `runClient` per loader with `--quickPlaySingleplayer "New World"` added temporarily: world joined, ~30 s without exceptions from the mod; a temporary check hook/screenshot that the ghost-block preview, outline and radial menu render (remove every temporary change afterwards). Kill the process.
5. Jar metadata correct (version 4.3.0, MC range, loader floors, SB/Core optional), no nested jars.
6. Fresh-copy build: copy the worktree without `.gradle`, `build`, `run*`, `logs` to a new temporary folder outside the repo, run `build-all.ps1` there -> green; delete the copy.

## Return format
1. Changes by area (common / each loader / tests / build) with key files.
2. Every behaviour difference to mc/1.21.1 and why.
3. Commands run with results (counts, exact lines).
4. Notes for the next port in the chain (what was hardest, what to watch).

## One game window at a time (all agents, session 2026-09-25)
Several agents work in parallel, but the user wants as few game windows as possible. Before you start ANY Minecraft
client (runClient, runSmokeClient, runGametest does not count), take the window lock
`C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated\local\game-window.lock`:
- If the file exists and its LastWriteTime is less than 20 minutes old, wait (poll every 30 s; do other work meanwhile).
  Older than 20 minutes = stale: delete it.
- Create it atomically: `New-Item -ItemType File -Path <lock> -Value "<your task id> <ISO time>" -ErrorAction Stop`
  (if that throws, someone else was faster: wait again).
- Delete it as soon as your client process has exited or you killed it (also on failure). Never hold it while you
  edit code. Dedicated servers and runSmokeServer need no lock (no window).

## Smoke harness adoption (H* tasks)
Reference = mc/1.21.1 commit 4354629 (worktree `versions\1.21.1`, read-only): `common/src/smoketest`,
`common/src/smoketestBackpacks`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`, the smoke wiring in each
`<loader>/build.gradle` (+ `fabric/gradle.properties`), branch `TESTING.md`, README testing section. `git show 4354629`
shows every file. Port it to your branch with the smallest possible API adaptations:
- `runSmokeClient` and `runSmokeServer` on every loader folder of the branch, same result contract, same check names.
- `sb.*` checks only on loaders that register `META-INF/services/...IBackpackIntegration`; the other loaders must not
  compile against SB (drop the smoketestBackpacks source set there, exactly like forge on 1.21.1).
- Keep the harness dev-only (never in the release jar: check the jar listing).
- Also port the two T1 production fixes from 4354629 if missing on your branch: Fabric `FabricCommonEvents` held-slot
  resend when a build click cancels vanilla placement; Forge `src/main/templates/pack.mcmeta` supported_formats
  (use this version's resource/data pack formats). Plus anything else 4354629 changed in `src/main`.
- Done = on every loader: `gradlew build` green (report test counts), `runSmokeServer` passes, `runSmokeClient` passes
  (report every check line; SB loaders must show passing `sb.*` checks), screenshots look right (open 2-3 PNGs and
  say what you see), release jar contains no smoketest classes. Every client under the window lock above.

## Agent limit (user, 2026-09-25)
At most 10 agents run in the whole session. Do NOT spawn your own subagents; do the work yourself, sequentially.

## Scratch files and process load (2026-09-25)
- All agents share one session scratchpad: always write into your own subfolder `<scratchpad>\<task id>\`, never into its root.
- The user saw "cmd.exe - Application Error 0xc0000142" popups (console processes failing to start when too many run).
  Keep process count low: one Gradle invocation at a time per agent, no background Gradle runs you do not wait for,
  and never leave servers/clients running after a check.

## ModDevGradle Legacy on CI (B4)
MDG Legacy skips recompiling Minecraft when the env var `CI=true` (GitHub Actions) and then keeps Forge's jar signature
on remapped classes -> unit tests fail on CI only ("SHA-256 digest error for ...IForgePlayer.class"). Every MDG Legacy
forge build must use `legacyForge { enable { forgeVersion = "..."; disableRecompilation = false } }` (see mc/1.20.1
forge/build.gradle) and be checked once with `CI=true` set locally.

## Dependency floors (B5)
fabric.mod.json: `"fabric-api": ">=${fabric_api_version_min}"` with `fabric_api_version_min` = the Fabric API version the
branch runs (see mc/1.21.8). NeoForge/Forge optional SB/Core `versionRange` = the builds the branch actually ran.
Never `"*"` or untested older floors.
