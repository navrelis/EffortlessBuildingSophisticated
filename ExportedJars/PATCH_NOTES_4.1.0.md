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

### Survival mass breaking (Fabric + NeoForge)

The mod could previously only place blocks in bulk; breaking in bulk only worked in Creative. That
is now fixed:

* **How to break at all:** hold a non-block item (a tool, or nothing) and the crosshair outline
  turns red instead of the placing outline — left-click to break. Multi-click build modes (Line,
  Wall, Floor, Fill, Filter, Diagonal Line/Wall...) work exactly the same way for breaking as they
  already do for placing.
* **Survival is now supported**, with rules chosen to match vanilla balance:
  * You need a suitable tool: in your main hand, hotbar, main inventory, offhand, or inside a
    backpack that has an **enabled Tool Swapper or Advanced Tool Swapper upgrade** whose mode is
    not "No swap" (Advanced Tool Swapper's item filters are respected).
  * Durability, drops, and hunger cost are exactly like vanilla mining (`mineBlock`,
    `Block.getDrops`, `causeFoodExhaustion`). Silk Touch and Fortune apply normally.
  * A block that requires the correct tool for its drops (e.g. a pickaxe for ore) is only ever
    broken by a correct tool — a wrong-tier tool skips that block instead of destroying it without
    its drops.
  * Tools are never broken by the mod: a damageable tool with 1 use left is skipped in favour of
    the next candidate.
  * Unbreakable blocks (bedrock, etc.) are always skipped. Hardness-0 blocks (grass, torches,
    redstone dust, ...) are broken with the empty hand for free, exactly like vanilla.
  * A short mining delay applies (up to 2 seconds per operation, scaling with how long vanilla
    mining would take for the blocks and tools involved) — mass-breaking is not instant in
    survival. Creative stays instant.
  * Spawn protection and adventure-mode restrictions are respected; all the existing survival
    limits (blocks per click/axis, reach by power level, `allowInSurvival`, whitelist, protected
    tile entities) are unchanged.
* **HUD:** while breaking in survival, the tools that will be used are shown near the crosshair
  with the number of blocks each will take; a red barrier icon shows the count of blocks that
  cannot be broken with anything currently available. Those un-breakable blocks are outlined in
  grey instead of red in the preview.
* **Build-mode breaking replaces vanilla mining while a build mode is active** — the mod's rules
  above apply, in Creative and Survival alike. Switch to **Disable mode** (radial menu or its
  keybind) for plain vanilla mining/placing.

### Disable mode fix

* Disable mode is now re-synced to the server every time you join a world, so it can no longer get
  stuck blocking vanilla placing/breaking after a rejoin (the client's Disable/build-mode and
  quick-replace state used to only be sent when changed in-game, never on join).

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

### Server config

Four new server config keys govern survival breaking (`ServerConfig.survivalBreaking`):
`enabled` (default `true`), `stopBeforeToolBreaks` (default `true`), `maxDelayTicks` (default
`40`), `exhaustionPerBlock` (default `0.005`). **Caveat:** on Fabric these are in-memory defaults
only — Fabric's config loading is not implemented yet, so the values above always apply regardless
of any config file. On NeoForge they are real, file-backed config options.

## Update recommendation

* Use **4.1.0** NeoForge and Fabric builds together if you maintain parallel loaders for the same
  Minecraft version.
* If you use the Building Upgrade, double-check it is in the state you want after updating (see
  the enabled/disabled migration note above).
* Re-test worn-backpack Building Upgrade setups (Trinkets/Accessories/Curios) alongside the fix.
