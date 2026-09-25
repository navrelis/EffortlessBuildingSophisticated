# Sophisticated Building Update – 4.3.0 (Minecraft 1.16.5 and 1.16.4)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.16.5-4.3.0.jar` — Fabric (Minecraft 1.16.4 and 1.16.5)
* `sophisticatedbuilding-forge-1.16.5-4.3.0.jar` — Forge (Minecraft 1.16.5)
* `sophisticatedbuilding-forge-1.16.4-4.3.0.jar` — Forge (Minecraft 1.16.4)

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.16.5 and 1.16.4

Sophisticated Building is now available for Minecraft 1.16.5 and 1.16.4 on Fabric and Forge (Java 8),
with the same features as the 1.21.1 build. One Fabric jar covers both versions; Forge has one jar
per version. Where 1.16.x works differently:

* **Sophisticated Backpacks integration on Forge only.** Sophisticated Backpacks has no Fabric
  release for 1.16.x, so on Fabric the Building Upgrade items are plain placeholder items (they have
  no recipes) and backpacks are not used as a block or tool source.
* **Forge:** Sophisticated Backpacks 1.16.x has no upgrade count limits; the Building Upgrade itself
  refuses a second Building Upgrade in the same backpack ("Only one building upgrade can be
  installed per backpack"), the same one-per-backpack rule as on 1.21.1. Swapping a Building
  Upgrade for another tier still works.
* **Forge:** the Tool Swapper of Sophisticated Backpacks 1.16.x filters per tool type; a tool in a
  Tool Swapper backpack is used for mass breaking when the filter of one of its tool types allows it.
* **Recipes:** 1.16.x has no deepslate and no amethyst. Compressed cobbled deepslate is crafted
  from 9 blackstone (and decompresses into 9 blackstone); the Diamond and Omega randomizer bags use
  prismarine crystals instead of amethyst shards.
* Items keep their data in NBT (1.16.x has no data components). Storage blocks placed with build
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
* The Building Upgrade state and the backpack tool list are re-sent to the client every 10 ticks.
* The Omega bag and randomizer bag menus close as soon as the bag leaves both hands.
* Backpack count packets ignore unknown items instead of erroring.

## Requirements

### Fabric

* Minecraft 1.16.4 or 1.16.5, Java 8.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.42.0+1.16 or newer (built and tested against it, on 1.16.4 too: it declares Minecraft
  1.16.2 and later, although the download sites list it for 1.16.5 only).

### Forge (Minecraft 1.16.5)

* Minecraft 1.16.5, Java 8.
* Forge 36.2.42 or newer (built and tested against 36.2.42).
* Optional: Sophisticated Backpacks, official Forge build 1.16.5-3.15.20 or newer (tested with
  1.16.5-3.15.20.755; Sophisticated Core does not exist for 1.16.x).
* Optional: Curios API (worn-backpack fallback scan), tested with 1.16.5-4.1.0.0.

### Forge (Minecraft 1.16.4)

* Minecraft 1.16.4, Java 8.
* Forge 35.1.37 or newer (built and tested against 35.1.37).
* Optional: Sophisticated Backpacks, official Forge build 1.16.4-3.0.0.289 (the last 1.16.4 build;
  the 1.16.5 builds need the 1.16.5 jar).
* Optional: Curios API (worn-backpack fallback scan), tested with 1.16.5-4.1.0.0 (declared for
  1.16.4 and 1.16.5).

## Known issues

* The `ERROR` line "No key layers in MapLike[{}]; ... Not a registry ops" when a dedicated server
  creates a superflat world from `server.properties` comes from Minecraft 1.16.x itself, not from
  this mod; the server then uses the default superflat layers.
* **Forge 1.16.4:** a dedicated Forge 1.16.4 server (Forge 35) does not start on Java 8u321 or
  newer (`NoSuchMethodError: sun.security.util.ManifestEntryVerifier`), with or without this mod.
  Run it on an older Java 8 or replace its modlauncher 8.0.9 library with 8.1.3 (the version Forge
  36.2 uses for 1.16.5). The Minecraft launcher's own Java 8 is not affected.
* **Forge 1.16.4:** the `ERROR` line "No data fixer registered for" at server start comes from
  Sophisticated Backpacks 1.16.4 registering its entity types, not from this mod.
