# Sophisticated Building - Minecraft 1.21.1

This branch (`mc/1.21.1`) holds Sophisticated Building for Minecraft 1.21.1 on Fabric, NeoForge and Forge. It is the
reference layout for the other Minecraft versions: loader-neutral code lives once in `common/`, and every loader
folder is a standalone Gradle build that compiles `common/` together with its own sources into one mod jar.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version (read by every loader build)
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader APIs only through sophisticated.building.platform.Services
  src/main/resources         assets, data (recipes carry Fabric, NeoForge and Forge load conditions), mixin config
  src/test/java              unit tests, run by every loader build
fabric/                    Fabric build (Loom): entry points, platform services, JSON config backend,
                           Sophisticated Backpacks integration (unofficial Fabric port), GameTests (src/gametest)
neoforge/                  NeoForge build (ModDevGradle): entry points, platform services, ModConfigSpec configs,
                           power level attachment, Sophisticated Backpacks integration (official build) with Curios fallback
forge/                     Forge build (ForgeGradle 7): entry points, platform services, ForgeConfigSpec configs,
                           power level capability; no backpack integration (no Sophisticated Backpacks for Forge 1.21.1)
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

`runClient`, `runServer` in either folder (plus `runGametest` on Fabric, `runGameTestServer` on NeoForge) start with
Sophisticated Backpacks/Core in the dev runtime. Accept the EULA in `<loader>/run/eula.txt` for `runServer`.
On NeoForge the dev runtime uses the newest Sophisticated builds that still run on the pinned NeoForge version
(see `neoforge/gradle.properties`).

`runClientExported` starts a client that loads only the jars in `<loader>/run-exported/mods` (for testing
exported jars; on Fabric put Fabric API there too).
