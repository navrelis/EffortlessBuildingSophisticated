# Sophisticated Building Update – 4.3.0 (Minecraft 26.2)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-26.2-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-26.2-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-26.2-4.3.0.jar` — Forge

## New

### Minecraft 26.2

Sophisticated Building 4.3.0 is the first release for Minecraft 26.2, on Fabric, NeoForge and
Forge (26.2 only: its rendering changes mean the 26.1.x jars do not work on it and these do not
work on 26.1.x). It has the same features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 26.2, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **World previews:** Minecraft 26.2 no longer lets mods draw into the world directly; the block
  previews, ghost blocks, mirror and array lines and planes and the preview outlines are now handed
  to the game with the rest of the world's geometry and drawn by it, on all three loaders at the
  same point: after entities and other translucent objects, before translucent blocks such as
  water and glass (earlier versions drew them after the translucent blocks, on Forge after the
  whole world including the weather). They look as before; behind water or stained glass they
  are now tinted by it like any other object.
* **Key bindings:** the mod's keys are listed under their own "Sophisticated Building" category
  as before; the category is registered the way Minecraft 1.21.9+ requires, so a custom language
  pack has to translate `key.category.sophisticatedbuilding.main` (was
  `key.sophisticatedbuilding.category`). Existing key bindings keep working.
* **NeoForge:** the randomizer bags offer their contents to other mods through NeoForge's new
  item transfer capability (NeoForge 21.10 replaced the old item handler capability).
* Internally, the menus and HUD were moved to Minecraft 1.21.6's new GUI rendering (the radial
  menu, the modifier screen widgets and icons, the HUD texts and item counts) and 1.21.9's new
  mouse and keyboard input handling, the world previews to Minecraft 1.21.11's render types,
  everything to Minecraft 26.1's unobfuscated code, its render-state GUI (`GuiGraphicsExtractor`) and
  its new block model and depth-state APIs, and the world previews to Minecraft 26.2's submitted
  geometry. They look and work as before.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 26.2.
* Fabric Loader 0.19.5 or newer.
* Fabric API 0.161.0+26.2 or newer (built and tested against it).
* Java 25.

### NeoForge

* Minecraft 26.2.
* NeoForge 26.2.0.88 or newer (the latest 26.2 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 26.2-3.26.2.2154 with Core 26.2-1.5.0.2337). Older Backpacks/Core builds than 3.26.2 / 1.5.0 are not
  accepted.
* Curios API (compile-only, worn-backpack fallback scan): 16.0.0+26.2.

### Forge

* Minecraft 26.2.
* Forge 65.1.3 or newer (the latest 26.2 build, built and tested against it).

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod. Not re-checked on Forge 62 to 65 (the 26.x tests create their worlds
  in-game).
