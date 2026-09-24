# Changelog

One section per mod version. Every version before 4.3.0 shipped only for Minecraft 1.21.1
(NeoForge + Fabric); full unabridged patch notes for those live in `docs/history/PATCH_NOTES_*.md`.
Starting with 4.3.0, the mod is multi-version: each Minecraft version lives on its own `mc/<version>`
branch, with its own `changelog/` and its own jar version where the branches diverge — see that
branch's `changelog/` for version-specific detail beyond what's summarised here, and
`README.md`'s support matrix for what's released, in progress, or planned.

## 4.3.0 — multi-version restructuring

`main` became the multi-version **hub**: it no longer holds game code directly. The 1.21.1
NeoForge + Fabric code that used to live at the repository root moved to branch `mc/1.21.1` (with
full history), gained a `common/` + platform-services split (`sophisticated.building.platform`)
shared by every loader, and a `forge/` build was added alongside it. `main` now holds only the
docs, changelog, upstream-jar manifest, and scripts that tie the per-version branches together —
see `docs/ARCHITECTURE.md` for the common/platform-services split and `docs/PORTING.md` for how a
version branch is built and ported. No gameplay change in this version; it is purely the
repository restructuring that every later per-Minecraft-version release builds on.

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
