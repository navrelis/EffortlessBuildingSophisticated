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
  slot to the client. Before, a player holding exactly the blocks a build needed (for example one
  block plus a backpack with a Building Upgrade) lost the held block on the client side and the
  build was cancelled.
* When the Sophisticated Backpacks scan cannot link against the installed Backpacks build, the
  warning now includes the cause.

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
