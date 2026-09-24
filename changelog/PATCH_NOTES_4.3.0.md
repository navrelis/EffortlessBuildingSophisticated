# Sophisticated Building Update – 4.3.0 (Minecraft 1.21.8)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.21.8-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.8-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.8-4.3.0.jar` — Forge

## New

### Minecraft 1.21.8

Sophisticated Building 4.3.0 is the first release for Minecraft 1.21.8, on Fabric, NeoForge and
Forge. It has the same features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 1.21.8, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 1.21.8 has no render-stage event. The block previews, mirror and array
  lines and the preview outlines are drawn in one extra render pass after the rest of the world
  (after the translucent blocks, particles, clouds and weather), as on 1.21.4 and 1.21.5. On
  1.21.1 the outlines were drawn after the particles and before the weather.
* Internally, the menus and HUD were moved to Minecraft 1.21.6's new GUI rendering (the radial
  menu, the modifier screen widgets and icons, the HUD texts and item counts). They look and work
  as before.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 1.21.8.
* Fabric Loader 0.19.5 or newer.
* Fabric API (built against 0.136.1+1.21.8).

### NeoForge

* Minecraft 1.21.8.
* NeoForge 21.8.54 or newer (the latest 1.21.8 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 1.21.8-3.26.2.2159 with Core 1.21.8-1.5.0.2342).
* Curios API (compile-only, worn-backpack fallback scan): 12.0.0+1.21.8.

### Forge

* Minecraft 1.21.8.
* Forge 58.1.22 or newer (the latest 1.21.8 build, built and tested against it).

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod. Not re-checked on Forge 58 (the 1.21.8 tests create their worlds
  in-game).
