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
Backpacks: the server starts and the server-side build and backpack checks pass). Where 1.20.1 works
differently:

* Items keep their data in NBT (1.20.1 has no data components). Storage blocks placed with build
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
