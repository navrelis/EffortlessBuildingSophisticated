# Porting to a new Minecraft version

How to create (or port an existing build of) a `mc/<version>` branch, the toolchain each Minecraft
version needs, the API breaks to expect, and what "done" means for a port. Read
`docs/ARCHITECTURE.md` first for the common/platform-services shape every branch follows.

## Step by step

1. **Pick the branch name and scope.** One branch per distinct toolchain/API surface — see
   "Merge verdicts" below for which adjacent Minecraft versions can share one branch (e.g.
   `mc/1.16.5` covers both 1.16.4 and 1.16.5) and which need their own branch (e.g. `1.18.2` always
   gets its own branch, even though 1.18/1.18.1 can merge with each other). Check `README.md`'s
   support matrix for the branch's target status and which loaders it targets.
2. **Branch off the nearest already-ported branch**, not off `main` — `main` has no game code.
   Picking the branch whose Minecraft version and loader set is closest minimizes the porting diff
   (e.g. `mc/1.21.4` is a much smaller diff from `mc/1.21.1` than from `mc/1.18.2`).
3. **Update `gradle/shared.properties`**: `minecraft_version`, `minecraft_version_range`,
   `java_version` (see the Java column in the toolchain matrix below), and `mod_version` if this
   port also bumps the mod version.
4. **Update or add each loader's Gradle build** to the toolchain versions in the matrix below:
   Gradle wrapper version, loader plugin + version, mapping provider, and the
   Sophisticated Backpacks/Core dependency coordinates for this Minecraft version (see
   `upstream/README.md`'s file matrix and `upstream/manifest.json` for exact file names/CurseMaven
   coordinates). Add a `forge/` folder alongside `fabric/`/`neoforge/` if the branch targets Forge.
   If one jar of a loader cannot cover every Minecraft version the branch declares, add a `<loader>-<mc>/` folder
   for the older one (e.g. `forge-1.21/` on `mc/1.21.1`): a standalone build that compiles `../<loader>/src` except
   the files its own `src/` overrides, jar `<mod_id>-<loader>-<mc>-<mod_version>.jar` (see `docs/ARCHITECTURE.md`).
5. **Run `scripts/sync-branch-infra.ps1` for the new branch** (from `main`, e.g.
   `pwsh scripts/sync-branch-infra.ps1 -Mc <version>` once the worktree exists — see the last
   paragraph of this step-by-step list for registering it): copies the canonical
   `.github/workflows/build.yml`, `release.ps1` and `build-all.ps1` from `templates/branch/` onto
   the new branch's worktree, and appends `ci_gradle_jdk=<value>` to each loader's
   `gradle.properties` if it's missing (`-GradleJdk` to override the default of 21 when this
   branch's toolchain needs a different Gradle JVM — see the toolchain matrix's Gradle JVM
   constraints, e.g. ForgeGradle 6 needs JDK 17). Commit the result in the branch's own worktree;
   the script itself never commits. See `docs/RELEASING.md`'s CI section for the full
   templates + sync workflow.
6. **Port `common/`** against the new Minecraft version, fixing the API breaks listed below for
   every version between the source branch and the target. `checkCommonIsLoaderNeutral` (part of
   `check`) will fail the build if a fix accidentally reaches for a loader or optional-mod API
   directly instead of going through `platform.services`.
7. **Port each loader's service implementations** (`platform/services/*` implementations,
   registered in `META-INF/services`) and entry points. Wire up `IBackpackIntegration` against the
   Sophisticated Backpacks/Core version for this (loader, Minecraft version) pair if one exists
   (see `upstream/README.md`); otherwise ship no `IBackpackIntegration` registration at all, so
   `Services.backpacks()` falls back to `IBackpackIntegration.NONE` (see `docs/ARCHITECTURE.md`).
8. **Update the branch's own `README.md`** (mirror `mc/1.21.1`'s) and `changelog/` with anything
   version-specific.
9. **Work through the definition of done** below before calling the port finished, and update
   `README.md`'s support matrix status on `main` once it's released.

Locally, register the new branch as a worktree with `scripts/setup-worktrees.ps1` (run from `main`)
once it's pushed to `origin`, so it shows up at `versions/<mc>/` alongside the others.

The working hello-world template builds referenced while researching each toolchain below (one per
Forge/NeoForge/Fabric version, build-verified, plus CurseMaven dependency examples) exist only
locally on the machine that did the research — they are not part of this repository. Recreate a
toolchain from the tables below and the official MDK/Loom/ModDevGradle documentation for the
Minecraft version in question if you need a fresh reference build.

