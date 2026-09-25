# Testing Sophisticated Building 1.16.3

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on Forge) |
| Fabric server tests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`; Minecraft 1.16.3 has no game test framework, they run on the harness' `ServerTestRunner` on a dedicated dev server) |
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
- **runSmokeServer** is headless (no GPU needed, for CI) and runs the server scenarios with fake survival players (and
  real backpacks on Forge), writes the same result file and exits. Minecraft 1.16.3 has no game test framework (it
  arrives with 1.17) and Fabric API 0.25 no game test API, so on both loaders the task starts a plain dedicated dev
  server and the harness' `ServerTestRunner` (`common/src/smoketest/.../servertest`) runs one test per scenario in its
  own area next to the world spawn, ticked by the server, then stops the server. Every run starts on a fresh superflat
  world: the task deletes the old world and writes `server.properties` (`level-type=flat`, its own `server-port` so it
  can run next to other dev servers: Forge 25734, Fabric 25735, Fabric `runGametest` 25736) and `eula.txt` (dev-only
  server). Minecraft 1.16 cannot read flat `generator-settings` from `server.properties` (it logs
  `ERROR ... Not a registry ops`, a vanilla bug, not the mod's) and uses the default superflat layers.

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
  on 1.17.1 that is Forge only. Fabric (no SB for 1.17.1) runs no `sb.*` checks and does not compile against SB.
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
| `sb.tool_swapper_tools` | **Skipped on 1.16.3**: Sophisticated Backpacks 1.16.4-1.0.0.94 has no Tool Swapper upgrade (the fixture's `whyNoToolSwapper()`). On newer branches, Forge: survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | Forge: the backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | Forge: the backpack worn in the Curios `back` slot supplies a line |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 10 checks (the `client.*` ones), Forge 17.

### Server (`runSmokeServer`, both loaders)

Server tests with a fake survival player: `VanillaFakePlayers` on both loaders, a vanilla `ServerPlayer` outside the
player list whose connection drops every packet (Fabric API 0.25 has no fake player; Forge 34's fake players have no
connection at all, so `setGameMode` and every packet to them throw). The block
sets are written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly
what arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | Forge only: as on the client, server side (`sb.tool_swapper_tools` skipped, see above) |
| `server.no_mod_errors` | As on the client |

Fabric reports 3 checks, Forge 9 (8 passed, 1 skipped). The server test runner only runs the harness' own tests.

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
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest`, depends on `fabric` (Fabric API's mod id on 1.17.1) | `META-INF/mods.toml` (`loaderVersion` `[37,)`) + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`); no SB fixture | MDG Legacy runs `smokeClient` (client) and `smokeServer` (dedicated server, `--nogui`) with `loadedMods` main + harness; SB and Curios through remapping configurations (`modLocalRuntime`, `modSmoketestLocalRuntime`) |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `ForgeSmokeServerTests`: vanilla `TestFunction`s, template `sophisticatedbuilding:smoketest_empty`, run on `FMLServerStartedEvent`, ticked on `TickEvent.ServerTickEvent` |
| Fake player | `VanillaFakePlayers` (Fabric API 0.46 has none) | `FakePlayerFactory` |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | none (no SB) | Curios 1.17.1-5.0.2.7 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke"). A loader without SB drops the
   `smoketestBackpacks` source directories, its `SmokeAccessorySlots` service and the `sb_` game tests (see the Fabric
   folder of this branch).
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.21.1 via 1.20.4, 1.20.1, 1.19.2 and 1.18.x to 1.17.1,
   and what older versions may need:
   - Java 16 on 1.17.1 (records, pattern `instanceof`, switch expressions still work); no `List#getFirst/getLast`.
   - Packets: no `StreamCodec`; `ServerScenarios#roundTrip` writes with the payload's `write(FriendlyByteBuf)` and
     reads with its `FriendlyByteBuf` constructor. Before 1.20.2 there is no `CustomPacketPayload`: the payloads are the
     mod's `ModPayload`.
   - `VanillaFakePlayers` (a vanilla server player for loaders without a fake player API): the `ServerPlayer`
     constructor has no profile public key, and `ServerGamePacketListenerImpl#send` takes a netty
     `GenericFutureListener` instead of a `PacketSendListener`.
   - World creation: no world preset registry and no `WorldOpenFlows`; the harness builds the flat generator itself
     (`FlatLevelSource` with `FlatLevelGeneratorSettings.getDefault`, what the private flat preset of the world creation
     screen does) and calls `Minecraft#createLevel(name, settings, registryAccess, worldGen)`. 1.17.1's
     `DimensionType.defaultDimensions` takes the dimension type, biome and noise settings registries and the seed.
   - Commands: `Commands#performCommand` (no `performPrefixedCommand`); options: `Options#renderDistance` is a field.
   - `ClientWindow`: old LWJGL has no `glfwGetMonitorWorkarea`; the window goes to the secondary monitor's position
     (`glfwGetMonitorPos`) + 40 px.
   - `ModErrorLogCapture`'s appender uses the `AbstractAppender(name, filter, layout, ignoreExceptions)` constructor
     (older log4j-core on the Forge dev classpath). Minecraft 1.17.1 has no SLF4J: every logger is Log4j.
   - Game test template folder `data/<ns>/structures/` (plural before 1.21) and the NBT `DataVersion` of
     `smoketest_empty.nbt` (2730 = 1.17.1; 2975 on 1.18.2, 3120 on 1.19.2, 3465 on 1.20.1, 3700 on 1.20.4, 3955 on
     1.21.1).
   - Forge 1.17.1 / MDG Legacy: `META-INF/mods.toml` with `loaderVersion="[37,)"` (a harness mod asking for a newer
     javafml fails the loading), `pack.mcmeta` with pack format 7, `TickEvent.ClientTickEvent` with a phase, the
     no-argument `@Mod` constructor; server lifecycle events are `net.minecraftforge.fmlserverevents.FMLServer*Event`
     and `ServerLifecycleHooks` lives in `net.minecraftforge.fmllegacy.server`; no game test launch target and no
     `@GameTestHolder` (see runSmokeServer above); mod dependencies are SRG-named and go through MDG Legacy's remapping
     configurations; Curios 5 returns a capability `LazyOptional` from `getCuriosHandler`.
   - SB fixture: Sophisticated Backpacks 1.17.1 has no Sophisticated Core, so the wrapper, upgrade wrapper and inventory
     types are `net.p3pp3rf1y.sophisticatedbackpacks.api.IBackpackWrapper`, `...api.IUpgradeWrapper` and
     `...backpack.wrapper.BackpackInventoryHandler`. The wrapper lookup differs per loader (`BackpackWrapperLookup.get`
     on the Fabric port of newer branches, the `CapabilityBackpackWrapper` capability on Forge), so the fixture resolves
     it by reflection; the fixture puts the contents into empty slots with `setStackInSlot`.
   - Fabric GameTests: no `BlockItem.setBlockEntityData` (the test writes the `BlockEntityTag` itself), and
     `PlayerList#placeNewPlayer` needs the profile cache the game test server does not have (the test player is added
     to the level with its own fake connection).
   - Unchanged and working as on 1.18.2: `LevelSettings`, `DataPackConfig`, `GlobalTestReporter`/`TestReporter`,
     `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, the `RadialMenu` / `ModifiersScreen`
     field names, Forge `FakePlayerFactory`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

## Findings of the first runs (1.17.1)

- Fabric: Fabric API 0.46.1+1.17 still has the mod id `fabric` (not `fabric-api`): with `"fabric-api"` in `depends`
  Fabric Loader refused to start ("requires version 0.46.1 or later of fabric-api, which is missing"). The mod and the
  harness depend on `fabric`.
- Fabric GameTests: all 17 failed with the 1.18.2 profile cache `NullPointerException` until `GameTestSupport` took over
  the 1.18.2 fix (test player added to the level, not the player list).
- Forge: no game test server in Forge 37; `runSmokeServer` runs the scenarios on a dedicated dev server (see above).
- All 17 client checks (7 `sb.*`) and 9 server checks (6 `sb.*`) pass on Forge 37.1.1 with Sophisticated Backpacks
  1.17.1-3.12.3.496 and Curios 1.17.1-5.0.2.7; Fabric passes its 10 client and 3 server checks.
