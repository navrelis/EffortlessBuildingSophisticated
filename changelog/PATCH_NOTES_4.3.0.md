# Sophisticated Building Update – 4.3.0 (Minecraft 1.19.4)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.19.4-4.3.0.jar` — Fabric, Minecraft 1.19.4
* `sophisticatedbuilding-forge-1.19.4-4.3.0.jar` — Forge, Minecraft 1.19.4

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.19.4

Sophisticated Building is now available for Minecraft 1.19.4 on Fabric and Forge, with the same
features as the 1.21.1 build. Where 1.19.4 works differently:

* **Sophisticated Backpacks integration on Fabric only.** Sophisticated Backpacks has no Forge
  release for 1.19.4, so on Forge the Building Upgrade items are plain placeholder items (their
  recipes are not loaded). On Fabric the integration works with the unofficial Fabric port, whose
  1.19.4 files are beta builds (tested with Backpacks 3.19.5 build 105 and Core 0.5.109 build 105).
* One Building Upgrade per backpack: Sophisticated Core 0.5.109 (1.19.4) has no upgrade groups or
  conflict rules, so the upgrade refuses a second one itself; a different tier replaces an installed
  one after taking the old one out.
* Items keep their data in NBT (1.19.4 has no data components). Storage blocks placed with build
  modes still keep the contents and custom name of the item stack they were placed from. The
  randomizer bags store their contents in the bag's `Items` tag and the Omega bag its slot weights
  in `SlotWeights`.
* **Forge:** there is no in-game config screen (Forge has no generic one); the config files work as
  on the other loaders.
* **Forge:** all of the mod's network messages share one channel (`sophisticatedbuilding:main`);
  the mod is needed on both the server and the client, as on the other loaders.

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
Forge). Open it with the new button in the radial menu (above Modifier Settings) or with the new key
*Open Player Settings* (unbound by default; set it in Controls, category Sophisticated Building).

## Internal restructure

This release reorganizes the mod internally; there is no change to how it plays.

* Loader-neutral code and assets now live once in a shared module compiled into every loader's
  jar, instead of being duplicated per loader.
* The mod no longer bundles Flywheel/Ponder. The rendering helpers it needs for ghost block
  previews and outlines are now included directly (MIT-licensed, attribution included in the
  jar), so there's no Flywheel/Ponder/Catnip dependency and no load-order dependency on Create
  any more.

## Fixes

* **Fabric:** holding exactly the blocks a build needs (e.g. 1 stone with the rest in a backpack) no longer loses the
  held block on the client when a build-mode click replaces the vanilla placement; the server now resends the slot,
  as on Forge.
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
  mod refuses (a cancelled place event on Forge; Fabric has no block place event), water plants in the Nether, or a
  block that is already there. Water plants refused in the Nether are no longer dropped as items either.
* **Survival, multi-item blocks:** a build now charges every item of the block it places, like undo and redo already
  did (three candles cost three, a double slab two). Before, such a block cost one item, which could duplicate items
  when the block the preview merged with was gone by the time the build arrived.
* **Undo stack:** undoing a build whose blocks were already removed (e.g. mined by another player) no longer fails
  every time and blocks all older undos; blocks that are already back in their old state count as undone.
* **Disable mode + Quick Replace:** the block that a single click replaces now shows its preview and outline, as in
  the other modes (plain Disable mode still places like vanilla without a preview).

## Behaviour notes

* **Fabric:** the four decompress recipes (compressed cobblestone/dirt/sand/deepslate back to
  9× the base block) use the ids `decompress_compressed_*`.
* The Building Upgrade state and the backpack tool list are re-sent to the client every 10 ticks.
* The Omega bag and randomizer bag menus close as soon as the bag leaves both hands.
* Backpack count packets ignore unknown items instead of erroring.

## Requirements

### Fabric

* Minecraft 1.19.4, Java 17.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.87.2+1.19.4 or newer (built and tested against it, the last Fabric API for 1.19.4).
* Optional: Sophisticated Core/Backpacks, unofficial Fabric port for 1.19.4 (beta builds; tested
  with Core 0.5.109+mc1.19.4-SNAPSHOT-build.105, CurseForge file 5450726, and Backpacks
  3.19.5+mc1.19.4-SNAPSHOT-build.105, file 5450746). Trinkets 3.6.0 (worn backpacks) was tested.

### Forge

* Minecraft 1.19.4, Java 17.
* Forge 45.4.5 or newer (built and tested against 45.4.5, the latest Forge for 1.19.4).

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.
