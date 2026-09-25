# Sophisticated Building - Minecraft 1.18.1 and 1.18

This branch (`mc/1.18.1`) holds Sophisticated Building for Minecraft 1.18.1 and 1.18 on Fabric and Forge: one Fabric
jar covers both, Forge has a jar per version (`forge/` for 1.18.1, `forge-1.18/` for 1.18). It is a port of the
`mc/1.18.2` / `mc/1.19.2` / `mc/1.21.1` branches with the same layout: loader-neutral code lives once in `common/`, and
every loader folder is a standalone Gradle build that compiles `common/` together with its own sources into one mod
jar. Sophisticated Backpacks exists for Forge only (the unofficial Fabric port starts at 1.19.2), so the Forge jars have
the backpack integration and the Fabric jar has none.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB fixture (Forge only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, access widener, GameTests (src/gametest)
forge/                     Forge build (ModDevGradle Legacy): entry points, platform services, ForgeConfigSpec configs,
                           power level capability, Sophisticated Backpacks integration (official build) with Curios fallback
forge-1.18/                Forge build for Minecraft 1.18 (Forge 38): compiles ../forge with the classes Forge 38 and
                           Sophisticated Backpacks 1.18 need replaced (see "Forge 1.18" below)
changelog/                 patch notes
build-all.ps1              builds every loader folder in turn
```

Platform services (`common/.../platform/services`, implementations registered in each loader's
`META-INF/services`): `IPlatformHelper`, `IBlockEventHelper`, `INetworkHelper`, `IConfigHelper`, `IClientHelper`
(client only, via `ClientServices`) and the optional `IBackpackIntegration` (falls back to a no-op when Sophisticated
Backpacks is absent or a loader build ships no integration; the Fabric build of this branch ships none). `common/` must
not import loader or optional-mod APIs; `checkCommonIsLoaderNeutral` (part of `check`) fails the build if it does.

The ghost block previews and outlines use the Catnip outliner and GUI widgets vendored under
`sophisticated.building.create.catnip` (MIT, see `LICENSE_Ponder.txt`); the mod has no Flywheel/Ponder/Catnip dependency.

## Versions

| | Fabric | Forge |
|---|---|---|
| Minecraft | 1.18 and 1.18.1 (`fabric.mod.json`: `>=1.18 <=1.18.1`) | 1.18.1 (`mods.toml`: `[1.18.1]`) |
| Java | 17 | 17 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.46.6+1.18 (minimum 0.44.0, mod id `fabric`) | Forge 39.1.2 (minimum 39.1.2, `loaderVersion` `[39,)`) |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, Parchment 2022.03.06, reobfuscated to SRG names |
| Sophisticated Core/Backpacks | none (no Fabric port for 1.18.x) | official build, Backpacks 1.18.1-3.15.15.550 (file 3693291, its latest 1.18.1 release, CurseMaven); Sophisticated Core is inside that jar (mod `sophisticatedcore`, same version), not a separate download yet |
| Curios / Trinkets | - | Curios 1.18.1-5.0.6.2 (CurseForge file 3661868, no Modrinth build for 1.18.1), compile only (and smoke runtime) |

Forge 39.1.2 is the latest (and recommended) Forge for 1.18.1; the Forge jar was also started on a real Forge 1.18.1
server (installer 39.1.2, only this jar plus Sophisticated Backpacks and Curios in `mods/`). Fabric API 0.46.6+1.18 is
the last Fabric API for 1.18.1; the Fabric jar was also run on Minecraft 1.18 with Fabric API 0.44.0+1.18 (smoke
server and client, see TESTING.md), hence the Fabric API floor 0.44.0. The Forge jar is 1.18.1 only: Forge 38 (1.18)
refuses it (it needs Forge 39), and with the last Sophisticated Backpacks build for 1.18 (1.18-3.12.1, whose shared
classes are still under `net.p3pp3rf1y.sophisticatedbackpacks`) the backpack integration could not link. Minecraft 1.18
therefore gets its own Forge jar from `forge-1.18/`.

### Forge 1.18

`forge-1.18/` builds `sophisticatedbuilding-forge-1.18-4.3.0.jar` (Minecraft `[1.18]`, Forge `[38.0.17,)`,
`loaderVersion` `[38,)`, optional Sophisticated Backpacks `[1.18-3.12.1,)`) with ModDevGradle Legacy 2.0.147 and Gradle
8.14.5, without Parchment (none exists for 1.18), reobfuscated to SRG names. Dev runtime: Forge 38.0.17 (the latest Forge
for 1.18), Sophisticated Backpacks 1.18-3.12.1.433 (CurseForge file 3555237, its latest 1.18 release) and Curios
1.18-5.0.2.5 (file 3567289, the last Curios built for 1.18). It was also started on a real Forge 1.18 server
(installer 38.0.17, only this jar plus Sophisticated Backpacks and Curios in `mods/`).

It compiles `../forge/src` (main and smoketest), `../forge/src/main/templates`, `../common` and
`../common/src/smoketestBackpacks` as they are, except the files its own `src/` has under the same path: a `Sync` task
copies the shared folders without those into `build/generated/shared`, so every shared file is compiled once and an
override replaces its original (the exclusion is decided when the copy runs, so an added override needs no
reconfiguration). The overrides:

* Sophisticated Backpacks 1.18 keeps everything later split off into Sophisticated Core under
  `net.p3pp3rf1y.sophisticatedbackpacks` (no `sophisticatedcore` package or mod): `IBackpackWrapper`, `IUpgradeWrapper`,
  `UpgradeType`, `UpgradeSlotChangeResult` in `api`, `BackpackInventoryHandler` / `BackpackUpgradeHandler` instead of
  `InventoryHandler` / `UpgradeHandler`, `BackpackScreen` instead of `StorageScreenBase`, the GUI and container classes in
  `client.gui` / `common.gui`. Same code, other imports: `BuildingUpgradeItem`, `BuildingUpgradeWrapper`,
  `BuildingUpgradeContainer`, `BuildingUpgradeHelper`, `BuildingUpgradeSettingsTab`, `SophisticatedBackpacksIntegration`,
  `SophisticatedBackpacksClientIntegration`, `ToolSwapperIntegration` and the smoke fixture
  `SophisticatedBackpacksFixture`.
* `BuildingUpgradeItem`: SB 1.18's `UpgradeItemBase` takes no creative tab and puts its items into the backpacks tab;
  `fillItemCategory` / `getCreativeTabs` show the Building Upgrades in this mod's tab, as on 1.18.1.
* `META-INF/mods.toml` template: the ranges above, and no `sophisticatedcore` dependency.
* Smoke harness: Forge 38 has no game test integration (`net.minecraftforge.gametest`, the game test server launch
  target and `RegisterGameTestsEvent` are Forge 39+). `runSmokeServer` starts a dedicated server (fresh superflat world,
  EULA accepted, no spawn protection) on which `SmokeServerRunner` turns the `@GameTest` methods of
  `ForgeSmokeServerTests` into vanilla test functions (named and batched as vanilla's `GameTestRegistry` does, template
  `sophisticatedbuilding:smoketest_empty`), runs them with its own `GameTestTicker` and stops the server when all are
  done; the checks and the result file are the same. `ForgeSmokeTest` hooks it to `ServerStartedEvent` /
  `ServerTickEvent`; the harness mod's `mods.toml` asks for `loaderVersion` `[38,)`.

As on `forge/`, the dev runs pin the launcher libraries of Forge 38.0.17's profile (securejarhandler 0.9.54, forgespi
4.0.10, coremods 5.0.1). Everything else of `../forge` (Forge 39 code: `RenderLevelLastEvent`, `ForgeRegistries`
deferred registers, networking, configs, capabilities, Curios compatibility) compiles and runs on Forge 38 unchanged.

## Differences from the 1.21.1 branch

Minecraft 1.18.1 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload`, no `GuiGraphics`, no
`TagKey`, a static `Registry` and older loader APIs; the port keeps the behaviour wherever the game allows. The
differences of `mc/1.18.2` apply, plus the ones marked **1.18.1**:

* No Sophisticated Backpacks on Fabric: the Fabric jar ships no backpack integration, the Building Upgrade items are
  placeholders without function (as on the loaders of other branches without Sophisticated Backpacks), and their
  recipes are only loaded when Sophisticated Backpacks is installed (`fabric:load_conditions` / `forge:mod_loaded`).
* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`), in vanilla `BlockItem.place` order. The randomizer bags keep
  their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag its weights in
  `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id. Forge
  identifies the messages of a `SimpleChannel` by their class, so all payloads travel on one channel,
  `sophisticatedbuilding:main`, as one message that carries the payload id before the body (`ForgeNetworking`); both
  sides need the mod (protocol version "1"). Forge 1.18.x has no `consumerMainThread`: the handler queues itself on the
  main thread. A block set's nullable block state is written as a presence flag plus the state (the bytes
  `FriendlyByteBuf#writeNullable` writes on newer versions, which 1.18.x lacks).
