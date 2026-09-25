# Changelog

One section per mod version. Every version before 4.3.0 shipped only for Minecraft 1.21.1
(NeoForge + Fabric); full unabridged patch notes for those live in `docs/history/PATCH_NOTES_*.md`.
Starting with 4.3.0, the mod is multi-version: each Minecraft version lives on its own `mc/<version>`
branch with its own `changelog/PATCH_NOTES_<version>.md`, which lists what differs on that Minecraft
version. This file summarises them; `README.md`'s support matrix lists every jar with its Minecraft
range, minimum loader version and Sophisticated Backpacks support.

## 4.3.0 — multi-version

4.3.0 brings the mod to every Minecraft version from 1.17.1 to 26.2 that has a Sophisticated Backpacks
release, on Fabric, NeoForge and Forge where the loader exists for that version (Minecraft 1.16.3 and
1.16.5 are still in progress). The features are those of 4.2.1 on every version; where an older or newer
Minecraft lacks an API, the branch uses the closest equivalent and its patch notes say so (for example
item data in NBT before 1.20.5, vanilla tool classes instead of tool item tags on 1.19, no in-game config
screen on Forge and on NeoForge 20.4).

### Minecraft versions and loaders

* 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.2: Fabric, NeoForge and Forge. Sophisticated Backpacks
  integration on NeoForge.
* 26.1, 26.1.1, 26.1.2 (branch `mc/26.1.2`): one Fabric jar and one Forge jar for all three; NeoForge for
  26.1.2 only, because NeoForge 26.1 and 26.1.1 were only released as betas. Backpacks integration on
  NeoForge.
* 1.21 and 1.21.1 (branch `mc/1.21.1`): the Fabric and NeoForge jars now also run on 1.21. Forge is new:
  one jar for 1.21.1 and a separate jar for 1.21 (`forge-1.21/`), because Forge 51 cannot load the 1.21.1
  jar. Backpacks integration on NeoForge (1.21 and 1.21.1) and Fabric (1.21.1; the Fabric port has no 1.21
  build).
* 1.20.4: Fabric, NeoForge and Forge. Backpacks integration on Fabric and NeoForge.
* 1.20.1: Fabric and Forge; the Forge jar also runs on NeoForge 1.20.1 (server smoke test on 1.20.1-47.1.106), so
  there is no separate NeoForge jar. Backpacks integration on both.
* 1.19, 1.19.1, 1.19.2: one Fabric jar for all three; Forge has a jar for 1.19.2 and one for 1.19 and
  1.19.1 (`forge-1.19/`), because the Backpacks/Core builds for 1.19 and 1.19.1 have an older API.
  Backpacks integration on Forge and on Fabric 1.19.2 (the Fabric port exists for 1.19.2 only).
* 1.18.2: Fabric and Forge. Backpacks integration on Forge.
* 1.18 and 1.18.1: one Fabric jar for both; Forge has a jar for 1.18.1 and one for 1.18 (`forge-1.18/`),
  because Sophisticated Backpacks for 1.18 keeps everything in its own package (no Sophisticated Core, other
  class names). Backpacks integration on Forge.
* 1.17.1: Fabric and Forge. Backpacks integration on Forge.

Where Sophisticated Backpacks has no build for a loader and Minecraft version, the jar has no backpack
integration: the Building Upgrade items are placeholders without recipes, and everything else works as
usual. Jar names now include the Minecraft version: `sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`
(before: `sophisticatedbuilding-<loader>-<version>.jar`). Declared minimum loader, Fabric API and
Sophisticated Backpacks/Core versions are the versions the jar was run with, not wildcards.

### Changes on Minecraft 1.21.1 compared with 4.2.1

* Loader-neutral code and assets are compiled once from a shared `common/` source tree into every
  loader's jar; jars are about 0.9 MB instead of about 3 MB.
* The mod no longer bundles Flywheel/Ponder. The rendering helpers for ghost block previews and
  outlines are included directly (Catnip, MIT, attribution in the jar); there is no dependency on
  Flywheel, Ponder, Catnip or Create any more.
* Fabric: the four decompress recipes use the ids `decompress_compressed_*` (a recipe book unlock of the
  old ids is reset; the recipes craft the same).
* NeoForge: the Building Upgrade state and the backpack tool list are re-sent to the client every 10
  ticks, not only while holding a building block or a Building Upgrade backpack. The Omega bag and
  randomizer bag menus close as soon as the bag leaves both hands, as on Fabric.
* Backpack count packets ignore unknown items instead of failing, on both loaders.
* Fabric: Minecraft 1.21 or 1.21.1 (4.2.1 declared 1.21.1 and every later 1.21.x, never tested there);
  Fabric API 0.108.0 or newer is required (older builds lack `ClientWorldEvents` and the client crashed at
  start); built against Fabric Loader 0.19.5, still requires 0.18.6.
