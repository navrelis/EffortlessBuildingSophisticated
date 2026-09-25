# Testing Sophisticated Building 1.20.4

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on NeoForge and Forge) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`) plus the Porting Lib self test nested in the Fabric Sophisticated Core port (18 required tests) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration on Fabric and NeoForge |

Forge 1.20.4 has no Sophisticated Backpacks release, so the Forge build ships no SB integration and its smoke runs have
no `sb.*` checks (and no SB fixture source set), as Forge on 1.21.1.

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `neoforge` and `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or `.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. On the title screen the harness creates a fresh superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through the vanilla world creation flow (no quick play), runs the
  client scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server (Forge 49's works, unlike Forge 55's)
  runs the server scenarios with fake survival players (and real backpacks on Fabric and NeoForge), writes the same
  result file and exits.

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
  on 1.20.4 Fabric and NeoForge do. Forge 1.20.4 has no SB and reports none.
- The file is rewritten after every check (atomically), so a crash or a kill still leaves the checks done so far.

## Scenarios

### Client (`runSmokeClient`, all loaders; the `sb.*` rows on Fabric and NeoForge only)

Every step goes through the path a player uses: clicks are real key mapping presses (the mod's `ClientEvents` mouse
handling calls `BuilderChain`, and vanilla's own interaction runs next to it, as for a player), build modes are picked
in the radial menu, the mirror is added in the modifier screen, and the builds reach the integrated server as the
mod's packets. Setup (inventories, backpacks, game mode) and all assertions run on the server thread against the
server world.

| Check | Asserts |
|---|---|
| `client.world_joined` | A fresh superflat world was created and joined (fails at once on a failure screen) |
| `client.mod_data_pack_compatible` | The mod's data pack is enabled and not flagged incompatible (Forge/NeoForge, pack_format; Fabric serves mod data through one combined pack) |
| `client.radial_menu_opens` | The radial key opens `RadialMenu`, it renders (its hit-test highlights LINE under the mouse), a click selects LINE, releasing the key closes it. Screenshot `radial_menu` |
| `client.buildmode_line_preview` | After the first right click and turning to the end point, the preview holds exactly the 5 expected positions, all valid. Screenshot `line_preview` |
| `client.place_line` | The second click places those 5 stone (server world), nothing around them, creative inventory unchanged. Screenshot `line_placed` |
| `client.break_line` | Two left clicks break the line again (creative mass break) |
| `client.mirror_modifier` | "Add Mirror" in the modifier screen adds a mirror; a 3 block line places 6 blocks (line + mirror image). Screenshots `modifiers_screen`, `mirror_placed` |
| `client.place_line_survival` | Survival (power level 3 via `/powerlevel`): a 5 block line consumes exactly 5 planks |
| `client.undo_redo` | Undo removes the 5 blocks and gives the planks back (mined with the axe), redo restores them and charges them again |
| `client.randomizer_bag_screens` | For each of the 4 bags (randomizer, golden, diamond, omega): sneak + use (looking at the sky) opens its screen class; mouse clicks pick up the stone, drop one into bag slot 0 and put the rest back; Escape closes it (the server closes the menu too); the server's bag holds 1 stone and the player 63; reopening shows the stone in slot 0. Omega: the mouse wheel over slot 0 raises its weight 1 -> 2 and the Reset button sets it back to 1, both checked in the server's bag data. Screenshots `randomizer_bag`, `golden_randomizer_bag`, `diamond_randomizer_bag`, `omega_randomizer_bag`, `omega_randomizer_bag_weights` |
| `client.player_settings_gui` | `PlayerSettingsGui` opens through the mod's only entry point (`ModeOptions` action `OPEN_PLAYER_SETTINGS`; no key or radial button opens it), renders and closes on Escape. It is a stub: its button and slider are render-only and nothing is stored, so there is no setting to check. Screenshot `player_settings` |
| `client.modifier_entry_widgets` | The mod's checkbox and number widgets where a player uses them: in the modifier screen "Add Array" adds an array, a click on the entry's enable checkbox switches it off, the mouse wheel on its Count input raises 5 -> 6, the close button closes the screen, and the server stores the array with these values (`ModifierSettingsPacket`, player data `sophisticatedbuilding:buildModifiers`). Screenshot `modifier_widgets` |
| `sb.hud_count_synced` | The client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | A 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | The backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | The backpack worn in an accessory slot supplies a line: Curios `back` (NeoForge), Trinkets `chest/back` (Fabric). Skipped with the reason if no accessory mod is in the runtime |
| `sb.upgrade_settings_tab` | Using a backpack with an enabled tier 1 Building Upgrade (looking at the sky) opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (stored on the upgrade), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

### Server (`runSmokeServer`, all loaders; the `sb.*` rows on Fabric and NeoForge only)

Game tests with a fake survival player (Fabric API `FakePlayer`, NeoForge `FakePlayerFactory`, on Forge 49, which has
no fake player API, `VanillaFakePlayers`). The block sets are
written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly what
arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side |
| `server.no_mod_errors` | As on the client |

Game tests of other mods in the runtime are not checks: on Fabric the Porting Lib self test (`Tests.test`) runs too
and is only logged when it passes; if it fails, the run fails with a `server.foreign_game_test` check.

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structures/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture; SophisticatedBackpacksScreens, client only;
                                       net.p3pp3rf1y API), only for loaders with SB
<loader>/src/smoketest                 loader glue: mod metadata, entry points, fake players, accessory slots
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run.

How it drives the client: `SmokeClient.init()` starts a harness thread; `ClientDriver` submits every action to the
client thread (`Minecraft#submit`) or the integrated server thread and waits for it, and counts client ticks through
`SmokeClient.onClientTickEnd()`, so scenarios read as linear scripts. Key presses use `KeyMapping.set/click` like
`MouseHandler`; aiming sets the player's rotation. The radial menu is steered by writing its accumulated mouse offset
(it tracks the mouse as a delta from the screen centre), the hit-testing, highlighting and selection are the menu's
own. In every other screen (`GuiScenarios`) the harness moves the pointer by writing `MouseHandler`'s `xpos`/`ypos`
(what the detached cursor callback would do) and clicks, turns the wheel and presses keys by calling `MouseHandler`'s
`onPress`/`onScroll` and `KeyboardHandler#keyPress` with the window handle, the code GLFW's callbacks call, so the
screens receive `mouseClicked`/`mouseReleased`/`mouseScrolled`/`keyPressed` (loader screen events included) at real
GUI coordinates, and hover state and tooltips come from their own render pass.

