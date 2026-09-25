# Sophisticated Building - Minecraft 1.21.8

This branch (`mc/1.21.8`) holds Sophisticated Building for Minecraft 1.21.8 on Fabric, NeoForge and Forge. It was
ported from `mc/1.21.5` and keeps its layout: loader-neutral code lives once in `common/`, and every loader folder is
a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

The jars declare exactly Minecraft 1.21.8: 1.21.6 and 1.21.7 were not run, so they are not claimed (Sophisticated
Backpacks for NeoForge 1.21.8 itself requires Minecraft 1.21.8).

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets (item model definitions in assets/<modid>/items), data (recipes carry Fabric,
                             NeoForge and Forge load conditions), mixin config (GuiGraphics accessor), GUI stencil shader
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev only, see TESTING.md)
  src/smoketestBackpacks     Sophisticated Backpacks fixture of the harness (NeoForge only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, GameTests
                           (src/gametest); no backpack integration (no Sophisticated Backpacks for Fabric 1.21.8)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.21.8)
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
| Loader (built against) | Loader 0.19.5, Fabric API 0.136.1+1.21.8 | 21.8.54 | 58.1.22 |
| Minimum declared | Loader 0.19.5 | 21.8.54 | 58.1.22 |
| Build plugin, Gradle | Loom 1.17.21, Gradle 9.5.1 | ModDevGradle 2.0.147, Gradle 9.2.1 | ForgeGradle 7.0.40, Gradle 9.3.1 |
| Sophisticated Backpacks | none | Backpacks 1.21.8-3.26.2.2159, Core 1.21.8-1.5.0.2342 (Modrinth maven) | none |

Mappings: Mojang (Fabric, Forge), Mojang + Parchment 2025.09.14 (NeoForge). Java 21. Curios 12.0.0+1.21.8
(NeoForge, compile only and in the smoke runtime).

## Differences to mc/1.21.1

* Minecraft 1.21.2+ needs an item's registry id in its `Item.Properties` before the item is created:
  `IPlatformHelper#registerItem` takes a `Function<Item.Properties, T>` and passes properties with the id set; the
  item constructors (and `IBackpackIntegration#createBuildingUpgrade`) take those properties.
* `Item#use` returns `InteractionResult`. The randomizer bag calls the template item's own `use`, not
  `ItemStack#use`, which would replace the bag in the hand with the template stack on 1.21.2+.
* Recipes use the 1.21.2 ingredient syntax (`"minecraft:dirt"`, `"#minecraft:planks"`); every registered item has a
  client item definition in `assets/sophisticatedbuilding/items/`.
* Removed dead vendored code that no longer compiles and is unused: `PartialItemModelRenderer`,
  `create.foundation.render.RenderTypes`, `TagDependentIngredientItem`.
* **Minecraft 1.21.5:** world render types (mirror/array lines and planes, outlines) are built on `RenderPipeline`s.
  `CompoundTag` getters return `Optional` (the mod uses `getIntOr`/`getBooleanOr`/...). Fabric GameTests use the
  1.21.5 `@GameTest` + test-environment JSON style; the run also reports vanilla's `minecraft:always_pass`.
