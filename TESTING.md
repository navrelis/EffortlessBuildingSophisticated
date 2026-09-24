# Testing Sophisticated Building 1.21.5

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on NeoForge and Forge) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`; the run reports 18 with vanilla's `minecraft:always_pass`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration on NeoForge |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

On Minecraft 1.21.5 only NeoForge has Sophisticated Backpacks (official build); Fabric and Forge have none, so their
smoke runs have no `sb.*` checks and do not compile the SB fixture.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `neoforge` and `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or
`.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. On the title screen the
  harness creates a fresh superflat world with a unique name (`sb-smoketest-<time>`, older ones are deleted) through
  the vanilla world creation flow (no quick play), runs the client scenarios, writes the result and stops the game. It
  takes well under a minute after the game has loaded.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server (Forge 55: a dedicated server, see
  "Server" below) runs the server scenarios with fake
  survival players (and real backpacks on NeoForge), writes the same result file and exits.

The game exits by itself in every case: after the scenarios, on a failure screen, on a crash (a JVM shutdown hook
writes a failing `harness.completed` check), and after the internal watchdog (5 min,
`-Dsophisticatedbuilding.smoketest.timeoutSeconds=<s>` to change; writes a failing `harness.watchdog` check). The
Gradle task additionally has a 15 minute timeout for a game that hangs before the harness even starts.

The task passes only if the result file was written, every check passed and the game exited normally
(`../gradle/smoketest.gradle`); it prints every check. Old results and screenshots in the output directory are
deleted when the task starts.

### Result contract

`<dir>/smoketest-result.json`:

```json
{
  "passed": true,
  "checks": [ { "name": "sb.tier_cap", "passed": true, "detail": "..." } ],
  "screenshots": [ "C:\\...\\screenshots\\line_preview.png" ]
}
```

- `passed` is true when there is at least one check and all passed.
- A skipped check has `"passed": true`, `"skipped": true` and a detail starting with `SKIPPED:`.
- Checks named `sb.*` are Sophisticated Backpacks checks. A loader build that ships the SB integration
  (`META-INF/services/sophisticated.building.platform.services.IBackpackIntegration`) must report passing `sb.*` checks;
  on 1.21.5 that is NeoForge only. Fabric and Forge 1.21.5 have no SB and report none.
- The file is rewritten after every check (atomically), so a crash or a kill still leaves the checks done so far.

## Scenarios

### Client (`runSmokeClient`, all loaders)

Every step goes through the path a player uses: clicks are real key mapping presses (the mod's `ClientEvents` mouse
handling calls `BuilderChain`, and vanilla's own interaction runs next to it, as for a player), build modes are picked
in the radial menu, the mirror is added in the modifier screen, and the builds reach the integrated server as the
mod's packets. Setup (inventories, backpacks, game mode) and all assertions run on the server thread against the
server world.

| Check | Asserts |
|---|---|
| `client.world_joined` | A fresh superflat world was created and joined (fails at once on a failure screen) |
| `client.mod_data_pack_compatible` | The mod's data pack is enabled and not flagged incompatible (Forge/NeoForge; pack_format) |
| `client.radial_menu_opens` | The radial key opens `RadialMenu`, it renders (its hit-test highlights LINE under the mouse), a click selects LINE, releasing the key closes it. Screenshot `radial_menu` |
| `client.buildmode_line_preview` | After the first right click and turning to the end point, the preview holds exactly the 5 expected positions, all valid. Screenshot `line_preview` |
| `client.place_line` | The second click places those 5 stone (server world), nothing around them, creative inventory unchanged. Screenshot `line_placed` |
| `client.break_line` | Two left clicks break the line again (creative mass break) |
| `client.mirror_modifier` | "Add Mirror" in the modifier screen adds a mirror; a 3 block line places 6 blocks (line + mirror image). Screenshots `modifiers_screen`, `mirror_placed` |
| `client.place_line_survival` | Survival (power level 3 via `/powerlevel`): a 5 block line consumes exactly 5 planks |
| `client.undo_redo` | Undo removes the 5 blocks and gives the planks back (mined with the axe), redo restores them and charges them again |
| `sb.hud_count_synced` | The client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | A 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | The backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | The backpack worn in the Curios `back` slot supplies a line. Skipped with the reason if no accessory mod is in the runtime |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

The `sb.*` rows run on NeoForge only.

### Server (`runSmokeServer`, all loaders)

Game tests with a fake survival player (the loader's fake player, or `VanillaFakePlayers` on Forge 55 which has none).
The block sets are encoded and decoded with the packets' stream codecs and handed to the packets' server handlers,
exactly what arrives from a client.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side (NeoForge only) |
| `server.no_mod_errors` | As on the client |

Since Minecraft 1.21.5 a game test is a test function (code) plus a test instance (data). The scenarios are registered
as test functions `sophisticatedbuilding_smoketest:<name>` by the loader glue (`SmokeServerTests#functions`); the
instances that run them are data in the harness resources: `data/sophisticatedbuilding/test_instance/<name>.json`
(template `sophisticatedbuilding:smoketest_empty`, 400 ticks) with one `test_environment` `smoke_<n>` each, so every
test is its own batch and they run one after the other, as on 1.21.1. The `sb_` instances live in
`common/src/smoketestBackpacks/resources`, so only loaders with SB run them. The reporter turns only this mod's
instances into checks (the game test server also runs vanilla's optional `minecraft:always_pass`).

Forge 55 has no working game test server (its game test launch target starts a normal dedicated server; Forge's
`Main` patch has the game test server commented out, still in 55.1.14). On Forge `runSmokeServer` therefore starts
that dedicated server (EULA auto-accepted by Forge for the game test target, a fresh `gametest_world\<time>` world),
and the harness runs the same test instances itself (`SmokeServerRunner`: batches by environment, a structure grid
40 blocks below the build limit above spawn, the reporter finished and the server halted when all are done), exactly
what the vanilla game test server does.

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerTests, SmokeServerRunner, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeAccessorySlots                   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structure/smoketest_empty.nbt (empty 8x8x8 game test template),
                                       test_instance/server_*.json, test_environment/smoke_1..2.json
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture, net.p3pp3rf1y API) and the sb_* test
                                       instances (smoke_3..8), only for loaders with SB (NeoForge)
