# 01 – Repository overview

## What this is

"Sophisticated Building" (mod id `sophisticatedbuilding`, version 4.0.0) is a fork of
Effortless Building for Minecraft 1.21.1 with two loader projects that share the same Java
package layout (`sophisticated.building.*`):

| Directory | Loader | Notes |
|-----------|--------|-------|
| `Fabric-0.18.6-1.21.1/` | Fabric Loader 0.18.6, Fabric API 0.116.6, Loom 1.10.1, official Mojang mappings | Primary target of the current work. 190 Java files, ~20k lines. |
| `Neoforge-21.1.217-1.21.1/` | NeoForge 21.1.217 | 186 Java files. Kept in feature parity by hand. |
| `Fabric-0.19.2-1.21.11/` | Fabric for MC 1.21.11 | **git-ignored**, experimental port, not part of this work. Contains a few newer ideas (e.g. `FallbackPreviewRenderer`, keybinding categories) that are not in 1.21.1. |
| `DevInstance_Fabric/run/`, `DevInstance_NeoForge/run/` | – | Run directories used by the `clientExported` run config / the `run_dev_*_from_export.ps1` scripts. They load the *exported* jar from `run/mods` instead of the dev classpath. |
| `ExportedJars/` | – | Output of `rebuild_all_and_export_jar.ps1`. Jars are git-ignored, only patch notes are tracked. |
| `graphify-out/` | – | Knowledge graph of the repo (graph.json, graph.html, GRAPH_REPORT.md, manifest.json). |
| `ANALYSIS_AND_INSTRUCTIONS/` | – | This folder. |

Both projects are built by `rebuild_all_and_export_jar.ps1` (NeoForge `build`, Fabric `remapJar`),
which copies `sophisticatedbuilding-neoforge-<v>.jar` and `sophisticatedbuilding-fabric-<v>.jar`
plus `PATCH_NOTES_4.0.0.md` into `ExportedJars/`.

## Runtime architecture (identical on both loaders)

Client side (all under `sophisticated.building`):

- `ClientEvents` – keybindings (index 0 = radial menu, default Left Alt; 1 = modifier settings, Numpad +; 2/3 = undo/redo with Ctrl; 4/5 unbound), per-tick mouse handling, opens `RadialMenu`.
- `fabric.FabricClientEvents` – registers key mappings, menu screens, tick/render hooks. `ClientEvents.onGuiOpen()` is fired for **every** screen change and cancels the current build (`BUILDER_CHAIN.cancel()`), including when the radial menu opens.
- `systems.BuilderChain` – the core client loop: every tick determines the start position, runs the active build mode (`BuildModes` → `BuildModeEnum.instance`), modifiers (`BuildModifiers`), filters (`BuilderFilter`), computes new block states (`BlockEntry.setItemAndFindNewBlockState` via `MyPlaceContext`), and marks entries `invalid` when `ItemUsageTracker.increaseUsageCount()` says the player lacks items. On the placing click it sends `ServerPlaceBlocksPacket(BlockSet, placeTime)`. **`BlockSet.encode` drops every entry flagged `invalid`**, so the client's item-availability check is authoritative for what reaches the server.
- `systems.ItemUsageTracker.increaseUsageCount()` → `utilities.InventoryHelper.findTotalItemsInInventory(player, item)`.
- `render.BlockPreviews`, `render.RenderHandler`, `create.*` – ghost block previews through the embedded Create/Ponder/Flywheel stack (Catnip). Optional at runtime (`CompatHelper.isCatnipLoaded()`).
- `gui.buildmode.RadialMenu` – the Alt-key wheel. `gui.buildmode.PlayerSettingsGui` – unfinished.
- `client.ClientBackpackItemCache` – per-item counts of blocks inside backpacks, fed by `BackpackItemCountPacket` from the server.

Server side:

- `systems.ServerBlockPlacer` – validates the received `BlockSet`, delays placement to `placeTime`, applies entries, then `InventoryHelper.removeFromInventory(player, ITEM_USAGE_TRACKER.placed)` which prefers backpacks first (`removeFromBackpacks`).
- `fabric.FabricCommonEvents` – server tick sync of backpack counts for the held block/randomizer bag templates (every 10 ticks, only when changed), join/respawn handling, power level packets.
- `attachment.AttachmentHandler` / `PowerLevel` – survival progression limits (reach, blocks per axis, blocks per click, mirror radius) from `CommonConfig`.

Sophisticated Backpacks integration (optional, reflection-guarded so the mod loads without SB):