Loader glue per build:

| | Fabric | NeoForge | Forge |
|---|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `META-INF/mods.toml`, `@Mod` | `META-INF/mods.toml` + `pack.mcmeta` (22/26), `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 6 runs `smokeClient` (`parent runs.client`) / `smokeServer` (`parent runs.gameTestServer`), `mods` main + harness |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` | `TickEvent.ClientTickEvent.Post` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` | `@GameTestHolder`, template `sophisticatedbuilding:smoketest_empty` |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` (Forge 49 has no fake player API) |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | Trinkets 3.8.1 + Cardinal Components 5.4.0 (smoke runtime only) | Curios 7.4.3 (smoke runtime only) | - (no SB) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke").
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.21.1 to 1.20.4, and what older versions may need:
   - Java 17: no `List#getFirst/getLast` (`get(0)`, `get(size() - 1)`).
   - Packets: no `StreamCodec`; `ServerScenarios#roundTrip` writes with the payload's `write(FriendlyByteBuf)` and
     reads with its `FriendlyByteBuf` constructor.
   - `CommonListenerCookie.createInitial(profile)` (no `transferred` flag before 1.20.5; the class itself is 1.20.2+),
     used by `VanillaFakePlayers` (Forge only). 1.20.4's `Connection#setListener` reads the protocol attribute of a
     real channel (NPE on the fake connection), so the fake connection overrides it with a no-op, as NeoForge 20.4's
     own `FakePlayer` does.
   - Forge 49 (ForgeGradle 6): the smoke runs are separate run configurations that inherit from `client` /
     `gameTestServer` (`parent`) and add the harness source set to their `mods`; ForgeGradle names their tasks
     `runSmokeClient` / `runSmokeServer` directly. Forge 49's game test server works (`forge.gameTestServer`), so no
     own test runner is needed (unlike Forge 55, see the 1.21.5 branch).
   - Game test template folder `data/<ns>/structures/` (plural before 1.21) and the NBT `DataVersion` of
     `smoketest_empty.nbt` (3700 = 1.20.4; 3955 on 1.21.1).
   - NeoForge 20.4: `META-INF/mods.toml` instead of `neoforge.mods.toml`; `TickEvent.ClientTickEvent` with a phase
     instead of `ClientTickEvent.Post`.
   - SB fixture: `BackpackWrapper.fromData(stack)` (1.21.1: `fromStack`), `new ResourceLocation(ns, path)`; the Fabric
     port's inventory only takes items through the Fabric Transfer API, so the fixture puts the contents into empty
     slots with `setStackInSlot` (both loaders have it) instead of `insertItem`.
   - Trinkets for 1.20.4 is 3.8.1 with Cardinal Components 5.4.0, group `dev.onyxstudios.cardinal-components-api`
     (renamed `org.ladysnake.cardinal-components-api` in 6.x); both from the same Mavens as on 1.21.1.
   - Unchanged and working as on 1.21.1: world creation (`WorldOpenFlows#createFreshLevel` with a screen,
     `LevelSettings`, `WorldDataConfiguration`, `WorldPresets.FLAT`), `GlobalTestReporter`/`TestReporter`,
     `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, the `RadialMenu` / `ModifiersScreen`
     field names, Fabric `FakePlayer`, NeoForge `FakePlayerFactory`, `GameTestHolder`/`PrefixGameTestTemplate`,
     Curios `getCuriosInventory` (returns `Optional` on NeoForge 20.4 too).
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.
4. Fabric: when a mod dependency (here Trinkets) is added to a build whose Loom remap cache already holds the
   Sophisticated Backpacks jar, that cached jar keeps its calls into the new dependency in intermediary names
   (`TrinketInventory.method_5439`), which fails with `NoSuchMethodError` in the dev runs (the mod logs "Could not link
   SophisticatedBackpacks' PlayerInventoryProvider.runOnBackpacks", `sb.worn_backpack` fails). Delete
   `fabric/.gradle/loom-cache/remapped_mods` (or run once with `--refresh-dependencies`); a fresh clone is not affected.

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers `pointAt`/`clickAt`/`scrollAt`/`pressKey`
in `ClientDriver`, `SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with
its SB implementation `SophisticatedBackpacksScreens` + `META-INF/services` entry in `common/src/smoketestBackpacks`).
The version-dependent calls are listed in the 1.21.1 branch's `TESTING.md` ("Porting the H5 checks"). From 1.21.1 to
1.20.4 only `List#getLast` changed (Java 17: `get(size() - 1)`) and the fixture keeps `BackpackWrapper.fromData`;
everything else is unchanged and works on all three loaders: `MouseHandler` `xpos`/`ypos`/`onPress`/`onScroll`
(Mojang names in every dev runtime of this branch, ForgeGradle 6 included), `KeyboardHandler#keyPress`,
`AbstractWidget#getX/getY`, `isHovered()`, the mod's screen and widget fields, and the Sophisticated Core GUI classes
(`StorageScreenBase`, `SettingsTabControl`, `ButtonBase`, `ToggleButton`, `WidgetBase`) on NeoForge 20.4 and the
Fabric port.

