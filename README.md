# Sophisticated Building - Minecraft 1.20.1

This branch (`mc/1.20.1`) holds Sophisticated Building for Minecraft 1.20.1 on Fabric and Forge. The Forge jar also
runs on NeoForge 1.20.1 (a fork of Forge 47.1 that loads Forge mods), so there is no separate NeoForge build. It is a
port of the `mc/1.20.4` / `mc/1.21.1` branches with the same layout: loader-neutral code lives once in `common/`, and
every loader folder is a standalone Gradle build that compiles `common/` together with its own sources into one mod
jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB fixture
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
forge/                     Forge build (ModDevGradle Legacy): entry points, platform services, ForgeConfigSpec configs,
                           power level capability, Sophisticated Backpacks integration (official build) with Curios fallback
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

| | Fabric | Forge (also NeoForge 1.20.1) |
|---|---|---|
| Minecraft | 1.20.1 (`fabric.mod.json`: exactly 1.20.1) | 1.20.1 (`mods.toml`: `[1.20.1]`) |
| Java | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.92.12+1.20.1 | Forge 47.1.3 (minimum 47.1.3, `loaderVersion` `[47,)`); NeoForge 1.20.1-47.1.106 |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, Parchment 2023.09.03, reobfuscated to SRG names |
| Sophisticated Core/Backpacks | unofficial Fabric port, Core 1.20.1-1.2.7.15.166 (file 7341057), Backpacks 1.20.1-3.23.4.5.110 (file 7147929), CurseMaven; Forge Config API Port v8.0.3 in the dev runtime | official build, Core 1.20.1-1.5.1.2335 (file 8839328), Backpacks 1.20.1-3.26.3.2157 (file 8845923), CurseMaven |
| Curios / Trinkets | Trinkets 3.7.2 + Cardinal Components 5.2.2 (smoke server runtime only) | Curios 5.14.1+1.20.1, compile only (and smoke runtime) |

Forge is compiled against and run on Forge 47.1.3, the oldest Forge 47 build that Sophisticated Backpacks 3.26 accepts
(`[47.1,)`): NeoForge 1.20.1 forked Forge at 47.1, so a jar built against 47.1.x also runs there. The Forge build was
also run on NeoForge 1.20.1-47.1.106 (the latest NeoForge 1.20.1) with the same Sophisticated Backpacks and Curios
jars: its game test server starts and `runSmokeServer` passes (9/9 incl. every `sb.*` check). The release jar
`forge/release/sophisticatedbuilding-forge-1.20.1-4.3.0.jar` itself was then run in a NeoForge 1.20.1-47.1.106 client
and game test server with the smoke harness (the jar remapped from SRG for the dev runtime, Sophisticated
Backpacks/Core for Forge 1.20.1 and Curios): client 21/21 incl. 8 `sb.*` checks and the settings tab, server 9/9
(see `TESTING.md`, "Findings").

The Fabric Sophisticated Core jar nests the Porting Lib modules it was built with (`2.3.2+1.20.1`), MixinExtras and
Team Reborn Energy. `fabric/build.gradle` extracts them (recursively, one jar per mod id) from the resolved Core jar
into `fabric/build/sophisticatedcore-nested` and adds them to the compile classpath and the dev runtime. Core 1.20.1
does not nest Forge Config API Port, which Core and Backpacks need at runtime; the dev runs get it from the Modrinth
maven.

## Differences from the 1.21.1 branch

