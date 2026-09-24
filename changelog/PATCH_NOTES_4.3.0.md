# Sophisticated Building Update – 4.3.0 (Minecraft 1.21.4)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.21.4-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.4-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.4-4.3.0.jar` — Forge

## New

### Minecraft 1.21.4

Sophisticated Building 4.3.0 is the first release for Minecraft 1.21.4, on Fabric, NeoForge and
Forge. It has the same features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 1.21.4, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 1.21.4 has no render-stage event any more. The block previews, mirror and
  array lines and the preview outlines are drawn in one extra render pass after the rest of the
  world (after the translucent blocks, particles, clouds and weather). On 1.21.1 the outlines were
  drawn after the particles and before the weather.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 1.21.4.
* Fabric Loader 0.19.5 or newer.
* Fabric API 0.119.4+1.21.4 or newer (built and tested against it).

### NeoForge

* Minecraft 1.21.4.
* NeoForge 21.4.157 or newer (the latest 1.21.4 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 1.21.4-3.27.2.2153 with Core 1.21.4-1.5.0.2336). Older Backpacks/Core builds than 3.27.2 / 1.5.0 are not
  accepted.
* Curios API (compile-only, worn-backpack fallback scan): 10.0.1+1.21.4.

### Forge

* Minecraft 1.21.4.
* Forge 54.1.5 or newer (the first build with the render-pass and HUD-layer events the mod uses;
  tested on 54.1.5 and 54.1.18, the latest 1.21.4 build).

## Known issues

* **Forge 54.1.x:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, reproduced on Forge 54.1.18 with Forge's example mod alone, not
  caused by this mod.
