# Sophisticated Building - Minecraft 1.19.4

This branch (`mc/1.19.4`) holds Sophisticated Building for Minecraft 1.19.4 on Fabric and Forge. It is a port of the
`mc/1.19.2` branch (with the 1.19.3/1.19.4 API changes the `mc/1.20.1` branch already has) with the same layout:
loader-neutral code lives once in `common/`, and every loader folder is a standalone Gradle build that compiles
`common/` together with its own sources into one mod jar.

Sophisticated Backpacks (SB) exists for 1.19.4 only as the unofficial Fabric port, and only as beta files: the Fabric
jar has the backpack integration, the Forge jar has none (the official Forge builds skip 1.19.3 and 1.19.4).
Minecraft 1.19.3 has no SB build on any loader and is not covered (both jars declare exactly 1.19.4).

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB
                             fixture (Fabric only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, access widener,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
forge/                     Forge build (ModDevGradle Legacy): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no Sophisticated Backpacks integration
changelog/                 patch notes
build-all.ps1              builds every loader folder in turn
```

Platform services (`common/.../platform/services`, implementations registered in each loader's
`META-INF/services`): `IPlatformHelper`, `IBlockEventHelper`, `INetworkHelper`, `IConfigHelper`, `IClientHelper`
(client only, via `ClientServices`) and the optional `IBackpackIntegration` (falls back to a no-op when Sophisticated
Backpacks is absent or a loader build ships no integration; the Forge build of this branch ships none). `common/` must
not import loader or optional-mod APIs; `checkCommonIsLoaderNeutral` (part of `check`) fails the build if it does.

The ghost block previews and outlines use the Catnip outliner and GUI widgets vendored under
`sophisticated.building.create.catnip` (MIT, see `LICENSE_Ponder.txt`); the mod has no Flywheel/Ponder/Catnip dependency.

## Versions

| | Fabric | Forge |
|---|---|---|
| Minecraft | 1.19.4 (`fabric.mod.json`: exactly 1.19.4) | 1.19.4 (`mods.toml`: `[1.19.4]`) |
| Java | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.87.2+1.19.4 (the last one for 1.19.4; minimum 0.87.2, mod id `fabric-api`) | Forge 45.4.5 (the latest for 1.19.4; minimum 45.4.5, `loaderVersion` `[45,)`) |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, Parchment 2023.06.26, reobfuscated to SRG names |
| Sophisticated Core/Backpacks | unofficial Fabric port, beta files only for 1.19.4: Core 0.5.109+mc1.19.4-SNAPSHOT-build.105 (file 5450726), Backpacks 3.19.5+mc1.19.4-SNAPSHOT-build.105 (file 5450746), CurseMaven; optional (`suggests`) | none (no Forge build for 1.19.4) |
| Curios / Trinkets | Trinkets 3.6.0 (Modrinth maven) + Cardinal Components 5.2.0 (smoke runtime only) | - |

The Sophisticated Core and Backpacks Fabric jars nest the Porting Lib modules they were built with (`2.1.2+1.19.4`),
Forge Config API Port 6.0.2, MixinExtras, Team Reborn Energy and Cardinal Components. `fabric/build.gradle` extracts
them (recursively, one jar per mod id) from both resolved jars into `fabric/build/sophisticatedcore-nested` and adds
them to the compile classpath and the dev runtime. Both SB jars require Fabric API `>=0.87.2+1.19.4`.

The Fabric release jar was started on a real Fabric 1.19.4 server (Fabric Loader 0.19.5, the Fabric API
0.87.2+1.19.4 modules, SB 3.19.5 build 105 + Core 0.5.109 build 105 in `mods/`, Java 17): "Registered Sophisticated
Backpacks upgrade containers", `Done`, no error from the mod.

## Differences from the 1.21.1 branch

Minecraft 1.19.4 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload`, no `GuiGraphics`, no
creative tab registry and older loader APIs; the port keeps the behaviour wherever the game allows:

* No Sophisticated Backpacks on Forge: the Forge jar ships no backpack integration and the Building Upgrade items are
  placeholders without function (as on the loaders of other branches without SB); their recipes carry Forge's
  `forge:mod_loaded` condition and are not loaded.
* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`, as vanilla does), in vanilla `BlockItem.place` order.
  The randomizer bags keep their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag
  its weights in `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id. Forge
  identifies the messages of a `SimpleChannel` by their class, so all payloads travel on one channel,
  `sophisticatedbuilding:main`, as one message that carries the payload id before the body (`ForgeNetworking`); both
  sides need the mod (protocol version "1", as the 1.21.1 Forge build).
* GUI: 1.19.4 renders with a `PoseStack` and `GuiComponent`'s static helpers. `common/.../client/gui/GuiGraphics` is a
  small class with the subset of 1.20's `GuiGraphics` the mod uses (`fill`, `blit`, `blitNineSliced`, `drawString`,
  `renderItem`, `renderComponentTooltip`, ...), implemented with the 1.19.4 API; the vanilla render callbacks
  (`Screen.render`, `AbstractWidget.renderWidget`, list entries, HUD hooks) wrap their `PoseStack` in it, so the GUI
  code matches the newer branches. The mouse wheel only reports the vertical axis, and screens draw the dimmed world
  themselves.
