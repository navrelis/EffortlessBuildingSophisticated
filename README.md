# Sophisticated Building - Minecraft 1.17.1

This branch (`mc/1.17.1`) holds Sophisticated Building for Minecraft 1.17.1 on Fabric and Forge. It is a port of the
`mc/1.18.x` / `mc/1.19.2` / `mc/1.20.1` / `mc/1.21.1` branches with the same layout: loader-neutral code lives once in
`common/`, and every loader folder is a standalone Gradle build that compiles `common/` together with its own sources
into one mod jar. Sophisticated Backpacks exists for Forge 1.17.1 only (the unofficial Fabric port starts at 1.19.2),
so the Forge jar has the backpack integration and the Fabric jar has none.

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
| Minecraft | 1.17.1 (`fabric.mod.json`: exactly 1.17.1) | 1.17.1 (`mods.toml`: `[1.17.1]`) |
| Java | 16 | 16 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.46.1+1.17 (minimum 0.46.1, mod id `fabric`) | Forge 37.1.1 (minimum 37.1.1, `loaderVersion` `[37,)`) |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | ModDevGradle Legacy 2.0.147, Gradle 8.14.5, Parchment 2021.12.12, reobfuscated to SRG names |
| Sophisticated Backpacks | none (no Fabric port for 1.17.1) | official build 1.17.1-3.12.3.496 (file 3594054, CurseMaven, its latest 1.17.1 release; no separate Sophisticated Core on 1.17.1) |
| Curios / Trinkets | - | Curios 1.17.1-5.0.2.7, compile only (and smoke runtime) |

Forge 37.1.1 is the latest (and recommended) Forge for 1.17.1; the Forge jar was also started on a real Forge 1.17.1
server (installer 37.1.1, only this jar plus Sophisticated Backpacks and Curios in `mods/`). Plain 1.17 is not covered:
the jars accept 1.17.1 only (1.17 was not run).

## Differences from the 1.21.1 branch

Minecraft 1.17.1 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload`, no `GuiGraphics`, no
`TagKey`, no SLF4J, a static `Registry` and older loader APIs; the port keeps the behaviour wherever the game allows:

* No Sophisticated Backpacks on Fabric: the Fabric jar ships no backpack integration, the Building Upgrade items are
  placeholders without function (as on the loaders of other branches without Sophisticated Backpacks), and their
  recipes are only loaded when Sophisticated Backpacks is installed (`fabric:load_conditions` / `forge:mod_loaded`).
* Sophisticated Backpacks 1.17.1 has no Sophisticated Core: the upgrade framework (upgrade items and wrappers, upgrade
  containers, settings tabs, `IBackpackWrapper`) is in `net.p3pp3rf1y.sophisticatedbackpacks`. Its upgrade items are
  created in SB's creative tab (no-argument `UpgradeItemBase` constructor); the Building Upgrades override
  `allowdedIn` / `getCreativeTabs` so they are listed in this mod's tab (and the search tab) as on 1.21.1. SB 1.17.1 has
  no upgrade count limits: a second Building Upgrade in the same backpack is refused by the upgrade itself
  (`canAddUpgradeTo`, message "Only one building upgrade can be installed per backpack"), the same limit of one that
  1.21.1 sets through SB's count limit config; swapping one tier for another stays possible.
* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`), in vanilla `BlockItem.place` order. The randomizer bags keep
  their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag its weights in
  `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id. Forge
  identifies the messages of a `SimpleChannel` by their class, so all payloads travel on one channel,
  `sophisticatedbuilding:main`, as one message that carries the payload id before the body (`ForgeNetworking`); both
  sides need the mod (protocol version "1"). Forge 1.17.1 has no `consumerMainThread`: the handler queues itself on the
  main thread. A block set's nullable block state is written as a presence flag plus the state (the bytes
  `FriendlyByteBuf#writeNullable` writes on newer versions).
* Text components are `TextComponent` / `TranslatableComponent` / `KeybindComponent` (1.19 replaced them with
  `Component.literal`/`translatable`).
* GUI: the screens, widgets and HUD draw through `sophisticated.building.client.gui.GuiGraphics`, a shim with the
  1.20 `GuiGraphics` methods over `GuiComponent` helpers and a `PoseStack`. Immediate-mode quads end with
  `BufferBuilder#end()` + `BufferUploader.end(...)`. Math types are `com.mojang.math` (no JOML); creative tab, item
  and registry code use the static `Registry` and `Item.Properties#tab`. 1.17.1 widgets have `isHovered()` and
  `isFocused()` (no `isHoveredOrFocused()`).
