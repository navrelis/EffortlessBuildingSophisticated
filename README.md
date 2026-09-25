# Sophisticated Building - Minecraft 1.19 - 1.19.2

This branch (`mc/1.19.2`) holds Sophisticated Building for Minecraft 1.19, 1.19.1 and 1.19.2 on Fabric and Forge. It is
a port of the `mc/1.20.1` / `mc/1.21.1` branches with the same layout: loader-neutral code lives once in `common/`, and
every loader folder is a standalone Gradle build that compiles `common/` together with its own sources into one mod
jar.

The Fabric jar covers 1.19 - 1.19.2 (declared `>=1.19 <=1.19.2`; Sophisticated Backpacks has a Fabric port for 1.19.2
only, so on 1.19 and 1.19.1 the mod runs without the backpack integration). Forge needs two jars: the Sophisticated
Backpacks/Core builds for 1.19 and 1.19.1 have an older API than the 1.19.2 ones, so `forge/` builds the jar for
1.19.2 and `forge-1.19/` the jar for 1.19 and 1.19.1. See TESTING.md, "Minecraft 1.19 and 1.19.1".

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB fixture
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, access widener,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
forge/                     Forge build for Minecraft 1.19.2 (ModDevGradle Legacy): entry points, platform services,
                           ForgeConfigSpec configs, power level capability, Sophisticated Backpacks integration (official
                           build) with Curios fallback
forge-1.19/                Forge build for Minecraft 1.19 and 1.19.1 (ModDevGradle Legacy): ../forge/src with the
                           Building Upgrade item replaced for the Sophisticated Core API of 1.19 (see "Forge 1.19" below)
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

| | Fabric | Forge (`forge/`) | Forge (`forge-1.19/`) |
|---|---|---|---|
| Minecraft | 1.19 - 1.19.2 (`fabric.mod.json`: `>=1.19 <=1.19.2`) | 1.19.2 (`mods.toml`: `[1.19.2]`) | 1.19, 1.19.1 (`mods.toml`: `[1.19,1.19.1]`) |
| Java | 17 | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.77.0+1.19.2 (minimum 0.58.0, see below) | Forge 43.5.2 (minimum 43.5.2, `loaderVersion` `[43,)`) | Forge 41.1.0 (minimum 41.1.0, `loaderVersion` `[41,)`); run on 42.0.9 (1.19.1) too |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, Parchment 2022.11.27, reobfuscated to SRG names | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, official Mojang mappings (no Parchment for 1.19), SRG |
| Sophisticated Core/Backpacks | unofficial Fabric port (1.19.2 only), Core 1.19.2-0.6.4.30 (file 5803819), Backpacks 1.19.2-3.20.2.22 (file 5803830), CurseMaven | official build, Core 1.19.2-0.6.4.730 (file 5870031), Backpacks 1.19.2-3.20.2.1035 (file 5194759), CurseMaven; `mods.toml` ranges from those builds | official build, Core 1.19-0.4.10.87 (file 3901972), Backpacks 1.19-3.18.9.661 (file 3903449), CurseMaven; `mods.toml` ranges `[<those>,1.19.2)` |
| Curios / Trinkets | Trinkets 3.4.2 (Modrinth maven) + Cardinal Components 5.0.2 (smoke server runtime only) | Curios 1.19.2-5.1.6.4, compile only (and smoke runtime) | Curios 1.19.2-5.1.6.4 (also released for 1.19/1.19.1) |

The Fabric jar depends on the mod id `fabric` `>=0.58.0`, not on `fabric-api`: the Fabric API builds for 1.19
(0.58.0+1.19) and 1.19.1 (0.58.x+1.19.1) are the mod `fabric`; the 1.19.2 builds are `fabric-api` and provide `fabric`.
0.58.0 is the oldest Fabric API the jar was run on.

The Sophisticated Core and Backpacks Fabric jars nest the Porting Lib modules they were built with
(`2.1.1305+1.19.2`), Forge Config API Port 4.2.11, MixinExtras and Team Reborn Energy. `fabric/build.gradle` extracts
them (recursively, one jar per mod id) from both resolved jars into `fabric/build/sophisticatedcore-nested` and adds
them to the compile classpath and the dev runtime.

## Differences from the 1.21.1 branch

