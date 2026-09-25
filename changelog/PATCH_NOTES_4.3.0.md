# Sophisticated Building Update – 4.3.0 (Minecraft 1.16.3)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.16.3-4.3.0.jar` — Fabric
* `sophisticatedbuilding-forge-1.16.3-4.3.0.jar` — Forge

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
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

## Internal restructure

This release reorganizes the mod internally; there is no change to how it plays.

* Loader-neutral code and assets now live once in a shared module compiled into every loader's
  jar, instead of being duplicated per loader.
* The mod no longer bundles Flywheel/Ponder. The rendering helpers it needs for ghost block
  previews and outlines are now included directly (MIT-licensed, attribution included in the
  jar), so there's no Flywheel/Ponder/Catnip dependency and no load-order dependency on Create
  any more.

## Fixes

* **Fabric:** a build-mode click that cancels the vanilla block placement now resends the held
  slot to the client. Before, a player holding exactly the blocks a build needed lost the held
  block on the client side and the build was cancelled.

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
