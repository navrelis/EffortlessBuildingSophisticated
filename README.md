# Sophisticated Building - Minecraft 1.21.10

This branch (`mc/1.21.10`) holds Sophisticated Building for Minecraft 1.21.10 on Fabric, NeoForge and Forge. It was
ported from `mc/1.21.8` and keeps its layout: loader-neutral code lives once in `common/`, and every loader folder is
a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

The jars declare exactly Minecraft 1.21.10: 1.21.9 was not run, so it is not claimed (Sophisticated Backpacks for
NeoForge 1.21.10 itself requires Minecraft 1.21.10).

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
                           (src/gametest); no backpack integration (no Sophisticated Backpacks for Fabric 1.21.10)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.21.10)
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
| Loader (built against) | Loader 0.19.5, Fabric API 0.138.4+1.21.10 | 21.10.64 | 60.1.15 |
| Minimum declared | Loader 0.19.5 | 21.10.64 | 60.1.15 |
| Build plugin, Gradle | Loom 1.17.21, Gradle 9.5.1 | ModDevGradle 2.0.147, Gradle 9.2.1 | ForgeGradle 7.0.40, Gradle 9.3.1 |
| Sophisticated Backpacks | none | Backpacks 1.21.10-3.26.2.2151, Core 1.21.10-1.5.0.2339 (Modrinth maven) | none |

Mappings: Mojang (Fabric, Forge), Mojang + Parchment 2025.10.12 (NeoForge). Java 21. Curios 13.0.0+1.21.10
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
* Fabric: no Sophisticated Backpacks integration (the Building Upgrades are placeholder items, their recipes are not
  loaded). The HUD is registered with `HudElementRegistry.addLast` (Fabric API for 1.21.6+ deprecates
  `HudRenderCallback`). Fabric API for 1.21.9+ has no `WorldRenderEvents.AFTER_TRANSLUCENT`: the previews, lines and
  outlines are drawn at `WorldRenderEvents.END_MAIN` (end of the main pass, after the translucent terrain, before
  particles and weather).
* NeoForge: Sophisticated Backpacks 3.26 (`UpgradeItemBase` takes the item properties). NeoForge 21.8: the power level
  attachment serializer writes a `ValueOutput` (same `powerLevel` key, existing player data keeps loading); one
  `RenderLevelStageEvent` subclass per stage (`AfterTranslucentBlocks`, `AfterParticles`); client packets go through
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
  `eventbus-validator` annotation processor checks the listeners at compile time. `pack.mcmeta` declares
  `min_format` 64 .. `max_format` 88 (plus the legacy `pack_format`/`supported_formats`): Forge 60 checks every mod
  pack, the data pack too, against the resource pack version 69, so a data-only declaration (88, as in the Forge MDK)
  is flagged `TOO_NEW` (Forge's own pack is).

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

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge and Forge).
Only the NeoForge dev runtime has Sophisticated Backpacks/Core. Accept the EULA in `<loader>/run/eula.txt` for
`runServer`.

`runClientExported` (Fabric, NeoForge) starts a client that loads only the jars in `<loader>/run-exported/mods` (for
testing exported jars; on Fabric put Fabric API there too).
