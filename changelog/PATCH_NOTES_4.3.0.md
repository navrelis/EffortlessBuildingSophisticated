# Sophisticated Building Update – 4.3.0 (Minecraft 1.20.4)

*Draft — lead to confirm before release.*

## Artifacts

* `sophisticatedbuilding-fabric-1.20.4-4.3.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.20.4-4.3.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.20.4-4.3.0.jar` — Forge

Jar file names include the Minecraft version (`sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`);
releases before 4.3.0 were named `sophisticatedbuilding-<loader>-<version>.jar` without it, e.g.
`sophisticatedbuilding-fabric-4.2.1.jar`.

## New

### Minecraft 1.20.4

Sophisticated Building is now available for Minecraft 1.20.4 on Fabric, NeoForge and Forge, with the
same features as the 1.21.1 build. Where 1.20.4 works differently:

* Items keep their data in NBT (1.20.4 has no data components). Storage blocks placed with build
  modes still keep the contents and custom name of the item stack they were placed from. The
  randomizer bags store their contents in the bag's `Items` tag and the Omega bag its slot weights
  in `SlotWeights`.
* **NeoForge:** there is no in-game config screen (NeoForge 20.4 has no generic one); the config
  files work as on 1.21.1.
* **NeoForge:** experience from blocks broken with build modes is dropped directly for the tool used,
  since NeoForge 20.4 has no block drops event other mods could change it through.
* **Forge:** no Sophisticated Backpacks integration, since Sophisticated Backpacks has no Forge
  release for 1.20.4 — the Building Upgrade items are placeholders without recipes, and the
  backpack tool list is Fabric/NeoForge-only, as on Forge 1.21.1.

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
* **NeoForge:** the Building Upgrade state and the backpack tool list are re-sent to the client
  every 10 ticks.
* **NeoForge:** the Omega bag and randomizer bag menus close as soon as the bag leaves both hands,
  as on Fabric.
* Backpack count packets ignore unknown items instead of erroring, on both loaders.

## Requirements

### Fabric

* Minecraft 1.20.4, Java 17.
* Fabric Loader 0.19.5 or newer (built and tested against 0.19.5).
* Fabric API 0.97.3+1.20.4.
* Optional: Sophisticated Core/Backpacks, unofficial Fabric port for 1.20.4 (tested with Core
  1.20.4-0.6.27.138, CurseForge file 6448724, and Backpacks 1.20.4-3.20.7.101, file 6522961).

### NeoForge

* Minecraft 1.20.4, Java 17.
* NeoForge 20.4.251 or newer (built and tested against 20.4.251, the latest 1.20.4 release).
* Optional: Sophisticated Core/Backpacks, official NeoForge build (Core 1.20.4-0.6.21.608 or newer,
  Backpacks 1.20.4-3.20.6.1051 or newer).
* Curios API (compile-only, worn-backpack fallback scan): 7.4.3+1.20.4.

### Forge

* Minecraft 1.20.4, Java 17.
* Forge 49.2.9 or newer (built and tested against 49.2.9, the latest 1.20.4 release).
* No Sophisticated Backpacks integration (no Forge release for 1.20.4).

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.
