# Sophisticated Building Update – 4.3.0 (Minecraft 1.19 – 1.19.2)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.19.2-4.3.0.jar` — Fabric, Minecraft 1.19, 1.19.1 and 1.19.2
* `sophisticatedbuilding-forge-1.19.2-4.3.0.jar` — Forge, Minecraft 1.19.2
* `sophisticatedbuilding-forge-1.19-4.3.0.jar` — Forge, Minecraft 1.19 and 1.19.1

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.19 – 1.19.2

Sophisticated Building is now available for Minecraft 1.19, 1.19.1 and 1.19.2 on Fabric and Forge, with
the same features as the 1.21.1 build. Forge needs two jars: the Sophisticated Backpacks builds for 1.19
and 1.19.1 have an older API than the one for 1.19.2. Where 1.19.x works differently:

* Items keep their data in NBT (1.19 has no data components). Storage blocks placed with build
  modes still keep the contents and custom name of the item stack they were placed from. The
  randomizer bags store their contents in the bag's `Items` tag and the Omega bag its slot weights
  in `SlotWeights`.
* The creative tab lists the items in registration order (1.19 tabs are filled from the items).
* The tool check of survival breaking uses the vanilla tool classes (pickaxes, axes, shovels, hoes,
  shears); Minecraft 1.19 has no tool item tags.
* One Building Upgrade per backpack is kept through the upgrade group limit (1.19.2) or by the
  upgrade itself (1.19 / 1.19.1); Sophisticated Core for 1.19.x has no upgrade conflict rules. On
  1.19 / 1.19.1 a different tier replaces an installed one after taking the old one out.
* **Fabric:** Sophisticated Backpacks' Fabric port exists for 1.19.2 only; on 1.19 and 1.19.1 the mod
  runs without the backpack integration.
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

* Minecraft 1.19, 1.19.1 or 1.19.2, Java 17.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.58.0 or newer (tested with 0.58.0+1.19, 0.58.5+1.19.1 and 0.77.0+1.19.2).
* Optional (1.19.2 only): Sophisticated Core/Backpacks, unofficial Fabric port for 1.19.2 (tested with
  Core 1.19.2-0.6.4.30, CurseForge file 5803819, and Backpacks 1.19.2-3.20.2.22, file 5803830).
  Trinkets 3.4.2 (worn backpacks) was tested on a dedicated server.

### Forge, Minecraft 1.19.2 (`sophisticatedbuilding-forge-1.19.2-4.3.0.jar`)

* Minecraft 1.19.2, Java 17.
* Forge 43.5.2 or newer (built and tested against 43.5.2).
* Optional: Sophisticated Core/Backpacks, official Forge build (Core 1.19.2-0.6.4.730 or newer,
  Backpacks 1.19.2-3.20.2.1035 or newer).
* Curios API (compile-only, worn-backpack fallback scan): 1.19.2-5.1.6.4.

### Forge, Minecraft 1.19 and 1.19.1 (`sophisticatedbuilding-forge-1.19-4.3.0.jar`)

* Minecraft 1.19 or 1.19.1, Java 17.
* Forge 41.1.0 or newer (built and tested against 41.1.0; also tested on 42.0.9 for 1.19.1).
* Optional: Sophisticated Core/Backpacks, official Forge build for 1.19 (Core 1.19-0.4.10.87,
  Backpacks 1.19-3.18.9.661, their last 1.19 builds, which also run on 1.19.1).
* Curios API (compile-only, worn-backpack fallback scan): 1.19.2-5.1.6.4 (also released for 1.19
  and 1.19.1).

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.