- `SophisticatedBuilding.createBuildingUpgrade()` creates `item.upgrade.BuildingUpgradeItem` (a `UpgradeItemBase`) when SB is loaded, otherwise a plain placeholder `Item`.
- `integration.SophisticatedBackpacksIntegration.registerUpgradeContainers()` registers `gui.BuildingUpgradeContainer` for the 5 upgrade item ids in `UpgradeContainerRegistry` (called on `SERVER_STARTED` and `CLIENT_STARTED`).
- `integration.SophisticatedBackpacksClientIntegration.registerUpgradeTab()` registers `client.gui.BuildingUpgradeSettingsTab` (a tab with one Enabled/Disabled toggle).
- `item.upgrade.BuildingUpgradeWrapper` – the per-installed-upgrade wrapper (tier, max blocks, extract/count from the backpack inventory).
- `item.upgrade.BuildingUpgradeHelper` – finds wrappers on a player (`findBestBuildingUpgrade`, `findAllBuildingUpgrades`), extracts blocks, counts, syncs counts to the client.
- Data: `data/sophisticatedbackpacks/tags/item/upgrade.json` adds the 5 items to the `sophisticatedbackpacks:upgrade` tag (required by SB's upgrade slot validation).

## Build configuration facts (Fabric project)

`build.gradle` dependency summary:

- Create rendering stack embedded (`include`) unless `-PembedCreateDeps=false`: Flywheel 1.0.6, Ponder 1.0.81 (+ Vanillin, Catnip transitively), `teamreborn:energy:4.1.0` as local runtime.
- Sophisticated deps come from CurseMaven as `modCompileOnly` (or `modImplementation` with `-PuseCurseRuntimeDeps=true`). **The Backpacks coordinate `979322:7147929` is the 1.20.1 file (`sophisticatedbackpacks-1.20.1-3.23.4.5.110.jar`), not 1.21.1. Core is not declared at all.** Compilation currently only succeeds because `compileOnly(fileTree("other_mods"))` was pointing at *Loom-remapped ("named") copies* of the 1.21.1 jars that someone copied back into `other_mods/`.
- `other_mods/` (git-ignored, `*.jar` ignored): as of this session it contains the **real production (intermediary) CurseForge jars** of Core/Backpacks/Storage 1.21.1 (see `02_…`). The previous named copies were moved to `other_mods/_named_dev_copies_old/` and can be deleted. Plain `compileOnly(files(...))` cannot consume production jars (they reference `net.minecraft.class_XXXX`); they must go through a `mod*` configuration so Loom remaps them.
- `gradle.properties` still lists `sophisticatedcore_version=1.21.1-1.0.6.1132` and `sophisticatedbackpacks_version=1.21.1-3.22.9.1161` (old NeoForge versions). Nothing reads them.
- Run configs: `client`, `server`, `clientExported` (runDir `../DevInstance_Fabric/run`, uses an empty `exportedStub` source set so only jars in `run/mods` are loaded).
- Mixins: `sophisticatedbuilding.mixins.json` is empty (no mixins on Fabric 1.21.1).

NeoForge project: Sophisticated deps resolved from Modrinth Maven (`maven.modrinth:sophisticated-core:1.21.1-1.4.38.1847`, `sophisticated-backpacks:1.21.1-3.25.44.1736`) as `compileOnly`, optional `other_mods/` override (folder currently absent).

## Local evidence sources used in the analysis

- Dev run logs: `Fabric-0.18.6-1.21.1/run/logs/` (April, ran with SB 3.23.4.3.106 + SC 1.2.9.21.168 + SS 1.3.7.9.139 and mod version 3.2) and `DevInstance_Fabric/run/logs/` (May, ran **without** SB, so no integration evidence there).
- The user's real CurseForge instances `C:\Users\nikol\curseforge\minecraft\Instances\Nytheria` and `…\NytheriaDevelopment` run exactly the jar set from the screenshot plus `sophisticatedbuilding-fabric-4.0.0.jar`, together with **Trinkets 3.10.0, Accessories 1.1.0-beta.53 and the Accessories compat layer**. Their `latest.log` shows `Registered Sophisticated Backpacks upgrade containers` and no error from this mod. Only INFO level is logged there, so the `logger.debug` messages in `BuildingUpgradeHelper` are invisible.

## Git

- Only 3 commits on `main`; a `backup-before-reupload-2026-04-23` branch exists.
- Tracked: both loader projects, scripts, LICENSE, PATCH_NOTES_4.0.0.md, `ExportedJars/.gitkeep`. Not tracked: every other `*.md` (README.md, DOCUMENTATION.md, docs/), all jars, `net/`, the 1.21.11 project, run dirs.
