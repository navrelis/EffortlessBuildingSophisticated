# Sophisticated Building - Minecraft 1.21.5

This branch (`mc/1.21.5`) holds Sophisticated Building for Minecraft 1.21.5 on Fabric, NeoForge and Forge. It was
ported from `mc/1.21.4` and keeps its layout: loader-neutral code lives once in `common/`, and every loader folder is
a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets (item model definitions in assets/<modid>/items), data (recipes carry Fabric,
                             NeoForge and Forge load conditions), mixin config, GUI stencil shader
  src/test/java              unit tests, run by every loader build
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, GameTests
                           (src/gametest, the 1.21.5 @GameTest + test-environment JSON style); no backpack
                           integration (no Sophisticated Backpacks for Fabric 1.21.5)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability, world-render mixin (no Forge event replaces
                           RenderLevelStageEvent on 1.21.5); no backpack integration (no Sophisticated Backpacks for
                           Forge 1.21.5)
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
| Loader (built against) | Loader 0.19.5, Fabric API 0.128.2+1.21.5 | 21.5.98 | 55.0.24 (also run on 55.1.14) |
| Minimum declared | Loader 0.19.5 | 21.5.98 | 55.0.24 |
| Build plugin, Gradle | Loom 1.17.21, Gradle 9.5.1 | ModDevGradle 2.0.147, Gradle 9.2.1 | ForgeGradle 7.0.40, Gradle 9.3.1 |
| Sophisticated Backpacks | none | Backpacks 1.21.5-3.27.2.2152, Core 1.21.5-1.5.0.2338 (Modrinth maven) | none |

Mappings: Mojang (Fabric, Forge), Mojang + Parchment 2025.06.15 (NeoForge). Java 21.

## Differences to mc/1.21.1

* Minecraft 1.21.2+ needs an item's registry id in its `Item.Properties` before the item is created:
  `IPlatformHelper#registerItem` takes a `Function<Item.Properties, T>` and passes properties with the id set; the
  item constructors (and `IBackpackIntegration#createBuildingUpgrade`) take those properties.
* `Item#use` returns `InteractionResult`. The randomizer bag calls the template item's own `use`, not
  `ItemStack#use`, which would replace the bag in the hand with the template stack on 1.21.2+.
* Recipes use the 1.21.2 ingredient syntax (`"minecraft:dirt"`, `"#minecraft:planks"`); every registered item has a
  client item definition in `assets/sophisticatedbuilding/items/`.
* GUI: `GuiGraphics#blit`/`blitSprite` take the render type (`RenderType::guiTextured`), tints are passed as the blit
  color instead of the shader color, and immediate-mode draws (radial menu, box widgets, gradients, stencils) flush
  the batched `GuiGraphics` first so the draw order stays as on 1.21.1.
* Render types: `TextureStateShard` takes a `TriState`; the removed entity-translucent-cull shader is replaced by the
  entity-translucent shader with culling.
* Removed dead vendored code that no longer compiles and is unused: `PartialItemModelRenderer`,
  `create.foundation.render.RenderTypes`, `TagDependentIngredientItem`.
* **Minecraft 1.21.5:** `RenderPipeline` replaces the old render-state/shader-instance API; every world and GUI
  render type used for the previews, mirror/array lines, outlines and radial menu (lines, planes, stencil, box
  widgets) is rebuilt on a `RenderPipeline`/`RenderPipelines` snippet instead of a `RenderType.CompositeState`.
  `CompoundTag` getters return `Optional`; the mod uses `getIntOr`/`getBooleanOr`/... instead of the old
  `getInt`/`getBoolean`. Fabric's GameTest framework was reworked: tests are `@GameTest`-annotated methods run
  against a named test-environment JSON (`src/gametest/resources/data/.../test_environment/*.json`) instead of a
  structure-template class; the harness also runs vanilla's own `minecraft:always_pass` self-test alongside this
  mod's 17.
* Fabric: no Sophisticated Backpacks integration (the Building Upgrades are placeholder items, their recipes are not
  loaded). `fabric.mod.json` declares exactly Minecraft 1.21.5.
* NeoForge: Sophisticated Backpacks 3.27 (`UpgradeItemBase` takes the item properties; its `getName(ItemStack)`
  already returns the translated description id, so the Building Upgrade's own override is gone).
* Forge: Forge 55 removes `RenderLevelStageEvent` and adds **no replacement event** for rendering into the level
  (unlike Forge 54's `AddFramePassEvent`, which 1.21.4 used). The previews, mirror/array lines and outlines are
  drawn through a client-only Mixin (`forge/.../mixin/LevelRendererMixin`, `@Inject` at the tail of
  `LevelRenderer#addLateDebugPass`) instead of a supported Forge API, after all vanilla passes (so after the
  weather too, same visual position as on 1.21.4). The mixin config is declared in the jar manifest
  (`MixinConfigs: sophisticatedbuilding.forge.mixins.json`) and loaded on `--mixin.config=...` in the dev runs;
  Forge 1.21.5 runs Mojang (official) names at runtime, so no refmap is needed or produced.

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
