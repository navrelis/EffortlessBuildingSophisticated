# Sophisticated Building Update – 4.3.0 (Minecraft 26.1.2)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-26.1.2-4.3.0.jar` — Fabric (also runs on 26.1 and 26.1.1)
* `sophisticatedbuilding-neoforge-26.1.2-4.3.0.jar` — NeoForge for 26.1.2
* `sophisticatedbuilding-neoforge-26.1-4.3.0.jar` — NeoForge for 26.1 and 26.1.1 (NeoForge beta builds)
* `sophisticatedbuilding-forge-26.1.2-4.3.0.jar` — Forge (also runs on 26.1 and 26.1.1)

## New

### Minecraft 26.1.2

Sophisticated Building 4.3.0 is the first release for Minecraft 26.1.2, on Fabric, NeoForge and
Forge. The Fabric and Forge jars also run on Minecraft 26.1 and 26.1.1 (tested). On NeoForge, 26.1.2
has its own jar, and a second jar covers 26.1 and 26.1.1, for which NeoForge only ever published
beta builds (tested on the last ones, 26.1.0.19-beta and 26.1.1.15-beta). It has the same
features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 26.1.x, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 26.1.x has no render-stage event. The block previews, mirror and array
  lines and the preview outlines are drawn in one extra render pass after the rest of the world
  (after the translucent blocks, particles, clouds and weather), as on 1.21.4 to 1.21.11. On
  1.21.1 the outlines were drawn after the particles and before the weather.
* **Fabric:** the block previews, lines and outlines are drawn at the end of the main world pass
  (after the translucent blocks, before particles and weather); Fabric API for 1.21.10+ has no
  "after translucent" hook any more.
* **Key bindings:** the mod's keys are listed under their own "Sophisticated Building" category
  as before; the category is registered the way Minecraft 1.21.9+ requires, so a custom language
  pack has to translate `key.category.sophisticatedbuilding.main` (was
  `key.sophisticatedbuilding.category`). Existing key bindings keep working.
* **NeoForge:** the preview outlines are drawn after the particles, as before (Minecraft 26.1 draws
  the particles in two passes; the outlines follow the second). The randomizer bags offer their
  contents to other mods through NeoForge's new item transfer capability (NeoForge 21.10 replaced
  the old item handler capability).
* Internally, the menus and HUD were moved to Minecraft 1.21.6's new GUI rendering (the radial
  menu, the modifier screen widgets and icons, the HUD texts and item counts) and 1.21.9's new
  mouse and keyboard input handling, the world previews to Minecraft 1.21.11's render types, and
  everything to Minecraft 26.1's unobfuscated code, its render-state GUI (`GuiGraphicsExtractor`) and
  its new block model and depth-state APIs. They look and work as before.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 26.1, 26.1.1 or 26.1.2.
* Fabric Loader 0.19.5 or newer.
* Fabric API 0.155.3+26.1.2 or newer (built against it and tested with it on all three versions).
* Java 25.

### NeoForge

* Minecraft 26.1.2.
* NeoForge 26.1.2.109 or newer (the latest 26.1.2 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 26.1.2-3.26.2.2156 with Core 26.1.2-1.5.0.2334). Older Backpacks/Core builds than 3.26.2 / 1.5.0 are not
  accepted.
* Curios API (compile-only, worn-backpack fallback scan): 15.0.0+26.1.2.

### NeoForge for 26.1 and 26.1.1 (`sophisticatedbuilding-neoforge-26.1-4.3.0.jar`)

* Minecraft 26.1 or 26.1.1.
* NeoForge 26.1.0.19-beta or newer (built against it; tested on 26.1.0.19-beta for 26.1 and
  26.1.1.15-beta for 26.1.1, the last builds NeoForge published for these versions).
* Optional: Sophisticated Backpacks for the Building Upgrades (tested with Backpacks
  26.1-3.25.48.1681 and Core 26.1-1.4.26.1688). Newer 26.1 Backpacks builds (3.25.49 to 3.25.51)
  require a Core 1.4.28 that was never released, and Core 1.4.27 needs NeoForge 26.1.2 (it
  crashes the server on the beta builds when a backpack is opened, with or without this mod), so
  this jar declares Core 1.4.26 up to (not including) 1.4.27: with 1.4.27 installed, the game stops at
  loading with a message naming the Core version instead of crashing later.

### Forge

* Minecraft 26.1, 26.1.1 or 26.1.2.
* Forge 62.0.9 or newer (built against 64.1.3, the latest 26.1.2 build; tested on 62.0.9 for
  26.1, 63.0.2 for 26.1.1 and 64.1.3 for 26.1.2).

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod. Not re-checked on Forge 62 to 64 (the 26.1.x tests create their worlds
  in-game).
