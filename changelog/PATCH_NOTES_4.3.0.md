# Sophisticated Building Update – 4.3.0 (Minecraft 1.21.10)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.21.10-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.10-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.10-4.3.0.jar` — Forge

## New

### Minecraft 1.21.10

Sophisticated Building 4.3.0 is the first release for Minecraft 1.21.10, on Fabric, NeoForge and
Forge. It has the same features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 1.21.10, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 1.21.10 has no render-stage event. The block previews, mirror and array
  lines and the preview outlines are drawn in one extra render pass after the rest of the world
  (after the translucent blocks, particles, clouds and weather), as on 1.21.4 to 1.21.8. On
  1.21.1 the outlines were drawn after the particles and before the weather.
* **Fabric:** the block previews, lines and outlines are drawn at the end of the main world pass
  (after the translucent blocks, before particles and weather); Fabric API for 1.21.10 has no
  "after translucent" hook any more.
* **Key bindings:** the mod's keys are listed under their own "Sophisticated Building" category
  as before; the category is registered the way Minecraft 1.21.9+ requires, so a custom language
  pack has to translate `key.category.sophisticatedbuilding.main` (was
  `key.sophisticatedbuilding.category`). Existing key bindings keep working.
* **NeoForge:** the randomizer bags offer their contents to other mods through NeoForge's new
  item transfer capability (NeoForge 21.10 replaced the old item handler capability).
* Internally, the menus were moved to Minecraft 1.21.9's new mouse and keyboard input handling
  (radial menu, modifier screen and its inputs). They look and work as before.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 1.21.10.
* Fabric Loader 0.19.5 or newer.
* Fabric API 0.138.4+1.21.10 or newer (built and tested against it).

### NeoForge

* Minecraft 1.21.10.
* NeoForge 21.10.64 or newer (the latest 1.21.10 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 1.21.10-3.26.2.2151 with Core 1.21.10-1.5.0.2339). Older Backpacks/Core builds than
  3.26.2 / 1.5.0 are not accepted.
* Curios API (compile-only, worn-backpack fallback scan): 13.0.0+1.21.10.

### Forge

* Minecraft 1.21.10.
* Forge 60.1.15 or newer (the latest 1.21.10 build, built and tested against it).

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod. Not re-checked on Forge 60 (the 1.21.10 tests create their worlds
  in-game).
