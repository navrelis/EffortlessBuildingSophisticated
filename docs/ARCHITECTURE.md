# Architecture

This describes the internal layout of **one version branch** (`mc/<version>`) — `common/` plus one
standalone Gradle build per loader, tied together by platform services. `main` (the hub) has no
game code of its own; this is what every version branch (`mc/1.21.1`, `mc/1.20.4`, ...) looks like
inside. Concrete package names below (`sophisticated.building.*`) are the reference layout used by
`mc/1.21.1`; a ported branch keeps the same shape with its own package if it uses one.

## Layout inside a version branch

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version;
                           read by every loader's settings.gradle so these are defined exactly once
common/                    loader-neutral code and assets, no build of its own
  src/main/java              mod logic; loader/optional-mod APIs only through platform services
  src/main/resources         assets, data (recipes carry per-loader load conditions), mixin config
  src/test/java               unit tests, compiled and run by every loader build
fabric/                    Fabric build (Loom): entry points, service implementations, JSON config
                           backend, Sophisticated Backpacks integration, GameTests (src/gametest)
neoforge/                  NeoForge build (ModDevGradle): entry points, service implementations,
                           ModConfigSpec configs, Sophisticated Backpacks integration with Curios
                           fallback where applicable
forge/                     Forge build (ForgeGradle), where the branch targets Forge
<loader>-<mc>/             a second build of one loader for another Minecraft version of the branch (e.g.
                           forge-1.21/ on mc/1.21.1), when that loader needs its own jar there
changelog/                 per-branch patch notes
build-all.ps1              builds every loader folder in turn, stops at the first failure
<loader>/release/          built jars for that branch (see docs/RELEASING.md)
```

Each loader folder is a **complete, standalone Gradle project** with its own wrapper: it compiles
`common/`'s sources directly into its own source set (`java.srcDir "${commonDir}/src/main/java"`,
`resources.srcDir "${commonDir}/src/main/resources"`) alongside its own loader-specific sources,
and produces one mod jar. There is no separate `common` Gradle module or published artifact —
`common/` is purely a shared source tree, included by path by every loader build.

A `<loader>-<mc>/` folder exists where one jar per loader cannot cover every Minecraft version of a branch
(`mc/1.21.1`: Forge 51 for 1.21 cannot load the Forge 52 jar for 1.21.1, while the Fabric and NeoForge jars cover
both). It is a standalone build like the others, but compiles `../<loader>/src` as well: a `Sync` task copies those
sources without the files its own `src/` has under the same path, so only the classes the older loader cannot run
are duplicated. Its own `gradle.properties` sets the Minecraft version it targets (`forge_minecraft_version`,
`forge_minecraft_version_range`), and its jar is `<mod_id>-<loader>-<mc>-<mod_version>.jar` (see
docs/RELEASING.md).

## Platform services

`common/` must never import a loader API or an optional integration's API directly — it can only
be Minecraft/vanilla plus its own code. Everything that differs between loaders goes through a
small set of service interfaces in `<package>.platform.services`, accessed via two loader-neutral
entry points:

- **`sophisticated.building.platform.Services`** — the always-available services: `IPlatformHelper`,
  `IBlockEventHelper`, `INetworkHelper`, `IConfigHelper`. Each is loaded once via
  `ServiceLoader.load(...).findFirst()` and cached in a `static final` field, so a missing
  implementation fails fast at class-init time with a clear `NullPointerException` rather than a
  confusing `NoSuchMethodError` deep in game logic.
- **`sophisticated.building.platform.ClientServices`** — the client-only service (`IClientHelper`),
  kept in its own class specifically so a dedicated server JVM never touches it (referencing the
  class at all would trigger its static initialiser and load client-only implementations).

Each loader project registers its implementation of every interface it supports as a
`META-INF/services/<fully.qualified.InterfaceName>` file under `<loader>/src/main/resources/`,
naming the implementation class — the standard Java `ServiceLoader` mechanism. `fabric/` and
`neoforge/` each carry the full set of six service files (five plus the client one); `forge/`
mirrors the same set for its own implementations.

### Optional integration: Sophisticated Backpacks

The Sophisticated Backpacks integration (Building Upgrade items, backpack block supply, Tool
Swapper tools) is the one service that's allowed to be **absent**: `IBackpackIntegration` is not
loaded eagerly through `Services.load`. Instead, `Services.backpacks()` first checks
`CompatHelper.isSophisticatedBackpacksLoaded()`; if Sophisticated Backpacks isn't present, or the
loader project ships no integration at all (no `META-INF/services` entry, e.g. a Minecraft version
with no Sophisticated Backpacks build for that loader), it returns the no-op singleton
**`IBackpackIntegration.NONE`** — every method on the interface has a default implementation that
does nothing / returns an empty result, so calling code never needs a null check. The lookup result
is cached after the first successful or failed attempt. Every call site that reaches into
Sophisticated Backpacks/Core catches both `Exception` and `LinkageError` (a binary-incompatible
upstream change surfaces as a `LinkageError`, e.g. `NoSuchMethodError`, not a plain exception — see
`docs/history/PATCH_NOTES_4.1.1.md` for the incident that established this pattern), so a broken or
missing integration degrades to "no Building Upgrade found" instead of crashing.

### Build-time enforcement

Every loader build registers a `checkCommonIsLoaderNeutral` task (part of `check`) that scans every
`.java` file under `common/src/main/java` for a forbidden import prefix (`net.fabricmc`,
`net.neoforged`, `net.minecraftforge`, `net.p3pp3rf1y` (Sophisticated), `net.createmod`,
`dev.engine_room` (Catnip/Flywheel-family)) and fails the build if it finds one. This is what
actually guarantees the loader-neutral boundary — the platform-services pattern only works if
`common/` can't reach around it.

## Ghost previews / GUI widgets

The block placement ghost previews and outline rendering use the **Catnip** outliner and GUI
widgets, vendored directly under `sophisticated.building.create.catnip` (MIT-licensed, see
`LICENSE_Ponder.txt` in the branch). The mod has no runtime dependency on Flywheel, Ponder, or
Catnip themselves — only this vendored subset of Catnip's rendering code.

## Where this fits in the multi-version project

`gradle/shared.properties` is what makes a version branch's Minecraft version and mod version a
single source of truth read by every loader's `settings.gradle`; bumping `mod_version` there is the
first step of a release (`docs/RELEASING.md`). The toolchain each loader build uses (Gradle
version, mapping provider, Sophisticated Backpacks/Core dependency) differs per Minecraft version —
see `docs/PORTING.md` for the full matrix and the API breaks that show up when porting `common/` or
a loader's service implementations to a new Minecraft version.