* Text components are `TextComponent` / `TranslatableComponent` / `KeybindComponent` (1.19 replaced them with
  `Component.literal`/`translatable`).
* GUI: the screens, widgets and HUD draw through `sophisticated.building.client.gui.GuiGraphics`, a shim with the
  1.20 `GuiGraphics` methods over 1.18's `GuiComponent` helpers and a `PoseStack` (scissor areas are converted to
  window pixels as 1.19.2's `GuiComponent.enableScissor` does). Immediate-mode quads end with
  `BufferBuilder#end()` + `BufferUploader.end(...)`. Math types are `com.mojang.math` (no JOML); creative tab, item
  and registry code use the static `Registry` and `Item.Properties#tab`. The radial menu click sound uses the 1.18
  `SimpleSoundInstance` constructor (no random source).
* **1.18.1:** no `TagKey` (the vendored `TagDependentIngredientItem` holds a `Tag<Item>`) and no
  `com.mojang.logging.LogUtils` (SLF4J `LoggerFactory`).
* Rendering: Minecraft 1.18.x keeps the composite `RenderType` factory private (public from 1.19); the Fabric build
  opens it with an access widener (`fabric/src/main/resources/sophisticatedbuilding.accesswidener`), Forge's own access
  transformer already opens it. **1.18.1:** the same access widener also opens `MenuType.MenuSupplier`, the
  `MenuType` constructor and `MenuScreens.register` (Fabric API 0.46 has no transitive access wideners yet). Model quads use a `java.util.Random` (no `RandomSource`). Fabric draws the ghost block
  quads with its own copy of vanilla's `putBulkData` loop so the preview alpha is kept.
