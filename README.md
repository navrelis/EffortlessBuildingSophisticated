# Sophisticated Building - Minecraft 1.21.1

This branch (`mc/1.21.1`) holds Sophisticated Building for Minecraft 1.21.1 and 1.21 on Fabric, NeoForge and Forge.
It is the reference layout for the other Minecraft versions: loader-neutral code lives once in `common/`, and every
loader folder is a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

The Fabric and NeoForge jars also run on Minecraft 1.21 (declared `[1.21,1.21.1]`; Fabric needs Fabric API 0.108.0 or
newer, i.e. a `+1.21.1` Fabric API build, which runs on 1.21 too). Forge 51 (1.21) cannot load the Forge 1.21.1 jar, so
Minecraft 1.21 has its own Forge jar from `forge-1.21/`. See TESTING.md, "Minecraft 1.21 check".

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry Fabric, NeoForge and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
  src/smoketest              in-game smoke test harness (dev-only, see TESTING.md); src/smoketestBackpacks: its SB fixture
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.21.1)
forge-1.21/                Forge build for Minecraft 1.21 (Forge 51, ForgeGradle 7): ../forge/src with the few classes
                           Forge 51 cannot run replaced by its own src (see "Forge 1.21" below); no backpack integration
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

## Build and test

Each loader folder has its own Gradle wrapper (Fabric: Gradle 9.5.1, NeoForge: Gradle 9.2.1, Forge and
Forge 1.21: Gradle 9.3.1).
Java 21.

```
cd fabric   && ./gradlew build          # jar in fabric/build/libs, runs common + Fabric unit tests
cd fabric   && ./gradlew runGametest    # in-world GameTests (not part of build)
cd neoforge && ./gradlew build          # jar in neoforge/build/libs, runs the common unit tests
cd forge    && ./gradlew build          # jar in forge/build/libs, runs the common unit tests
cd forge-1.21 && ./gradlew build        # jar in forge-1.21/build/libs (Minecraft 1.21), runs the common unit tests
./build-all.ps1                         # all four, stops at the first failure
```

Forge: the first build sets up Minecraft through ForgeGradle's Mavenizer (several minutes); do not run it in parallel
with another ForgeGradle 7 build on a cold cache. The Forge dev runs (`runClient`, `runServer`, `runGameTestServer`)
have no Sophisticated Backpacks and no `runClientExported`.

### Forge 1.21

Forge 51, the only Forge for Minecraft 1.21, cannot load the Forge 1.21.1 jar, so `forge-1.21/` builds a second Forge
jar (`sophisticatedbuilding-forge-1.21-5.0.0.jar`, Minecraft `[1.21]`, Forge `[51.0.33,)`). It compiles `../forge/src`
(main and smoketest), `../forge/src/main/templates` and `../common` as they are, except the files its own `src/` has
under the same path: a `Sync` task copies `../forge/src/<set>/<kind>` without those into
`build/generated/sharedForge`, so every shared file is compiled once and an override replaces its original (the
exclusion is decided when the copy runs, so an added override needs no reconfiguration, also with the configuration
cache). The overrides, everything Forge 51 lacks:

* `SophisticatedBuildingForge`: a no-argument mod constructor with `FMLJavaModLoadingContext.get()` and
  `ModLoadingContext.get().registerConfig` (constructor injection of `FMLJavaModLoadingContext` is Forge 52+).
* `SophisticatedBuildingForgeClient` + `mixin/GuiMixin` (`sophisticatedbuilding.forge.mixins.json`, `MixinConfigs`
  manifest entry): Forge 51 has no way to add HUD layers (`AddGuiOverlayLayersEvent`/`ForgeLayeredDraw` are Forge 52+,
  its `RegisterGuiOverlaysEvent` is never posted, `RenderGuiEvent` only comes from the unused `ForgeGui`), so the
  material cost overlay and the build HUD are drawn after vanilla's whole HUD (on 1.21.1: the cost overlay above the
  crosshair, the build HUD on top). The cost overlay still hides with F1.
* Smoke harness: `ForgeSmokeTest` (no-argument constructor; runs `ServerLifecycleHooks.handleServerAboutToStart` for
  the game test server, which Forge 51 never does, so the SERVER config is loaded) and its `mods.toml`
  (`loaderVersion="[51,)"`).

Forge 51's `bootstrap-api` pulls in jopt-simple 6.0-alpha-3 (module `joptsimple`) while its modlauncher needs 5.0.4
(module `jopt.simple`); `build.gradle` forces 5.0.4, otherwise every dev run stops with "Module jopt.simple not
found". Forge runs on Mojang names since 1.20.6 (the Forge 51 universal jar references `Minecraft.options`, not
`f_91066_`), so the jar is not reobfuscated, as on 1.21.1.

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

## In-game smoke tests

`gradlew runSmokeClient -PsmoketestOut=<dir>` (real client, fresh world) and `gradlew runSmokeServer -PsmoketestOut=<dir>`
(headless game test server) in any loader folder run the in-game smoke scenarios, including the Sophisticated
Backpacks integration on Fabric and NeoForge, and write `<dir>/smoketest-result.json`; the game exits by itself. The
harness (`common/src/smoketest`, `common/src/smoketestBackpacks`, `<loader>/src/smoketest`, `gradle/smoketest.gradle`)
is dev-only and never packaged. See [TESTING.md](TESTING.md) for the scenarios, the result contract and how a port
adopts it.

## Run

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge) start with
Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.
On NeoForge the dev runtime uses the newest Sophisticated builds that still run on the pinned NeoForge version
(see `neoforge/gradle.properties`).

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).

## Build, CI and release

`build-all.ps1` and `.github/workflows/build.yml` discover loader folders the same way: any top-level folder
containing both `settings.gradle` and `gradlew` (today fabric, forge, forge-1.21, neoforge). Neither hard-codes the loader
list, so both files can be copied unchanged onto every other `mc/*` branch, including older branches that only
have `forge` + `fabric`.

Each loader's `gradle.properties` sets `ci_gradle_jdk` (21 on this branch): the JDK **CI uses to run Gradle
itself**, independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Older branches need a different value here — e.g. ForgeGradle 6 needs JDK 17, Loom 1.18 needs JDK 25 — because
that Gradle/plugin combination cannot run on JDK 21. CI reads `ci_gradle_jdk` per loader and defaults to 21 if
the key is absent.

`.github/workflows/build.yml` runs on push/PR to `mc/**` and on manual dispatch: a `discover` job builds the
loader matrix (loader name, `ci_gradle_jdk`, whether it has a `src/gametest` folder), then a `build` job builds
each loader with `gradlew build --no-daemon --stacktrace`, runs `gradlew runGametest` for loaders that have one,
and uploads the built jar (excluding `-sources`) and test reports as workflow artifacts.

`release.ps1` (PowerShell 7, run from the repo root) builds every discovered loader, checks that the version
embedded in each jar's mod metadata (`fabric.mod.json` / `neoforge.mods.toml` / `mods.toml`) matches
`mod_version` in `gradle/shared.properties`, then replaces the contents of `<loader>/release/` with the new jar
and a `SHA256SUMS.txt`:

```
pwsh ./release.ps1            # gradlew build for every loader, then publish
pwsh ./release.ps1 -NoBuild   # reuse the jars already in <loader>/build/libs
```

It prints a summary table and exits non-zero if any loader fails to build, produces no matching jar, or has a
version mismatch. `<loader>/release/*.jar` and `<loader>/release/SHA256SUMS.txt` are the only tracked files
under `release/` (see `.gitignore`).
