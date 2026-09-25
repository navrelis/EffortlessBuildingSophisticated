# Sophisticated Building Update – 5.0.0 (Minecraft 1.16.3)

## Artifacts

* `sophisticatedbuilding-fabric-1.16.3-5.0.0.jar` — Fabric
* `sophisticatedbuilding-forge-1.16.3-5.0.0.jar` — Forge

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-5.0.0.jar`);
releases before 5.0.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.16.3

Sophisticated Building is now available for Minecraft 1.16.3 on Fabric and Forge, with the same
features as the 1.21.1 build. Where 1.16.3 works differently:

* **Sophisticated Backpacks integration on Forge only.** Sophisticated Backpacks has no Fabric
  release for 1.16.3, so on Fabric the Building Upgrade items are plain placeholder items (their
  recipes are not included) and backpacks are not used as a block source.
* **Forge:** Sophisticated Backpacks for 1.16.3 (1.16.4-1.0.0.94) is one of its first releases:
  * it has no Tool Swapper upgrade, so breaking never takes tools from backpacks;
  * it has no upgrade slot checks, so a backpack accepts more than one Building Upgrade; each
    backpack counts once, with the highest enabled tier in it;
  * the Building Upgrade's enable toggle in the backpack screen works as on 1.21.1 (the upgrade
    stores the switch itself).
* Minecraft 1.16 has no deepslate and no amethyst: Compressed Cobbled Deepslate is crafted from
  (and decompressed into) blackstone, the Diamond and Omega Randomizer Bags use prismarine crystals
  instead of amethyst shards.
* Items keep their data in NBT (1.16.3 has no data components). Storage blocks placed with build
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

## Fixes

* **Fabric:** a build-mode click that cancels the vanilla block placement now resends the held slot to the client.
  Before, a player holding exactly the blocks a build needed lost the held block on the client side and the build was
  cancelled.
* The Omega Randomizer Bag screen no longer allocates a new native buffer for every weight badge on every frame.
* **Forge:** when the installed Sophisticated Backpacks build cannot be linked for the backpack scan, the warning now
  names the cause instead of only saying that the scan is disabled.
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
* **Survival, undo of a merge:** undoing a merge (one more snow layer, a slab made double, one more sea pickle or
  turtle egg) now takes that one item off the block again and gives it back; redo charges it again. Previously
  undoing a snow-layer merge lost the layer, and the other merges could not be undone at all (with survival replace on
  they were mined instead, and turtle eggs mined that way dropped nothing).
* **Survival, failed placements:** a block the server does not place is no longer charged: a placement a protection
  mod refuses (a cancelled place event on Forge), water plants in the Nether, or a block that is already there. Water
  plants refused in the Nether are no longer dropped as items either.
* **Survival, multi-item blocks:** a build now charges every item of the block it places, like undo and redo already
  did (three sea pickles cost three, a double slab two). Before, such a block cost one item, which could duplicate
  items when the block the preview merged with was gone by the time the build arrived.
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

* The four decompress recipes (compressed cobblestone/dirt/sand/deepslate back to 9× the base
  block) use the ids `decompress_compressed_*`.
* The Building Upgrade state is re-sent to the client every 10 ticks.
* The Omega bag and randomizer bag menus close as soon as the bag leaves both hands.
* Backpack count packets ignore unknown items instead of erroring.

## Requirements

### Fabric

* Minecraft 1.16.3, Java 8.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.25.0+build.415-1.16 or newer (built and tested against it, the last Fabric API for 1.16.3).

### Forge

* Minecraft 1.16.3, Java 8. A dedicated server needs a Java 8 older than 8u321 (Forge 1.16.3 itself
  does not start on newer ones, see Known issues); the Minecraft launcher's own Java 8 is fine.
* Forge 34.1.42 or newer (built and tested against 34.1.42, the last Forge for 1.16.3).
* Optional: Sophisticated Backpacks, official Forge build 1.16.4-1.0.0.94 or newer (tested with
  1.16.4-1.0.0.94, its release for Minecraft 1.16.3; Sophisticated Core does not exist for 1.16.3).
* Optional: Curios API (worn-backpack fallback scan), tested with 1.16.4-4.0.3.0.

## Known issues

* **Forge:** Forge 1.16.3 (34.x) crashes at start on Java 8u321 and newer with
  `NoSuchMethodError: sun.security.util.ManifestEntryVerifier.<init>` — with or without this mod.
  Run the server on an older Java 8 (tested with 8u202).
* **Forge:** the `ERROR` line "No data fixer registered for" at server start comes from Sophisticated Backpacks
  1.16.4-1.0.0.94, not from this mod; it is logged with and without Sophisticated Building installed.
* The `ERROR` line "No key layers in MapLike[{}]; ... Not a registry ops" when a dedicated server
  creates a superflat world from `server.properties` comes from Minecraft 1.16.x itself, not from
  this mod; the server then uses the default superflat layers.