* Block breaking: vanilla 1.18's `spawnAfterBreak` has no "drop experience" flag and always drops it (what 1.19+ is
  told to do); on Forge the experience is the break event's, popped after the drops, as on the 1.21.1 Forge build.
* Fabric: Fabric API for 1.18 has the v1 command registration callback and no client world change event (world
  load/unload is detected at the start of each client tick). **1.18.1:** Fabric API 0.46 still has the mod id `fabric`
  (not `fabric-api`), which `fabric.mod.json` depends on.
* Forge 1.18.x names: `WorldEvent`, `TickEvent.WorldTickEvent`, `InputEvent.KeyInputEvent`, `ScreenOpenEvent`,
  `ClientPlayerNetworkEvent.LoggedInEvent`/`LoggedOutEvent`, `event.world.BlockEvent`. The HUD is an
  `OverlayRegistry` overlay above the crosshair plus `RenderGameOverlayEvent.Post` for the whole HUD
  (`ElementType.ALL`); key mappings are registered with `ClientRegistry` in client setup; the power level capability is
  registered in `RegisterCapabilitiesEvent` (no `@AutoRegisterCapability`); model quads are asked with
  `EmptyModelData` and the drawn layer set through `ForgeHooksClient.setRenderType` (Forge 1.18.x has no render type
  parameter); the tooltip line breaking uses the selected language's `Locale`.
* **1.18.1, Forge 39:** no render stages (`RenderLevelStageEvent` is Forge 40): the previews, mirror/array lines, ghost
  blocks and then the outlines are drawn in `RenderLevelLastEvent`, after the whole level (also after particles,
  clouds and weather, where the stages draw them after the translucent blocks and after the particles). Deferred
  registers are created from `ForgeRegistries` (no registry keys). The dev runs pin the launcher libraries of Forge
  39.1.2's own profile (securejarhandler 1.0.3, forgespi 4.0.10, coremods 5.0.1): the newer builds Gradle would pick
  crash the dev runs at startup.
* **1.18.1, Sophisticated Backpacks 3.15:** Sophisticated Core ships inside the Backpacks jar (the same
  `net.p3pp3rf1y.sophisticatedcore` packages as the later separate Core). There are no upgrade count limits or
  upgrade groups yet: the Building Upgrade refuses to go into a backpack that already has one in `canAddUpgradeTo`
  (as SB's own battery and tool swapper upgrades do on this version), so a tier change means taking the old upgrade
  out first (on 1.18.2+ it can be swapped in place). The backpack scan callback has three arguments (stack, handler
  name, slot).
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End);
  the SB backpack wrapper comes from the stack's `CapabilityBackpackWrapper` capability; fake players are skipped by the
  event handlers; no `SpecialPlantable`; no generic config screen (the config files are the same); the randomizer bags
  have no item handler capability.