Minecraft 1.19.2 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload`, no `GuiGraphics`, no JOML,
no `BuiltInRegistries` and older loader APIs; the port keeps the behaviour wherever the game allows:

* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`, as vanilla does), in vanilla `BlockItem.place` order.
  The randomizer bags keep their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag
  its weights in `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id. Forge
  identifies the messages of a `SimpleChannel` by their class, so all payloads travel on one channel,
  `sophisticatedbuilding:main`, as one message that carries the payload id before the body (`ForgeNetworking`); both
  sides need the mod (protocol version "1", as the 1.21.1 Forge build).
* GUI: 1.19.2 renders with a `PoseStack` and `GuiComponent`'s static helpers. `common/.../client/gui/GuiGraphics` is a
  small class with the subset of 1.20's `GuiGraphics` the mod uses (`fill`, `blit`, `blitNineSliced`, `drawString`,
  `renderItem`, `renderComponentTooltip`, ...), implemented with the 1.19.2 API; the vanilla render callbacks
  (`Screen.render`, `AbstractWidget.renderButton`, list entries, HUD hooks) wrap their `PoseStack` in it, so the GUI code
  matches the newer branches. Items in GUIs are drawn through the model-view stack (1.19.2's item renderer has no
  `PoseStack` parameter). Widgets have public `x`/`y` fields: both `AbstractSimiWidget` bases add the `getX`/`setX`/
  `getY`/`setY`/`isHovered` accessors of 1.19.3+. Buttons use the `Button` constructor (no builder), the mouse wheel
  only reports the vertical axis, and screens draw the dimmed world themselves.
* Rendering: `com.mojang.math` (`Vector3f`, `Vector4f`, `Matrix4f`, `Quaternion`) instead of JOML; `Vector3f.XP` etc.
  instead of `Axis`. Fabric draws the ghost block quads with its own copy of vanilla's `putBulkData` loop so the preview
  alpha is kept.
* Registries: static `Registry.ITEM`/`Registry.BLOCK`/`..._REGISTRY` keys. Creative tabs are no registry entries: the
  tab is created first (`IPlatformHelper.createCreativeTab`, Fabric `FabricItemGroupBuilder`, Forge's label
  constructor) and the items join it through `Item.Properties#tab`, so it lists them in registration order (the
  same order as 1.21.1's `displayItems`). Its title key is `itemGroup.sophisticatedbuilding.main`.
* Blocks: "replaceable" is the block material's flag (`getMaterial().isReplaceable()`), the ice-to-water check uses
  the material too, `BlockPos.containing` is the floor-rounding `BlockPos` constructor. No `FLOWER_AMOUNT` (pink petals
  are 1.19.4+). Survival breaking recognises tools by the vanilla tool classes (`DiggerItem`, `ShearsItem`): 1.19.2 has
  no tool item tags.
* Sophisticated Backpacks 1.19.2: `UpgradeItemBase(CreativeModeTab, IUpgradeCountLimitConfig)`, no upgrade conflict
  definitions (one Building Upgrade per backpack through the upgrade group limit of 1), `canSwapUpgradeFor` without a
  slot index. `runOnBackpacks` returns void (handled by `BackpackScanCompat` as on the other branches).
* Fabric: `fabric/src/main/resources/sophisticatedbuilding.accesswidener` makes `RenderType.create` (with its sort
  flags) and `RenderType.CompositeState` accessible for the render types of the previews and outlines. Fabric API 0.77
  does not widen them (its 1.20+ builds do); in the dev runtime the transitive access widener of Porting Lib (nested in
  Sophisticated Core) hid that, and a 1.19.2 client without Sophisticated Backpacks crashed with `IllegalAccessError`
  on the first world render until the mod shipped its own widener. Fabric API 0.77 has no `FakePlayer`: the game tests
  use a vanilla `ServerPlayer` on an embedded-channel connection added to the level (the 1.19.2 game test server has
  no profile cache, which `PlayerList#placeNewPlayer` needs). No client world change event in this Fabric API, so world
  load/unload is detected at the start of each client tick; a backpack's wrapper is looked up with
  `BackpackWrapperLookup.get(stack)`.
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End),
  registered through `RegisterCapabilitiesEvent` (`@AutoRegisterCapability` is Forge 43+, and `forge-1.19` runs on
  Forge 41). The SB backpack wrapper comes from the stack's `CapabilityBackpackWrapper` capability. Fake players are
  skipped by the event handlers. Experience of blocks broken with build modes is the break event's, popped after the
  drops, as on the 1.21.1 Forge build. No `SpecialPlantable` exists, so no block places itself that way. No generic
  config screen (the config files are the same). The HUD is a GUI overlay above the crosshair plus
  `RenderGuiEvent.Post`. The randomizer bags have no item handler capability (the mod reads the `Items` tag directly,
  as on Fabric). Curios 5.1 hands out the curios inventory through `CuriosApi.getCuriosHelper().getCuriosHandler`.