<loader>/src/smoketest                 loader glue: mod metadata, entry points, test function registration, fake players, accessory slots
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run.

How it drives the client: `SmokeClient.init()` starts a harness thread, which first waits for the `Minecraft` instance
(NeoForge 21.5 constructs mods before it exists); `ClientDriver` submits every action to the client thread
(`Minecraft#submit`) or the integrated server thread and waits for it, and counts client ticks through
`SmokeClient.onClientTickEnd()`, so scenarios read as linear scripts. Key presses use `KeyMapping.set/click` like
`MouseHandler`; aiming sets the player's rotation. The radial menu is steered by writing its accumulated mouse offset
(it tracks the mouse as a delta from the screen centre), the hit-testing, highlighting and selection are the menu's
own.

Loader glue per build:

| | Fabric | NeoForge | Forge |
|---|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client` | `neoforge.mods.toml`, `@Mod` | `mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 7 creates `runSmoketestClient`/`runSmoketestGameTestServer`; `runSmokeClient`/`runSmokeServer` depend on them |
| Test functions | `Registry.register(BuiltInRegistries.TEST_FUNCTION, ...)` | `DeferredRegister` on `Registries.TEST_FUNCTION` | `DeferredRegister` on `Registries.TEST_FUNCTION` |
| Test runner | Fabric API game test server (`-Dfabric-api.gametest`) | NeoForge game test server | `SmokeServerRunner` on the dedicated server (Forge 55 has no game test server) |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` |
| SB fixture | - | `common/src/smoketestBackpacks` | - |
| Accessory slot | - | Curios (smoke runtime only) | - |

Forge's world previews and outlines come from the client mixin `LevelRendererMixin` (Forge 55 has no level render
event). The smoke runs inherit the `--mixin.config=sophisticatedbuilding.forge.mixins.json` argument from
`minecraft.runs.configureEach`, so the mixin is applied in `runSmokeClient` (log: "Mixing LevelRendererMixin from
sophisticatedbuilding.forge.mixins.json into net.minecraft.client.renderer.LevelRenderer") and its config registered in
`runSmokeServer`; the `line_preview` screenshot is the runtime proof that the previews render.

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch, and the `smoketest` source set, run and `check` wiring from each
   `<loader>/build.gradle` (search for "smoke"). A loader without SB leaves `smoketestBackpacks` out of its source set
   and has no accessory slot glue (as Fabric and Forge here); its `sb_` test instances then simply do not exist.
2. Compile (`gradlew smoketestClasses`). What is version-specific and may need adapting:
   - World creation: `WorldOpenFlows#createFreshLevel(name, LevelSettings, WorldOptions, dimensions getter, screen)`,
     `LevelSettings` / `WorldDataConfiguration` / `GameRules` constructors, `WorldPresets.FLAT` (`ClientScenarios#joinFreshWorld`).
   - Game test API (1.21.5+ shape): test functions in `Registries.TEST_FUNCTION`, data-driven `test_instance` /
     `test_environment`, `GameTestInfo#id()`, `GameTestHelper#fail/assertTrue` taking a `Component`,
     `GlobalTestReporter`/`TestReporter`, and whether the loader's game test server works at all (Forge 55 does not).
     The NBT `DataVersion` of `smoketest_empty.nbt` is 3955 (1.21.1); old templates are upgraded by DataFixer (1.21.4 =
     4189, 1.21.5 = 4325).
   - Packets: `StreamCodec` round trip in `ServerScenarios#roundTrip` (1.20.5+; older versions use `FriendlyByteBuf`
     write/read methods).
   - Client: `Screenshot.takeScreenshot` (asynchronous since 1.21.5), `KeyMapping.set/click`, `Minecraft#submit`, the
     toast manager, `Inventory#getSelectedSlot/setSelectedSlot` (1.21.5; the `selected` field before),
     `CommonListenerCookie` (1.20.2+), the mod's `RadialMenu` field names (`accumulatedMouseX/Y`, `mouseInitialized`,
     `ringInnerEdge/OuterEdge`) and `ModifiersScreen#addMirrorButton`.
   - Registries: `Registry#getValue` (1.21.2+; `get` returns an `Optional<Holder.Reference>`).
   - SB: the `IBackpackWrapper` / `UpgradeHandler` API (`SophisticatedBackpacksFixture`), block/item ids.
   - Accessory mods: the Curios / Trinkets glue and their versions.
   - Forge: the `pack.mcmeta` formats of the harness mod (and of the mod itself) and the `loaderVersion` in its `mods.toml`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

