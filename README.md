# Sophisticated Building - Minecraft 1.16.3

This branch (`mc/1.16.3`) holds Sophisticated Building for Minecraft 1.16.3 on Fabric and Forge. It is a port of the
`mc/1.16.5` branch (itself a port of `mc/1.17.1` ... `mc/1.21.1`) with the same layout: loader-neutral code lives once
in `common/`, and every loader folder is a standalone Gradle build that compiles `common/` together with its own sources
into one mod jar. Sophisticated Backpacks exists for Forge 1.16.3 only (the unofficial Fabric port starts at 1.19.2),
so the Forge jar has the backpack integration and the Fabric jar has none. 1.16.3 stays a branch of its own (1.16.4 and
1.16.5 share `mc/1.16.5`): its Fabric API, Forge and Sophisticated Backpacks builds are all older than the 1.16.4+ ones.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (the Building Upgrade recipes carry Fabric and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness and server test runner (dev-only, see TESTING.md);
                             src/smoketestBackpacks: its SB fixture (Forge only)
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend, access widener,
                           mixins for the events Fabric API 0.25 lacks, server tests (src/gametest)
forge/                     Forge build (Architectury Loom): entry points, platform services, ForgeConfigSpec configs,
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
| Minecraft | 1.16.3 (`fabric.mod.json`: exactly 1.16.3) | 1.16.3 (`mods.toml`: `[1.16.3]`) |
| Java | 8 | 8 |
| Loader | Fabric Loader 0.19.5 (minimum 0.19.5), Fabric API 0.25.0+build.415-1.16 (the last one for 1.16.3; minimum 0.25.0, mod id `fabric`) | Forge 34.1.42 (the last one for 1.16.3; minimum 34.1.42, `loaderVersion` `[34,)`) |
| Toolchain | Loom 1.17.21, Gradle 9.5.1, official Mojang mappings | Architectury Loom 1.17.493, Gradle 9.5.1, official Mojang mappings, reobfuscated to SRG names (`remapJar`) |
| Sophisticated Backpacks | none (no Fabric port for 1.16.3) | official build `1.16.4-1.0.0.94` (file 3142665 `sophisticatedbackpacks-1.16.4-1.0.0.94.jar`, CurseMaven; tagged for Minecraft 1.16.3 on CurseForge and its latest 1.16.3 release; mods.toml range `[1.16.4-1.0.0.94,)`; no Sophisticated Core) |
| Curios / Trinkets | - | Curios `1.16.4-4.0.3.0` (file 3122651, tagged 1.16.3 + 1.16.4; there is no 1.16.3 build on Modrinth), compile only (and smoke runtime) |

The Forge jar was also started on a real Forge 1.16.3 server (installer 34.1.42, only this jar plus Sophisticated
Backpacks and Curios in `mods/`, Java 8u202): "Registered Sophisticated Backpacks upgrade containers", `Done`; again
with only this jar and Sophisticated Backpacks, and with this jar alone (no SB: `Done`, no upgrade containers). The
Fabric jar was started on a real Fabric 1.16.3 server (Fabric Loader 0.19.5, the Fabric API 0.25.0 modules, Java 8):
`Done`, no errors from the mod.

**Java for Forge 1.16.3:** Forge 34 ships modlauncher 8.0.6, which cannot load a single class on Java 8u321 or newer
(`NoSuchMethodError: sun.security.util.ManifestEntryVerifier.<init>(Ljava/util/jar/Manifest;)V`, with or without this
mod). The Minecraft launcher runs 1.16.3 on its bundled Java 8u51, so clients are not affected; a dedicated Forge 1.16.3
server needs a Java 8 older than 8u321. The dev runs of this branch force modlauncher 8.1.3 (the fix Forge 36.2.26 took
for 1.16.5, API compatible) in `forge/build.gradle`, so they work on the current Java 8 that Gradle provisions.

## Differences from the 1.21.1 branch

Minecraft 1.16.3 has no data components, no `StreamCodec`, no vanilla `CustomPacketPayload`, no `GuiGraphics`, no
`TagKey`, no core shaders, no SLF4J, a static `Registry`, Java 8 and older loader APIs; the port keeps the behaviour
wherever the game allows. What differs from 1.16.5/1.17.1 because of 1.16.3 is marked **(1.16.3)**.

* No Sophisticated Backpacks on Fabric: the Fabric jar ships no backpack integration and the Building Upgrade items
  are placeholders without function (as on the loaders of other branches without Sophisticated Backpacks). Fabric API
  0.25 has no resource conditions, so their recipes are left out of the Fabric jar; on Forge they are only loaded when
  Sophisticated Backpacks is installed (`forge:mod_loaded`).
* **(1.16.3) Sophisticated Backpacks 1.0.0.94** is one of SB's first releases:
  * No Tool Swapper upgrade: the mass break never takes tools from backpacks (the Forge backpack tool list stays empty,
    the smoke check `sb.tool_swapper_tools` is skipped).
  * No enable/disable switch for upgrades: the Building Upgrade keeps its own `enabled` tag in the upgrade stack (the
    tag later SB builds use), and its settings tab toggles it as on 1.21.1; disabled upgrades are ignored.
  * No upgrade slot checks (`canAddUpgradeTo` and upgrade count limits come later): a backpack cannot refuse a second
    Building Upgrade. Each backpack is used once, with the highest enabled tier installed in it.
  * Upgrade wrappers are created from the upgrade stack alone; the wrapper is linked to the backpack it was found in.
    The backpack wrapper comes from the stack's `BackpackWrapper.BACKPACK_WRAPPER_CAPABILITY`, the backpack scan is the
    static `PlayerInventoryProvider.runOnBackpacks` (inventory, offhand, chest slot and SB's own Curios handler), and
    worn Curios backpacks found by the fallback scan are de-duplicated by stack identity (there is no contents UUID).
  * The settings tab is registered with `UpgradeSettingsTabManager`, its toggle states are built from `GuiHelper`
    with translation keys.
  * SB's upgrade framework lives in `net.p3pp3rf1y.sophisticatedbackpacks` (`util`, `upgrades`, `common.gui`,
    `client.gui`); its upgrade items are created in SB's creative tab (no-argument `UpgradeItemBase`); the Building
    Upgrades override `allowdedIn` / `getCreativeTabs` so they are listed in this mod's tab as on 1.21.1.
* **(1.16.3) Fabric API 0.25.0** (the last one for 1.16.3) has only the networking v0 API and none of the connection,
  player, world change or world render events: payloads travel through `ServerSidePacketRegistry` /
  `ClientSidePacketRegistry` (one channel per payload id, as on the other branches); mixins
  (`fabric/src/main/java/sophisticated/building/fabric/mixin`, `sophisticatedbuilding.fabric.mixins.json`) call the
  join/leave (`PlayerList.placeNewPlayer` / `remove`), respawn (`PlayerList.respawn`) and dimension change
  (`ServerPlayer.changeDimension` / cross-dimension `teleportTo`) handlers, and draw the previews, mirror/array lines,
  ghost blocks and outlines right after the level is rendered (`GameRenderer.renderLevel`, where Forge 1.16 posts
  `RenderWorldLastEvent`). Client log in/out is detected at the start of each client tick. Its mod id is `fabric`, so
  `fabric.mod.json` depends on `"fabric": ">=0.25.0"`. It has no GameTest API: the 17 Fabric server tests run on the
  harness' `ServerTestRunner` (`gradlew runGametest`).
* **(1.16.3) Vanilla:** `RenderSystem` has no scissor methods (the GUI shim calls OpenGL directly) and selection lists
  cannot switch off their dirt background (the modifier list draws its entries, scroll bar and decorations itself over
  its dark background, as the 1.21 builds look).
* **(1.16.3) Forge 34:** fake players have no connection; packets to such players are dropped.
* Rendering: Minecraft 1.16 has no core shaders; the preview and outline render types use the fixed-function states
  of the vanilla entity / line render types and OpenGL primitive modes.
* Item data is NBT. Storage blocks placed with build modes get the stack's `BlockStateTag` and `BlockEntityTag`
  (and the custom name through the block's `setPlacedBy`), in vanilla `BlockItem.place` order. The randomizer bags keep
  their inventory in the stack's `Items` tag, the Omega bag its weights in `SlotWeights`.
* Payloads implement the mod's own `ModPayload` (`write(FriendlyByteBuf)`, a reading constructor and a
  `ResourceLocation` id; same ids and fields as on 1.21.1). Forge identifies the messages of a `SimpleChannel` by their
  class, so all payloads travel on one channel, `sophisticatedbuilding:main`, as one message that carries the payload
  id before the body (`ForgeNetworking`); both sides need the mod.
* Text components are `TextComponent` / `TranslatableComponent` / `KeybindComponent`; the screens, widgets and HUD
  draw through `sophisticated.building.client.gui.GuiGraphics`, a shim with the 1.20 `GuiGraphics` methods over
  `GuiComponent` helpers and a `PoseStack`. The player inventory is the `Player.inventory` field.
* Forge 34: the power level is a player capability registered with `CapabilityManager` in common setup (saved with the
  player, copied on death and on return from the End); the material cost overlay is drawn after the crosshair
  (`RenderGameOverlayEvent.Post`, no overlay registry); fake players are skipped by the event handlers; no generic
  config screen (the config files are the same).
* Recipes: Minecraft 1.16 has no deepslate and no amethyst: Compressed Cobbled Deepslate is made from (and
  decompressed back into) blackstone, the Diamond and Omega Randomizer Bags use prismarine crystals instead of amethyst
  shards. Data files use the pre-1.21 folder names; the Forge `pack.mcmeta` declares pack format 6.

## Build and test

Each loader folder has its own Gradle wrapper (Gradle 9.5.1). Gradle runs on Java 21; the mod is compiled for and run
on Java 8 (toolchain, downloaded by the Foojay resolver if missing).

```
cd fabric && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests (77)
cd fabric && ./gradlew runGametest    # 17 server tests on a dedicated dev server (not part of build)
cd forge  && ./gradlew build          # reobfuscated jar in forge/build/libs, runs the common unit tests (65)
./build-all.ps1                       # both, stops at the first failure
```

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless: a dedicated dev server on a fresh superflat world; Minecraft 1.16.3 has no game test framework, the harness'
`ServerTestRunner` runs the scenarios) in either loader folder run the in-game smoke scenarios and write
`<dir>/smoketest-result.json`; the game exits by itself. Forge also runs the Sophisticated Backpacks checks (`sb.*`,
`sb.tool_swapper_tools` skipped: no Tool Swapper in SB 1.0.0.94); Fabric has no backpack integration on 1.16.3 and runs
none. The harness is dev-only and never packaged. See [TESTING.md](TESTING.md).

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric); the Forge runs have Sophisticated Backpacks
in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.

`runClientExported` (Fabric) starts a client that loads only the jars in `fabric/run-exported/mods` (put Fabric API
there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge). Neither hard-codes the loader list, so
both files are copied unchanged from `templates/branch` on `main` and stay in sync via `scripts/sync-branch-infra.ps1`
(see `docs/RELEASING.md`).

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Loom 1.17 and Architectury Loom 1.17 need Gradle JVM 21 even though the mod compiles for and runs on Java 8.

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