* Data files use the 1.19 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`, and the recipes
  that need Sophisticated Backpacks carry Forge's `conditions` (`forge:mod_loaded`) next to Fabric's load conditions.
  Forge `pack.mcmeta`: pack format 9 with `forge:resource_pack_format` 9 and `forge:data_pack_format` 10.

### Forge 1.19

The Forge 1.19.2 jar needs the Sophisticated Backpacks/Core builds of 1.19.2; next to the 1.19 builds (Backpacks
1.19-3.18.9.661 with Core 1.19-0.4.10.87, the last ones for 1.19, which also run on 1.19.1) its Building Upgrade items
cannot be created (Core 0.4.10 has no `IUpgradeCountLimitConfig`, `UpgradeGroup`, `TranslationHelper.translUpgradeGroup`
and has other `UpgradeItemBase`/`IUpgradeItem` signatures). So `forge-1.19/` builds a second Forge jar
(`sophisticatedbuilding-forge-1.19-4.3.0.jar`, Minecraft `[1.19,1.19.1]`, Forge `[41.1.0,)`, compiled against Forge
41.1.0). It compiles `../forge/src` (main and smoketest), `../forge/src/main/templates` and `../common` as they are,
except the files its own `src/` has under the same path: a `Sync` task copies `../forge/src/<set>/<kind>` without those
into `build/generated/sharedForge`, so every shared file is compiled once and an override replaces its original (the
exclusion is decided when the copy runs, so an added override needs no reconfiguration, also with the configuration
cache). The overrides:

* `item/upgrade/BuildingUpgradeItem`: `UpgradeItemBase(CreativeModeTab)`, the 1.19 `canAddUpgradeTo` /
  `canRemoveUpgradeFrom` / `canSwapUpgradeFor` signatures. Core 1.19 has no upgrade groups or count limits, so the
  item keeps "one Building Upgrade per backpack" itself: `canAddUpgradeTo` refuses a second one (message
  `gui.sophisticatedcore.error.add.building_upgrade_conflict`); another tier replaces an installed one after taking the
  old one out.
* `src/smoketest/resources/META-INF/mods.toml`: the harness mod with `loaderVersion` `[41,)`.

Its `gradle.properties` sets `forge_minecraft_version` (Forge artifact, jar name), `forge_minecraft_version_range`, the
Forge version/range and the Sophisticated Backpacks/Core coordinates and `mods.toml` ranges of 1.19; `../forge` sets
the same keys for 1.19.2 (`forge_minecraft_version_range` overrides the shared `minecraft_version_range`, which is the
Fabric jar's).

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, both Forge folders: Gradle 8.14.5). Gradle runs
on Java 21; the mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric     && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric     && ./gradlew runGametest    # in-world GameTests (not part of build)
cd forge      && ./gradlew build          # reobfuscated 1.19.2 jar in forge/build/libs, runs the common unit tests
cd forge-1.19 && ./gradlew build          # reobfuscated 1.19/1.19.1 jar in forge-1.19/build/libs, same tests
./build-all.ps1                           # all three, stops at the first failure
```

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server) in every loader folder run the in-game smoke scenarios, including the Sophisticated
Backpacks integration on all three, and write `<dir>/smoketest-result.json`; the game exits by itself. The harness
(`common/src/smoketest`, `common/src/smoketestBackpacks`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`) is
dev-only and never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port
adopts it.

## Run

`runClient`, `runServer` in every folder (plus `runGametest` on Fabric, `runGameTestServer` on Forge) start with
Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge, forge-1.19). Neither hard-codes the loader list,
so both files are copied unchanged from `templates/branch` on `main` and stay in sync via
`scripts/sync-branch-infra.ps1` (see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for all loaders here (Loom 1.17 needs it; Gradle 8.14.5 with ModDevGradle Legacy runs on it)
even though the mod itself compiles for and runs on Java 17. CI reads `ci_gradle_jdk` per loader and defaults to 21
if the key is absent. Both Forge builds always decompile and recompile Minecraft+Forge (`disableRecompilation =
false`): ModDevGradle skips that on CI (`CI=true`) otherwise and the unit tests then fail on the signed Forge classes.

`.github/workflows/build.yml` runs on push/PR to `mc/**` and on manual dispatch: a `discover` job builds the
loader matrix (loader name, `ci_gradle_jdk`, whether it has a `src/gametest` folder and a smoke harness), then a
`build` job builds each loader with `gradlew build --no-daemon --stacktrace`, runs `gradlew runGametest` for loaders
that have one and `gradlew runSmokeServer` for loaders with the smoke harness, and uploads the built jar (excluding
`-sources`), the test reports and the smoke result as workflow artifacts.

`release.ps1` (PowerShell 7, run from the repo root) builds every discovered loader, checks that the version
embedded in each jar's mod metadata (`fabric.mod.json` / `META-INF/mods.toml`) matches `mod_version` in
`gradle/shared.properties`, then replaces the contents of `<loader>/release/` with the new jar and a
`SHA256SUMS.txt` (`forge-1.19/` publishes `sophisticatedbuilding-forge-1.19-4.3.0.jar`):

```
pwsh ./release.ps1            # gradlew build for every loader, then publish
pwsh ./release.ps1 -NoBuild   # reuse the jars already in <loader>/build/libs
```

It prints a summary table and exits non-zero if any loader fails to build, produces no matching jar, or has a
version mismatch. `<loader>/release/*.jar` and `<loader>/release/SHA256SUMS.txt` are the only tracked files
under `release/` (see `.gitignore`).