### What the 1.21.1 -> 1.21.4 adoption changed

- `ClientScenarios#joinFreshWorld`: `new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())` (1.21.2+ takes the
  feature flags); the dimensions getter receives a `HolderLookup.Provider`:
  `registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)` instead of
  `registryOrThrow(...).getHolderOrThrow(...)`.
- `ClientDriver#screenshot`: `Minecraft#getToastManager()` instead of `getToasts()`.
- `SophisticatedBackpacksFixture#item`: `BuiltInRegistries.ITEM.getValue(id)` instead of `get(id)`.
- Forge harness mod: `loaderVersion="[54,)"`, `pack.mcmeta` `pack_format` 46 with `supported_formats: [46, 61]`.
- Fabric: no SB on 1.21.4, so no `smoketestBackpacks` in the source set, no Trinkets/Cardinal Components smoke
  dependencies, no `TrinketsAccessorySlots`, no `sb_` game tests.
- Unchanged and working as on 1.21.1: `RadialMenuDriver`, `ServerScenarios`, `VanillaFakePlayers`,
  `ServerGamePacketListenerImpl#teleport(x, y, z, yaw, pitch)`, the game test glue of all three loaders, the
  1.21.1 `smoketest_empty.nbt`.

### What the 1.21.4 -> 1.21.5 adoption changed