* NeoForge: Minecraft 1.21 or 1.21.1, NeoForge 21.0.167 or newer; Sophisticated Backpacks 3.20.26 / Core
  0.7.13 or newer (the last 1.21 builds) are accepted.

### Fixes

Bugs that 4.2.1 (Minecraft 1.21.1) had, fixed on every branch:

* Fabric: a build-mode click that cancels the vanilla block placement now resends the held slot to the
  client. Before, a player holding exactly the blocks a build needed (for example one block plus a
  backpack with a Building Upgrade) lost the held block on the client side and the build was cancelled.
* The Omega bag screen no longer allocates a native buffer per weight badge per frame.
* When the Sophisticated Backpacks scan cannot link against the installed Backpacks build, the warning
  now includes the cause.

Found while porting, fixed before any release of the affected jar:

* Forge: the mod's data pack was listed as incompatible (`pack.mcmeta` without the right
  `supported_formats`); every Forge jar now declares the pack formats of its Minecraft version
  (1.21.5 needed its own).
* NeoForge 1.20.4: in singleplayer, multi-block builds placed nothing. NeoForge 20.2-20.4 passes payload
  objects through the in-memory connection without encoding them, so the server received the client's
  live block set, which the client cleared on its next tick; the mod now sends a decoded copy.
* Fabric 1.19.2: the client crashed without Sophisticated Backpacks installed
  (`IllegalAccessError` on `RenderType.create`; Fabric API 0.77 does not widen it); an
  access widener opens it, as on the Fabric jars for 1.17.1 to 1.18.2.
* Forge 26.1 and 26.1.1: `AbstractMethodError` at runtime; the 26.1.2 Forge jar now runs on all three.

### Build, CI and testing

* `main` is now the hub (docs, this changelog, upstream-jar manifest, scripts, `templates/branch/`); game
  code lives on the `mc/<version>` branches, each with one standalone Gradle build per loader folder (see
  `docs/ARCHITECTURE.md` and `docs/PORTING.md`).
* Every branch has the same CI workflow (GitHub Actions: build, unit tests, Fabric GameTests, headless
  smoke server), `build-all.ps1` (always `--no-daemon`) and `release.ps1`, synced from `templates/branch/`.
* In-game smoke test harness on every branch and loader (`runSmokeServer`, `runSmokeClient`): building,
  undo/redo, survival rules and, where Sophisticated Backpacks exists, `sb.*` checks with real backpacks
  (supply from the Building Upgrade, disabled upgrade, tier cap, HUD count sync, Tool Swapper, worn
  backpack). `mc/1.21.1` also checks the randomizer bag screens, the player settings screen and the
  Backpacks upgrade settings tab. Dev-only; never in the release jars.
* `scripts/test-all-versions.ps1` runs build, unit tests, GameTests, servers and the smoke harness for
  every branch and loader; several instances can run in parallel (per-version locks, one shared game
  window lock, `-SmokeTasks`, `-MergeReports`). See `docs/TESTING.md`.
* ModDevGradle Legacy builds (Forge 1.17.1-1.20.1) always recompile Minecraft, also on CI: without it,
  unit tests failed on CI only with a signature error for Forge's signed classes.

## 4.2.1

* Undo/redo now costs and restores the real number of items a block is made of (double slabs,
  stacked candles/sea pickles/turtle eggs/snow layers/pink petals) instead of always charging one
  item, which previously let undo duplicate items.
* Failed undo/redo entries stay on the stack instead of being dropped, and are retried on the next
  undo/redo.
* Same-block merges (slab → double slab, stacking candles/etc.) are limited to blocks that are
  actually meant to stack that way; blocks with other state (crop growth, composter fill,
  respawn-anchor charge, ...) can no longer be advanced for free by building over them.
* Disable mode no longer double-processes a normal vanilla click; mirror/array copies and Quick
  Replace still go through the mod as before.
* Build-mode placing/breaking (including mirror/array copies) now respects spawn protection, the
  world border, and adventure-mode rules in every game mode.
* NeoForge: the Building Upgrade now caps supply per build at its tier's block count (matching
  Fabric) and keeps the last block in hand instead of consuming it while the backpack still has
  more; the tooltip explaining the cap is back.
* Fabric: a broken config file (out-of-range value, wrong type, unknown key) is now corrected
  automatically, with the original kept as a `.bak` file, instead of re-warning on every startup.
* Randomizer bag builds no longer strip the name/custom data from the remaining stack in your
  inventory.

## 4.2.0

