# Sophisticated Building - Minecraft 1.16.5 and 1.16.4

This branch (`mc/1.16.5`) holds Sophisticated Building for Minecraft 1.16.5 and 1.16.4 on Fabric and Forge: one Fabric
jar covers both, Forge has a jar per version (`forge/` for 1.16.5, `forge-1.16.4/` for 1.16.4). It is a port of the
`mc/1.17.1` / `mc/1.18.x` / `mc/1.19.2` / `mc/1.20.1` / `mc/1.21.1` branches with the same layout: loader-neutral code
lives once in `common/`, and every loader folder is a standalone Gradle build that compiles `common/` together with its
own sources into one mod jar. Sophisticated Backpacks exists for Forge 1.16.x only (the unofficial Fabric port starts at
1.19.2), so the Forge jars have the backpack integration and the Fabric jar has none.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry both Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness and the server test runner (dev-only, see TESTING.md);
                             src/smoketestBackpacks: its SB fixture (Forge only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, access widener,
                           server tests of the building rules (src/gametest)
forge/                     Forge build (Architectury Loom): entry points, platform services, ForgeConfigSpec configs,
                           power level capability, Sophisticated Backpacks integration (official build) with Curios fallback
forge-1.16.4/              Forge build for Minecraft 1.16.4 (Forge 35): compiles ../forge with the classes Sophisticated
                           Backpacks 1.16.4 needs replaced (see "Minecraft 1.16.4" below)
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

| | Fabric | Forge (`forge/`) | Forge 1.16.4 (`forge-1.16.4/`) |
|---|---|---|---|
| Minecraft | 1.16.4 and 1.16.5 (`fabric.mod.json`: `>=1.16.4 <=1.16.5`) | 1.16.5 (`mods.toml`: `[1.16.5]`) | 1.16.4 (`mods.toml`: `[1.16.4]`) |
| Java | 8 | 8 | 8 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.42.0+1.16 (minimum 0.42.0, mod id `fabric`) | Forge 36.2.42 (minimum 36.2.42, `loaderVersion` `[36,)`) | Forge 35.1.37 (minimum 35.1.37, `loaderVersion` `[35,)`) |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | Architectury Loom 1.17.493, Gradle 9.5.1, official Mojang mappings, reobfuscated to SRG names | as `forge/` |
| Sophisticated Backpacks | none (no Fabric port for 1.16.x) | official build 1.16.5-3.15.20.755 (file 4167327, CurseMaven, its latest 1.16.5 release; no separate Sophisticated Core on 1.16.x), optional `[1.16.5-3.15.20,)` | official build 1.16.4-3.0.0.289 (file 3399778, its latest 1.16.4 release), optional `[1.16.4-3.0.0.289,1.16.5)` |
| Curios / Trinkets | - | Curios 1.16.5-4.1.0.0 (Modrinth), compile only (and smoke runtime) | the same Curios build (declared for 1.16.4 and 1.16.5) |

Forge 36.2.42 is the latest Forge for 1.16.5; the Forge jar was also started on a real Forge 1.16.5 server (installer
36.2.42, only this jar plus Sophisticated Backpacks and Curios in `mods/`), the Fabric jar on real Fabric 1.16.5 and
1.16.4 servers (Fabric Loader 0.19.5, only this jar plus Fabric API 0.42.0+1.16 in `mods/`). Minecraft 1.16.4: see
"Minecraft 1.16.4" below.

Why Architectury Loom for Forge: ForgeGradle 6's `official` mappings for Minecraft 1.16.5 only rename fields and
methods; the classes keep their MCP names (`net.minecraft.util.ResourceLocation`, `net.minecraft.entity.player.PlayerEntity`,
...), while `common/` is written against the Mojang class names that Fabric 1.16.5 and every later Forge use.
Architectury Loom maps Forge 1.16.5 to the full Mojang names in the dev environment and reobfuscates the jar to Forge's
SRG runtime names (`remapJar`), so `common/` compiles unchanged for both loaders. ModDevGradle Legacy does not support
1.16. Architectury Loom's one flaw here: in the dev runtime of Forge 36.2 it leaves two calls of vanilla `BlockMath` under their SRG
names (Forge 36.2 moved `Transformation#compose`/`inverse` into default methods of `IForgeTransformationMatrix`, which
Loom renames), so every dev client crashed while baking models (`NoSuchMethodError: Transformation.func_227987_b_`);
`forge/build.gradle` gives `runClient`/`runSmokeClient` a copy of Loom's named Minecraft jar in which `BlockMath` calls
`compose`/`inverse` (`fixDevMinecraft`, dev runtime only; the mod jar and real Forge are not affected). Forge 35
(1.16.4) keeps the methods in the class and needs no fix.

## Differences from the 1.21.1 branch

Minecraft 1.16.5 runs on Java 8 and has no core shaders, no data components, no `StreamCodec`, no vanilla
`CustomPacketPayload`, no `GuiGraphics`, no `TagKey`, no SLF4J, no game test framework, a static `Registry` and older
loader APIs; the port keeps the behaviour wherever the game allows:

* Java 8: records became final classes with the same accessors, switch expressions and `instanceof` patterns plain
  Java 8 code, `var` explicit types, `List.of`/`Set.of`/`Map.of` the `Collections`/`Arrays` equivalents (unmodifiable
  where the original was), and the Java 9+ APIs (`Stream#toList`, `Optional#isEmpty/or`, `ServiceLoader#findFirst`,
  `Files#readString/writeString`, `String#strip/isBlank`) their Java 8 counterparts. No behaviour change.
* Content Minecraft 1.16.5 does not have: the compressed cobbled deepslate is crafted from (and decompressed into) 9
  blackstone, the Diamond and Omega randomizer bags use prismarine crystals where the newer versions use amethyst
  shards; candles and glow lichen (counted/vine-like blocks of the placement helpers) do not exist; the "dirt path"
  item requirement is the grass path; the cauldron checks look for the cauldron block (no cauldron tag).
* No Sophisticated Backpacks on Fabric: the Fabric jar ships no backpack integration, the Building Upgrade items are
  placeholders without function (as on the loaders of other branches without Sophisticated Backpacks). Fabric API 0.42
  has no resource conditions (`fabric:load_conditions`), so the Building Upgrade recipes are left out of the Fabric jar
  instead of being skipped at load time (same result, no recipe errors in the log). Forge loads them only when
  Sophisticated Backpacks is installed (`forge:mod_loaded`).
* Sophisticated Backpacks 1.16.5 has no Sophisticated Core: the upgrade framework (upgrade items and wrappers, upgrade
  containers, settings tabs, `IBackpackWrapper`) is in `net.p3pp3rf1y.sophisticatedbackpacks`. Its upgrade items are
  created in SB's creative tab (no-argument `UpgradeItemBase` constructor); the Building Upgrades override
  `allowdedIn` / `getCreativeTabs` so they are listed in this mod's tab (and the search tab) as on 1.21.1. SB 1.16.5 has
  no upgrade count limits: a second Building Upgrade in the same backpack is refused by the upgrade itself
  (`canAddUpgradeTo`, message "Only one building upgrade can be installed per backpack"), the same limit of one that
  1.21.1 sets through SB's count limit config. The player's backpacks are scanned through the instance of
  `PlayerInventoryProvider` that `SophisticatedBackpacks.PROXY` holds (its consumer also gets the slot identifier).
  SB 1.16.5's Tool Swapper has one tool filter per tool type instead of one allow/deny list: a backpack tool is used
  when the filter of one of its tool types lets it through (tools without a tool type, such as shears, always).
* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`), in vanilla `BlockItem.place` order. The randomizer bags keep
  their inventory in the stack's `Items` tag (vanilla container list format), the Omega bag its weights in
  `SlotWeights`; "a stack with data" (placement templates) means a non-empty tag.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Fabric sends them on the channel named by the id. Forge
  identifies the messages of a `SimpleChannel` by their class, so all payloads travel on one channel,
  `sophisticatedbuilding:main`, as one message that carries the payload id before the body (`ForgeNetworking`); both
  sides need the mod (protocol version "1"). Forge 1.16.5 has no `consumerMainThread`: the handler queues itself on the
  main thread. A block set's nullable block state is written as a presence flag plus the state, a stack list as a
  var-int count plus the stacks (what `FriendlyByteBuf#writeNullable` / `writeCollection` write on newer versions).
* Text components are `TextComponent` / `TranslatableComponent` / `KeybindComponent`; colours are `TextColor`s.
* Rendering: Minecraft 1.16.5 has no core shaders. The outline, ghost block and preview render types use the
  fixed-function states of vanilla's entity solid / entity translucent (cull) render types (texture, transparency,
  diffuse lighting, alpha test, lightmap, overlay) instead of the entity shaders, and the build lines a 2 px GL line
  width; draw modes are GL constants. GUI quads that the newer versions draw with the position-color shader are drawn
  untextured with smooth shading and no alpha test (`GuiGraphics.beginPositionColor/endPositionColor`, what vanilla's
  gradient fill does), textures are bound through the texture manager and colours set with `RenderSystem.color4f`.
  The screenshots of the smoke runs show the same previews, outlines, radial menu and screens as 1.21.1.
