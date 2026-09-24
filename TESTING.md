# Testing Sophisticated Building 1.20.1

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on Forge) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration |

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
  (`sb-smoketest-<time>`, older ones are deleted) through the vanilla world creation flow (no quick play), runs the
  client scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players and real backpacks, writes the same result file and exits. On Forge the task starts every run on a
  fresh superflat world (it writes the game directory's `server.properties` and deletes the old world): Forge 1.20.1's
  game test server takes its world from `server.properties`, and with the default normal terrain the test structures
  at y -60 sat in caves, where falling gravel could fill them (one flaky `sb.worn_backpack` in the first runs).

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
  on 1.20.1 both Fabric and Forge do.
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
| `sb.hud_count_synced` | The client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | A 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | The backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | The backpack worn in an accessory slot supplies a line: Curios `back` (Forge), Trinkets `chest/back` (Fabric server run). Skipped with the reason if no accessory mod is in the runtime: the Fabric client run has none, see "Adopting" item 5 |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

### Server (`runSmokeServer`, both loaders)

Game tests with a fake survival player (Fabric API `FakePlayer`, Forge `FakePlayerFactory`). The block sets are
written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly what
arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side |
| `server.no_mod_errors` | As on the client |

Game tests of other mods in the runtime are not checks: they are only logged when they pass; if one fails, the run
fails with a `server.foreign_game_test` check (on 1.20.1 no other mod in the dev runtime registers one).

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform
    backpack/SmokeBackpacks, SmokeAccessorySlots                   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structures/smoketest_empty.nbt (empty game test template)
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

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `META-INF/mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG Legacy runs `smokeClient`/`smokeServer` (`loadedMods` main + harness); SB and Curios through remapping configurations (`modLocalRuntime`, `modSmoketestLocalRuntime`) |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | Trinkets 3.7.2 + Cardinal Components 5.2.2 (smoke server run only) | Curios 5.14.1 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke").
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.21.1 via 1.20.4 to 1.20.1, and what older versions
   may need:
   - Java 17: no `List#getFirst/getLast` (`get(0)`, `get(size() - 1)`).
   - Packets: no `StreamCodec`; `ServerScenarios#roundTrip` writes with the payload's `write(FriendlyByteBuf)` and
     reads with its `FriendlyByteBuf` constructor. 1.20.1 has no `CustomPacketPayload`: the payloads are the mod's
     `ModPayload`.
   - `VanillaFakePlayers` (a vanilla server player for loaders without a fake player API) is gone: it needs
     `CommonListenerCookie` (1.20.2+), and both 1.20.1 loaders have a fake player API.
   - World creation: `WorldOpenFlows#createFreshLevel(name, settings, options, dimensions)` has no screen argument
     in 1.20.1.
   - `ModErrorLogCapture`'s appender uses the `AbstractAppender(name, filter, layout, ignoreExceptions)` constructor:
     the Forge 1.20.1 dev compile classpath has a log4j-core without `Property.EMPTY_ARRAY` and the properties
     constructor.
   - Game test template folder `data/<ns>/structures/` (plural before 1.21) and the NBT `DataVersion` of
     `smoketest_empty.nbt` (3465 = 1.20.1; 3700 on 1.20.4, 3955 on 1.21.1).
   - Forge 1.20.1 / MDG Legacy: `META-INF/mods.toml` with `mandatory=` dependencies, a `pack.mcmeta` (pack format
     15), `TickEvent.ClientTickEvent` with a phase, the no-argument `@Mod` constructor; mod dependencies are SRG-named
     and go through MDG Legacy's remapping configurations; Curios 5 returns a capability `LazyOptional` from
     `getCuriosInventory` (`resolve()` to an `Optional`).
   - SB fixture: the wrapper lookup differs per loader (`BackpackWrapperLookup.get(stack)` on the Fabric port, the
     `CapabilityBackpackWrapper` capability on Forge; both `LazyOptional`s), so the fixture resolves it by
     reflection; `new ResourceLocation(ns, path)`; the Fabric port's inventory only takes items through the Fabric
     Transfer API, so the fixture puts the contents into empty slots with `setStackInSlot` (both loaders have it)
     instead of `insertItem`.
   - Trinkets for 1.20.1 is 3.7.2 with Cardinal Components 5.2.2, group `dev.onyxstudios.cardinal-components-api`
     (renamed `org.ladysnake.cardinal-components-api` in 6.x); both from the same Mavens as on 1.21.1.
   - Unchanged and working as on 1.20.4: `LevelSettings`, `WorldDataConfiguration`, `WorldPresets.FLAT`,
     `GlobalTestReporter`/`TestReporter`, `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, the
     `RadialMenu` / `ModifiersScreen` field names, Fabric `FakePlayer`, Forge `FakePlayerFactory`,
     `GameTestHolder`/`PrefixGameTestTemplate`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.
4. Fabric: when a mod dependency (here Trinkets) is added to a build whose Loom remap cache already holds the
   Sophisticated Backpacks jar, that cached jar keeps its calls into the new dependency in intermediary names
   (`TrinketInventory.method_5439`), which fails with `NoSuchMethodError` in the dev runs (the mod logs "Could not link
   SophisticatedBackpacks' PlayerInventoryProvider.runOnBackpacks", `sb.worn_backpack` fails). Delete
   `fabric/.gradle/loom-cache/remapped_mods` (or run once with `--refresh-dependencies`); a fresh clone is not affected.
5. Fabric 1.20.1: Trinkets 3.7.2 was built with Loom 0.11, and its client mixin `ClickableWidgetMixin` shadows
   `AbstractWidget`'s field by its Yarn name (`hovered`), which the Mojang-mapped dev client cannot resolve: the client
   crashes at startup (players run the intermediary jar and are not affected; Trinkets 3.8.1 for 1.20.4 is built with
   Loom 1.4 and has the intermediary name). `fabric/build.gradle` therefore leaves Trinkets and Cardinal Components out
   of the `runSmokeClient` classpath; the client reports `sb.worn_backpack` as skipped, `runSmokeServer` (no client
   mixins) runs it with Trinkets.

## Findings of the first runs (1.20.1)

- Forge: the recipes that need Sophisticated Backpacks carried only Fabric and NeoForge 20.4 load conditions; Forge
  1.20.1 reads `conditions` with `forge:mod_loaded`. Added, so a Forge server without SB loads the recipes without
  errors (checked with a dev server without SB).
- Forge: Forge 1.20.1's game test server built a normal-terrain world, see `runSmokeServer` above.
- Fabric: the held-slot resend of 1.21.1 (`FabricCommonEvents`, a player holding exactly the blocks a build needs lost
  the held block client side) is ported; `sb.upgrade_supplies_blocks`, `sb.tier_cap` and `sb.worn_backpack_chest`
  pass with it.
- Forge sends every payload encoded (a `SimpleChannel` message is written to a buffer when it is sent, also in
  singleplayer), so the unencoded in-memory payload problem of NeoForge 1.20.4 does not exist here.
- The Fabric SB port rescans Trinkets slots for backpacks only every 100 ticks: a backpack put into a Trinkets slot
  supplies blocks after up to 5 s (the scenarios wait for it).
- The same Forge harness also passes `runSmokeServer` on NeoForge 1.20.1-47.1.106 (9/9, all `sb.*`), with
  `legacyForge { enable { neoForgeVersion = "1.20.1-47.1.106" } }` in place of the Forge version.
