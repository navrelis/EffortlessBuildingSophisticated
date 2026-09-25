# Sophisticated Building Update – 5.0.0

## Artifacts

* `sophisticatedbuilding-fabric-1.21.1-5.0.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.1-5.0.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.1-5.0.0.jar` — Forge
* `sophisticatedbuilding-forge-1.21-5.0.0.jar` — Forge for Minecraft 1.21

Jar file names now include the Minecraft version (`sophisticatedbuilding-<loader>-1.21.1-5.0.0.jar`);
previous releases were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.21 (Fabric and NeoForge)

The Fabric and NeoForge jars now also run on Minecraft 1.21 (not only 1.21.1). On NeoForge 1.21 the Sophisticated
Backpacks integration works with the last 1.21 builds (Backpacks 3.20.26, Core 0.7.13). On Fabric 1.21 there is no
Sophisticated Backpacks port; use Fabric API 0.108.0 or newer (its 1.21.1 builds run on 1.21).

### Forge build for 1.21

Forge for Minecraft 1.21 (Forge 51) lacks APIs the Forge 1.21.1 jar needs, so 1.21 has its own Forge jar,
`sophisticatedbuilding-forge-1.21-5.0.0.jar` (Forge 51.0.33 or newer), with the same features as the 1.21.1 Forge jar
and, like it, no Sophisticated Backpacks integration (there is no Sophisticated Backpacks for Forge 1.21). The mod's
HUD elements (material cost list, build hints, block counts) are drawn on top of the whole vanilla HUD there, since
Forge 51 has no way to insert them between vanilla's HUD layers.

### Forge build for 1.21.1

Sophisticated Building is now available on Forge for 1.21.1 (Forge 52.1.2 or newer). This build has no Sophisticated
Backpacks integration, since Sophisticated Backpacks has no Forge release for 1.21.1 — Building Upgrade items and the
backpack tool list are Fabric/NeoForge-only for now.

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

## Internal restructure

This release reorganizes the mod internally; there is no change to how it plays.

* Loader-neutral code and assets now live once in a shared module compiled into every loader's
  jar, instead of being duplicated per loader.
* Jars are roughly 0.9 MB instead of roughly 3 MB.
* The mod no longer bundles Flywheel/Ponder. The rendering helpers it needs for ghost block
  previews and outlines are now included directly (MIT-licensed, attribution included in the
  jar), so there's no Flywheel/Ponder/Catnip dependency and no load-order dependency on Create
  any more.

## Behaviour notes

* **Fabric:** the four decompress recipes (compressed cobblestone/dirt/sand/deepslate back to
  9× the base block) now use the ids `decompress_compressed_*` instead of their previous ids. If
  you had one of these recipes unlocked in your recipe book, that unlock is reset — the recipes
  themselves still craft exactly as before.
* **NeoForge:** the Building Upgrade state and the backpack tool list are now re-sent to the
  client every 10 ticks. Previously they were only re-sent while you were holding a building
  block or a Building Upgrade backpack.
* **NeoForge:** the Omega bag and randomizer bag menus now close as soon as the bag leaves both
  hands, matching Fabric. Previously they stayed open on NeoForge after the bag left your hands.
* Backpack count packets now ignore unknown items instead of erroring, on both loaders.

## Requirements / dependency updates

### Fabric

* Requires Fabric Loader 0.18.6 or newer (was tested against and previously required 0.18.6;
  now built against 0.19.5).
* Fabric API: 0.116.6+1.21.1 → 0.116.17+1.21.1; the jar now requires Fabric API 0.108.0 or newer (older ones lack
  an event the mod uses and crashed the client at start).
* Minecraft: 1.21 or 1.21.1 (was: 1.21.1 and every later 1.21.x, which it was never tested on).
* Sophisticated Core/Backpacks (unofficial Fabric port): unchanged, still the frozen Core file
  7344653 / Backpacks file 6844426.

### NeoForge

* Minecraft 1.21 or 1.21.1 (was: 1.21.1).
* Requires NeoForge 21.0.167 (Minecraft 1.21) or newer (built against 21.1.251, the latest 1.21.1 release; tested on
  21.0.167 and 21.1.251).
* Optional Sophisticated Backpacks/Core: Backpacks 3.20.26 / Core 0.7.13 or newer (the last 1.21 builds) are accepted.
* Sophisticated Core/Backpacks (official NeoForge build): unchanged, still Core 1.21.1-1.5.1.2341
  / Backpacks 1.21.1-3.26.3.2158.
* Curios API (compile-only, worn-backpack fallback scan): unchanged, still 9.5.1+1.21.1.

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.
