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

### Player Settings screen

The Player Settings screen is now an editor of your client settings (previously it only showed placeholder controls
that did nothing):

* Visuals: block previews on/off, previews only while building, mini block preview, max block previews, appear and
  break animation length, preview scale.
* Performance: preview distance, update throttling, max mini previews.

Switches are ON/OFF buttons, numbers are sliders over the allowed range (whole-number settings move in whole steps;
the two block limits use a curve so the common values of a few thousand are easy to hit). Every setting has a
tooltip; changes apply at once, *Reset to Defaults* restores them, *Done* (or Escape) saves them to your client
config file (`config/sophisticatedbuilding-client.json` on Fabric, `config/sophisticatedbuilding-client.toml` on
NeoForge and Forge). Open it with the new button in the radial menu (above Modifier Settings) or with the new key
*Open Player Settings* (unbound by default; set it in Controls, category Sophisticated Building).

## Fixes (compared with 4.2.1 for Minecraft 1.21.1)

* **Fabric:** holding exactly the blocks a build needs (e.g. exactly 1 stone for a 1 block build) no longer loses the
  held block on the client when a build-mode click replaces the vanilla placement; the server now resends the slot,
  as on NeoForge and Forge.
* The Omega Randomizer Bag screen no longer allocates a new native buffer for every weight badge on every frame.
* When the installed Sophisticated Backpacks build cannot be linked for the backpack scan, the warning now names the
  cause instead of only saying that the scan is disabled.
* Randomizer bag titles no longer run past the edge of the bag window: a long title is drawn smaller, and a very long
  one (e.g. a bag renamed in an anvil) is cut with "..." and shown in full when you point at it. Bag windows now show
  the bag's own name, so a renamed bag shows its new name.
* The radial menu's *Mini Block Preview* toggle is now saved: it changes the `showMiniBlockPreview` client setting
  (previously a setting changed in the config file was only read at start-up and the toggle was forgotten on restart).
* The Terrain Mound options in the radial menu (Natural Variation and Terrain Shape) had blank buttons; they now have
  icons, and the active variation and shape are highlighted like the other options.
* The radial menu's side buttons stay inside the window at every window size and GUI scale (on small windows the
  Terrain Shape and tile-entity protection buttons were partly off screen); the power level summary no longer pops up
  on top of a button's tooltip.
* Very long numbers in the modifier settings fields (e.g. coordinates far from spawn) are drawn smaller instead of
  past the field's edges.
* **Survival, undo of a merge:** undoing a merge (one more snow layer, a slab made double, one more candle, sea
  pickle, turtle egg or pink petal) now takes that one item off the block again and gives it back; redo charges it
  again. Previously undoing a snow-layer merge lost the layer, and the other merges could not be undone at all (with
  survival replace on they were mined instead, and turtle eggs mined that way dropped nothing).
* **Survival, failed placements:** a block the server does not place is no longer charged: a placement a protection
  mod refuses (a cancelled place event on NeoForge and Forge), water plants in the Nether, or a block that is already
  there. Water plants refused in the Nether are no longer dropped as items either.
* **Survival, multi-item blocks:** a build now charges every item of the block it places, like undo and redo already
  did (three candles cost three, a double slab two). Before, such a block cost one item, which could duplicate items
  when the block the preview merged with was gone by the time the build arrived.
* **Undo stack:** undoing a build whose blocks were already removed (e.g. mined by another player) no longer fails
  every time and blocks all older undos; blocks that are already back in their old state count as undone.
* **Disable mode + Quick Replace:** the block that a single click replaces now shows its preview and outline, as in
  the other modes (plain Disable mode still places like vanilla without a preview).

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