## Toolchain matrix

Java per Minecraft version: 8 (1.16.x), 16 (1.17.1), 17 (1.18–1.20.4), 21 (1.21.x), 25 (26.x).
Mojang mappings cover 1.16.3–1.21.11; 26.x ships unobfuscated (no mappings/intermediary/Parchment
layer at all).

### Forge

| MC | Forge (latest / recommended) | Plugin + Gradle | Parchment |
|---|---|---|---|
| 1.16.3 | 34.1.42 / 34.1.0 | ForgeGradle 6.0.54 + Gradle 8.4 (copy the 1.16.5 template; the official MDK is stale for this version) | - |
| 1.16.4 | 35.1.37 / 35.1.4 | same | - |
| 1.16.5 | 36.2.42 / 36.2.34 | FG [6.0,6.2) + Gradle 8.4 (Gradle JVM JDK 17, target Java 8) | 2022.03.06 |
| 1.17.1 | 37.1.1 | ModDevGradle Legacy 2.0.147 + Gradle 8.14.5, or FG6 + Gradle 8.8 | 2021.12.12 |
| 1.18 / 1.18.1 | 38.0.17 / 39.1.2 | MDG Legacy or FG6 | - / 2022.03.06 |
| 1.18.2 | 40.3.12 / 40.3.0 | FG6 + Gradle 8.8 | 2022.11.06 |
| 1.19 / 1.19.1 / 1.19.2 | 41.1.0 / 42.0.9 / 43.5.2 (rec. 43.5.0) | MDG Legacy or FG6 + Gradle 8.8 | - / - / 2022.11.27 |
| 1.20.1 | 47.4.23 / 47.4.10 (compile against 47.1.3 for NeoForge 47.1 cross-compat, see below) | MDG Legacy 2.0.147 + Gradle 8.14.5 (recommended) | 2023.09.03 |
| 1.20.4 | 49.2.9 | FG >=6.0.16 + Gradle 8.12.1 (reobf) | 2024.04.14 |
| 1.21 | 51.0.33 (the only one) | FG7 7.0.40 + Gradle 9.3.1 with `jopt-simple` forced to 5.0.4 (see Gotchas; no reobf since 1.20.6) | 2024.11.10 |
| 1.21.1 | 52.1.16 / 52.1.0 | FG [7.0.3,8) -> 7.0.40 + Gradle 9.3.1 | 2024.11.17 |
| 1.21.4 / 1.21.5 / 1.21.8 / 1.21.10 | 54.1.18 / 55.1.14 / 58.1.22 / 60.1.15 | FG7 + Gradle 9.3.1 | 2025.03.23 / 2025.06.15 / 2025.09.14 / 2025.10.12 |
| 1.21.11 | 61.2.1 / 61.2.0 | FG7 + Gradle 9.5.0 | 2025.12.20 |
| 26.1 / 26.1.1 / 26.1.2 | 62.0.9 / 63.0.2 / 64.1.3 | FG7 (>=7.0.17) + Gradle 9.3.1–9.5.0, no mappings line | n/a |
| 26.2 | 65.1.3 / 65.1.0 | FG 7.0.40 + Gradle 9.5.0 | n/a |

### NeoForge

ModDevGradle 2.0.147, Gradle 9.2.1 (26.x needs Gradle >=9.1, Java 25; NeoForge never reobfuscates).

- 1.20.1: `net.neoforged:forge:1.20.1-47.1.106` (NeoForge's 1.20.1 build is a fork of the Forge jar
  itself — ship the Forge jar for this version, see the merge note below).
- 1.20.4: 20.4.251 (Java 17, metadata in `META-INF/mods.toml`).
- 1.21: 21.0.167. 1.21.1: 21.1.251. 1.21.4: 21.4.157. 1.21.5: 21.5.98. 1.21.8: 21.8.54.
  1.21.10: 21.10.64. 1.21.11: 21.11.45.
- 26.1 / 26.1.1: beta-only releases (26.1.0.19-beta / 26.1.1.15-beta). 26.1.2: 26.1.2.109.
  26.2: 26.2.0.88.

### Fabric

Fabric Loader 0.19.5 across every version. Loom 1.18.2 needs Gradle >=9.7 on JDK 25 (use Gradle
9.8.0); fall back to Loom 1.17.21 on JDK 21 with Gradle >=9.5 for older Minecraft versions.

