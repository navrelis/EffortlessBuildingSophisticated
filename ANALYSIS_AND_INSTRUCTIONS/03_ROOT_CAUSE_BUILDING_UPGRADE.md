# 03 – Root cause: Building Upgrade has no effect with the current Sophisticated ports

Symptom (user report): with SB 3.23.4.3.106 / SC 1.2.9.21.168 / SS 1.3.7.9.139 the Building
Upgrade items can still be inserted into a backpack and the switch can be turned on, but placing
blocks from the backpack does not work.

What is confirmed to work (from the user's real logs and the upstream code):
- Items register as `BuildingUpgradeItem` and are insertable (upstream `isItemValid` = `instanceof IUpgradeItem` + tag `sophisticatedbackpacks:upgrade`, both satisfied).
- `Registered Sophisticated Backpacks upgrade containers` is logged; no exception from this mod.
- Nothing in this mod's Fabric integration classes throws at runtime; all API signatures used still exist in the new jars.

So the failure is logical, not a crash. Four independent causes were found. Any single one of
RC1/RC2 is enough to produce the reported symptom; RC3 makes "turning it on" unreliable; RC4 is a
build-configuration hazard that hides API drift.

## RC1 – Worn backpacks are never found (Fabric `CuriosCompatHelper` is a stub)

`item.upgrade.BuildingUpgradeHelper.findBestBuildingUpgrade / findAllBuildingUpgrades` scan only
`player.getInventory()` and then call `CuriosCompatHelper.getBackpacksFromCurios(player)`, which on
Fabric is hard-coded to `return List.of();` (`compatibility/CuriosCompatHelper.java:23-25`).

The user's modpack (Nytheria) runs Trinkets 3.10.0 and Accessories with its compat layer. The
Backpacks port registers a `"trinkets"` inventory handler in `PlayerInventoryProvider`; a backpack
worn in the back slot lives in the Trinkets component, not in `player.getInventory()`. For such a
player every helper method returns "no upgrade" on the server: max blocks = 0, counts = 0,
extraction = 0. Holding the backpack in the hotbar/inventory would work, wearing it would not.

Fix: enumerate backpacks with `PlayerInventoryProvider.get().runOnBackpacks(...)` (covers main,
offhand, armor chest slot, Trinkets and Curios handlers automatically). Delete the stub path.

## RC2 – The client validates block availability by introspecting the backpack locally

Flow: `BuilderChain.findNewBlockStates` → `ItemUsageTracker.increaseUsageCount` →
`InventoryHelper.findTotalItemsInInventory(player, item)` **on the client**. That method (Fabric
version, `utilities/InventoryHelper.java:107-136` and `getReservedHeldCount`) calls
`BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer` and `countBlockInBackpacksForDisplay`,
which construct `BackpackWrapper.fromStack(stack)` **on the client** and read its `UpgradeHandler`.

On the client that handler is built from `BackpackStorage.clientStorageCopy`, which only ever
receives `settings` (when a backpack GUI opens) or `inventory`/`upgradeInventory` when the
tooltip requests it. The wrapper cache (`StorageWrapperRepository`) is keyed by ItemStack
*identity*; the server re-sends the backpack slot after every component change (open tab id,
settings, UUID creation …), giving the client a new stack instance and therefore a fresh wrapper
with an empty upgrade handler. Result on the client: `maxFromUpgrade == 0`, backpack blocks are
not added to `have`, every block beyond what is physically in the player inventory is flagged
`invalid`, `BlockSet.encode` filters those entries out, and the server never even sees them.
The HUD (`RenderHandler.drawRandomizerBagHUD` → `findTotalItemsForDisplay` → `ClientBackpackItemCache`)
still shows the server-synced counts, which makes the feature look "on but doing nothing".

This code path only works by accident when the client wrapper for the exact same stack instance
was populated by an open backpack menu and no slot re-sync happened since. Older port versions
kept a client-side copy of the full contents; the current port does not.

Fix: the client must never inspect `BackpackWrapper`. The server is authoritative and syncs
(a) the effective upgrade state (tier, max blocks) and (b) per-item backpack counts; the client
uses only synced values (`ClientBackpackItemCache` + a new client upgrade-state holder).

## RC3 – `enabled` is stored differently from Core and never refreshes Core's type cache

`BuildingUpgradeWrapper implements IUpgradeWrapper` directly, keeps `enabled` in a field
initialised from `DataComponents.CUSTOM_DATA` tag `"enabled"`, and `setEnabled` writes the tag and
calls the save handler only. Core's `UpgradeWrapperBase.setEnabled` stores
`ModCoreDataComponents.ENABLED` **and calls `refreshWrappersThatImplementAndTypeWrappers()`**.
`UpgradeHandler.getTypeWrappers(TYPE)` only contains wrappers that were enabled when its cache was
last built, so after our wrapper is switched off and on again, `getTypeWrappers` (which the helper
uses) can keep returning an empty list until an unrelated slot change rebuilds the cache. The
server menu wrapper and the wrapper used by the placement code are the same cached object, so the
stale cache is exactly what the placement code sees. Additionally Core's own per-slot switch
(`StorageScreenBase` → `setUpgradeEnabled`) and our tab toggle both drive our `setEnabled`, but the
Core switch displays `isEnabled()` from a wrapper that may have been constructed from a stack copy,
so on/off can look inconsistent.

Fix: extend `UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem>` and drop the custom
enabled storage. Behaviour change to document: previously-disabled Building Upgrades come back
enabled after the update (the old NBT flag is ignored).

## RC4 – Build configuration hides API drift

- `build.gradle` (Fabric) compiles against CurseMaven file `979322:7147929`, which is the
  **1.20.1** Backpacks build (`sophisticatedbackpacks-1.20.1-3.23.4.5.110.jar`), declares no Core
  at all, and adds `compileOnly(fileTree("other_mods"))` which only worked because Loom-remapped
  ("named") copies had been placed there. Any real CurseForge jar dropped into `other_mods/` would
  make compilation fail or silently compile against the wrong classes.
- Correct 1.21.1 coordinates: core `curse.maven:sophisticated-core-unofficial-fabric-port-979317:7344653`,
  backpacks `curse.maven:sophisticated-backpacks-unofficial-fabric-port-979322:6844426`,
  storage `curse.maven:sophisticated-storage-unofficial-fabric-port-979326:7344673`.
- The production jars are now in `other_mods/`; they must be consumed through `modLocalRuntime(files(...))` /
  `modCompileOnly(files(...))` so Loom remaps them.

## Secondary observations (not the cause, but worth fixing while there)

- Count sync is inconsistent: `syncItemCount` and the periodic sync send the tier-clamped count,
  `extractBlockFromBackpack` sends the unclamped total. The client HUD therefore flips between two
  numbers. Decision: always send the unclamped total; the client clamps with the synced max.
- `ClientBackpackItemCache.clear()` is never called; stale counts survive a world change.
- The per-tick server sync (`FabricCommonEvents.syncPeriodicBackpackCounts`) only covers the held
  block/randomizer templates; the new upgrade-state packet must be sent there too when it changes,
  and on join/respawn/dimension change.
- NeoForge `InventoryHelper` deliberately does **not** clamp backpack contributions ("tier only
  gates access"), Fabric clamps to `maxBlocks` and additionally reserves one held block as the build
  anchor. The item tooltip promises "Place up to %d blocks at once from backpack inventory", so the
  Fabric clamping semantics are the documented ones. Keep them on Fabric; do not change NeoForge
  gameplay semantics in this round beyond what RC1–RC3 require.
