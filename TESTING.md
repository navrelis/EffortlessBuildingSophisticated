# Testing Sophisticated Building 1.16.5 (Fabric also 1.16.4; Forge 1.16.4 in forge-1.16.4/)

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (104 tests on Fabric incl. its config tests, 90 on `forge/` and `forge-1.16.4/`) |
| Fabric server tests | `gradlew runGametest` | 24 server-side building rules (`fabric/src/gametest`, the game tests of the other branches) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, on Forge including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness and the server tests (so they cannot rot) but never runs them. The harness is
dev-only: it lives in its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Server tests instead of game tests

Minecraft 1.16.5 ships its game test framework (`net.minecraft.gametest.framework`) stripped: `GameTestHelper` has no
methods, `TestFunction` no public constructor, there is no `@GameTest`, no `GlobalTestReporter` and no game test
server; Fabric API 0.42 has no game test API and Forge 36 none either. The harness therefore brings a small
replacement in `common/src/smoketest/java/sophisticated/building/smoketest/servertest`:

- `ServerTestHelper`: the part of vanilla's `GameTestHelper` the tests use, with the same method names
  (`absolutePos`, `getLevel`, `getBlockState`, `getBlockEntity`, `setBlock`, `assertBlockPresent`, `startSequence`,
  `succeed`, `fail`);
- `ServerTestSequence`: `thenWaitUntil`, `thenIdle`, `thenExecute`, `thenSucceed` with vanilla's semantics (a step that
  throws `ServerTestAssertException` is retried on the next tick until the timeout, any other exception fails the test);
- `@ServerTest(batch, timeoutTicks, required)` for test methods;
- `ServerTestRunner`: runs the tests one after the other (vanilla runs a batch in parallel) in one area 32 blocks from
  the world spawn, which it empties before every test (air from the test's origin layer up, stone below it, entities
  other than players removed), and reports every result to a listener. The loader glue starts it once the server has
  started and ticks it after every server tick.