## Findings of the first runs (1.20.4)

- NeoForge: Minecraft 1.20.4 does not encode packets on the in-memory (singleplayer) connection, so the integrated
  server received the client's own `ServerPlaceBlocksPacket` object, whose `BlockSet` is the `BuilderChain`'s live
  set that the client clears on its next tick: in singleplayer no multi-block build was placed ("No blocks to place",
  only the vanilla click placed a block). `NeoForgeNetworkHelper` now sends a decoded copy of every payload; caught
  by `client.place_line` and every later placing scenario. Dedicated servers, Fabric (always encodes) and 1.20.5+
  (encodes in memory) were not affected.
- Fabric: the held-slot resend of 1.21.1 (`FabricCommonEvents`, a player holding exactly the blocks a build needs lost
  the held block client side) is ported; `sb.upgrade_supplies_blocks`, `sb.tier_cap` and `sb.worn_backpack_chest`
  pass with it.
- The Fabric SB port rescans Trinkets slots for backpacks only every 100 ticks: a backpack put into a Trinkets slot
  supplies blocks after up to 5 s (the scenarios wait for it).
- Forge: the server scenarios failed at first with an NPE in `Connection#setListener` (1.20.4 reads the channel's
  protocol attribute there) when `VanillaFakePlayers` built its fake connection; harness-only, fixed there. The
  client run passed its 10 checks at once. `client.mod_data_pack_compatible` lists `vanilla` and `mod:forge` as
  `TOO_OLD` in the Forge 49 dev runtime; the mod's own pack is compatible (pack.mcmeta `supported_formats [22, 26]`).
- Forge: the release jar is reobfuscated to SRG names (`reobfJar`); it was checked once on a production Forge 49.2.9
  server (installer `--installServer`, the jar alone in `mods/`): loaded, `/powerlevel` registered, data pack
  `mod:sophisticatedbuilding` enabled, `/reload` without errors. The smoke runs use the dev (Mojang-named) classes.
- GUI checks (H5): every screen works on all three loader builds at the first run (client 21/21 on Fabric and
  NeoForge, 13/13 on Forge); the older GUI code of this branch (`GuiGraphics` screens, the Omega weights badge and
  tooltip, the Building Upgrade settings tab on NeoForge 20.4 and the Fabric SB port) needed no fix. Server runs
  unchanged (9/9 on Fabric and NeoForge, 3/3 on Forge). Cosmetic, original code: the golden and diamond bag titles
  are wider than their GUI texture and run past its right edge; `PlayerSettingsGui` is a render-only stub.