- Game test rework: the class/annotation based tests (`FabricSmokeServerTests`, `NeoForgeSmokeServerTests` with
  `@GameTestHolder`, `ForgeSmokeServerTests`) are gone. New `SmokeServerTests` (test functions by name, registered by
  each loader's glue in `Registries.TEST_FUNCTION`) plus data: `test_instance/<name>.json` (`minecraft:function`,
  template `sophisticatedbuilding:smoketest_empty`, `max_ticks` 400) and one `test_environment/smoke_<n>.json`
  (`minecraft:all_of`, empty) per test to keep one batch per test. `ServerScenarios.TIMEOUT_TICKS` moved into the JSON.
  NeoForge 21.5 has no `@GameTestHolder`/`@PrefixGameTestTemplate` and no namespace filter any more; Fabric's own
  `@GameTest` annotation (used by `fabric/src/gametest`) would work for Fabric only, the data approach works on all three.
- `SmokeServer`: the check name comes from `GameTestInfo#id().getPath()` (was `getTestName()`); tests outside the
  `sophisticatedbuilding` namespace (vanilla's `minecraft:always_pass`) are not checks.
- `ServerScenarios`: `GameTestHelper#fail` and `#assertTrue` take a `Component` (`Component.literal(...)`).
- Forge 55: no game test server; `SmokeServerRunner` runs the instances on the dedicated server the game test launch
  target starts (hooked on `ServerStartedEvent` and `TickEvent.ServerTickEvent.Post`). The dedicated server pauses
  when it has been empty for 60 s; the two tests finish in about a second.
- `ClientDriver#screenshot`: `Screenshot.takeScreenshot(RenderTarget, Consumer<NativeImage>)` reads the frame back
  asynchronously; the callback writes and closes the image and completes a future the harness waits for.
- `Inventory#selected` is private: `getSelectedSlot()` / `setSelectedSlot(int)` (`ClientDriver#selectHotbarSlot`,
  every fake player) and `getSelectedItem()` (Fabric held-slot fix).
- `SmokeClient`: waits for `Minecraft.getInstance()` before creating the `ClientDriver` (NeoForge 21.5 runs the mod
  constructors before the client exists; the first run failed with a NullPointerException in `ClientDriver`).
- Forge harness mod: `loaderVersion="[55,)"`, `pack.mcmeta` `pack_format` 55 with `supported_formats: [55, 71]`.
- Unchanged: `ClientScenarios` (world creation, radial menu, modifier screen), `RadialMenuDriver`,
  `SophisticatedBackpacksFixture` (SB 1.21.5-3.27.2.2152 API identical), Curios glue, `VanillaFakePlayers`
  construction, the 1.21.1 `smoketest_empty.nbt`.

## Findings (1.21.5)

- The two fixes found by the first 1.21.1 runs are applied here too: Fabric `FabricCommonEvents` resends the held slot
  when a build-mode click cancels the vanilla placement (the client had predicted the item use; a player holding
  exactly the blocks a build needs lost the held block client side), and Forge `pack.mcmeta` declares `pack_format` 55
  with `supported_formats: [55, 71]` (1.21.5 resource packs are 55, data packs 71; the port still had 46 from
  1.21.4), so the mod's data pack is not flagged incompatible; checked by `client.mod_data_pack_compatible`.
- Forge 55 (55.0.24 and the latest 55.1.14) has no working game test server, see above.