Both server runs use a plain dedicated dev server on a fresh superflat world: the task deletes the old world and writes
`server.properties` (`level-type=flat`, its own port: Forge smoke 25731, Fabric smoke 25732, Fabric server tests 25733,
`forge-1.16.4` smoke 25735, so they can run next to other dev servers) and `eula.txt` (dev-only server). Minecraft 1.16.5 cannot read flat
`generator-settings` from `server.properties` (it logs `ERROR ... Not a registry ops`, a vanilla bug, not the mod's) and
uses the default superflat layers.

`runGametest` (Fabric) runs every `@ServerTest` of the classes listed under the `sophisticatedbuilding-servertest`
entrypoint of `fabric/src/gametest/resources/fabric.mod.json` (`FabricServerTests`), writes
`fabric/build/gametest/junit.xml`, logs `All 24 required tests passed :)` and stops the server; the task prints every
test and fails unless all passed. The test names are `<class>.<method>` in lower case, as vanilla names game tests.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `forge` and `forge-1.16.4`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or `.../server`
(from PowerShell, `gradlew.bat` splits `-P` arguments at `=`: quote the whole argument or set
`ORG_GRADLE_PROJECT_smoketestOut` instead).

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. It never touches the OS mouse cursor or the focus: the window is created unfocused
  (harness mixin `SmokeWindowMixin`, GLFW hints FOCUSED/FOCUS_ON_SHOW off right before the window is created: on
  Fabric before vanilla's `glfwCreateWindow`, on Forge 35/36 before `EarlyProgressVisualization.handOffWindow`; the
  Forge smoke client runs with `-Dfml.earlyprogresswindow=false` - Forge 1.16 has no `fml.toml` switch - so FML's early
  progress window, which is created before any mod code and takes the focus, is not used and the game creates its
  window itself; the mixin config is passed with `--mixin.config`), is marked inactive for the whole run so the game
  never grabs, hides or warps the cursor (`ClientWindow#keepOffTheCursor`: focus callback removed,
  `Minecraft#windowActive` false, Windows `WS_EX_NOACTIVATE`), and the harness moves only the game's own pointer
  (`MouseHandler#xpos/ypos`) and calls the input handlers directly. On the title screen the harness creates a fresh
  superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through `Minecraft#createLevel` (no quick play), runs the client
  scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`.
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
  on 1.16.5 that is Forge only. Fabric (no SB for 1.16.5) runs no `sb.*` checks and does not compile against SB.
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
| `client.mini_block_preview` | On that preview: a mini block preview (small ghost) in each of the 5 positions by default, none with `showMiniBlockPreview` off, none with `maxMiniBlockPreviews` 4 (below the 5 blocks). Screenshots `mini_preview_on`, `mini_preview_off` |
| `client.place_line` | The second click places those 5 stone (server world), nothing around them, creative inventory unchanged. Screenshot `line_placed` |
| `client.break_line` | Two left clicks break the line again (creative mass break) |
| `client.mirror_modifier` | "Add Mirror" in the modifier screen adds a mirror; a 3 block line places 6 blocks (line + mirror image). Screenshots `modifiers_screen`, `mirror_placed` |
| `client.disable_quick_replace_preview` | Disable mode, looking at a stone block with planks in hand: no outline of the mod without Quick Replace (vanilla places the block), with Quick Replace the preview of the replaced block and its outline are shown. Screenshots `disable_plain`, `disable_quick_replace_preview` |
| `client.place_line_survival` | Survival (power level 3 via `/powerlevel`): a 5 block line consumes exactly 5 planks |
| `client.undo_redo` | Undo removes the 5 blocks and gives the planks back (mined with the axe), redo restores them and charges them again |
| `client.randomizer_bag_screens` | For each of the 4 bags (randomizer, golden, diamond, omega): sneak + use (looking at the sky) opens its screen class; mouse clicks pick up the stone, drop one into bag slot 0 and put the rest back; Escape closes it (the server closes the menu too); the server's bag holds 1 stone and the player 63; reopening shows the stone in slot 0. Omega: the mouse wheel over slot 0 raises its weight 1 -> 2 and the Reset button sets it back to 1, both checked in the server's bag data. Screenshots `randomizer_bag`, `golden_randomizer_bag`, `diamond_randomizer_bag`, `omega_randomizer_bag`, `omega_randomizer_bag_weights`. Every bag title fits its texture (`BagTitle`/`TitleFit`: scaled, at most to 0.6, then cut with "..."); a bag renamed in an anvil to a 59 character name shows that name, cut, and the full name as tooltip on hover. Screenshot `renamed_bag_title` |
| `client.player_settings_gui` | The radial menu's player settings button (above Modifier Settings) opens `PlayerSettingsGui`; a click flips `onlyShowBlockPreviewsWhenBuilding`, a drag sets the Appear Animation slider 5 -> 20 ticks, Done closes it; the loader's client config holds both values in memory and in its file (`config/sophisticatedbuilding-client.json` on Fabric, `.toml` on Forge). The "Open Player Settings" key (unbound by default, bound to F7 for the test) reopens it showing the saved value, Reset to Defaults and the key again restore and save the defaults. Screenshots `radial_player_settings`, `player_settings` |
| `client.modifier_entry_widgets` | In the modifier screen "Add Array" adds an array, a click on the entry's enable checkbox switches it off, the mouse wheel on its Count input raises 5 -> 6, the close button closes the screen, and the server stores the array with these values (`ModifierSettingsPacket`, player data `sophisticatedbuilding:buildModifiers`). Screenshot `modifier_widgets` |
| `client.radial_option_icons` | Every icon the radial menu draws (build modes, actions and options) has pixels in `textures/gui/icons.png` (read from the resource manager, cell position from `AllIcons`); Terrain Mound selected in the radial menu shows its Natural Variation and Terrain Shape option buttons (all 7 hovered, the active ones highlighted), the active shape is clicked again and the previous build mode restored. With the menu open every build mode is switched to and all side buttons (`RadialMenu#sideButtons()`) must lie fully inside the window, clear of the ring and without overlapping each other. Screenshots `radial_terrain_options`, `radial_terrain_mountain` |
| `sb.hud_count_synced` | Forge: the client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Forge: holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | Forge: a 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Forge: upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Forge: survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | Forge: the backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | Forge: the backpack worn in the Curios `back` slot supplies a line |
| `sb.upgrade_settings_tab` | Forge: using a backpack with an enabled tier 1 Building Upgrade opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server, and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 16 checks (the `client.*` ones), `forge/` and `forge-1.16.4/` 24 each.

### Server (`runSmokeServer`, both loaders)

Server tests with a fake survival player (Forge 36 `FakePlayerFactory`; on Fabric, whose API 0.42 has no fake player,
and on Forge 35 (`forge-1.16.4/`), whose fake player has no connection, `VanillaFakePlayers`: a vanilla `ServerPlayer`
outside the player list whose connection drops every packet). The block
sets are written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly
what arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `server.merge_undo_refund` | Survival merges (+1 snow layer, +1 sea pickle; 1.16 has no candles) cost one item each; undo puts both blocks back without mining and gives the items back, redo charges them again |
| `server.refused_place_not_charged` | The loader's block place event refuses 2 of a 5 block line (as a protection mod would): only the 3 placed planks are charged and undo gives back exactly those. Skipped on Fabric (no place event; `ChargeGameTest` covers refused placements) |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | Forge only (where the SB fixture exists): as on the client, server side |
| `server.no_mod_errors` | As on the client |

Fabric reports 5 checks (1 skipped), `forge/` and `forge-1.16.4/` 11 each.

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
                                       net.p3pp3rf1y API), only for loaders with SB (Forge)