* Rendering: 1.17.1 already has core shaders, `RenderType` and `VertexConsumer`, so the preview code is the 1.18 code.
  The composite `RenderType` factory is private (public from 1.19); the Fabric build opens it with an access widener
  (`fabric/src/main/resources/sophisticatedbuilding.accesswidener`), Forge's own access transformer already opens it.
  Model quads use a `java.util.Random`. Fabric draws the ghost block quads with its own copy of vanilla's
  `putBulkData` loop so the preview alpha is kept. Forge 1.17.1 has no render stages: previews, mirror/array lines,
  ghost blocks and then the outlines are drawn in `RenderWorldLastEvent`, in the order of the stages on the other
  loaders.
* Tags are `Tag<Item>` objects (no `TagKey`); logging is Log4j (`LogManager`, no SLF4J/`LogUtils`); the rail placement
  helper creates missing chunk sections with 1.17's `LevelChunkSection(sectionY)`.
* Block breaking: vanilla's `spawnAfterBreak` has no "drop experience" flag and always drops it (what 1.19+ is told
  to do); on Forge the experience is the break event's, popped after the drops, as on the 1.21.1 Forge build.
* Fabric: Fabric API 0.46 for 1.17.1 has the v1 command registration callback and no client world change event (world
  load/unload is detected at the start of each client tick); its mod id is `fabric`, so `fabric.mod.json` depends on
  `"fabric": ">=0.46.1"`. The JSON config backend reads with Gson 2.8.0 (`new JsonParser().parse`, `entrySet`).
* Forge 1.17.1 names: `WorldEvent`, `TickEvent.WorldTickEvent`, `InputEvent.KeyInputEvent`, `GuiOpenEvent`,
  `RenderWorldLastEvent`, `FMLServerStoppedEvent` (`net.minecraftforge.fmlserverevents`),
  `ClientPlayerNetworkEvent.LoggedInEvent`/`LoggedOutEvent`, `event.world.BlockEvent`; networking in
  `net.minecraftforge.fmllegacy.network`. The HUD is an `OverlayRegistry` overlay above the crosshair plus
  `RenderGameOverlayEvent.Post` for the whole HUD (`ElementType.ALL`); key mappings are registered with
  `net.minecraftforge.fmlclient.registry.ClientRegistry` in client setup; deferred registers are created from
  `ForgeRegistries`; the power level capability is registered in `RegisterCapabilitiesEvent` (no
  `@AutoRegisterCapability`); model quads are asked with `EmptyModelData` and the drawn layer set through
  `ForgeHooksClient.setRenderLayer`; the tooltip line breaking uses the selected language's `Locale`.
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End);
  the SB backpack wrapper comes from the stack's `CapabilityBackpackWrapper` capability; fake players are skipped by the
  event handlers; no `SpecialPlantable`; no generic config screen (the config files are the same); the randomizer bags
  have no item handler capability. JetBrains annotations are compile-only (Forge 1.17.1 does not provide them).
* Data files use the pre-1.21 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`. The Forge
  `pack.mcmeta` declares pack format 7 (Minecraft 1.17.1, resource and data packs).
* Player Settings screen (`PlayerSettingsGui`): 1.17.1 screens do not draw the dimmed world themselves and have no
  widget tooltips (`Tooltip`, `setTooltipForNextRenderPass` are 1.19.3+): the screen draws the background, then the
  hovered setting's tooltip (label or control) after everything else; the list switches off its dirt background and
  dirt bands and clips its rows to its area instead; buttons are `new Button(...)` (no builder), widget positions are
  the public `x`/`y` fields. The bag title tooltip is drawn by the bag screens' `render` the same way.
* `ForgeConfigSpec.ConfigValue` of Forge 37 has no `getDefault()`: `ForgeConfigHelper` keeps the default each
  value was defined with (used by "Reset to Defaults"); `set` and `ForgeConfigSpec#save()` exist.
* The merge-undo GameTests leave out pink petals (Minecraft 1.20+); short grass is `Blocks.GRASS` in 1.17.1 and a player
  changes level with `ServerPlayer#setLevel`.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, Forge: Gradle 8.14.5). Gradle runs on Java 21;
the mod is compiled for and run on Java 16 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests (117)
cd fabric && ./gradlew runGametest    # 37 in-world GameTests (not part of build)
cd forge  && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests (103)
./build-all.ps1                       # both, stops at the first failure
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
(headless: Fabric's game test server; on Forge, which has no game test server before 1.18, a dedicated dev server that
runs the same scenarios as vanilla game tests) in either loader folder run the in-game smoke scenarios and write
`<dir>/smoketest-result.json`; the game exits by itself. Forge also runs the Sophisticated Backpacks checks (`sb.*`);
Fabric has no backpack integration on 1.17.1 and runs none. The harness (`common/src/smoketest`,
`common/src/smoketestBackpacks` (Forge only), `<loader>/src/smoketest`, `gradle/smoketest.gradle`) is dev-only and never
packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port adopts it.

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric; Forge 1.17.1 has no game test server); the
Forge runs have Sophisticated Backpacks in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

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
even though the mod itself compiles for and runs on Java 16. CI reads `ci_gradle_jdk` per loader and defaults to 21
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