- Up to 1.21.11: plugin `net.fabricmc.fabric-loom-remap`, `mappings loom.officialMojangMappings()`.
- 26.x: plugin `net.fabricmc.fabric-loom` (no `-remap` suffix), no mappings block at all — plain
  `implementation`/`compileOnly` dependencies, and the build's own output task is `jar`, not
  `remapJar`.
- Fabric API per Minecraft version: 1.16.3 `0.25.0+build.415-1.16`; 1.16.4 `0.29.3+1.16`;
  1.16.5 `0.42.0+1.16`; 1.17.1 `0.46.1+1.17`; 1.18 `0.44.0+1.18`; 1.18.1 `0.46.6+1.18`;
  1.18.2 `0.77.0+1.18.2`; 1.19 `0.58.0+1.19`; 1.19.1 `0.58.5+1.19.1`; 1.19.2 `0.77.0+1.19.2`;
  1.20.1 `0.92.12+1.20.1`; 1.20.4 `0.97.3+1.20.4`; 1.21 `0.102.0+1.21`; 1.21.1 `0.116.17+1.21.1`;
  1.21.4 `0.119.4`; 1.21.5 `0.128.2`; 1.21.8 `0.136.1`; 1.21.10 `0.138.4`; 1.21.11 `0.141.6`;
  26.1.x `0.155.3+26.1.2` (tagged for 26.1, 26.1.1, and 26.1.2 alike); 26.2 `0.161.0+26.2`.
- Fabric API `ClientWorldEvents` (used by the Fabric build since 1.21.1) exists only from Fabric API 0.108.0+1.21.1
  (`fabric-lifecycle-events-v1` 2.5.0); every `+1.21.1` build declares Minecraft `>=1.21 <1.21.2`, so it also runs on
  1.21, while the newest `+1.21` build (0.102.0) lacks it and the client crashed with `NoClassDefFoundError`. Declare
  the Fabric API floor the code needs (`"fabric-api": ">=0.108.0"` on `mc/1.21.1`) instead of `"*"`.

### Optional dependency (Curios / Trinkets / Accessories) availability

Curios: Forge 1.16.3–1.20.4; NeoForge 1.20.1, 1.20.4, 1.21.1, 1.21.4–1.21.11, 26.1.x
(15.0.0+26.1.2), 26.2 (16.0.0). Trinkets (Fabric): 1.16.3–1.21.1. Accessories: 1.20.1–1.21.10.
Flywheel/Ponder are not a dependency of this mod any more (dropped when the vendored-Catnip preview
rendering replaced them — see `docs/ARCHITECTURE.md`).

## Merge verdicts — which Minecraft versions can share one branch

Based on an API diff between adjacent versions; always confirm with a runtime test (`runClient`)
before relying on a merge, not just a successful compile.

- **1.16.4 + 1.16.5**: merge (compile against Forge 35.1.37, range `[1.16.4,1.16.5]`; the Fabric
  API diff between them is trivial). 1.16.3 stays a separate branch.
- **1.18 + 1.18.1**: merge on Fabric; on Forge only if the branch doesn't use the permission API
  that changed between them. 1.18.2 is always separate (introduces `TagKey`/`Holder`).
- **1.19.1 + 1.19.2**: merge. Adding plain 1.19 to that merge is possible (the only difference is
  chat signing) but needs a runtime test to confirm before doing it.
- **1.21 + 1.21.1**: merged on `mc/1.21.1` for Fabric and NeoForge, verified by running the 1.21.1 release jars
  with the smoke harness in a 1.21 runtime (Fabric API 0.108.0+1.21.1; NeoForge 21.0.167 with Sophisticated Backpacks
  1.21-3.20.26 / Core 1.21-0.7.13, all `sb.*` checks passing except the Curios one, as no Curios build exists for
  NeoForge 21.0). Range `[1.21,1.21.1]` (Fabric `>=1.21 <=1.21.1`), NeoForge floor 21.0.167, optional
  Backpacks `[3.20.26,)` / Core `[0.7.13,)`. Forge is **not** merged: Forge 51 (1.21) cannot load the Forge 52 jar
  (no `FMLJavaModLoadingContext` constructor injection, no `AddGuiOverlayLayersEvent`/`ForgeLayeredDraw`, and its
  `RegisterGuiOverlaysEvent` is never posted), so 1.21 gets its own `forge-1.21/` folder (no-argument mod
  constructor, HUD drawn by a `Gui.render` mixin). Details: `TESTING.md` "Minecraft 1.21 check" on `mc/1.21.1`.