Minecraft 1.20.1 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload` and older loader APIs; the
port keeps the behaviour wherever the game allows:

* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`, as vanilla 1.20.1 does), in vanilla `BlockItem.place` order.
  The randomizer bags keep their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag
  its weights in `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1), since 1.20.1 has no vanilla `CustomPacketPayload`. Fabric
  sends them on the channel named by the id. Forge 1.20.1 identifies the messages of a `SimpleChannel` by their class,
  so all payloads travel on one channel, `sophisticatedbuilding:main`, as one message that carries the payload id
  before the body (`ForgeNetworking`); both sides need the mod (protocol version "1", as the 1.21.1 Forge build).
* Rendering uses the 1.20.1 vertex API (`vertex(...)...endVertex()`, `Tesselator.getBuilder()`,
  `ChunkBufferBuilderPack`). Fabric draws the ghost block quads with its own copy of vanilla's `putBulkData` loop so the
  preview alpha is kept. The frozen-while-paused partial tick is the last value read before the pause.
* GUI: 1.20.1 screens draw the dimmed world themselves (the mod's screens call `renderBackground` first), the mouse
  wheel only reports the vertical axis, the checkbox uses the nine-sliced disabled button of `widgets.png` (1.20.2+
  has a sprite for it), and the modifier list switches off the 1.20.1 dirt list background for a translucent dark one
  like on 1.21.
* Fabric: no client world change event in Fabric API 0.92, so world load/unload is detected at the start of each
  client tick. The Transfer API inventory of the 1.20.1 backpack port is extracted from in a transaction, and a
  backpack's wrapper is looked up with `BackpackWrapperLookup.get(stack)`.
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End);
  the SB backpack wrapper comes from the stack's `CapabilityBackpackWrapper` capability. Fake players are skipped by the
  event handlers (Forge 47 still has `FakePlayer`, unlike Forge 52 on 1.21.1). Experience of blocks broken with build
  modes is the break event's (Forge moved it there from `spawnAfterBreak`), popped after the drops, as on the 1.21.1
  Forge build. No `SpecialPlantable` exists, so no block places itself that way. No generic config screen (the config
  files are the same). The HUD is a GUI overlay above the crosshair plus `RenderGuiEvent.Post`, as on NeoForge 20.4.
  The randomizer bags have no item handler capability (the mod reads the `Items` tag directly, as on Fabric).
* Data files use the 1.20.1 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`, and the recipes
  that need Sophisticated Backpacks carry Forge's `conditions` (`forge:mod_loaded`) next to Fabric's load conditions.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, Forge: Gradle 8.14.5). Gradle runs on Java 21;
the mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric && ./gradlew runGametest    # in-world GameTests (not part of build)
cd forge  && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests
./build-all.ps1                       # both, stops at the first failure
```

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server) in either loader folder run the in-game smoke scenarios, including the Sophisticated
Backpacks integration on both loaders, and write `<dir>/smoketest-result.json`; the game exits by itself. The
harness (`common/src/smoketest`, `common/src/smoketestBackpacks`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`)
is dev-only and never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port
adopts it.

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on Forge) start with
Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge). Neither hard-codes the loader list, so
both files are copied unchanged from `templates/branch` on `main` and stay in sync via `scripts/sync-branch-infra.ps1`
(see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for both loaders here (Loom 1.17 needs it; Gradle 8.14.5 with ModDevGradle Legacy runs on it)
even though the mod itself compiles for and runs on Java 17. CI reads `ci_gradle_jdk` per loader and defaults to 21
if the key is absent.

`.github/workflows/build.yml` runs on push/PR to `mc/**` and on manual dispatch: a `discover` job builds the
loader matrix (loader name, `ci_gradle_jdk`, whether it has a `src/gametest` folder and a smoke harness), then a
`build` job builds each loader with `gradlew build --no-daemon --stacktrace`, runs `gradlew runGametest` for loaders
that have one and `gradlew runSmokeServer` for loaders with the smoke harness, and uploads the built jar (excluding
`-sources`), the test reports and the smoke result as workflow artifacts.

`release.ps1` (PowerShell 7, run from the repo root) builds every discovered loader, checks that the version
embedded in each jar's mod metadata (`fabric.mod.json` / `META-INF/mods.toml`) matches `mod_version` in
`gradle/shared.properties`, then replaces the contents of `<loader>/release/` with the new jar and a
`SHA256SUMS.txt`:

```
pwsh ./release.ps1            # gradlew build for every loader, then publish
pwsh ./release.ps1 -NoBuild   # reuse the jars already in <loader>/build/libs
```

It prints a summary table and exits non-zero if any loader fails to build, produces no matching jar, or has a
version mismatch. `<loader>/release/*.jar` and `<loader>/release/SHA256SUMS.txt` are the only tracked files
under `release/` (see `.gitignore`).
