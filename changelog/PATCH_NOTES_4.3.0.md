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