* Creative tab: Minecraft 1.19.4 builds tabs from a `CreativeModeTab.Builder` but has no tab registry;
  `IPlatformHelper.registerCreativeTab(path, builder -> ...)` lets Fabric create it with `FabricItemGroup.builder` and
  Forge in `CreativeModeTabEvent.Register`. Same icon and item order as 1.21.1's `displayItems`; its title key is
  `itemGroup.sophisticatedbuilding.main`.
* Blocks: "replaceable" is the block material's flag (`getMaterial().isReplaceable()`), the ice-to-water check uses the
  material too (1.20 replaced block materials with block state flags).
* Sophisticated Core 0.5.109 (Fabric port for 1.19.4): no `IUpgradeCountLimitConfig`, no upgrade groups and no conflict
  definitions, `UpgradeItemBase()` without arguments. The Building Upgrade keeps "one per backpack" itself:
  `canAddUpgradeTo` refuses a second one (message `gui.sophisticatedcore.error.add.building_upgrade_conflict`, as the
  `forge-1.19` folder of `mc/1.19.2`); a different tier replaces an installed one after taking the old one out.
  `BackpackWrapperLookup.get(stack)` returns an `Optional` (a Porting Lib `LazyOptional` on the newer ports).
* Fabric: `fabric/src/main/resources/sophisticatedbuilding.accesswidener` makes `RenderType.create` (with its sort
  flags) and `RenderType.CompositeState` accessible for the render types of the previews and outlines (Fabric API 0.87
  does not widen them). The game tests use a vanilla `ServerPlayer` on an embedded-channel connection added to the
  level (as on 1.19.2). No client world change event in this Fabric API, so world load/unload is detected at the start
  of each client tick.
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End),
  registered through `RegisterCapabilitiesEvent`. Fake players are skipped by the event handlers. Experience of blocks
  broken with build modes is the break event's, popped after the drops, as on the 1.21.1 Forge build. No
  `SpecialPlantable` exists, so no block places itself that way. No generic config screen (the config files are the
  same). The HUD is a GUI overlay above the crosshair plus `RenderGuiEvent.Post`. The randomizer bags have no item
  handler capability (the mod reads the `Items` tag directly, as on Fabric).
* Data files use the pre-1.21 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`, and the recipes
  that need Sophisticated Backpacks carry Forge's `conditions` (`forge:mod_loaded`) next to Fabric's load conditions.
  Forge `pack.mcmeta`: pack format 13 with `forge:resource_pack_format` 13 and `forge:data_pack_format` 12.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, Forge: Gradle 8.14.5). Gradle runs on Java 21; the
mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests (77)
cd fabric && ./gradlew runGametest    # in-world GameTests (17, not part of build)
cd forge  && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests (65)
./build-all.ps1                       # both, stops at the first failure
```

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server) in either loader folder run the in-game smoke scenarios and write
`<dir>/smoketest-result.json`; the game exits by itself. Fabric also runs the Sophisticated Backpacks checks (`sb.*`);
Forge has no backpack integration on 1.19.4 and runs none. The harness (`common/src/smoketest`,
`common/src/smoketestBackpacks` (Fabric only), `<loader>/src/smoketest`, `gradle/smoketest.gradle`) is dev-only and
never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port adopts it.

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on Forge); the Fabric runs
start with Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge). Neither hard-codes the loader list,
so both files are copied unchanged from `templates/branch` on `main` and stay in sync via
`scripts/sync-branch-infra.ps1` (see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for all loaders here (Loom 1.17 needs it; Gradle 8.14.5 with ModDevGradle Legacy runs on it)
even though the mod itself compiles for and runs on Java 17. CI reads `ci_gradle_jdk` per loader and defaults to 21
if the key is absent. The Forge build always decompiles and recompiles Minecraft+Forge (`disableRecompilation =
false`): ModDevGradle skips that on CI (`CI=true`) otherwise and the unit tests then fail on the signed Forge classes.

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