<loader>/src/smoketest                 loader glue: mod metadata, entry points, fake players, accessory slots
fabric/src/gametest                    the 17 server tests of the building rules and their runner glue (FabricServerTests)
forge-1.16.4/src                       overrides of ../forge/src and ../common/src/smoketestBackpacks for Forge 35 and SB 1.16.4
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run. The Fabric server tests start only
with `sophisticatedbuilding.gametest.report` (set by `runGametest`).

Loader glue per build:

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`, depends on `fabric` (Fabric API's mod id on 1.16.5) | `META-INF/mods.toml` (`loaderVersion` `[36,)`) + `pack.mcmeta` (format 6), `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`); no SB fixture | Architectury Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) with their own FML `MOD_CLASSES` (main + harness, resources first); SB through `modLocalRuntime`, Curios through `modSmoketestRuntimeOnly` |
| Server scenarios | `ServerLifecycleEvents.SERVER_STARTED` -> `SmokeServer.start`, `ServerTickEvents.END_SERVER_TICK` -> `SmokeServer.tick` | `FMLServerStartedEvent` -> `SmokeServer.start`, `TickEvent.ServerTickEvent` (END) -> `SmokeServer.tick` |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Fake player | `VanillaFakePlayers` (Fabric API 0.42 has none) | `FakePlayerFactory` (`forge-1.16.4/`: `VanillaFakePlayers`, Forge 35 fake players have no connection) |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | none (no SB) | Curios 1.16.5-4.1.0.0 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke"). A loader without SB drops the
   `smoketestBackpacks` source directories and its `SmokeAccessorySlots` service; `SmokeServer` only runs the `sb_`
   scenarios where the SB fixture is registered.
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.17.1 to 1.16.5, and what older versions may need:
   - Java 8: no records, switch expressions, `instanceof` patterns, `var`, `List.of`, `Optional#isEmpty`,
     `ServiceLoader#findFirst/stream` (`SmokeTest.firstService/allServices`), `Stream#toList`, `Files#writeString`.
   - No game test framework: the server scenarios and the Fabric game tests run on the `servertest` runner above (a
     port to a version with game tests can keep it or go back to vanilla's; the helper has vanilla's method names).
   - Client-only members are stripped from the dedicated server on both loaders (`@Environment(CLIENT)` /
     `@OnlyIn(Dist.CLIENT)`): `BlockPos#toShortString`, `Block#getName`, `Ingredient#getItems`,
     `FriendlyByteBuf#readUtf()` (no-argument) and `Mth#equal` are client only in 1.16.5, so server code must not call
     them (`ServerTestHelper.shortString`).
   - Entity/player accessors are fields: `player.inventory`, `player.abilities`, `entity.yRot/xRot`, `entity.removed`;
     `Slot#getContainerSlot` is the private field `slot` (reflection), the carried stack is `Inventory#getCarried`,
     widgets have public `x`/`y`, a screen's render-only widgets are `Screen#buttons` (no `renderables`),
     `Entity#getEyePosition(float)`, no `Mth.RAD_TO_DEG`, `GameRules.IntegerValue#tryDeserialize` (no `set`),
     `Screenshot.takeScreenshot(width, height, target)`, no `Block.UPDATE_*` constants (3 = update all, 2 = clients).
   - `VanillaFakePlayers`: `new ServerPlayer(server, level, profile, new ServerPlayerGameMode(level))`.
   - Forge 1.16.5 through Architectury Loom: mod classes per run come from FML's `MOD_CLASSES` (a mod's resource
     directory must come first, FML reads `META-INF/mods.toml` from it); server lifecycle events are
     `net.minecraftforge.fml.event.server.FMLServer*Event`; Curios 4 returns a capability `LazyOptional`.
   - SB fixture: Sophisticated Backpacks 1.16.5 has no Sophisticated Core; the GUI classes are
     `net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen`, `...controls.BackpackWidget` (Core's
     `WidgetBase`), `ButtonBase`, `ToggleButton`, and `SettingsTabControl#getOpenTab` is protected (reflection).
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

