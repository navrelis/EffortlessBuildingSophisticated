# Sophisticated Building - Minecraft 26.1.2

This branch (`mc/26.1.2`) holds Sophisticated Building for Minecraft 26.1.2 on Fabric, NeoForge and Forge (the Fabric
and Forge jars also run on 26.1 and 26.1.1). It was ported from `mc/1.21.11` and keeps its layout: loader-neutral
code lives once in `common/`, and every loader folder is a standalone Gradle build that compiles `common/` together
with its own sources into one mod jar.

Declared Minecraft versions, each one run (TESTING.md):

* Fabric `>=26.1 <=26.1.2` and Forge `[26.1,26.1.2]`: 26.1, 26.1.1 and 26.1.2 are the same game for this mod; the
  release jars passed `runSmokeServer` on 26.1 and 26.1.1 and `runSmokeClient` on 26.1 (Fabric API 0.155.3+26.1.2 on
  all three; Forge 62.0.9 for 26.1, 63.0.2 for 26.1.1). The Forge jar accepts Forge 62.0.9 and later.
* NeoForge `[26.1.2]` only: NeoForge for 26.1 and 26.1.1 never left beta, and the Sophisticated Backpacks/Core
  builds for them are older, with a different API (see `docs/PORTING.md` on `main`); 26.1.2 renamed `BlockEvent.BreakEvent`.

Minecraft is unobfuscated since 26.1: no mappings (no Mojang mappings, no Parchment), Java 25.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets (item model definitions in assets/<modid>/items), data (recipes carry Fabric,
                             NeoForge and Forge load conditions), mixin config (GuiGraphicsExtractor accessor), GUI stencil shader
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev only, see TESTING.md)
  src/smoketestBackpacks     Sophisticated Backpacks fixture of the harness (NeoForge only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, GameTests
                           (src/gametest); no backpack integration (no Sophisticated Backpacks for Fabric 26.1.2)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 26.1.2)
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
| Minecraft declared | 26.1 - 26.1.2 | 26.1.2 | 26.1 - 26.1.2 |
| Loader (built against) | Loader 0.19.5, Fabric API 0.155.3+26.1.2 | 26.1.2.109 | 64.1.3 |
| Minimum declared | Loader 0.19.5, Fabric API 0.155.3 | 26.1.2.109; Backpacks 3.26.2, Core 1.5.0 (optional) | 62.0.9 (run on 62.0.9, 63.0.2, 64.1.3) |
| Build plugin, Gradle | Loom 1.18.2 (`fabric-loom`, no remapping), Gradle 9.8.0 | ModDevGradle 2.0.147, Gradle 9.2.1 | ForgeGradle 7.0.40, Gradle 9.5.0 |
| Sophisticated Backpacks | none | Backpacks 26.1.2-3.26.2.2156, Core 26.1.2-1.5.0.2334 (Modrinth maven) | none |

Mappings: none (unobfuscated Minecraft). Java 25. Curios 15.0.0+26.1.2
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
  draw translucent models with `RenderTypes.translucentMovingBlock()` (1.21.5: `translucent()`), and
  `IClientHelper#collectModelParts` no longer takes a render type.
* **Minecraft 1.21.9 input rework:** screens and widgets receive `MouseButtonEvent` / `KeyEvent` / `CharacterEvent`
  records instead of raw coordinates, buttons and GLFW codes (`mouseClicked(event, doubleClick)`,
  `onClick(event, doubleClick)`, `keyPressed(event)`, ...); `IClientHelper#matchesKey` / `#isActiveAndMatches` take the
  `KeyEvent`. `Screen.hasShiftDown()` etc. moved to `Minecraft#hasShiftDown()`; `Window#handle()` is the GLFW handle.
* **Minecraft 1.21.9 key categories:** a key mapping takes a registered `KeyMapping.Category` instead of a translation
  key. The mod's keys are in `sophisticatedbuilding:main` (translation key `key.category.sophisticatedbuilding.main`,
  was `key.sophisticatedbuilding.category`), created through `IClientHelper#createKeyCategory` (Fabric and Forge
  register it with vanilla, NeoForge in `RegisterKeyMappingsEvent#registerCategory`).
* **Minecraft 1.21.9 selection lists:** list entries are positioned by the list (`renderContent` with the entry's own
  `getX()`/`getContentY()`, `children()` is read-only). The modifier screen adds, removes and moves its entries
  through `ModifiersScreenList` methods (moving keeps the scroll position, as before); the removed
  `headerHeight = 3` is kept as a 3 pixel gap above each entry, so the panels sit where they did.
