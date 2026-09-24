# Sophisticated Building Update – 4.3.0 (Minecraft 1.21.5)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.21.5-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.5-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.5-4.3.0.jar` — Forge

## New

### Minecraft 1.21.5

Sophisticated Building 4.3.0 is the first release for Minecraft 1.21.5, on Fabric, NeoForge and
Forge. It has the same features as 4.3.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 1.21.5, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 1.21.5 removes the render-stage event entirely (no `RenderLevelStageEvent`,
  and unlike 1.21.4, no replacement event either). The block previews, mirror and array lines and
  the preview outlines are instead drawn by a small Mixin injected into the vanilla level
  renderer's own last render pass, after the rest of the world (after the translucent blocks,
  particles, clouds and weather) — the same visual position as on 1.21.4. This is dev-and-release
  verified: the mixin config is declared in the built jar's manifest and loads on every launch.
* Internally, the world and GUI render types (previews, mirror/array lines, outlines, radial menu,
  box widgets) were rebuilt on Minecraft 1.21.5's new `RenderPipeline` API, replacing the old
  render-state/shader-instance system. This has no gameplay-visible effect.

The mod no longer bundles Flywheel/Ponder; the rendering helpers it needs for the ghost block
previews and outlines are included directly (MIT-licensed, attribution included in the jar).

## Requirements

### Fabric

* Minecraft 1.21.5.
* Fabric Loader 0.19.5 or newer.
* Fabric API (built against 0.128.2+1.21.5).

### NeoForge

* Minecraft 1.21.5.
* NeoForge 21.5.98 or newer (the latest 1.21.5 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 1.21.5-3.27.2.2152 with Core 1.21.5-1.5.0.2338).
* Curios API (compile-only, worn-backpack fallback scan): 11.0.1+1.21.5.

### Forge

* Minecraft 1.21.5.
* Forge 55.0.24 or newer (the first 1.21.5 build with `AddGuiOverlayLayersEvent`, used for the HUD
  overlays; also run on 55.1.14, the latest 1.21.5 build). The world-render mixin needs no
  particular Forge build — it targets vanilla's `LevelRenderer`, not a Forge API.

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod.
