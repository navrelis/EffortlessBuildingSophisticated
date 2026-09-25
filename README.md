# Sophisticated Building - Minecraft 1.20.4

This branch (`mc/1.20.4`) holds Sophisticated Building for Minecraft 1.20.4 on Fabric, NeoForge and Forge. It is a port
of the `mc/1.21.1` branch with the same layout: loader-neutral code lives once in `common/`, and every loader folder
is a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry Fabric, NeoForge and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB fixture
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 6): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.20.4)
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

| | Fabric | NeoForge | Forge |
|---|---|---|---|
| Minecraft | 1.20.4 (`fabric.mod.json`: exactly 1.20.4) | 1.20.4 (`mods.toml`: `[1.20.4]`) | 1.20.4 (`mods.toml`: `[1.20.4]`) |
| Java | 17 | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.97.3+1.20.4 | NeoForge 20.4.251 (minimum 20.4.251), metadata `META-INF/mods.toml` | Forge 49.2.9 (minimum 49.2.9), metadata `META-INF/mods.toml` |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle 2.0.147, Gradle 9.2.1, Parchment 2024.04.14 | ForgeGradle 6.0.54, Gradle 8.12.1, official Mojang mappings, jar reobfuscated to SRG names |
| Sophisticated Core/Backpacks | unofficial Fabric port, Core file 6448724 (0.6.27.138), Backpacks file 6522961 (3.20.7.101), CurseMaven | official build, Core 0.6.21.608 (file 5296142), Backpacks 3.20.6.1051 (file 5297718), CurseMaven | - (no Forge release for 1.20.4; Building Upgrades are placeholder items, their recipes are not loaded) |
| Curios | - | 7.4.3+1.20.4, compile only | - |

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
  NeoForge registers them with `RegisterPayloadHandlerEvent`/`IPayloadRegistrar`. Minecraft 1.20.4 does not encode
  packets on the in-memory (singleplayer) connection, so NeoForge sends a decoded copy of every payload
  (`NeoForgeNetworkHelper`); otherwise the integrated server would get the client's live block set, which the client
  clears on its next tick (1.20.5+ encodes in memory too; Fabric always encodes).
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
* Forge: the payloads travel on one `EventNetworkChannel` (`sophisticatedbuilding:main`), each message being the
  payload's type id followed by the payload (Forge 49 has no payload channel); the HUD overlay is a GUI overlay above
  the crosshair and the build HUD is drawn in `RenderGuiEvent.Post`, as on NeoForge 20.4; the world render stages
  get the event's pose stack (it carries the camera rotation on 1.20.4).
* Data files use the 1.20.4 folder names (`recipes/`, `tags/items/`) and recipe results use `"item"`.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, NeoForge: Gradle 9.2.1, Forge: Gradle 8.12.1).
Gradle runs on Java 21; the mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if
missing).

```
cd fabric   && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric   && ./gradlew runGametest    # in-world GameTests (not part of build)
cd neoforge && ./gradlew build          # jar in neoforge/build/libs, runs the common unit tests
cd forge    && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests
./build-all.ps1                         # all three, stops at the first failure
```

Forge: ForgeGradle 6 sets up Minecraft on the first build (several minutes) and does not support the configuration
cache. `jar` is finalized by `reobfJar`, which rewrites `forge/build/libs/<name>.jar` in place with SRG names, so the
jar in `build/libs` (and in `forge/release`) is the one Forge 1.20.4 can load.

`runGametest` also runs the self-test that the Porting Lib gametest module nested in Sophisticated Core registers
(`porting_lib_gametest:Tests.test`), so it reports one more required test than this mod has.

## Player settings (client config)