* GUI: the screens, widgets and HUD draw through `sophisticated.building.client.gui.GuiGraphics`, a shim with the
  1.20 `GuiGraphics` methods over `GuiComponent` helpers and a `PoseStack` (items in the GUI through the fixed-function
  model-view matrix). Minecraft 1.16.5 screens render only their buttons: the Create-style screens keep their own list
  of renderable widgets (buttons and the modifier list, drawn in the order they were added, like the renderables of
  1.17+), the player settings screen puts its render-only widgets into `Screen#buttons`. 1.16.5 has no narration API
  (the widgets' narration overrides are gone), container menus return the clicked stack from `clicked` and keep the
  carried stack in the player inventory, and widgets have public `x`/`y` fields.
* Tags are `Tag<Item>` objects (no `TagKey`); logging is Log4j; the rail placement helper creates missing chunk
  sections with 1.16's `LevelChunkSection(bottomY)`; block entities load with their block state.
* Block breaking: vanilla's `spawnAfterBreak` has no "drop experience" flag and always drops it (what 1.19+ is told
  to do); on Forge the experience is the break event's, popped after the drops, as on the 1.21.1 Forge build.
* Fabric: Fabric API 0.42 for 1.16.5 has the v1 command registration callback and no client world change event (world
  load/unload is detected at the start of each client tick); its mod id is `fabric`, so `fabric.mod.json` depends on
  `"fabric": ">=0.42.0"`. The menu type and menu screen registration are opened with an access widener. The JSON
  config backend reads with Gson 2.8.0.