* Data files use the pre-1.21 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`. The Forge
  `pack.mcmeta` declares resource pack format 8 and data pack format 8 (Minecraft 1.18/1.18.1).
* Player Settings screen (`PlayerSettingsGui`): 1.18.x screens do not draw the dimmed world themselves and have no
  widget tooltips (`Tooltip`, `setTooltipForNextRenderPass` are 1.19.3+): the screen draws the background, then the
  hovered setting's tooltip (label or control) after everything else; the list switches off its dirt background and
  dirt bands and clips its rows to its area instead; buttons are `new Button(...)` (no builder), widget positions are
  the public `x`/`y` fields. The bag title tooltip is drawn by the bag screens' `render` the same way.
* `ForgeConfigSpec.ConfigValue` of Forge 38 and 39 has no `getDefault()`: `ForgeConfigHelper` keeps the default each
  value was defined with (used by "Reset to Defaults"); `set` and `ForgeConfigSpec#save()` exist.
* The merge-undo GameTests leave out pink petals (Minecraft 1.20+); short grass is `Blocks.GRASS` in 1.18.x and a player
  changes level with `ServerPlayer#setLevel`.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, Forge: Gradle 8.14.5). Gradle runs on Java 21;
the mod is compiled for and run on Java 17 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests (104)
cd fabric && ./gradlew runGametest    # 24 in-world GameTests (not part of build)
cd forge  && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests (90)
cd forge-1.18 && ./gradlew build      # the Forge 1.18 jar in forge-1.18/build/libs, same unit tests (90)
./build-all.ps1                       # all three, stops at the first failure
```

## Player settings (client config)

The Player Settings screen (`gui/buildmode/PlayerSettingsGui`) edits the client config (`ClientConfig`: Visuals and
Performance). It opens from the radial menu (button above Modifier Settings, action `OPEN_PLAYER_SETTINGS`) and with
the key "Open Player Settings" (unbound by default, category Sophisticated Building; `ClientEvents.PLAYER_SETTINGS_KEY`).
Switches are ON/OFF buttons, numbers are sliders over the config ranges (`gui/SliderValues`); changes apply at once,
"Reset to Defaults" restores them, Done/Escape/the key write the loader's file through `IConfigHelper#save`
(`config/sophisticatedbuilding-client.json` on Fabric, `config/sophisticatedbuilding-client.toml` on Forge).
The radial menu's Mini Block Preview toggle writes the same `showMiniBlockPreview` setting. The mini block previews are
the small ghosts of the new block (`previewScale`) that `BlockPreviews.renderBlockPreviews` draws inside the outline;
`maxMiniBlockPreviews` caps how many (0 = no limit).

## Survival charging and undo (server)

`ServerBlockPlacer` charges the item count of every placed state (`ReplaceRules.restoreCost`: a merge costs one item,
three candles onto air three), and only for blocks really set (`BlockHelper.placeSchematicBlock` reports it, the
loader's place event can refuse it; Fabric has none). Undo of a merge (`ReplaceRules.Action.UNMERGE`) puts the old
state back without mining and gives the merged item back; undo/redo of a block already in the target state counts as
done.

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server; on `forge-1.18` a dedicated server, see "Forge 1.18") in any loader folder run the in-game
smoke scenarios and write `<dir>/smoketest-result.json`; the game exits by itself. Both Forge folders also run the
Sophisticated Backpacks checks (`sb.*`);
Fabric has no backpack integration on 1.18.1 and runs none. The harness (`common/src/smoketest`,
`common/src/smoketestBackpacks` (Forge only), `<loader>/src/smoketest`, `gradle/smoketest.gradle`) is dev-only and never
packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port adopts it.

## Run

`runClient`, `runServer` in any folder (plus `runGametest` on Fabric, `runGameTestServer` on `forge/`); the Forge runs
have Sophisticated Backpacks (with Core inside on 1.18.1) in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge, forge-1.18). Neither hard-codes the loader list, so
both files are copied unchanged from `templates/branch` on `main` and stay in sync via `scripts/sync-branch-infra.ps1`
(see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for every loader folder here (Loom 1.17 needs it; Gradle 8.14.5 with ModDevGradle Legacy runs on it)
even though the mod itself compiles for and runs on Java 17. CI reads `ci_gradle_jdk` per loader and defaults to 21
if the key is absent. The Forge build always recompiles Minecraft (`disableRecompilation = false`), also when
`CI=true`, so the unit tests never load the signed Forge classes.

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
