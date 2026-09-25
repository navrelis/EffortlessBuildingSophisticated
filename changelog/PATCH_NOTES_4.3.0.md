# Sophisticated Building Update – 4.3.0 (Minecraft 1.17.1)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.17.1-4.3.0.jar` — Fabric
* `sophisticatedbuilding-forge-1.17.1-4.3.0.jar` — Forge

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.17.1

Sophisticated Building is now available for Minecraft 1.17.1 on Fabric and Forge, with the same
features as the 1.21.1 build. Where 1.17.1 works differently:

* **Sophisticated Backpacks integration on Forge only.** Sophisticated Backpacks has no Fabric
  release for 1.17.1, so on Fabric the Building Upgrade items are plain placeholder items (their
  recipes are not loaded) and backpacks are not used as a block or tool source.
* **Forge:** Sophisticated Backpacks 1.17.1 has no upgrade count limits; the Building Upgrade itself
  refuses a second Building Upgrade in the same backpack ("Only one building upgrade can be
  installed per backpack"), the same one-per-backpack rule as on 1.21.1. Swapping a Building
  Upgrade for another tier still works.
* Items keep their data in NBT (1.17.1 has no data components). Storage blocks placed with build
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

* **Fabric:** the four decompress recipes (compressed cobblestone/dirt/sand/deepslate back to
  9× the base block) use the ids `decompress_compressed_*`.
* The Building Upgrade state and the backpack tool list are re-sent to the client every 10 ticks.
* The Omega bag and randomizer bag menus close as soon as the bag leaves both hands.
* Backpack count packets ignore unknown items instead of erroring.

## Requirements

### Fabric

* Minecraft 1.17.1, Java 16.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.46.1+1.17 or newer (built and tested against it).

### Forge

* Minecraft 1.17.1, Java 16.
* Forge 37.1.1 or newer (built and tested against 37.1.1).
* Optional: Sophisticated Backpacks, official Forge build 1.17.1-3.12.3 or newer (tested with
  1.17.1-3.12.3.496; Sophisticated Core does not exist for 1.17.1).
* Optional: Curios API (worn-backpack fallback scan), tested with 1.17.1-5.0.2.7.

## Known issues

* **Fabric:** `ERROR` lines reading "Cannot push 'Fabric resource conditions: ...' to profiler if profiler tick
  hasn't started" at server start come from Fabric API 0.46.1+1.17's resource conditions, not from this mod. They are
  harmless and can be ignored.
* **Forge:** the `ERROR` line "No loader defined for tool_types" at server start comes from Sophisticated Backpacks
  1.17.1, not from this mod; it is logged with and without Sophisticated Building installed.
