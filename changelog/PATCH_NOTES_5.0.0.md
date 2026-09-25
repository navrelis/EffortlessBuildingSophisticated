# Sophisticated Building Update – 5.0.0 (Minecraft 1.21.11)

## Artifacts

* `sophisticatedbuilding-fabric-1.21.11-5.0.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.11-5.0.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.11-5.0.0.jar` — Forge

## New

### Minecraft 1.21.11

Sophisticated Building 5.0.0 is the first release for Minecraft 1.21.11, on Fabric, NeoForge and
Forge. It has the same features as 5.0.0 for 1.21.1, with these exceptions:

* **Sophisticated Backpacks integration on NeoForge only.** Sophisticated Backpacks has no Fabric
  or Forge release for 1.21.11, so on those loaders the Building Upgrade items are plain
  placeholder items (their recipes are not loaded) and backpacks are not used as a block or tool
  source.
* **Forge:** Forge for 1.21.11 has no render-stage event. The block previews, mirror and array
  lines and the preview outlines are drawn in one extra render pass after the rest of the world
  (after the translucent blocks, particles, clouds and weather), as on 1.21.4 to 1.21.10. On
  1.21.1 the outlines were drawn after the particles and before the weather.
* **Fabric:** the block previews, lines and outlines are drawn at the end of the main world pass
  (after the translucent blocks, before particles and weather); Fabric API for 1.21.10+ has no
  "after translucent" hook any more.
* **Key bindings:** the mod's keys are listed under their own "Sophisticated Building" category
  as before; the category is registered the way Minecraft 1.21.9+ requires, so a custom language
  pack has to translate `key.category.sophisticatedbuilding.main` (was
  `key.sophisticatedbuilding.category`). Existing key bindings keep working.
* **NeoForge:** the randomizer bags offer their contents to other mods through NeoForge's new
  item transfer capability (NeoForge 21.10 replaced the old item handler capability).
* Internally, the menus and HUD were moved to Minecraft 1.21.6's new GUI rendering (the radial
  menu, the modifier screen widgets and icons, the HUD texts and item counts) and 1.21.9's new
  mouse and keyboard input handling, and the world previews to Minecraft 1.21.11's render types.
  They look and work as before.

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

## Fixes

* **Fabric:** holding exactly the blocks a build needs (e.g. 1 stone with the rest in a backpack) no longer loses the
  held block on the client when a build-mode click replaces the vanilla placement; the server now resends the slot,
  as on NeoForge and Forge.
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

* Minecraft 1.21.11.
* Fabric Loader 0.19.5 or newer.
* Fabric API 0.141.6+1.21.11 or newer (built and tested against it).

### NeoForge

* Minecraft 1.21.11.
* NeoForge 21.11.45 or newer (the latest 1.21.11 release, built and tested against it).
* Optional: Sophisticated Backpacks for the Building Upgrades (built and tested against
  Backpacks 1.21.11-3.26.2.2155 with Core 1.21.11-1.5.0.2340). Older Backpacks/Core builds than 3.26.2 / 1.5.0 are not
  accepted.
* Curios API (compile-only, worn-backpack fallback scan): 14.0.0+1.21.11.

### Forge

* Minecraft 1.21.11.
* Forge 61.2.1 or newer (the latest 1.21.11 build, built and tested against it).

## Known issues

* **Forge:** loading an existing singleplayer world (seen when joining it directly with
  `--quickPlaySingleplayer`) can crash with "Can not retrieve LootModifierManager until resources
  have loaded once" when a block drops loot in the first world tick (for example fire burning
  out). This is a Forge bug, previously reproduced on 1.21.1/1.21.4 with Forge's example mod
  alone, not caused by this mod. Not re-checked on Forge 61 (the 1.21.11 tests create their worlds
  in-game).