* Forge 1.16.5 names: `WorldEvent`, `TickEvent.WorldTickEvent`, `InputEvent.KeyInputEvent`, `GuiOpenEvent`,
  `RenderWorldLastEvent`, `FMLServerStoppedEvent` (`net.minecraftforge.fml.event.server`),
  `ClientPlayerNetworkEvent.LoggedInEvent`/`LoggedOutEvent`, `event.world.BlockEvent`; networking in
  `net.minecraftforge.fml.network`. The HUD (material cost) is drawn after the crosshair in
  `RenderGameOverlayEvent.Post` (`ElementType.CROSSHAIRS`, no overlay registry), the rest of the mod's HUD once per
  frame after the whole HUD (`ElementType.ALL`); key mappings are registered with
  `net.minecraftforge.fml.client.registry.ClientRegistry` in client setup; deferred registers are created from
  `ForgeRegistries`; the power level capability is registered with `CapabilityManager` in common setup and injected with
  `@CapabilityInject` (no `RegisterCapabilitiesEvent`), and copied on clone without reviving the old player's
  capabilities (Forge 1.16.5 keeps them valid during the event); model quads are asked with `EmptyModelData`, the drawn
  layer set through `ForgeHooksClient.setRenderLayer` and put with Forge's `addVertexData`.
