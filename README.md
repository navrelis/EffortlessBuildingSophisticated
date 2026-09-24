# Sophisticated Building - Minecraft 1.20.4

This branch (`mc/1.20.4`) holds Sophisticated Building for Minecraft 1.20.4 on Fabric and NeoForge. It is a port
of the `mc/1.21.1` branch with the same layout: loader-neutral code lives once in `common/`, and every loader folder
is a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and NeoForge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
changelog/                 patch notes
build-all.ps1              builds every loader folder in turn
```

Platform services (`common/.../platform/services`, implementations registered in each loader's
`META-INF/services`): `IPlatformHelper`, `IBlockEventHelper`, `INetworkHelper`, `IConfigHelper`, `IClientHelper`
(client only, via `ClientServices`) and the optional `IBackpackIntegration` (falls back to a no-op when Sophisticated
Backpacks is absent or a loader build ships no integration). `common/` must not import loader or optional-mod APIs;
`checkCommonIsLoaderNeutral` (part of `check`) fails the build if it does.

The ghost block previews and outlines use the Catnip outliner and GUI widgets vendored under
`sophisticated.building.create.catnip` (MIT, see `LICENSE_Ponder.txt`); the mod has no Flywheel/Ponder/Catnip dependency.

## Versions

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.20.4 (`fabric.mod.json`: exactly 1.20.4) | 1.20.4 (`mods.toml`: `[1.20.4]`) |
| Java | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.97.3+1.20.4 | NeoForge 20.4.251 (minimum 20.4.251), metadata `META-INF/mods.toml` |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle 2.0.147, Gradle 9.2.1, Parchment 2024.04.14 |
| Sophisticated Core/Backpacks | unofficial Fabric port, Core file 6448724 (0.6.27.138), Backpacks file 6522961 (3.20.7.101), CurseMaven | official build, Core 0.6.21.608 (file 5296142), Backpacks 3.20.6.1051 (file 5297718), CurseMaven |
| Curios | - | 7.4.3+1.20.4, compile only |

The Fabric Sophisticated Core jar nests Porting Lib modules built for it (`3.1.0-soph.1+1.20.4`, on no public
Maven), Forge Config API Port and Team Reborn Energy. `fabric/build.gradle` extracts them (recursively, one jar per
mod id) from the resolved Core jar into `fabric/build/sophisticatedcore-nested` and adds them to the compile
classpath and the dev runtime.

## Differences from the 1.21.1 branch

Minecraft 1.20.4 has no data components, no `StreamCodec` and older loader APIs; the port keeps the behaviour
wherever the game allows:

* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`, as vanilla 1.20.4 does), in vanilla `BlockItem.place` order.
  The randomizer bags keep their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag
  its weights in `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads are vanilla 1.20.4 `CustomPacketPayload`s with `write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id (same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id,
  NeoForge registers them with `RegisterPayloadHandlerEvent`/`IPayloadRegistrar`.
* Rendering uses the 1.20.4 vertex API (`vertex(...)...endVertex()`, `Tesselator.getBuilder()`, `BufferBuilder`
  buffers). Fabric draws the ghost block quads with its own copy of vanilla's `putBulkData` loop so the preview alpha
  is kept (vanilla 1.20.4 always writes alpha 1). The frozen-while-paused partial tick is the last value read before
  the pause (1.20.4 keeps the paused value private).
* Fabric: no client world change event in Fabric API 0.97, so world load/unload is detected at the start of each
  client tick. The Transfer API inventory of the 1.20.4 backpack port is extracted from in a transaction.
* NeoForge: no generic config screen (the config files are the same); the HUD overlay is a GUI overlay above the
  crosshair; experience of blocks broken with build modes is popped directly for the used tool (20.4 has no block
  drops event); no `SpecialPlantable` exists, so no block places itself that way; the harvest check is
  `CommonHooks.isCorrectToolForDrops`.
* Data files use the 1.20.4 folder names (`recipes/`, `tags/items/`) and recipe results use `"item"`.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, NeoForge: Gradle 9.2.1). Gradle runs on Java
21; the mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric   && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric   && ./gradlew runGametest    # in-world GameTests (not part of build)
cd neoforge && ./gradlew build          # jar in neoforge/build/libs, runs the common unit tests
./build-all.ps1                         # both, stops at the first failure
```

`runGametest` also runs the self-test that the Porting Lib gametest module nested in Sophisticated Core registers
(`porting_lib_gametest:Tests.test`), so it reports one more required test than this mod has.

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge) start with
Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).