* **Minecraft 1.21.10 other:** `GuiGraphics#renderOutline` is `submitOutline` (1.21.10 only: drawn with the deferred
  elements, on top of the screen; the scroll inputs' focus frame), `Level#isClientSide()` is a method,
  `GameProfile#name()`, `LevelChunkSection(PalettedContainerFactory)` (`Level#palettedContainerFactory()`), the GUI
  render state `buildVertices` has no z. Removed unused widgets that no longer compile (`SlotGui`, `GuiScrollPane`,
  `GuiCollapsibleScrollEntry`).
* **Minecraft 1.21.11:**
  - `ResourceLocation` is `Identifier` (payload ids, key mapping categories, pipelines, textures, packets).
  - `RenderType` moved to `client.renderer.rendertype` and is a pipeline plus a `RenderSetup` (no `CompositeState`,
    no static factories; the vanilla ones are in `RenderTypes`). `OutlineRenderTypes` and `BuildRenderTypes` are plain
    holders of `RenderType.create(name, RenderSetup)`; the line width of the mirror/array lines is a vertex attribute
    (`setLineWidth(2)` on every vertex, it was `LineStateShard(2)`). The (unused) opaque ghost block renderer draws with
    `RenderTypes.solidMovingBlock()` (`RenderType.solid()` is gone; the chunk layers have their own pipelines).
  - `AbstractButton#renderWidget` is final (it also sets the cursor): the mod's buttons override `renderContents`.
    `Screen#init`/`resize` no longer take the `Minecraft`.
  - `GuiGraphics#renderOutline` is back and draws at once again (four fills; 1.21.10 had only the deferred
    `submitOutline`): the scroll inputs' focus frame uses it, as on 1.21.8. `TextureSetup.singleTexture` takes the
    texture's `GpuSampler`.
  - Game rules are typed (`GameRules.BLOCK_DROPS`, `level.gamerules` package); the "water evaporates" check of the
    block placement/break helpers is the `EnvironmentAttributes.WATER_EVAPORATES` attribute at the position (was
    `DimensionType#ultraWarm`); `/powerlevel` requires `Commands.LEVEL_GAMEMASTERS` (permission level 2 as before);
    `Camera#position()`; `net.minecraft.util.Util`; `MethodsReturnNonnullByDefault` is gone (annotation dropped).
  - NeoForge and Forge: `VertexConsumer#putBulkData` has no `readExistingColor` flag any more (quads have no
    per-vertex colour array; NeoForge multiplies the quad's baked colours in itself). Forge 61: `KeyMapping`
    constructors take a sort order (0, as vanilla's default).
* **Minecraft 26.1 (26.1.2):**
  - Unobfuscated game, Java 25: no mappings in any build; Fabric uses the non-remapping Loom plugin (`fabric-loom`,
    plain `implementation` dependencies, the mod jar comes from `jar`).
  - `GuiGraphics` is `GuiGraphicsExtractor`: screens and widgets fill it in `extractRenderState` /
    `extractWidgetRenderState` / `extractContents` (buttons) / `extractContent` (list entries) / `extractLabels` /
    `extractBackground` instead of `render*`; its draw methods are `text`, `centeredText`, `item`, `itemDecorations`,
    `outline`, `tooltip` (were `drawString`, `drawCenteredString`, `renderItem`, `renderItemDecorations`,
    `renderOutline`, `renderTooltip`). The GUI render state moved to `client.renderer.state.gui` and takes elements with
    `addGuiElement`; the accessor mixin `GuiGraphicsAccessor` now targets `GuiGraphicsExtractor`. The mod's own element
    and icon classes keep their `render` methods.
  - Container screens: the image size is final and passed to the constructor (`super(menu, inventory, title, 176,
    134)`), `renderBg` is gone (the randomizer bags draw their background and missing-item overlay in
    `extractBackground` after `super`), and vanilla draws the hovered-slot tooltip itself (the bags' own call is gone).
  - Block models: no `BlockRenderDispatcher`/static `ModelBlockRenderer.renderModel`; the ghost blocks take the model
    from `ModelManager#getBlockStateModelSet()`, `BlockStateModel`/`BlockStateModelPart` live in
    `client.renderer.block.dispatch`, `BakedQuad` is a record in `client.resources.model.geometry`, and the quads are
    written with `VertexConsumer#putBakedQuad(pose, quad, QuadInstance)` (colour, light and overlay in the
    `QuadInstance`; NeoForge multiplies the baked quad colours in itself). `IClientHelper#putQuad` is gone (the same
    call on every loader, now in `GhostBlockRenderer`); `collectModelParts` fills a list (`collectParts(random, list)`,
    NeoForge with `BlockAndTintGetter.EMPTY`, Forge with `ModelData.EMPTY`).
  - Render pipelines: depth test and depth writes are one optional `DepthStencilState` (the mirror/array lines and
    planes have none: no depth test, no depth writes, as before), blending is a `ColorTargetState`.
    `LightTexture.FULL_BRIGHT` is `LightCoordsUtil.FULL_BRIGHT`, `LevelRenderer#getLightCoords`. The vendored Catnip
    render buffer mirrors vanilla's new fixed buffers (item and block-item sheets; the chest, sign, bed, shield and
    shulker sheets are gone).
  - `Player#displayClientMessage(msg, actionBar)` is `sendOverlayMessage` / `sendSystemMessage`; `Level#random` is
    protected (`getRandom()`); `ClickType` is `ContainerInput`.
* Fabric API 0.155 uses Mojang's names: `KeyMappingHelper`, `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE`,
  `LevelRenderEvents.END_MAIN` (`context.poseStack()`), `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`,
  `ServerTickEvents.END_LEVEL_TICK`, `PayloadTypeRegistry.serverboundPlay()`/`clientboundPlay()`.
* NeoForge 26.1.2: `BlockEvent.BreakEvent` is `event.level.block.BreakBlockEvent` (same use: cancelling it denies the
  break); the particles are drawn in two passes, the outlines follow the second one
  (`RenderLevelStageEvent.AfterTranslucentParticles`, was `AfterParticles`).
* Forge 64: `ModList` is static (`ModList.isLoaded`). The frame pass binds its target in the two-argument
  `PassDefinition#extracts(bundle, pass)`: Forge 62/63 have only that one (abstract), Forge 64 added a `DeltaTracker`
  overload that calls it; overriding the new overload failed on Forge 62 with an `AbstractMethodError` (found by the
  26.1 client run). `pack.mcmeta` declares the Forge 64 MDK's range, `min_format` `[101, 1]` .. `max_format` 101
  (26.1 to 26.1.2 data packs are 101.1).
* Fabric: no Sophisticated Backpacks integration (the Building Upgrades are placeholder items, their recipes are not
  loaded). The HUD is registered with `HudElementRegistry.addLast` (Fabric API for 1.21.6+ deprecates
  `HudRenderCallback`). Fabric API for 1.21.9+ has no `WorldRenderEvents.AFTER_TRANSLUCENT`: the previews, lines and
  outlines are drawn at `WorldRenderEvents.END_MAIN` (end of the main pass, after the translucent terrain, before
  particles and weather).
* NeoForge: Sophisticated Backpacks 3.26 (`UpgradeItemBase` takes the item properties). NeoForge 21.8: the power level
  attachment serializer writes a `ValueOutput` (same `powerLevel` key, existing player data keeps loading); one
  `RenderLevelStageEvent` subclass per stage (`AfterTranslucentBlocks`, `AfterParticles`; 26.1.2: see above); client packets go through
  `ClientPacketDistributor`; bidirectional payloads register both handlers with `playBidirectional`.
  NeoForge 21.10 replaced the item handler capabilities with the transfer API: the randomizer bags expose their
  container component as `Capabilities.Item.ITEM` (`ItemAccessItemHandler`, was `ComponentItemHandler`), and the mod
  reads the bags from the component itself (`ItemStackHandler.BagItemStackHandler`, as on Fabric and Forge).
  Sophisticated Core 1.21.10 follows: the Building Upgrade takes blocks from the backpack with
  `InventoryHandler#extract` in a transaction (at most one stack per call, as the old `extractItem`).
  `FMLLoader.getCurrent()`, `FMLEnvironment.getDist()`.
* Forge 58+ (EventBus 7): listeners use `net.minecraftforge.eventbus.api.listener.SubscribeEvent`, mod-bus events are
  reached through `<Event>.getBus(BusGroup)` (Forge 60: `<Event>.BUS` for the key mapping and HUD layer events),
  cancelling listeners return `true`; a block break is denied with `Result.DENY` (Forge 58 checks the break event's
  result). The previews, mirror/array lines and outlines are drawn in a frame pass added through `AddFramePassEvent`
  (back in Forge 58), after all vanilla passes (so after the weather too, as on 1.21.4/1.21.5); 1.21.5's
  `LevelRendererMixin` is gone. Forge 60: tick events are records (`level()`, `player()`), `AttachCapabilitiesEvent`
  is split per type (`AttachCapabilitiesEvent.Entities`), the frame pass runs `executes(LevelRenderState)`, and the
  `eventbus-validator` annotation processor checks the listeners at compile time (7.0.5 on Forge 61). On 1.21.10
  `pack.mcmeta` declared `min_format` 64 .. `max_format` 88 (plus the legacy `pack_format`/`supported_formats`)
  because Forge 60 checks every mod pack, the data pack too, against the resource pack version 69. Forge 61 does not:
  the 1.21.11 `pack.mcmeta` declares the Forge 61 MDK's data pack range, `min_format` `[94, 1]` .. `max_format` 94,
  and the mod's data pack is enabled and compatible (`client.mod_data_pack_compatible`).

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.8.0, NeoForge: Gradle 9.2.1, Forge: Gradle 9.5.0).
Java 25, and Gradle itself must run on a JDK 25 (`JAVA_HOME`): Loom 1.18 and ModDevGradle for 26.x refuse to configure
on an older JVM (CI: `ci_gradle_jdk=25` in every loader's `gradle.properties`).

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

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge and Forge).
Only the NeoForge dev runtime has Sophisticated Backpacks/Core. Accept the EULA in `<loader>/run/eula.txt` for
`runServer`.

`runClientExported` (Fabric, NeoForge) starts a client that loads only the jars in `<loader>/run-exported/mods` (for
testing exported jars; on Fabric put Fabric API there too).
