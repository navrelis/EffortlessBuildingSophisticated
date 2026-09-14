# Sophisticated Building Update – 4.1.0

## Supported versions

* Minecraft **1.21.1**
* **NeoForge** (see `gradle.properties` `neo_version` in the NeoForge project)
* **Fabric Loader** + Fabric API (see Fabric project `gradle.properties`)
* **Sophisticated Backpacks / Sophisticated Storage** — Fabric now compiles and runs against the
  real 1.21.1 unofficial Fabric ports (Sophisticated Core 1.2.9.21.168, Sophisticated Backpacks
  3.23.4.3.106, Sophisticated Storage 1.3.7.9.139); NeoForge continues to use the official Modrinth
  port (Sophisticated Core 1.4.38.1847, Sophisticated Backpacks 3.25.44.1736).

## Artifacts

* `sophisticatedbuilding-neoforge-<version>.jar` — NeoForge
* `sophisticatedbuilding-fabric-<version>.jar` — Fabric

## Major changes

### Building Upgrade actually works now (Fabric + NeoForge)

The Building Upgrade (installed as a Sophisticated Backpacks upgrade item) previously had no
effect: blocks could not be placed from the backpack even with the upgrade installed and enabled.
Root-caused and fixed:

* **Worn backpacks are now found.** Backpacks worn in a Trinkets/Accessories/Curios slot (not just
  held or in the main inventory) are now scanned via Sophisticated Backpacks'
  `PlayerInventoryProvider.runOnBackpacks`, which already covers main inventory, offhand, the worn
  chest slot, and any compat layer (Trinkets/Curios) the backpacks mod itself registers.
* **The server is now authoritative for upgrade state and backpack counts.** The client no longer
  ever inspects a backpack's upgrade inventory directly — that inventory is not reliably synced to
  the client outside of an open backpack menu, so the client's own item-availability check could
  silently drop backpack blocks before they ever reached the server. A new
  `BuildingUpgradeStatePacket` syncs the player's best upgrade tier and effective max-blocks limit
  on join, respawn, dimension change, and whenever it changes; a new per-item backpack count sync
  carries the unclamped total, with the client applying the synced max-blocks clamp itself
  (Fabric) or gating access the same way (NeoForge, which keeps its existing non-clamping "tier
  only gates access, doesn't cap the amount" semantics).
* **Enabling/disabling the upgrade is now reliable.** `BuildingUpgradeWrapper` now extends
  Sophisticated Core's `UpgradeWrapperBase` instead of managing its own `enabled` flag, so toggling
  the upgrade (from our tab or Core's own per-slot switch) refreshes Core's internal upgrade-type
  cache immediately instead of only after an unrelated slot change.
  **Migration note:** because of this, a Building Upgrade you had previously turned **off** will
  come back **enabled** after updating (the old custom-tag flag is no longer read).
* The Building Upgrade settings tab now has its own title ("Building Upgrade" instead of reusing
  "Modifier Settings") and shows the upgrade's tier and max-blocks limit.

### Radial build menu (Alt key)

* **Fixed:** rebinding the radial-menu key to something other than the default now works — the
  menu no longer falls back to checking a hard-coded Left Alt while a screen is open.
* **Fixed:** opening the radial menu (to change an option, undo/redo, or just look) no longer
  discards an in-progress multi-click build. The build is only cancelled when the build **mode**
  itself is actually changed.

### Line Thickness and Diagonal Wall fill (previously advertised, not implemented)

* **Line** and **Diagonal Line** now support the **Line Thickness** option (1/3/5 blocks wide) that
  was already documented and had UI/icons but no effect.
* **Diagonal Wall** now supports **Hollow/Filled**: Hollow keeps only the diagonal line at the
  bottom and top plus the two vertical end columns in between; Filled is the previous
  always-solid behaviour.

### Fabric build/dependency fixes

* Fabric now compiles against the correct CurseMaven coordinates for all three Sophisticated
  1.21.1 ports (the previous Backpacks coordinate resolved to a 1.20.1 build).
* Fixed a dev-runtime crash (`NoClassDefFoundError` for `porting_lib` classes,
  `Failed to start the minecraft server`) caused by Sophisticated Core's bundled porting_lib
  jar-in-jar modules not being placed on the runtime classpath.

## Update recommendation

* Use **4.1.0** NeoForge and Fabric builds together if you maintain parallel loaders for the same
  Minecraft version.
* If you use the Building Upgrade, double-check it is in the state you want after updating (see
  the enabled/disabled migration note above).
* Re-test worn-backpack Building Upgrade setups (Trinkets/Accessories/Curios) alongside the fix.
