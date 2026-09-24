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
