# Testing Sophisticated Building 1.21.1

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on NeoForge and Forge) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

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
  takes about a minute after the game has loaded.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players and real backpacks, writes the same result file and exits.

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
  Forge 1.21.1 has no SB and reports none.
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
| `sb.worn_backpack` | The backpack worn in an accessory slot supplies a line: Curios `back` (NeoForge), Trinkets `chest/back` (Fabric). Skipped with the reason if no accessory mod is in the runtime |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

### Server (`runSmokeServer`, all loaders)

Game tests with a fake survival player (the loader's fake player, or `VanillaFakePlayers` on Forge 52 which has none).
The block sets are encoded and decoded with the packets' stream codecs and handed to the packets' server handlers,
exactly what arrives from a client.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side (Fabric, NeoForge) |
| `server.no_mod_errors` | As on the client |

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeAccessorySlots                   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structure/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture, net.p3pp3rf1y API), only for loaders with SB
<loader>/src/smoketest                 loader glue: mod metadata, entry points, fake players, accessory slots
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run.

How it drives the client: `SmokeClient.init()` starts a harness thread; `ClientDriver` submits every action to the
client thread (`Minecraft#submit`) or the integrated server thread and waits for it, and counts client ticks through
`SmokeClient.onClientTickEnd()`, so scenarios read as linear scripts. Key presses use `KeyMapping.set/click` like
`MouseHandler`; aiming sets the player's rotation. The radial menu is steered by writing its accumulated mouse offset
(it tracks the mouse as a delta from the screen centre), the hit-testing, highlighting and selection are the menu's
own.

Loader glue per build:

| | Fabric | NeoForge | Forge |
|---|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `neoforge.mods.toml`, `@Mod` | `mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 7 creates `runSmoketestClient`/`runSmoketestGameTestServer`; `runSmokeClient`/`runSmokeServer` depend on them |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` | `@GameTestHolder`, template `sophisticatedbuilding:smoketest_empty` |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` |
| Accessory slot | Trinkets (smoke runtime only) | Curios (smoke runtime only) | - |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch, and the `smoketest` source set, run and `check` wiring from each
   `<loader>/build.gradle` (search for "smoke").
2. Compile (`gradlew smoketestClasses`). What is 1.21.1-specific and may need adapting:
   - World creation: `WorldOpenFlows#createFreshLevel(name, LevelSettings, WorldOptions, dimensions getter, screen)`,
     `LevelSettings` / `WorldDataConfiguration` / `GameRules` constructors, `WorldPresets.FLAT` (`ClientScenarios#joinFreshWorld`).
   - Game test API: `GameTestHelper`, `GlobalTestReporter`/`TestReporter` (the framework was rewritten in 1.21.5), the annotations and
     template lookup of each loader, and the NBT `DataVersion` of `smoketest_empty.nbt` (3955 = 1.21.1; old templates
     are upgraded by DataFixer).
   - Packets: `StreamCodec` round trip in `ServerScenarios#roundTrip` (1.20.5+; older versions use `FriendlyByteBuf`
     write/read methods).
   - Client: `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, `CommonListenerCookie` (1.20.2+),
     the mod's `RadialMenu` field names (`accumulatedMouseX/Y`, `mouseInitialized`, `ringInnerEdge/OuterEdge`) and
     `ModifiersScreen#addMirrorButton`.
   - SB: the `IBackpackWrapper` / `UpgradeHandler` API (`SophisticatedBackpacksFixture`), block/item ids.
   - Accessory mods: the Curios / Trinkets glue and their versions.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

## Findings of the first runs (1.21.1)

- Fabric: the server cancelled the vanilla placement of a build-mode click but never corrected the client's predicted
  item use, so a player holding exactly the blocks a build needs (e.g. 1 stone + a backpack) lost the held block
  client side and the build was cancelled. Fixed in `FabricCommonEvents` (resend the slot, as NeoForge/Forge already do);
  caught by `sb.upgrade_supplies_blocks`, `sb.tier_cap` and `sb.worn_backpack_chest`.
- Forge: `pack.mcmeta` declared only `pack_format` 34 (resource packs), so the mod's data pack was flagged incompatible
  (data packs are 48 in 1.21.1). Now `supported_formats: [34, 48]`; checked by `client.mod_data_pack_compatible`.
- The Fabric SB port rescans Trinkets slots for backpacks only every 100 ticks: a backpack put into a Trinkets slot
  supplies blocks after up to 5 s.

## Minecraft 1.21 check (one jar for 1.21 and 1.21.1)

The release jars built from this branch (compiled against 1.21.1) were run in a Minecraft 1.21 runtime: a scratch copy
of the branch switched to the 1.21 loader/game versions, with the mod sources replaced by the release jar (its
`fabric.mod.json` / `neoforge.mods.toml` / `mods.toml` already declaring the 1.21 range) and the smoke harness
compiled against that jar.

| Loader | Runtime | Result |
|---|---|---|
| NeoForge | NeoForge 21.0.167, Sophisticated Backpacks 1.21-3.20.26.1151 + Core 1.21-0.7.13.797, no Curios (no build for NeoForge 21.0) | `runSmokeServer` 9 checks passed (1 skipped: `sb.worn_backpack`, no Curios), `runSmokeClient` 17 checks passed (same skip); every other `sb.*` check passes against the old Backpacks API |
| Fabric | Fabric Loader 0.19.5, Fabric API 0.108.0+1.21.1 (runs on 1.21), no Sophisticated Backpacks (the Fabric port needs exactly 1.21.1; the `sb_*` game tests were left out) | `runSmokeServer` 3 checks passed, `runSmokeClient` 10 checks passed |
| Forge | Forge 51.0.33 | The jar does not load: `NoSuchMethodException: SophisticatedBuildingForge.<init>()` (constructor injection of `FMLJavaModLoadingContext` is Forge 52+), and `AddGuiOverlayLayersEvent`/`ForgeLayeredDraw` do not exist in Forge 51 |

Hence `minecraft_version_range=[1.21,1.21.1]` (Fabric: `>=1.21 <=1.21.1`), Forge `forge_minecraft_version_range=[1.21.1]`,
NeoForge floor 21.0.167, NeoForge optional Backpacks `[3.20.26,)` / Core `[0.7.13,)` (the last 1.21 builds), Fabric
`"fabric-api": ">=0.108.0"`: the newest Fabric API tagged for 1.21 (0.102.0+1.21) has no `ClientWorldEvents` and the jar
crashed at client start with it (`NoClassDefFoundError`); with the floor Fabric Loader reports the missing update instead.
The Fabric API 1.21.1 builds before 0.108.0 lack it as well, so the floor also fixes those on 1.21.1.

Running Forge 51.0.33 in dev needs ForgeGradle 6 with `jopt-simple` forced to 5.0.4 (its `bootstrap-api` pulls
6.0-alpha-3, module `joptsimple`, while modlauncher requires `jopt.simple`); ForgeGradle 7 stopped with the same error (the override was not tried there).
