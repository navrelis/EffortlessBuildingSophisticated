# Sophisticated Building - Minecraft 1.21.4

This branch (`mc/1.21.4`) holds Sophisticated Building for Minecraft 1.21.4 on Fabric, NeoForge and Forge. It was
ported from `mc/1.21.1` and keeps its layout: loader-neutral code lives once in `common/`, and every loader folder is
a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets (incl. the 1.21.4 item model definitions in assets/<modid>/items), data (recipes
                             carry Fabric, NeoForge and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, GameTests
                           (src/gametest); no backpack integration (no Sophisticated Backpacks for Fabric 1.21.4)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.21.4)
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
| Loader (built against) | Loader 0.19.5, Fabric API 0.119.4+1.21.4 | 21.4.157 | 54.1.5 |
| Minimum declared | Loader 0.19.5 | 21.4.157 | 54.1.5 (also tested on 54.1.18) |
| Build plugin, Gradle | Loom 1.17.21, Gradle 9.5.1 | ModDevGradle 2.0.147, Gradle 9.2.1 | ForgeGradle 7.0.40, Gradle 9.3.1 |
| Sophisticated Backpacks | none | Backpacks 1.21.4-3.27.2.2153, Core 1.21.4-1.5.0.2336 (Modrinth maven) | none |

Mappings: Mojang (Fabric, Forge), Mojang + Parchment 2025.03.23 (NeoForge). Java 21.

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
* Fabric: no Sophisticated Backpacks integration (the Building Upgrades are placeholder items, their recipes are not
  loaded). `fabric.mod.json` declares exactly Minecraft 1.21.4.
* NeoForge: Sophisticated Backpacks 3.27 (`UpgradeItemBase` takes the item properties; its `getName(ItemStack)`
  already returns the translated description id, so the Building Upgrade's own override is gone).
* Forge: Forge 54 has no `RenderLevelStageEvent`; the previews and outlines are drawn in a frame pass added through
  `AddFramePassEvent`, after all vanilla passes (so after the weather too).

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

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge). Only the
NeoForge dev runtime has Sophisticated Backpacks/Core. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` (Fabric, NeoForge) starts a client that loads only the jars in `<loader>/run-exported/mods` (for
testing exported jars; on Fabric put Fabric API there too).