* Forge: the power level is a player capability (saved with the player, copied on death and on return from the End);
  the SB backpack wrapper comes from the stack's `CapabilityBackpackWrapper` capability; fake players are skipped by the
  event handlers; no `SpecialPlantable`; no generic config screen (the config files are the same); the randomizer bags
  have no item handler capability.
* Data files use the pre-1.21 folder names (`recipes/`, `tags/items/`), recipe results use `"item"`. The Forge
  `pack.mcmeta` declares pack format 6 (Minecraft 1.16.5, resource and data packs).

## Minecraft 1.16.4

The Fabric jar covers Minecraft 1.16.4 and 1.16.5 (`minecraft_version_range=[1.16.4,1.16.5]`, `fabric.mod.json`
`>=1.16.4 <=1.16.5`). Minecraft 1.16.5 is a small fix release of 1.16.4; the Fabric build compiled against
1.16.4 (Fabric API 0.42.0+1.16, which declares Minecraft `~1.16.2`) passes its 77 unit tests, the 17 server tests,
`runSmokeServer` (3 checks) and `runSmokeClient` (13 checks) on Minecraft 1.16.4; every intermediary name the 1.16.5
jar references exists in 1.16.4's intermediary mappings; and the jar starts on a real Fabric 1.16.4 server (see
TESTING.md, "Minecraft 1.16.4 check"). Fabric API 0.42.0+1.16 is listed for 1.16.5 only on Modrinth/CurseForge, but it
declares and runs on 1.16.4; players on 1.16.4 need it (or later) because of the `fabric` `>=0.42.0` floor.

The `forge/` jar is 1.16.5 only. Started on a real Forge 1.16.4 server (35.1.37) with its `mods.toml` widened for the
test, it loads and registers its upgrade containers, but it cannot use the last Sophisticated Backpacks build for 1.16.4
(1.16.4-3.0.0.289): that build's `PlayerInventoryProvider.runOnBackpacks` is static and its consumer gets no slot
identifier, and its Tool Swapper has a "swap tools" switch instead of a `ToolSwapMode`, so the backpack scan and the
Tool Swapper integration of the 1.16.5 jar are not binary compatible with it (compiling `forge/` against that build
fails at exactly these calls; not exercised at runtime). Minecraft 1.16.4 therefore gets its own Forge jar from
`forge-1.16.4/`.

### Forge 1.16.4

`forge-1.16.4/` builds `sophisticatedbuilding-forge-1.16.4-4.3.0.jar` (Minecraft `[1.16.4]`, Forge `[35.1.37,)`,
`loaderVersion` `[35,)`, optional Sophisticated Backpacks `[1.16.4-3.0.0.289,1.16.5)`) with the same toolchain as
`forge/` (Architectury Loom 1.17.493, Gradle 9.5.1, official Mojang mappings, reobfuscated to SRG names). Dev runtime:
Forge 35.1.37 (the latest Forge for 1.16.4), Sophisticated Backpacks 1.16.4-3.0.0.289 (CurseForge file 3399778, its
latest 1.16.4 release) and Curios 1.16.5-4.1.0.0 (declared for 1.16.4 and 1.16.5). It was also started on a real Forge
1.16.4 server (installer 35.1.37, only this jar plus Sophisticated Backpacks and Curios in `mods/`).

It compiles `../forge/src` (main and smoketest), `../forge/src/main/templates`, `../common` and
`../common/src/smoketestBackpacks` as they are, except the files its own `src/` has under the same path: a `Sync` task
copies the shared folders without those into `build/generated/shared`, so every shared file is compiled once and an
override replaces its original (the exclusion is decided when the copy runs, so an added override needs no
reconfiguration). Everything else of `../forge` (Forge 36 code) compiles and runs on Forge 35 unchanged. The overrides:

* `BackpackScanCompat`: calls SB 1.16.4's static `PlayerInventoryProvider.runOnBackpacks` directly and adapts its
  consumer to the one of the 1.16.5 build (the inventory handler name stands in for the slot identifier), so the callers
  (`BuildingUpgradeHelper`, `ToolSwapperIntegration`) are shared unchanged.