* **Survival replace** (off by default, `SurvivalReplace.enabled`): the radial-menu replace modes
  and Quick Replace now work in Survival, using the same survival-breaking rules as mass breaking
  (best available tool, durability, drops, hunger, protected/unbreakable-block skipping, mining
  delay). Undo/redo of a survival replace never creates blocks or items for free.
* **Real, file-backed config on Fabric** (`config/sophisticatedbuilding-{common,server,client}.json`),
  matching the NeoForge config's option names/ranges; missing keys are backfilled, out-of-range
  values clamped, and a broken file is left untouched with defaults used instead.
* Storage blocks (shulker boxes, Supplementaries sacks, ...) placed via a build mode now keep their
  contents and custom name instead of being placed empty (fix for #4).
* Build-mode placement preview is hardened against other mods' `getStateForPlacement` throwing for
  reasons other than a null player: that one block is skipped in the preview instead of crashing.
* Building Upgrade clarified (only an *enabled Building Upgrade* feeds a build, not Refill) and
  follow-up fixes: a Fabric bug where the last block of a held item could get stuck un-placeable;
  a NeoForge bug double-counting backpacks worn in Curios slots; a misleading NeoForge tooltip
  promising a per-use cap it doesn't enforce.
* The survival server now refuses to silently overwrite a non-replaceable block without mining it
  first. Undoing a survival break no longer throws and correctly refunds the block's item.

## 4.1.1 (hotfix)

Fixed a NeoForge server crash on login (`NoSuchMethodError` in
`PlayerInventoryProvider.runOnBackpacks`) when running against Sophisticated Backpacks 3.26.0+,
which changed that method's return type from `void` to `boolean` — a binary-incompatible change.
The mod now calls it through a reflection/`MethodHandle` shim (`BackpackScanCompat`) that works
against both old and new signatures, and every Sophisticated Backpacks/Core call site now catches
`LinkageError` in addition to `Exception`, so a future upstream break degrades to "no Building
Upgrade found" instead of crashing the server. Also removed a noisy harmless NeoForge startup
warning about a missing mixin refmap, and stale loot-modifier data for items that don't exist in
this fork. See `docs/history/PATCH_NOTES_4.1.1.md` and `docs/history/ANALYSIS_AND_INSTRUCTIONS/10_UPSTREAM_API_BREAK_4.1.1.md`
for the full root-cause record.

## 4.1.0

* **Survival mass breaking** (Fabric + NeoForge): bulk breaking now works in Survival, not just
  Creative, using vanilla-matching rules — a suitable tool (main hand, hotbar, inventory, offhand,
  or an enabled Tool Swapper/Advanced Tool Swapper backpack upgrade), vanilla durability/drops/
  hunger cost, correct-tool-for-drops enforcement, tools never broken by the mod, unbreakable
  blocks always skipped, hardness-0 blocks broken free-handed, and a scaling mining delay with an
  on-screen countdown/HUD. Switch to Disable mode for plain vanilla mining/placing.
* Disable mode now re-syncs to the server on every world join, fixing it getting stuck blocking
  vanilla placing/breaking after a rejoin.
* **The Building Upgrade actually works now**: worn backpacks (Trinkets/Accessories/Curios) are
  found via `PlayerInventoryProvider.runOnBackpacks`; the server is now authoritative for upgrade
  state and backpack counts via a new `BuildingUpgradeStatePacket` instead of the client
  inspecting backpack inventories directly; `BuildingUpgradeWrapper` now extends Sophisticated
  Core's `UpgradeWrapperBase` so enabling/disabling the upgrade is reliable (migration note: a
  previously-disabled Building Upgrade comes back enabled after this update).
* Radial build menu: a rebound menu key now works (no more hard-coded Left Alt fallback), and
  opening the menu no longer discards an in-progress multi-click build.
* Line Thickness (1/3/5 blocks) for Line/Diagonal Line, and Diagonal Wall Hollow/Filled, are now
  implemented (previously advertised with UI/icons but no effect).
* Fabric now compiles against the correct CurseMaven coordinates for all three Sophisticated
  1.21.1 ports, fixing a dev-runtime `NoClassDefFoundError` for porting_lib classes.
* New server config keys under `ServerConfig.survivalBreaking` (`enabled`, `stopBeforeToolBreaks`,
  `maxDelayTicks`, `exhaustionPerBlock`); NeoForge only — Fabric config loading wasn't implemented
  yet at this version, so these were in-memory defaults there.

## 4.0.0

* Fixed a `BlockPlaceContext.getPlayer()` null crash during client-side placement preview (some
  mods, e.g. Create Simulated's redstone magnet, assume a non-null player in
  `getStateForPlacement`); `MyPlaceContext` now carries the real player through.
* Fixed the radial build menu (Alt key) treating the cursor as stuck at the screen center after
  opening; it now seeds from the first frame's mouse position and accumulates movement deltas.