- **26.1 + 26.1.1 + 26.1.2**: vanilla-identical, so they merge on Fabric and Forge. NeoForge is the
  exception: 26.1 and 26.1.1 only ever got beta NeoForge releases, so NeoForge support starts at
  26.1.2 only (which also renamed `BlockEvent.BreakEvent` to `BreakBlockEvent`, see the API-break
  list below).
- **1.20.3 vs 1.20.4**: vanilla-identical; target NeoForge 20.4 only (1.20.3's NeoForge used the
  now-superseded `IPayloadRegistrar` API that 20.4 also still has, so there's no reason to support
  1.20.3 separately).
- **NeoForge 1.20.1 runs Forge 1.20.1 jars** — ship the Forge build for `mc/1.20.1`'s NeoForge
  column rather than maintaining a separate NeoForge target (compile against Forge 47.1.3 for
  cross-compat), which is why the support matrix in `README.md` lists NeoForge as unsupported and
  Forge as "the jar also runs on NeoForge 1.20.1" for that branch.

## API breaks relevant to a building mod

Ordered by Minecraft version; each entry is the break(s) that actually affect this mod's code
(placement, block state/entity access, networking, rendering, capabilities/attachments) — not a
full changelog of every vanilla or loader change in that version.

- **1.17**: Java 16 required. `BlockEntity` constructor gains `(pos, state)`. Core shaders replace
  the old shader system.
- **1.18**: SLF4J logging. **1.18.2**: LogUtils; `TagKey`/`Holder` replace raw `Tag`/direct registry
  object references.
- **1.19**: `Component.literal`/`translatable` replace the old text component factories.
  **1.19.1**: chat signing. **1.19.3**: `BuiltInRegistries`/`Registries` replace the old static
  registry fields; `CreativeModeTabs`; JOML replaces the old math library.
- **1.20**: `GuiGraphics` replaces `PoseStack`+`MultiBufferSource` rendering args.
  **1.20.2**: vanilla gets its own `CustomPacketPayload` networking; NeoForge moves to the
  `net.neoforged` package with a new `IEventBus` constructor; Forge does a full networking rewrite
  around `ChannelBuilder`. NeoForge 20.3 introduces `BlockCapability` + `AttachmentType`; 20.4
  introduces `IPayloadRegistrar`; 20.5/20.6 replace it with `PayloadRegistrar`. Forge keeps
  `LazyOptional` capabilities all the way through 26.x (no equivalent churn there).
- **1.20.5**: data components / `CustomData` replace NBT-based item data in many places;
  `StreamCodec` networking codecs; Java 21 required. **1.20.6**: Forge switches to official
  (Mojang) names at runtime.
- **1.21**: `ResourceLocation.fromNamespaceAndPath`/`.parse` replace the old public constructor.
  **1.21.2**: `EntityRenderState`; shader system changes; `ItemInteractionResult` removed;
  `Item`/`Block` `Properties` need an explicit `.setId(...)`.
- **1.21.4**: item model definitions move to data-driven JSON; `ItemStackRenderState`.
- **1.21.5**: `RenderPipeline` replaces the old render-state/shader instance API; `VertexBuffer`
  removed; `CompoundTag` getters return `Optional` (use `getIntOr` etc. instead of the old
  `getInt`); GameTest framework rework (`GameTestInstance` replaces the old class-based tests).
- **1.21.6**: `GuiRenderState`; `ValueInput`/`ValueOutput` replace direct `CompoundTag` read/write
  in many vanilla APIs.
- **1.21.9**: `KeyMapping.Category`; `SubmitNodeCollector`; `BlockEntityRenderState`;
  `MouseButtonEvent`.
- **1.21.10**: hotfix release, no API break of note for this mod.
- **1.21.11**: `ResourceLocation` renamed to `Identifier`.
- **26.1**: unobfuscated Java 25 runtime; `GuiGraphics` replaced by `GuiGraphicsExtractor` +
  `Screen#extractRenderState`; `ItemStackTemplate`. Forge: `RenderLevelStageEvent` replaced by
  `AddFramePassEvent`.
- **26.1.2**: NeoForge renames `BlockEvent.BreakEvent` to `BreakBlockEvent` (see the merge-verdict
  note above — this is why NeoForge support for the 26.1.x line starts at 26.1.2).
