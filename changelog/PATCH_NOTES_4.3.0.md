# Sophisticated Building Update – 4.3.0 (Minecraft 1.20.1)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.20.1-4.3.0.jar` — Fabric
* `sophisticatedbuilding-forge-1.20.1-4.3.0.jar` — Forge (also runs on NeoForge 1.20.1)

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.20.1

Sophisticated Building is now available for Minecraft 1.20.1 on Fabric and Forge, with the same
features as the 1.21.1 build. NeoForge for 1.20.1 is a fork of Forge 47.1 and loads Forge mods, so
the Forge jar is also the NeoForge 1.20.1 jar (tested on NeoForge 1.20.1-47.1.106 with Sophisticated
Backpacks: client and server start, building, the radial menu, the mod's screens and the backpack
checks incl. the Building Upgrade settings tab pass). Where 1.20.1 works
differently:

* Items keep their data in NBT (1.20.1 has no data components). Storage blocks placed with build
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
  mod refuses (a cancelled place event on Forge), water plants in the Nether, or a block that is already
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
* The mod no longer bundles Flywheel/Ponder. The rendering helpers it needs for ghost block
  previews and outlines are now included directly (MIT-licensed, attribution included in the
  jar), so there's no Flywheel/Ponder/Catnip dependency and no load-order dependency on Create
  any more.

## Behaviour notes

* **Fabric:** the four decompress recipes (compressed cobblestone/dirt/sand/deepslate back to
  9× the base block) use the ids `decompress_compressed_*`.
* The Building Upgrade state and the backpack tool list are re-sent to the client every 10 ticks.
* The Omega bag and randomizer bag menus close as soon as the bag leaves both hands.
* Backpack count packets ignore unknown items instead of erroring.

## Requirements

### Fabric

* Minecraft 1.20.1, Java 17.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.92.12+1.20.1 or newer.
* Optional: Sophisticated Core/Backpacks, unofficial Fabric port for 1.20.1 (tested with Core
  1.20.1-1.2.7.15.166, CurseForge file 7341057, and Backpacks 1.20.1-3.23.4.5.110, file 7147929,
  with Forge Config API Port v8.0.3-1.20.1-Fabric, which they need). Trinkets 3.7.2 (worn
  backpacks) was tested on a dedicated server.

### Forge / NeoForge

* Minecraft 1.20.1, Java 17.
* Forge 47.1.3 or newer (built and tested against 47.1.3), or NeoForge 1.20.1 (tested with
  47.1.106).
* Optional: Sophisticated Core/Backpacks, official Forge build (Core 1.20.1-1.5.1.2335 or newer,
  Backpacks 1.20.1-3.26.3.2157 or newer).
* Curios API (compile-only, worn-backpack fallback scan): 5.14.1+1.20.1.

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.