## Findings of the first runs (1.16.5)

- Server scenarios and Fabric server tests passed from the first run of the `servertest` runner (Forge 9 checks incl.
  6 `sb.*`, Fabric 3 checks, 17/17 server tests). The runner now also logs the stack trace of an unexpected exception
  that fails a test (it only reported `NullPointerException` before, which hid the Forge 35 fake player problem below).
- Forge dev client: every `runClient`/`runSmokeClient` of Forge 36.2.42 crashed on the loading screen with
  `NoSuchMethodError: com.mojang.math.Transformation.func_227987_b_()` in vanilla `BlockMath` (model baking). Forge 36.2
  moved `Transformation#compose`/`inverse` into default methods of `IForgeTransformationMatrix`; Architectury Loom renames
  those but leaves the two calls in `BlockMath` under their SRG names (its SRG-patched jar: the class keeps only private
  `composeVanilla`/`inverseVanilla`). A real Forge runtime is SRG-named on both sides and not affected. `forge/build.gradle`
  (`fixDevMinecraft`) puts a copy of Loom's named Minecraft jar with `BlockMath` calling `compose`/`inverse` first on the
  client runs' classpath. Forge 35 (`forge-1.16.4/`) keeps the methods and needs no fix.
- GUI checks (H5), the first runtime test of the 1.16.5 GUI shims (`GuiGraphics` over `GuiComponent`/`PoseStack`, the
  Create-style screens' own renderable list, `Screen#buttons` for the player settings screen, the SB settings tab): all
  pass without a change in `src/main`, on both Forge folders and on Fabric.
- No bug in `src/main` was found by these runs; every fix above is in the build or the harness.
- Screenshots (854x480, GUI scale auto): ghost blocks with the white outline and the "5 blocks (5x1x1)" HUD in
  `line_preview`; the radial menu with the build modes, the LINE segment highlighted and the option buttons on its left in
  `radial_menu`; the Omega bag with the stone in slot 0 and the "Weight: 2 / Chance: 100,0% / Scroll to adjust" tooltip
  and the Reset button in `omega_randomizer_bag_weights`; the Array entry with its checkbox off, the Count 6 and its
  tooltip in `modifier_widgets`; on Forge the SB backpack screen with the Building Upgrade tab open and its toggle showing
  "Disabled" in `sb_upgrade_settings_tab` (the backpack screen is larger than the 854x480 window, so SB draws it cut off
  at the top, SB's own layout). The randomizer bag titles are wider than their texture ("Sophisticated Leather
  Randomizer Bag"), as in the original code.

### Results before R2 (2026-09-25)

| Folder | Runtime | `gradlew build` (unit tests) | Server tests | `runSmokeServer` | `runSmokeClient` |
|---|---|---|---|---|---|
| `fabric/` | Minecraft 1.16.5, Fabric Loader 0.19.5, Fabric API 0.42.0+1.16 | 77/77 | 17/17 | 3/3 | 13/13 (H5: 3) |
| `fabric/` on 1.16.4 | Minecraft 1.16.4, same loader and Fabric API | 77/77 | 17/17 | 3/3 | 13/13 (H5: 3) |
| `forge/` | Forge 36.2.42, SB 1.16.5-3.15.20.755, Curios 1.16.5-4.1.0.0 | 65/65 | - | 9/9 (6 `sb.*`) | 21/21 (8 `sb.*`, H5: 4) |
| `forge-1.16.4/` | Forge 35.1.37, SB 1.16.4-3.0.0.289, Curios 1.16.5-4.1.0.0 | 65/65 | - | 9/9 (6 `sb.*`) | 21/21 (8 `sb.*`, H5: 4) |

`gradlew build` also passes with `CI=true` on all three folders (same test counts). Real servers with the release jars
(Java 8u504): Forge 1.16.5 (installer 36.2.42, SB 1.16.5-3.15.20.755, Curios): "Registered Sophisticated Backpacks
upgrade containers", "Done"; Forge 1.16.4 (installer 35.1.37, `forge-1.16.4` jar, SB 1.16.4-3.0.0.289, Curios): the same;
Fabric 1.16.5 and 1.16.4 (Fabric Loader 0.19.5, Fabric API 0.42.0+1.16): "Done". No `ERROR` or exception from the mod.
Errors from others: "No key layers ... Not a registry ops" (vanilla, flat world from `server.properties`) and, on Forge
1.16.4, "No data fixer registered for" while Sophisticated Backpacks 1.16.4 registers its entity types.

### R2 (player settings editor, bag titles, radial menu, gameplay fixes; port of mc/1.21.1 fb10ef2..766d18f)

- 1.16.5 specifics of the port: Java 8 (the new pure classes `SliderValues`, `TitleFit`, `RadialButtonLayout` and
  `RadialMenu.SideButton` are final classes instead of records); `PlayerSettingsGui` built from the 1.16 widgets (see
  README, "Player settings"); Forge 35/36 `ForgeConfigSpec.ConfigValue` has `set` and `save` but no `getDefault` (the
  adapter keeps the default); no candles and no pink petals, so the merge tests and the merge smoke scenario use snow
  layers, slabs, sea pickles and turtle eggs, and the charge test three sea pickles; `Blocks.GRASS` (1.20.3+ short grass),
  `Entity#setLevel` (1.20+ `setServerLevel`); `ItemStack#setHoverName`, `ResourceManager#getResource` and Gson 2.8.0 in the
  harness; Forge 1.16 place event `net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent`.
- Headless results (2026-09-25): unit tests Fabric 104, `forge/` and `forge-1.16.4/` 90 (0 failures, also with
  `CI=true`); Fabric `runGametest` "All 24 required tests passed"; `runSmokeServer` Fabric 5 checks (4 passed, 1
  skipped: `server.refused_place_not_charged`, no place event), `forge/` 11/11 and `forge-1.16.4/` 11/11 (6 `sb.*`
  each).
- Clients (`runSmokeClient`, 16 / 24 checks) not run yet: the lead holds them until the cursor-safe harness is on every
  branch.

## Minecraft 1.16.4 check

| Loader | Runtime | Result |
|---|---|---|
| Fabric | Minecraft 1.16.4, Fabric Loader 0.19.5, Fabric API 0.42.0+1.16 (declares `~1.16.2`) | The Fabric build compiled against 1.16.4 (a scratch worktree with `minecraft_version=1.16.4`): 77 unit tests, 17/17 server tests, `runSmokeServer` 3/3, `runSmokeClient` 13/13. The 1.16.5 release jar references 969 intermediary names, all present in 1.16.4's `intermediary-v2.tiny`, and starts on a real Fabric 1.16.4 server ("Done", no errors) |
| Forge | Real Forge 1.16.4 server (installer 35.1.37), SB 1.16.4-3.0.0.289 (the last SB for 1.16.4), Curios 1.16.5-4.1.0.0 | The 1.16.5 jar does not load as is (Minecraft `[1.16.5]`, javafml 36, Forge 36.2.42). With its `mods.toml` widened for the test (javafml `[35,)`, Forge `[35.1.37,)`, Minecraft `[1.16.4,1.16.5]`, SB `[1.16.4-3.0.0,)`) the server reaches "Done" and registers the upgrade containers, but compiling `forge/` against SB 1.16.4-3.0.0.289 fails in `BackpackScanCompat` (`runOnBackpacks` is static there, its consumer takes no slot identifier), `BuildingUpgradeHelper` and `ToolSwapperIntegration` (no `ToolSwapMode`): the jar's backpack integration is not binary compatible with that build |

Hence `minecraft_version_range=[1.16.4,1.16.5]` (Fabric `>=1.16.4 <=1.16.5`, Fabric API floor 0.42.0 unchanged) and
Forge `forge_minecraft_version_range=[1.16.5]`; Minecraft 1.16.4 gets its own Forge jar from `forge-1.16.4/` (below).
A dedicated Forge 1.16.4 server does not start at all on Java 8u321 or later (Forge 35's modlauncher 8.0.9:
`NoSuchMethodError: sun.security.util.ManifestEntryVerifier.<init>`), with or without this mod; the real-server test
replaced `libraries/cpw/mods/modlauncher/8.0.9/modlauncher-8.0.9.jar` with modlauncher 8.1.3.

### Forge 1.16.4 (`forge-1.16.4/`)

`forge-1.16.4/` builds `sophisticatedbuilding-forge-1.16.4-5.0.0.jar` (Minecraft `[1.16.4]`, Forge `[35.1.37,)`,
optional Sophisticated Backpacks `[1.16.4-3.0.0.289,1.16.5)`) from `../forge` with the classes Sophisticated Backpacks
1.16.4 and Forge 35 need replaced (README.md, "Forge 1.16.4"). Its smoke runs use the same scenarios and check names as
`forge/`, including every `sb.*` check (Forge 35.1.37, SB 1.16.4-3.0.0.289, Curios 1.16.5-4.1.0.0):

- `runSmokeServer`: 9 checks passed (6 `sb.*`). The first run failed every scenario with a `NullPointerException` in
  `ServerPlayer#setGameMode`: Forge 35's `FakePlayer` has no connection. `forge-1.16.4/src/smoketest` overrides
  `ForgeSmokeServerPlatform` with the vanilla fake player of the Fabric builds.
- `runSmokeClient`: 21 checks passed (8 `sb.*`, incl. `sb.upgrade_settings_tab` against SB 1.16.4's `Widget` classes);
  screenshots as on `forge/`.
- The dev runs force modlauncher 8.1.3 (see above; Java 8u504 from the Foojay toolchain).

## Standalone run without Sophisticated Backpacks

`gradlew runSmokeServer -PsmokeNoSb=true --no-daemon` (loader folders with the Sophisticated Backpacks integration)
proves the mod works without Sophisticated Backpacks and Sophisticated Core: `gradle/smoketest.gradle` drops every
dependency of the `localRuntime`, `modLocalRuntime`, `smoketestLocalRuntime`, `modSmoketestLocalRuntime` and
`modSmoketestRuntimeOnly` configurations (Sophisticated Backpacks and Core, their Fabric port libraries, Curios,
Trinkets), leaves the backpack fixture (every file under a `smoketestBackpacks` folder) out of the smoke source set
and passes `-Dsophisticatedbuilding.smoketest.noSb=true` to the run.
The `sb.*` scenarios do not run: `SmokeServer` registers them only when the backpack fixture is present (a registered one would report "skipped", or fail if the backpack integration were active anyway).
`server.place_line_survival`, `server.undo_redo`, `server.merge_undo_refund`, `server.refused_place_not_charged`
(skipped on Fabric: no place event there) and `server.no_mod_errors` must pass. The main code still compiles against
Sophisticated Backpacks (compile-only), so only the runtime changes. On the hub,
`scripts/test-all-versions.ps1 -SmokeTasks runSmokeServerNoSb` runs it for every loader folder with the integration.