* `ToolSwapperIntegration`: a Tool Swapper counts when it is enabled and `shouldSwapTools()` (1.16.5: `ToolSwapMode` other
  than `NO_SWAP`); the per-tool-type filters are the same.
* `META-INF/mods.toml` template: the ranges above.
* Smoke harness: `SophisticatedBackpacksScreens` (SB 1.16.4's widget base class is `Widget`, `BackpackWidget` in the
  1.16.5 builds); `ForgeSmokeServerPlatform` uses the vanilla fake player of the Fabric builds (Forge 35's
  `FakePlayer` has no connection, `setGameMode` throws); the harness mod's `mods.toml` asks for `loaderVersion` `[35,)`.

Forge 35 ships modlauncher 8.0.9, which cannot load any class on Java 8u321 or later (`NoSuchMethodError:
sun.security.util.ManifestEntryVerifier.<init>`); the dev runs force modlauncher 8.1.3 (the version Forge 36.2 uses).
Players are not affected on the Minecraft launcher (it runs 1.16.4 on its bundled Java 8u51); a dedicated Forge 1.16.4
server needs a Java 8 older than 8u321 or modlauncher 8.1.3 in its `libraries/` (the real-server test replaced
`libraries/cpw/mods/modlauncher/8.0.9/modlauncher-8.0.9.jar` with 8.1.3). This is Forge's, not the mod's.

## Build and test

Each loader folder has its own Gradle wrapper (Gradle 9.5.1 for all three). Gradle runs on Java 21; the mod is compiled
for and run on Java 8 (toolchain, downloaded by the Foojay resolver if missing; JDK 8's javac has no `--release`, so the
builds set no release flag).

```
cd fabric       && ./gradlew build        # jar in fabric/build/libs, runs common + Fabric unit tests (77)
cd fabric       && ./gradlew runGametest  # 17 server tests of the building rules (not part of build)
cd forge        && ./gradlew build        # reobfuscated jar in forge/build/libs, runs the common unit tests (65)
cd forge-1.16.4 && ./gradlew build        # the Forge 1.16.4 jar in forge-1.16.4/build/libs, same unit tests (65)
./build-all.ps1                            # every loader folder, stops at the first failure
```

Minecraft 1.16.5 ships its game test framework stripped and neither loader has a game test API for it, so the 17
Fabric game tests of the other branches run as server tests: the same test bodies on `ServerTestHelper` (the subset of
vanilla's `GameTestHelper` they use) and `@ServerTest`, run one after the other by `ServerTestRunner`
(`common/src/smoketest/.../servertest`) on a dedicated dev server with a fresh superflat world; `runGametest` checks the
JUnit report it writes. See TESTING.md.

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless dedicated dev server on a fresh superflat world; the scenarios run through the same `ServerTestRunner`) in
every loader folder run the in-game smoke scenarios and write `<dir>/smoketest-result.json`; the game exits by itself.
Both Forge folders also run the Sophisticated Backpacks checks (`sb.*`); Fabric has no backpack integration on 1.16.x and
runs none. The harness (`common/src/smoketest`, `common/src/smoketestBackpacks` (Forge only), `<loader>/src/smoketest`,
`gradle/smoketest.gradle`) is dev-only and never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result
contract and how a port adopts it.

## Run

`runClient`, `runServer` in every loader folder (plus `runGametest` on Fabric); the Forge runs have Sophisticated Backpacks
in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` (Fabric) starts a client that loads only the jars in `fabric/run-exported/mods` (for testing
exported jars; put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge, forge-1.16.4). Neither hard-codes the loader list, so
both files are copied unchanged from `templates/branch` on `main` and stay in sync via `scripts/sync-branch-infra.ps1`
(see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Gradle JVM 21 works for every loader folder here (Loom 1.17 and Architectury Loom 1.17 need it) even though the mod itself
compiles for and runs on Java 8.

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
