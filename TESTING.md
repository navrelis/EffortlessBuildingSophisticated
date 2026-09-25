# Testing Sophisticated Building 1.18.1 (Fabric also 1.18)

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on Forge) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, on Forge including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or `.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. On the title screen the harness creates a fresh superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through `Minecraft#createLevel` (no quick play), runs the client
  scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players (and real backpacks on Forge), writes the same result file and exits. On Forge the task starts every
  run on a fresh superflat world (it writes `level-type=flat` into the game directory's `server.properties` and deletes
  the old world): Forge's game test server takes its world from `server.properties`, and with the default normal
  terrain the test structures sit in caves, where falling gravel could fill them. Minecraft 1.18.x cannot read flat
  `generator-settings` from `server.properties` (it logs `ERROR ... WorldGenSettings: ... Not a registry ops`, a vanilla
  bug, not the mod's) and uses the default superflat layers.

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
  on 1.18.1 that is Forge only. Fabric (no SB for 1.18.x) runs no `sb.*` checks and does not compile against SB.
- The file is rewritten after every check (atomically), so a crash or a kill still leaves the checks done so far.

## Scenarios

### Client (`runSmokeClient`, both loaders)

Every step goes through the path a player uses: clicks are real key mapping presses (the mod's `ClientEvents` mouse
handling calls `BuilderChain`, and vanilla's own interaction runs next to it, as for a player), build modes are picked
in the radial menu, the mirror is added in the modifier screen, and the builds reach the integrated server as the
mod's packets. Setup (inventories, backpacks, game mode) and all assertions run on the server thread against the
server world.

| Check | Asserts |
|---|---|
| `client.world_joined` | A fresh superflat world was created and joined (fails at once on a failure screen) |
| `client.mod_data_pack_compatible` | The mod's data pack is enabled and not flagged incompatible (Forge; Fabric serves mod data through one combined pack) |
| `client.radial_menu_opens` | The radial key opens `RadialMenu`, it renders (its hit-test highlights LINE under the mouse), a click selects LINE, releasing the key closes it. Screenshot `radial_menu` |
| `client.buildmode_line_preview` | After the first right click and turning to the end point, the preview holds exactly the 5 expected positions, all valid. Screenshot `line_preview` |
| `client.place_line` | The second click places those 5 stone (server world), nothing around them, creative inventory unchanged. Screenshot `line_placed` |
| `client.break_line` | Two left clicks break the line again (creative mass break) |
| `client.mirror_modifier` | "Add Mirror" in the modifier screen adds a mirror; a 3 block line places 6 blocks (line + mirror image). Screenshots `modifiers_screen`, `mirror_placed` |
| `client.place_line_survival` | Survival (power level 3 via `/powerlevel`): a 5 block line consumes exactly 5 planks |
| `client.undo_redo` | Undo removes the 5 blocks and gives the planks back (mined with the axe), redo restores them and charges them again |
| `sb.hud_count_synced` | Forge: the client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Forge: holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | Forge: a 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Forge: upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Forge: survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | Forge: the backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | Forge: the backpack worn in the Curios `back` slot supplies a line |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 10 checks (the `client.*` ones), Forge 17.

### Server (`runSmokeServer`, both loaders)

Game tests with a fake survival player (Forge `FakePlayerFactory`; on Fabric, whose API 0.46 has no fake player,
`VanillaFakePlayers`: a vanilla `ServerPlayer` outside the player list whose connection drops every packet). The block
sets are written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly
what arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | Forge only: as on the client, server side |
| `server.no_mod_errors` | As on the client |

Fabric reports 3 checks, Forge 9. Game tests of other mods in the runtime are not checks: they are only logged when
they pass; if one fails, the run fails with a `server.foreign_game_test` check (on 1.18.1 no other mod in the dev
runtime registers one).

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeAccessorySlots                   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structures/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture, net.p3pp3rf1y API), only for loaders with SB (Forge)
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

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json` (depends on `fabric`, Fabric API's mod id on 1.18.x), entrypoints `main`/`client`/`fabric-gametest` | `META-INF/mods.toml` (`loaderVersion` `[39,)`) + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`); no SB fixture | MDG Legacy runs `smokeClient`/`smokeServer` (`loadedMods` main + harness); SB and Curios through remapping configurations (`modLocalRuntime`, `modSmoketestLocalRuntime`) |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` |
| Fake player | `VanillaFakePlayers` (Fabric API 0.46 has none) | `FakePlayerFactory` |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | none (no SB) | Curios 1.18.1-5.0.6.2 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke"). A loader without SB drops the
   `smoketestBackpacks` source directories, its `SmokeAccessorySlots` service and the `sb_` game tests (see the Fabric
   folder of this branch).
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.21.1 via 1.20.4, 1.20.1, 1.19.2 and 1.18.2 to
   1.18.1, and what older versions may need:
   - Java 17: no `List#getFirst/getLast` (`get(0)`, `get(size() - 1)`).
   - Packets: no `StreamCodec`; `ServerScenarios#roundTrip` writes with the payload's `write(FriendlyByteBuf)` and
     reads with its `FriendlyByteBuf` constructor. Before 1.20.2 there is no `CustomPacketPayload`: the payloads are the
     mod's `ModPayload`.
   - `VanillaFakePlayers` (a vanilla server player for loaders without a fake player API): 1.18.2's `ServerPlayer`
     constructor has no profile public key, and `ServerGamePacketListenerImpl#send` takes a netty
     `GenericFutureListener` instead of a `PacketSendListener`.
   - World creation: 1.18.x has no world preset registry and no `WorldOpenFlows`; the harness builds the flat
     generator itself (`FlatLevelSource` with `FlatLevelGeneratorSettings.getDefault`, what the private flat preset of
     the world creation screen does) and calls `Minecraft#createLevel(name, settings, registryAccess, worldGen)`.
     1.18.1 has no structure sets yet: `RegistryAccess.builtin()` (a `RegistryHolder`), `new FlatLevelSource(settings)`
     and `FlatLevelGeneratorSettings.getDefault(biomes)`.
   - Commands: `Commands#performCommand` (no `performPrefixedCommand`); options: `Options#renderDistance` is a field.
   - `ClientWindow`: LWJGL 3.2.1 (Forge 1.18.2's runtime) has no `glfwGetMonitorWorkarea`; the window goes to the
     secondary monitor's position (`glfwGetMonitorPos`) + 40 px.
   - `ModErrorLogCapture`'s appender uses the `AbstractAppender(name, filter, layout, ignoreExceptions)` constructor
     (older log4j-core on the Forge dev classpath).
   - Game test template folder `data/<ns>/structures/` (plural before 1.21) and the NBT `DataVersion` of
     `smoketest_empty.nbt` (2865 = 1.18.1; 2975 on 1.18.2, 3120 on 1.19.2, 3465 on 1.20.1, 3700 on 1.20.4, 3955 on
     1.21.1).
   - Fabric API for 1.18.1 (0.46) still has the mod id `fabric`: the harness mod depends on `fabric`, not `fabric-api`.
   - Forge 1.18.1 / MDG Legacy: `META-INF/mods.toml` with `loaderVersion="[39,)"` (a harness mod asking for a newer
     javafml fails the loading), `pack.mcmeta` with pack format 8 and `forge:data_pack_format` 8,
     `TickEvent.ClientTickEvent` with a phase, the no-argument `@Mod` constructor; mod dependencies are SRG-named and go
     through MDG Legacy's remapping configurations; Curios 5 returns a capability `LazyOptional` from
     `getCuriosInventory`. The game test server reads `level-type=flat` (no namespace in 1.18.x). Forge 39's dev runs
     need the launcher libraries of Forge 39.1.2's profile pinned (`forge/build.gradle`, securejarhandler 1.0.3): with
     the newer securejarhandler Gradle resolves, every run crashed at startup (`InaccessibleObjectException`,
     `java.lang.invoke` not opened to `cpw.mods.securejarhandler`).
   - SB 1.18.1 (3.15): Sophisticated Core is inside the Backpacks jar (same packages), no separate dependency.
   - SB fixture: the wrapper lookup differs per loader (`BackpackWrapperLookup.get(stack)` on the Fabric port, the
     `CapabilityBackpackWrapper` capability on Forge; both `LazyOptional`s), so the fixture resolves it by reflection;
     `new ResourceLocation(ns, path)`; the fixture puts the contents into empty slots with `setStackInSlot`.
   - Unchanged and working as on 1.20.1: `LevelSettings`, `DataPackConfig`, `GlobalTestReporter`/`TestReporter`,
     `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, the `RadialMenu` / `ModifiersScreen`
     field names, Forge `FakePlayerFactory`, `GameTestHolder`/`PrefixGameTestTemplate`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

## Findings of the first runs (1.18.2, inherited)

- Fabric GameTests: 1.18.2's `PlayerList#placeNewPlayer` reads the server's profile cache, which the game test server
  does not have (all 17 tests failed with a `NullPointerException`). `GameTestSupport#spawnPlayer` now adds the test
  player to the level with its own fake connection instead of the player list.
- Forge: the harness mod's `mods.toml` still asked for javafml 43 (1.19.2): Forge 1.18.2 refused to load it.
- Forge: `level-type=minecraft:flat` is not a 1.18.2 level type; the game test server silently built normal terrain.
  `runSmokeServer` now writes `level-type=flat`.
- All 17 client checks (7 `sb.*`) and 9 server checks (6 `sb.*`) pass on Forge 40.3.12 with Sophisticated Backpacks
  1.18.2-3.20.3.1063, Core 1.18.2-0.6.4.604 and Curios 1.18.2-5.0.9.2; Fabric passes its 10 client and 3 server
  checks.

## Findings of the first runs (1.18.1)

- Fabric: Fabric API 0.46.6+1.18 is the mod `fabric` (renamed `fabric-api` later): the mod's and the harness mod's
  `fabric.mod.json` depended on `fabric-api` and the loader refused to start ("requires fabric-api, which is missing").
- Fabric: Fabric API 0.46 has no transitive access wideners, so `MenuType.MenuSupplier`, the `MenuType` constructor and
  `MenuScreens.register` are opened by the mod's own access widener (compile errors before).
- Forge: the dev runs crashed at startup until the Forge 39.1.2 launcher libraries were pinned (see above). The same
  jar (reobfuscated) started cleanly on a real Forge 1.18.1 server (installer 39.1.2, mods: this jar, Sophisticated
  Backpacks 1.18.1-3.15.15.550, Curios 1.18.1-5.0.6.2): "Registered Sophisticated Backpacks upgrade containers", "Done".
- Results: Fabric 17/17 GameTests, 10 client and 3 server checks; Forge 17 client checks (7 `sb.*`) and 9 server
  checks (6 `sb.*`) on Forge 39.1.2 with Sophisticated Backpacks 1.18.1-3.15.15.550 and Curios 1.18.1-5.0.6.2.

## Minecraft 1.18 check (one Fabric jar for 1.18 and 1.18.1)

The release jars of this branch (compiled against 1.18.1) were run in a Minecraft 1.18 runtime: a scratch copy with
the 1.18 loader/game versions, the mod sources replaced by the release jar and the smoke harness compiled against that
jar.

| Loader | Runtime | Result |
|---|---|---|
| Fabric | Minecraft 1.18, Fabric Loader 0.19.5, Fabric API 0.44.0+1.18 (the release jar as is: `minecraft` `>=1.18 <=1.18.1`, `fabric` `>=0.44.0`) | `runSmokeServer` 3 checks passed, `runSmokeClient` 10 checks passed |
| Forge | Real Forge 1.18 server (installer 38.0.17), Sophisticated Backpacks 1.18-3.12.1.433 (the last SB for 1.18), Curios 1.18-5.0.2.5 | The release jar does not load ("needs language provider javafml:39 or above"). With its `mods.toml` widened for the test (javafml `[38,)`, Forge `[38.0.17,)`, Minecraft `[1.18,1.18.1]`, SB `[1.18-3.12.1,)`) the server reaches "Done", but the backpack integration cannot link: SB 3.12.1 still keeps the shared classes under `net.p3pp3rf1y.sophisticatedbackpacks` (no `net.p3pp3rf1y.sophisticatedcore` package), so `BuildingUpgradeItem` fails with `ClassNotFoundException: net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase` (6 ERRORs) and no upgrade containers are registered. Forge 38 has no game test integration either (`net.minecraftforge.gametest` and the game test server are Forge 39+), so `forge/`'s `runSmokeServer` cannot run there |

Hence `minecraft_version_range=[1.18,1.18.1]` (Fabric `>=1.18 <=1.18.1`) with the Fabric API floor 0.44.0 (the Fabric API
builds up to 0.46.3+1.18 accept 1.18, from 0.46.4+1.18 on they require exactly 1.18.1), and Forge
`forge_minecraft_version_range=[1.18.1]` with Forge floor 39.1.2 and the optional Sophisticated Backpacks/Core range
`[1.18.1-3.15.15,)`. Minecraft 1.18 gets its own Forge jar from `forge-1.18/` (below).

### Forge 1.18 (`forge-1.18/`)

`forge-1.18/` builds `sophisticatedbuilding-forge-1.18-4.3.0.jar` (Minecraft `[1.18]`, Forge `[38.0.17,)`, optional
Sophisticated Backpacks `[1.18-3.12.1,)`) from `../forge` with the classes Sophisticated Backpacks 1.18 and Forge 38
need replaced (README.md, "Forge 1.18"). Its smoke runs use the same scenarios and check names as `forge/`, including
every `sb.*` check (Forge 38.0.17, Sophisticated Backpacks 1.18-3.12.1.433, Curios 1.18-5.0.2.5):

- `runSmokeServer`: 9 checks passed (6 `sb.*`). Forge 38 has no game test server, so the task starts a dedicated
  server (`server.properties`: `level-type=flat`, `spawn-protection=0`; `eula.txt` accepted) and `SmokeServerRunner`
  (`forge-1.18/src/smoketest`) runs the scenarios as vanilla test functions at the world spawn, with its own
  `GameTestTicker` (the vanilla ticker only runs in a server started from an IDE), then stops the server. Spawn
  protection must be off: the fake players are no operators and the dedicated server would refuse their builds near the
  spawn.
- `runSmokeClient`: 17 checks passed (7 `sb.*`), screenshots as on 1.18.1.
- `runServer`: "Registered Sophisticated Backpacks upgrade containers", "Done". A real Forge 1.18 server (installer
  38.0.17, mods: this jar, Sophisticated Backpacks 1.18-3.12.1.433, Curios 1.18-5.0.2.5) the same, and it stops cleanly.
- The `ERROR` "No loader defined for tool_types" at server start comes from Sophisticated Backpacks 1.18's own data
  (`data/sophisticatedbackpacks/registry/tool_types.json`), not from this mod.