* **Minecraft 1.21.6 GUI rewrite:** the GUI is drawn from render states; `GuiGraphics#pose()` is a 2D
  `Matrix3x2fStack` (no z: the draw order is the submission order, elements whose bounds overlap are layered over the
  earlier ones), `drawSpecial` and the GUI render types are gone, `blit` takes a `RenderPipeline`
  (`RenderPipelines.GUI_TEXTURED`).
  - Free-form GUI quads (radial menu ring segments and buttons, box widgets, rotated gradients, stencilled icons) are
    one GUI element each, `client.gui.GuiQuads`, submitted to the `GuiGraphics` render state through the accessor
    mixin `mixin.GuiGraphicsAccessor` (common mixin config, now loaded on all three loaders; Forge declares it in the
    jar manifest). They are not clipped by a GUI scissor (the mod draws none of them inside a scrolled area).
  - The stencilled-icon pipeline is `GuiPipelines.STENCIL_GRADIENT`; its fragment shader reads `ColorModulator` from
    the `DynamicTransforms` uniform block.
  - Text colours need an alpha channel (1.21.6 skips text with alpha 0): `0xFFFFFF` style colours became
    `0xFFFFFFFF`, `ChatFormatting` colours go through `ARGB.opaque`.
  - Tooltips are deferred (`setComponentTooltipForNextFrame`), and only one per frame: the Omega bag's weight tooltip
    is drawn at once in its own stratum so it still shows next to the item tooltip.
  - `Screen` draws its background (blur + menu background) before `render`: the radial menu overrides
    `renderBackground` to keep only its own fading gradient, as on 1.21.1.
  - `LayeredDraw` is gone: `MaterialCostOverlay` is a plain class; each loader registers its `render` as a HUD layer.
* **Minecraft 1.21.6 other:** block entity data is loaded through `ValueInput`
  (`TagValueInput.create(ProblemReporter.DISCARDING, ...)`); chunk layers are `ChunkSectionLayer`s, so the ghost blocks
  draw translucent models with `RenderType.translucentMovingBlock()` (1.21.5: `translucent()`), and
  `IClientHelper#collectModelParts` no longer takes a render type.
* Fabric: no Sophisticated Backpacks integration (the Building Upgrades are placeholder items, their recipes are not
  loaded). The HUD is registered with `HudElementRegistry.addLast` (Fabric API for 1.21.6+ deprecates
  `HudRenderCallback`).
* NeoForge: Sophisticated Backpacks 3.26 (`UpgradeItemBase` takes the item properties). NeoForge 21.8: the power level
  attachment serializer writes a `ValueOutput` (same `powerLevel` key, existing player data keeps loading); one
  `RenderLevelStageEvent` subclass per stage (`AfterTranslucentBlocks`, `AfterParticles`); client packets go through
  `ClientPacketDistributor`; bidirectional payloads register both handlers with `playBidirectional`.
* Forge 58 (EventBus 7): listeners use `net.minecraftforge.eventbus.api.listener.SubscribeEvent`, mod-bus events are
  reached through `<Event>.getBus(BusGroup)`, cancelling listeners return `true`; a block break is denied with
  `Result.DENY` (Forge 58 checks the break event's result). The previews, mirror/array lines and outlines are drawn in
  a frame pass added through `AddFramePassEvent` (back in Forge 58), after all vanilla passes (so after the weather
  too, as on 1.21.4/1.21.5); 1.21.5's `LevelRendererMixin` is gone.

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, NeoForge: Gradle 9.2.1, Forge: Gradle 9.3.1).
Java 21.

```
cd fabric   && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric   && ./gradlew runGametest    # in-world GameTests (not part of build)
cd neoforge && ./gradlew build          # jar in neoforge/build/libs, runs the common unit tests
cd forge    && ./gradlew build          # jar in forge/build/libs, runs the common unit tests
./build-all.ps1                         # all three, stops at the first failure
```

In-game smoke tests (`gradlew runSmokeClient` / `runSmokeServer` in every loader folder, `sb.*` checks on NeoForge):
see [TESTING.md](TESTING.md).

Forge: the first build sets up Minecraft through ForgeGradle's Mavenizer (several minutes); do not run it in parallel
with another ForgeGradle 7 build on a cold cache. The Forge dev runs (`runClient`, `runServer`, `runGameTestServer`)
have no Sophisticated Backpacks and no `runClientExported`.

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

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge and Forge).
Only the NeoForge dev runtime has Sophisticated Backpacks/Core. Accept the EULA in `<loader>/run/eula.txt` for
`runServer`.

`runClientExported` (Fabric, NeoForge) starts a client that loads only the jars in `<loader>/run-exported/mods` (for
testing exported jars; on Fabric put Fabric API there too).