- **26.2**: `MultiBufferSource` and `Tesselator` removed outright (Vulkan rendering backend).

## Gotchas

- Gradle 8.4 (ForgeGradle 6) cannot run on JDK 21 — set the Gradle JVM to JDK 17 explicitly even if
  the project itself targets Java 8. FG6 does not work at all with Gradle 9.
- Forge 51 (1.21) dev runs stop with "Module jopt.simple not found, required by cpw.mods.modlauncher": its
  `bootstrap-api` 2.1.3 pulls in jopt-simple 6.0-alpha-3 (module `joptsimple`). Force 5.0.4 in `build.gradle`
  (`configurations.configureEach { resolutionStrategy.force 'net.sf.jopt-simple:jopt-simple:5.0.4' }`); the same
  happens with FG6. Its game test server also never runs `ServerLifecycleHooks.handleServerAboutToStart`, so SERVER
  configs stay unloaded in game tests unless the test mod calls it (see `forge-1.21/src/smoketest` on `mc/1.21.1`).
- Don't run two ForgeGradle 7 builds in parallel against a cold Gradle cache — it can truncate the
  shared fatjar mid-write. The first FG7 configuration on a clean machine takes 6–8 minutes; that's
  expected, not a hang.
- The official Forge MDKs are stale (don't reflect the recommended toolchain) for 1.16.3, 1.16.4,
  and 1.17.1–1.19.1 — copy the 1.16.5 or 1.18.2 template's build.gradle instead of trusting the
  MDK's own.
- FG7 build.gradle syntax differs from FG6:
  `implementation minecraft.dependency('net.minecraftforge:forge:...')`, repositories via
  `minecraft.mavenizer(it)`, `fg.forgeMaven`, `fg.minecraftLibsMaven`; the 26.x MDK additionally
  needs the `eventbus-validator` annotation processor. The CurseMaven repository must be declared
  in the main `repositories {}` block, before any dependency that resolves from it.
- FG6 + CurseMaven: do **not** use `exclusiveContent` for the CurseMaven repo — use
  `maven { url 'https://www.cursemaven.com'; content { includeGroup 'curse.maven' } }` with
  `compileOnly fg.deobf(...)` for the dependency itself.
- CurseMaven dependency configuration differs per toolchain: Loom-remap uses `modCompileOnly`;
  Loom 26.x uses plain `compileOnly`; ModDevGradle uses `compileOnly`; MDG Legacy uses
  `modCompileOnly`; FG6 uses `compileOnly fg.deobf(...)`; FG7 uses plain `compileOnly`.
- ModDevGradle Legacy does **not** support pre-1.17 — use FG6 for every 1.16.x branch.
- Forge before 1.18.2 has no jar-in-jar mechanism; shade a dependency instead of relying on
  jar-in-jar if a branch that old needs to embed one.

## Definition of done for a port

A version branch's port is not "done" until all of the following hold, for every loader it
targets:

1. `./build-all.ps1` succeeds end to end (or the individual `cd <loader> && ./gradlew build` for
   each loader folder), including `checkCommonIsLoaderNeutral`.
2. The common unit tests (`common/src/test/java`, run automatically as part of `build`) pass.
3. Fabric GameTests pass: `cd fabric && ./gradlew runGametest`.
4. `runServer` starts cleanly with Sophisticated Backpacks/Core present in the dev runtime (or,
   where no Sophisticated build exists for that (loader, Minecraft version) pair, starts cleanly
   without it and `Services.backpacks()` correctly returns `IBackpackIntegration.NONE` — see
   `docs/ARCHITECTURE.md`). Accept the EULA in `<loader>/run/eula.txt` first.
5. `runClient` starts, and a quick manual play-test (place/break in every build mode, undo/redo,
   and the Building Upgrade if Sophisticated Backpacks is available for this branch) works.
6. The built jar's metadata is correct: mod id, name, version, and Minecraft version range all
   match `gradle/shared.properties`, and the license file is present in the jar.
7. A **fresh-copy build** succeeds: clone/copy the branch to a clean directory (no existing
   `.gradle`, `build/`, or dev `run/` directories) and build it there with no manual setup beyond
   what's in this document and the branch's own `README.md`. This is what actually catches a
   toolchain step that only worked because of stale local Gradle/loom cache state.

Once all of the above hold and the branch is pushed, update the branch's own `README.md`,
`main`'s `README.md` support-matrix status, and cut a release per `docs/RELEASING.md`.