The Player Settings screen (`gui/buildmode/PlayerSettingsGui`) edits the client config (`ClientConfig`: Visuals and
Performance). It opens from the radial menu (button above Modifier Settings, action `OPEN_PLAYER_SETTINGS`) and with
the key "Open Player Settings" (unbound by default, category Sophisticated Building; `ClientEvents.PLAYER_SETTINGS_KEY`).
Switches are ON/OFF buttons, numbers are sliders over the config ranges (`gui/SliderValues`); changes apply at once,
"Reset to Defaults" restores them, Done/Escape/the key write the loader's file through `IConfigHelper#save`
(`config/sophisticatedbuilding-client.json` on Fabric, `config/sophisticatedbuilding-client.toml` on NeoForge and Forge).
The radial menu's Mini Block Preview toggle writes the same `showMiniBlockPreview` setting. The mini block previews are
the small ghosts of the new block (`previewScale`) that `BlockPreviews.renderBlockPreviews` draws inside the outline;
`maxMiniBlockPreviews` caps how many (0 = no limit).

## Survival charging and undo (server)

`ServerBlockPlacer` charges the item count of every placed state (`ReplaceRules.restoreCost`: a merge costs one item,
three candles onto air three), and only for blocks really set (`BlockHelper.placeSchematicBlock` reports it, the
loader's place event can refuse it). Undo of a merge (`ReplaceRules.Action.UNMERGE`) puts the old state back without
mining and gives the merged item back; undo/redo of a block already in the target state counts as done.

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server) in any loader folder run the in-game smoke scenarios, including the Sophisticated
Backpacks integration on Fabric and NeoForge (Forge 1.20.4 has no SB, so no `sb.*` checks there), and write
`<dir>/smoketest-result.json`; the game exits by itself. The
harness (`common/src/smoketest`, `common/src/smoketestBackpacks`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`)
is dev-only and never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port
adopts it.

## Run

`runClient`, `runServer` in the fabric and neoforge folders (plus `runGametest` on Fabric, `runGameTestServer` on
NeoForge) start with Sophisticated Backpacks/Core in the dev runtime. The Forge dev runs (`runClient`, `runServer`,
`runGameTestServer`) have no Sophisticated Backpacks and no `runClientExported`. Accept the EULA in
`<loader>/run/eula.txt` for `runServer`.

On Forge, create worlds in-game rather than quick-playing into an existing save (`--quickPlaySingleplayer`): Forge
can crash there with "Can not retrieve LootModifierManager until resources have loaded once" (upstream bug).

`runClientExported` (fabric, neoforge) starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, neoforge, forge). Neither hard-codes the loader list, so
both files are copied unchanged from `mc/1.21.1` and stay in sync via `scripts/sync-branch-infra.ps1` on `main`
(see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for all three loaders here (ForgeGradle 6 included: Gradle 8.12.1 runs on Java 21) even though the mod itself compiles for and runs on Java 17 — the
Java 17 compile toolchain is auto-provisioned separately. CI reads `ci_gradle_jdk` per loader and defaults to 21
if the key is absent.

`.github/workflows/build.yml` runs on push/PR to `mc/**` and on manual dispatch: a `discover` job builds the
loader matrix (loader name, `ci_gradle_jdk`, whether it has a `src/gametest` folder), then a `build` job builds
each loader with `gradlew build --no-daemon --stacktrace`, runs `gradlew runGametest` for loaders that have one,
and uploads the built jar (excluding `-sources`) and test reports as workflow artifacts.

`release.ps1` (PowerShell 7, run from the repo root) builds every discovered loader, checks that the version
embedded in each jar's mod metadata (`fabric.mod.json` / `mods.toml`) matches `mod_version` in
`gradle/shared.properties`, then replaces the contents of `<loader>/release/` with the new jar and a
`SHA256SUMS.txt`:

```
pwsh ./release.ps1            # gradlew build for every loader, then publish
pwsh ./release.ps1 -NoBuild   # reuse the jars already in <loader>/build/libs
```

It prints a summary table and exits non-zero if any loader fails to build, produces no matching jar, or has a
version mismatch. `<loader>/release/*.jar` and `<loader>/release/SHA256SUMS.txt` are the only tracked files
under `release/` (see `.gitignore`).
