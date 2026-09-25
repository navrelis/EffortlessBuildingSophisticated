# Testing Sophisticated Building 1.16.3

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on Forge) |
| Fabric server tests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`, the game tests of the other branches) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, on Forge including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness and the server tests (so they cannot rot) but never runs them. The harness is
dev-only: it lives in its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Server tests instead of game tests

Minecraft 1.16.3 ships its game test framework (`net.minecraft.gametest.framework`) stripped (no `@GameTest`, no game
test server; it arrives with 1.17), Fabric API 0.25 has no game test API and Forge 34 none either. The harness
therefore brings a small replacement in `common/src/smoketest/java/sophisticated/building/smoketest/servertest`:

- `ServerTestHelper`: the part of vanilla's `GameTestHelper` the tests use, with the same method names
  (`absolutePos`, `getLevel`, `getBlockState`, `getBlockEntity`, `setBlock`, `assertBlockPresent`, `startSequence`,
  `succeed`, `fail`);
- `ServerTestSequence`: `thenWaitUntil`, `thenIdle`, `thenExecute`, `thenSucceed` with vanilla's semantics (a step that
  throws `ServerTestAssertException` is retried on the next tick until the timeout, any other exception fails the test);
- `@ServerTest(batch, timeoutTicks, required)` for test methods;
- `ServerTestRunner`: runs the tests one after the other in one area 32 blocks from the world spawn, which it empties
  before every test, and reports every result to a listener. The loader glue starts it once the server has started and
  ticks it after every server tick.

Both server runs use a plain dedicated dev server on a fresh superflat world: the task deletes the old world and writes
`server.properties` (`level-type=flat`, its own port: Forge smoke 25737, Fabric smoke 25738, Fabric server tests 25739,
so they can run next to other dev servers, including the 25731-25735 of `mc/1.16.5`) and `eula.txt` (dev-only server).
Minecraft 1.16.3 cannot read flat `generator-settings` from `server.properties` (it logs `ERROR ... Not a registry ops`,
a vanilla bug, not the mod's) and uses the default superflat layers.

`runGametest` (Fabric) runs every `@ServerTest` of the classes listed under the `sophisticatedbuilding-servertest`
entrypoint of `fabric/src/gametest/resources/fabric.mod.json` (`FabricServerTests`), writes
`fabric/build/gametest/junit.xml`, logs `All 17 required tests passed :)` and stops the server; the task prints every
test and fails unless all passed.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or `.../server`
(from PowerShell, `gradlew.bat` splits `-P` arguments at `=`: quote the whole argument or set
`ORG_GRADLE_PROJECT_smoketestOut` instead).

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. On the title screen the harness creates a fresh superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through `Minecraft#createLevel` (no quick play), runs the client
  scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI) and runs the server scenarios as server tests with fake
  survival players (and real backpacks on Forge) on the dedicated dev server described above (`SmokeServer`), writes
  the same result file and stops the server.

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
  on 1.16.3 that is Forge only. Fabric (no SB for 1.16.3) runs no `sb.*` checks and does not compile against SB.
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
| `client.randomizer_bag_screens` | For each of the 4 bags (randomizer, golden, diamond, omega): sneak + use (looking at the sky) opens its screen class; mouse clicks pick up the stone, drop one into bag slot 0 and put the rest back; Escape closes it (the server closes the menu too); the server's bag holds 1 stone and the player 63; reopening shows the stone in slot 0. Omega: the mouse wheel over slot 0 raises its weight 1 -> 2 and the Reset button sets it back to 1, both checked in the server's bag data. Screenshots `randomizer_bag`, `golden_randomizer_bag`, `diamond_randomizer_bag`, `omega_randomizer_bag`, `omega_randomizer_bag_weights` |
| `client.player_settings_gui` | `PlayerSettingsGui` opens through the mod's only entry point (`ModeOptions` action `OPEN_PLAYER_SETTINGS`), renders and closes on Escape. It is a stub: its buttons and slider are render-only (`Screen#buttons` on 1.16.3) and nothing is stored. Screenshot `player_settings` |
| `client.modifier_entry_widgets` | In the modifier screen "Add Array" adds an array, a click on the entry's enable checkbox switches it off, the mouse wheel on its Count input raises 5 -> 6, the close button closes the screen, and the server stores the array with these values (`ModifierSettingsPacket`, player data `sophisticatedbuilding:buildModifiers`). Screenshot `modifier_widgets` |
| `sb.hud_count_synced` | Forge: the client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Forge: holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | Forge: a 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Forge: upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | **Skipped on 1.16.3**: Sophisticated Backpacks 1.16.4-1.0.0.94 has no Tool Swapper upgrade (the fixture's `whyNoToolSwapper()`). On newer branches, Forge: survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack |
| `sb.worn_backpack_chest` | Forge: the backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | Forge: the backpack worn in the Curios `back` slot supplies a line |
| `sb.upgrade_settings_tab` | Forge: using a backpack with an enabled tier 1 Building Upgrade opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (the upgrade's own `enabled` tag on 1.16.3), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 13 checks (the `client.*` ones), Forge 21 (20 passed, 1 skipped).

### Server (`runSmokeServer`, both loaders)

Server tests with a fake survival player: `VanillaFakePlayers` on both loaders, a vanilla `ServerPlayer` outside the
player list whose connection drops every packet (Fabric API 0.25 has no fake player; Forge 34's fake players have no
connection at all, so `setGameMode` and every packet to them throw). The block sets are written with the packets'
`write` methods and read back with their `FriendlyByteBuf` constructors, exactly what arrives from a client, and handed
to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | Forge only: as on the client, server side (`sb.tool_swapper_tools` skipped, see above) |
| `server.no_mod_errors` | As on the client |

Fabric reports 3 checks, Forge 9 (8 passed, 1 skipped).

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    servertest/ServerTestRunner, ServerTestHelper, ServerTestSequence, ServerTest, ServerTestAssertException
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture, SophisticatedBackpacksScreens,
                                       net.p3pp3rf1y API of SB 1.16.4-1.0.0.94), only for loaders with SB (Forge)
<loader>/src/smoketest                 loader glue: mod metadata, entry points, fake players, accessory slots
fabric/src/gametest                    the 17 server tests of the building rules and their runner glue (FabricServerTests)
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run. The Fabric server tests start only
with `sophisticatedbuilding.gametest.report` (set by `runGametest`).

Loader glue per build:

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`, depends on `fabric` (Fabric API's mod id on 1.16.3) | `META-INF/mods.toml` + `pack.mcmeta` (format 6), `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`); no SB fixture | Architectury Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) with their own FML `MOD_CLASSES` (main + harness, resources first); SB through `modLocalRuntime`, Curios through `modSmoketestRuntimeOnly` |
| Server scenarios | `ServerLifecycleEvents.SERVER_STARTED` -> `SmokeServer.start`, `ServerTickEvents.END_SERVER_TICK` -> `SmokeServer.tick` | `FMLServerStartedEvent` -> `SmokeServer.start`, `TickEvent.ServerTickEvent` (END) -> `SmokeServer.tick` |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Fake player | `VanillaFakePlayers` (Fabric API 0.25 has none) | `VanillaFakePlayers` (Forge 34 fake players have no connection) |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | none (no SB) | Curios 1.16.4-4.0.3.0 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke"). A loader without SB drops the
   `smoketestBackpacks` source directories and its `SmokeAccessorySlots` service; `SmokeServer` only runs the `sb_`
   scenarios where the SB fixture is registered.
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.16.5 to 1.16.3 (everything else as on `mc/1.16.5`,
   see its TESTING.md for the 1.17.1 -> 1.16.5 steps: Java 8, the server test runner, client-only members, fields
   instead of accessors):
   - SB fixture: Sophisticated Backpacks 1.16.4-1.0.0.94 keeps the wrapper, upgrade and inventory handlers in
     `net.p3pp3rf1y.sophisticatedbackpacks.util` (the wrapper comes from the stack's `BACKPACK_WRAPPER_CAPABILITY`),
     has no Tool Swapper (`sb.tool_swapper_tools` skipped) and no upgrade enable switch: the fixture reads and writes
     the Building Upgrade's own `enabled` tag in the upgrade stack.
   - SB GUI (`SophisticatedBackpacksScreens`): the widget base class is `...client.gui.controls.Widget`; the tabs sit in
     `BackpackScreen#getUpgradeControl()` (`UpgradeSettingsControl`, no `SettingsTabControl`), whose open tab is the
     private field `openTab` (reflection); a tab's icon is an `ItemButton` (a `ButtonBase`).
   - Forge 34 fake players have no connection: `ForgeSmokeServerPlatform` uses `VanillaFakePlayers`.
   - Forge 34's modlauncher 8.0.6 fails on current Java 8 builds (see README): the dev runs force modlauncher 8.1.3.
   - Not needed on Forge 34: the `fixDevMinecraft` task of `mc/1.16.5` (Forge 36.2 dev clients crashed on SRG calls in
     `BlockMath`); Forge 34's named Minecraft jar already calls `compose`/`inverse`, so this branch drops it.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

## Findings (1.16.3)

- Everything passed without a change in `src/main`; the fixes were in the harness (SB 1.0.0.94 fixture and GUI
  lookup, fake players) and the build (modlauncher 8.1.3 for dev runs, ports).
- GUI checks (H5), the first runtime test of the 1.16 GUI shims on 1.16.3 (`GuiGraphics` over `GuiComponent`/`PoseStack`
  with direct OpenGL scissor calls, the Create-style screens' own renderable list, `Screen#buttons` for the player
  settings screen, the SB 1.0.0.94 settings tab): all pass on both loaders.
- Screenshots (854x480, GUI scale auto): the radial menu identical to the 1.21.1 one (build modes ring, LINE
  highlighted, option buttons on its left, "Power Level: Creative"); ghost stone with the white outline and the
  "5 blocks (5x1x1)" HUD in `line_preview`; the Omega bag with the stone in slot 0, the "Weight: 2 / Chance: 100,0% /
  Scroll to adjust" tooltip and the Reset button in `omega_randomizer_bag_weights`; the Array entry with its checkbox
  off, Count 6 and its tooltip in `modifier_widgets`; the player settings stub ("Shader type: Dissolve Blue", "Speed:
  1,00", Done); on Forge the SB backpack screen with the Building Upgrade tab toggle showing "Disabled" in
  `sb_upgrade_settings_tab` (the backpack screen is taller than the 854x480 window, so SB draws it cut off at the
  top, SB's own layout). The randomizer bag titles are wider than their texture ("Sophisticated Leather Randomizer
  Bag"), as in the original code.

### Results (2026-09-25)

| Folder | Runtime | `gradlew build` (unit tests) | Server tests | `runSmokeServer` | `runSmokeClient` |
|---|---|---|---|---|---|
| `fabric/` | Minecraft 1.16.3, Fabric Loader 0.19.5, Fabric API 0.25.0+build.415-1.16 | 77/77 | 17/17 | 3/3 | 13/13 (H5: 3) |
| `forge/` | Forge 34.1.42 (modlauncher 8.1.3), SB 1.16.4-1.0.0.94, Curios 1.16.4-4.0.3.0 | 65/65 | - | 9 (8 passed, 1 skipped; 5 `sb.*` passed) | 21 (20 passed, 1 skipped; 7 `sb.*` passed, H5: 4) |

`gradlew build` also passes with `CI=true` on both folders (same test counts, byte-identical jars). Real servers with
the release jars: Forge 1.16.3 (installer 34.1.42, Java 8u202, SB 1.16.4-1.0.0.94; also with Curios, and without SB):
"Registered Sophisticated Backpacks upgrade containers", "Done"; Fabric 1.16.3 (Fabric Loader 0.19.5, the Fabric API
0.25.0 modules, Java 8u504): "Done". No `ERROR` or exception from the mod. Errors from others: "No key layers ... Not a
registry ops" (vanilla, flat world from `server.properties`) and "No data fixer registered for" (Sophisticated Backpacks
1.16.4-1.0.0.94; gone without SB).
