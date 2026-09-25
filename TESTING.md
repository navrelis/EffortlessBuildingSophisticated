# Testing Sophisticated Building 1.19.4

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (117 tests on Fabric incl. its config tests, 103 on Forge) |
| Fabric GameTests | `gradlew runGametest` | 38 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, on Fabric including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or
`.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. It never touches the OS mouse cursor or the focus: the window is created unfocused
  (harness mixin `SmokeWindowMixin` on both loaders, GLFW hints FOCUSED/FOCUS_ON_SHOW off right before the window is
  created; on Forge 45 the smoke client task first writes `splashscreen = false` into the run's `config/fml.toml`, so
  FML's early splash window, which is created before any mod code and takes the focus, is not used and the game
  creates its window itself through `EarlyProgressVisualization#handOffWindow`), is marked inactive for the whole run
  so the game never grabs, hides or warps the cursor (`ClientWindow#keepOffTheCursor`: focus callback removed,
  `Minecraft#windowActive` false, Windows `WS_EX_NOACTIVATE`), and the harness moves only the game's own pointer
  (`MouseHandler#xpos/ypos`) and calls the input handlers directly. On the title screen the harness creates a fresh
  superflat world with a unique name (`sb-smoketest-<time>`, older ones are deleted) through the vanilla world creation
  flow (no quick play), runs the client scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players (and real backpacks on Fabric), writes the same result file and exits. On Forge the task starts
  every run on a fresh superflat world (it writes the game directory's `server.properties` and deletes the old world): Forge's
  game test server (found on 1.20.1) takes its world from `server.properties`, and with the default normal terrain
  the test structures at y -60 sat in caves, where falling gravel could fill them (one flaky `sb.worn_backpack` in
  the first runs).

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
  on 1.19.4 that is Fabric only. Forge (no SB for 1.19.4) runs no `sb.*` checks and does not compile against SB.
- The file is rewritten after every check (atomically), so a crash or a kill still leaves the checks done so far.

## Scenarios

### Client (`runSmokeClient`, every loader folder)

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
| `client.modifier_entry_widgets` | The mod's checkbox and number widgets where a player uses them: in the modifier screen "Add Array" adds an array, a click on the entry's enable checkbox switches it off, the mouse wheel on its Count input raises 5 -> 6, the close button closes the screen, and the server stores the array with these values (`ModifierSettingsPacket`, player data `sophisticatedbuilding:buildModifiers`). Screenshot `modifier_widgets` |
| `client.radial_option_icons` | Every icon the radial menu draws (15 build modes, 33 actions and options) has pixels in `textures/gui/icons.png` (read from the resource manager, cell position from `AllIcons`); Terrain Mound selected in the radial menu shows its Natural Variation and Terrain Shape option buttons (all 7 hovered, the active ones highlighted), the active shape is clicked again and the previous build mode restored. With the menu open every build mode is switched to and all side buttons (actions and that mode's options, as the menu drew them: `RadialMenu#sideButtons()`) must lie fully inside the window, clear of the ring and without overlapping each other. Screenshots `radial_terrain_options`, `radial_terrain_mountain` |
| `sb.hud_count_synced` | The client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | A 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | The backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | The backpack worn in the Trinkets `chest/back` slot supplies a line. Skipped with the reason if no accessory mod is in the runtime |
| `sb.upgrade_settings_tab` | Using a backpack with an enabled tier 1 Building Upgrade (looking at the sky) opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (stored on the upgrade), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 24 checks (8 `sb.*`), Forge 16 (the `client.*` ones).

### Server (`runSmokeServer`, every loader folder)

Game tests with a fake survival player (Fabric API `FakePlayer`, Forge `FakePlayerFactory`). The block sets are
written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly what
arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `server.merge_undo_refund` | Survival merges (+1 snow layer, +1 candle) cost one item each; undo puts both blocks back without mining and gives the items back, redo charges them again |
| `server.refused_place_not_charged` | The loader's block place event refuses 2 of a 5 block line (as a protection mod would): only the 3 placed planks are charged and undo gives back exactly those. Skipped on Fabric (no place event; `ChargeGameTest` covers refused placements) |
| `server.request_limits` | A survival player's build requests over the power level limits (start 60 blocks away, a 20 block extent over the 8 blocks per axis) are refused, nothing placed or charged; a normal 5 block line after them is placed |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side |
| `server.no_mod_errors` | As on the client |

Fabric reports 12 checks (6 `sb.*`; `server.refused_place_not_charged` skipped), Forge 6.

Game tests of other mods in the runtime are not checks: they are only logged when they pass; if one fails, the run
fails with a `server.foreign_game_test` check (on this branch no other mod in the dev runtime registers one).

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structures/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture; SophisticatedBackpacksScreens, client only;
                                       net.p3pp3rf1y API), only for loaders with SB (Fabric)
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

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `META-INF/mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`); SB and Trinkets through `modLocalRuntime` / `modSmoketestRuntimeOnly` | MDG Legacy runs `smokeClient`/`smokeServer` (`loadedMods` main + harness); no SB fixture |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` (no `sb_` tests) |
| Fake player | Fabric API `FakePlayer` (0.87) | `FakePlayerFactory` |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | Trinkets 3.6.0 + Cardinal Components 5.2.0 (smoke runtime only) | none (no SB) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke"). A loader without SB drops the
   `smoketestBackpacks` source directories, its `SmokeAccessorySlots` service and the `sb_` game tests (see the Forge
   folder of this branch).
2. Compile (`gradlew smoketestClasses`). What was adapted from `mc/1.19.2` to 1.19.4 (see the `TESTING.md` of
   `mc/1.19.2` for the steps from 1.21.1 down to 1.19.2); with it the harness is the same as on `mc/1.20.1`:
   - World creation: `WorldOpenFlows#createFreshLevel(name, settings, WorldOptions, registries -> flat preset's
     createWorldDimensions())` with `WorldDataConfiguration.DEFAULT` in `LevelSettings` (1.19.3+).
   - Muting through `Options#getSoundSourceOptionInstance(MASTER).set(0.0)`; widgets have `getX()`/`getY()`
     (1.19.3+); `GameTestHelper#assertTrue` exists (the harness' own copy is gone); `new ServerPlayer(server, level,
     profile)` without a profile key.
   - Fake players: Fabric API 0.87 has `FakePlayer`, so the Fabric server platform uses it (as on 1.20.1) and
     `VanillaFakePlayers` is gone. Forge keeps `FakePlayerFactory`.
   - SB fixture: the Fabric port for 1.19.4 returns the wrapper from `BackpackWrapperLookup.get(stack)` as a plain
     `Optional` (the other ports return a Porting Lib `LazyOptional` with `resolve()`); the fixture accepts both.
     `SettingsTabControl#getOpenTab` is public in Core 0.5.109.
   - Forge: `pack.mcmeta` pack format 13 with `forge:resource_pack_format` 13 and `forge:data_pack_format` 12, the
     harness mod's `loaderVersion` `[45,)`. Forge 1.19.4 has no SB, so its `ForgeSmokeServerTests` has no `sb_` tests
     and the Forge smoke source set does not compile `common/src/smoketestBackpacks` (the first `runSmokeServer`
     failed its six `sb_` tests with "No SmokeBackpacks fixture registered").
   - The `smoketest_empty.nbt` template keeps `DataVersion` 3120 (1.19.2); 1.19.4 (3337) loads it without complaint.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.
4. Fabric: when a mod dependency (here Trinkets) is added to a build whose Loom remap cache already holds the
   Sophisticated Backpacks jar, that cached jar keeps its calls into the new dependency in intermediary names, which
   fails with `NoSuchMethodError` in the dev runs. Delete `fabric/.gradle/loom-cache/remapped_mods` (or run once with
   `--refresh-dependencies`); a fresh clone is not affected.
5. Fabric: Trinkets 3.4.2 (1.19.2) and 3.7.2 (1.20.1) were built with Loom 0.11, and their client mixin
   `ClickableWidgetMixin` shadows `AbstractWidget`'s field by its Yarn name (`hovered`), which the Mojang-mapped dev
   client cannot resolve. `fabric/build.gradle` therefore leaves Trinkets and Cardinal Components out of the
   `runSmokeClient` classpath (the client then reports `sb.worn_backpack` as skipped); `runSmokeServer` (no client
   mixins) runs it with Trinkets 3.6.0.
6. Fabric: the dev Minecraft jar gets the transitive access wideners of every mod dependency, here Porting Lib's
   (nested in Sophisticated Core). Code that needs such a widening compiles and runs in the dev runs but crashes for
   players without that mod; the mod's own access widener covers `RenderType.create` (see README).

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers in `ClientDriver`,
`SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with its SB
implementation `SophisticatedBackpacksScreens` in `common/src/smoketestBackpacks`). On 1.19.4 `GuiScenarios` and
`SophisticatedBackpacksScreens` are the `mc/1.20.1` files unchanged: widgets have `getX()`/`getY()`, and
`SettingsTabControl#getOpenTab` is public in Sophisticated Core 0.5.109.

Since R2 (player settings editor, bag title fit, radial icons and layout, mini preview, gameplay fixes) the harness is
the `mc/1.21.1` one with these 1.19.4 differences:

- `client.randomizer_bag_screens`: the anvil name of the renamed bag is set with `ItemStack#setHoverName` (no data
  components before 1.20.5).
- `server.merge_undo_refund` / `server.undo_redo`: the undo/redo packets are round-tripped with their
  `FriendlyByteBuf` constructors (`PerformUndoPacket::new`), not a `StreamCodec`.
- Forge `ForgeSmokeServerTests`: the two new tests use the template `smoketest_empty` like the others
  (`@PrefixGameTestTemplate(false)`; the 1.21.1 name `sophisticatedbuilding:smoketest_empty` became
  `sophisticatedbuilding:sophisticatedbuilding:smoketest_empty` and crashed the game test server). Its
  `refusePlacementsAt` cancels `BlockEvent.EntityPlaceEvent` (package `event.level`) on `MinecraftForge.EVENT_BUS`.
- Fabric `ChargeGameTest`: `ServerPlayer#setLevel` (1.20: `setServerLevel`) and `Blocks.GRASS`/`Items.GRASS` (renamed
  `SHORT_GRASS` in 1.20.3). `MergeUndoGameTest` keeps pink petals (`FLOWER_AMOUNT` exists on 1.19.4).
- Focus: the Fabric `SmokeWindowMixin` targets `glfwCreateWindow` in `Window.<init>` as on 1.21.1. Forge 45 has no
  `ImmediateWindowHandler`: its `Window.<init>` gets the window from `EarlyProgressVisualization#handOffWindow`, which
  creates it itself when the splash screen is off; the Forge mixin injects before that call and the smoke client task
  writes `splashscreen = false` (Forge 45's key; `earlyWindowControl` from Forge 47.1 on). The harness mixin configs
  use `compatibilityLevel` `JAVA_17`; Forge registers the config with `--mixin.config=...` on the smoke client run.

## Findings of the first runs (1.19.4)

- Main code: compiled against 1.19.4 after the 1.19.3/1.19.4 API changes (JOML, `BuiltInRegistries`/`Registries`,
  `BlockPos.containing`, `NbtUtils.readBlockState(HolderGetter, tag)`, `Renderable`, the `Button` builder,
  `AbstractWidget#renderWidget(PoseStack, ...)` and its private `x`/`y`, `updateWidgetNarration`, `Font.DisplayMode`,
  `ItemDisplayContext`, the builder-made creative tab, `MenuType` feature flags, item rendering with a `PoseStack`).
  Features 1.19.4 has again, like 1.20+: survival breaking also recognises tools by the item tags (`pickaxes`, `axes`,
  `shovels`, `hoes`), pink petals count in `COUNT_PROPERTIES`, and hanging signs have their buffer.
- Fabric: Sophisticated Core 0.5.109 (the 1.19.4 port) predates upgrade groups and count limits; the Building Upgrade
  keeps "one per backpack" itself (as `forge-1.19` on `mc/1.19.2`). All `sb.*` server checks pass, including the Tool
  Swapper and the Trinkets slot.
- Forge: without SB the Forge build loses its integration classes (`integration/*`, `item/upgrade/*`,
  `gui/BuildingUpgradeContainer`, `client/gui/BuildingUpgradeSettingsTab`, `compatibility/CuriosCompatHelper`), the
  `IBackpackIntegration` service file, the SB/Core/Curios dependencies and `mods.toml` entries.

- R2: every GUI and gameplay change of `mc/1.21.1` (fb10ef2..766d18f) is in; the six unused widget classes were
  deleted (all six still existed here). Conflicts only where the 1.19.4 GUI uses `PoseStack` callbacks and the
  `GuiGraphics` shim (bag screens, `LabeledScrollInput`, `PlayerSettingsGui`) and in `ServerBlockPlacer` (`player.level`
  field). `PlayerSettingsGui` on 1.19.4: `render(PoseStack, ...)` draws the dimmed world first, the list is the 6
  argument `ContainerObjectSelectionList` with `setRenderBackground(false)`/`setRenderTopAndBottom(false)` and its own
  translucent background (like `ModifiersScreenList`), rows render with a `PoseStack`; `Button.builder`, `Tooltip`,
  `setTooltipForNextRenderPass(Tooltip, DefaultTooltipPositioner, true)` and `AbstractSliderButton` are the same as on
  1.21.1. Forge 45's `ForgeConfigSpec.ConfigValue#set/getDefault` and `ForgeConfigSpec#save()` exist.

- R3 (5.0.1 fixes of `mc/1.21.1` d8ab383..48261e8): Fabric per-player data saved with the player (`PlayerDataMixin`,
  `ServerPlayerEvents.COPY_FROM`), server checks of build requests with the common config synced to the client
  (`CommonConfigSyncPacket` as a `ModPayload` with `writeVarIntArray`/`readVarIntArray`), array limit, Fabric break
  events and Common Protection API 1.0.0, offhand bag filter, material cost list, previous build mode, translatable
  texts (`LangKeysTest`). 1.19.4 differences: the start reach uses the vanilla reach 5 (creative) / 4.5 (no
  `Player#blockInteractionRange` before 1.20.5); `PowerLevel#serializeNBT()` without a registry provider; item tooltips
  keep the `Level` signature; the GameTests build their players without `ClientInformation` and use
  `helper.assertTrue(!...)` (no `assertFalse`). Found on the way: the new Fabric mixin config was copied with
  `compatibilityLevel` `JAVA_21`, which the Java 17 runtime refuses (every Fabric start failed; now `JAVA_17`); the
  smoke players are moved with the five-argument `moveTo`, because `ServerPlayer#moveTo(x, y, z)` teleports through the
  connection and Forge 45's fake player ignores that (all Forge line scenarios were refused as out of reach); the new
  Forge test uses the template `smoketest_empty`. `scripts/check-fabric-no-sb-bytecode.ps1 -Mc 1.19.4`: 350 classes
  compared, 0 differ.

### Results (5.0.1 fixes, 2026-09-25)

| Folder | `gradlew build` (unit tests) | Game tests | `runSmokeServer` | `runSmokeServer -PsmokeNoSb=true` | `runSmokeClient` |
|---|---|---|---|---|---|
| `fabric/` | 117/117 | 38/38 | 12 (11 passed, 1 skipped; 6 `sb.*`) | 12 (5 passed, 7 skipped) | pending (no clients) |
| `forge/` | 103/103 | - | 6/6 | 6/6 | pending (no clients) |

### Results of the port and R2 (2026-09-25)

| Folder | Runtime | `gradlew build` (unit tests) | Game tests | `runSmokeServer` | `runSmokeClient` |
|---|---|---|---|---|---|
| `fabric/` | Minecraft 1.19.4, Fabric Loader 0.19.5, Fabric API 0.87.2+1.19.4, SB 3.19.5 build 105 + Core 0.5.109 build 105, Trinkets 3.6.0 | 104/104 | 24/24 | 11 (10 passed, 1 skipped; 6 `sb.*`) | pending (client runs on hold) |
| `forge/` | Forge 45.4.5 | 90/90 | - | 5/5 | pending (client runs on hold) |

`gradlew build` also passes with `CI=true` on `forge/` (same test counts), and a fresh copy of the branch builds with
`build-all.ps1`. Real server: Fabric 1.19.4 with the release jar, the Fabric API 0.87.2+1.19.4 modules and SB/Core
build 105 (Java 17): "Registered Sophisticated Backpacks upgrade containers", "Done", no `ERROR` or exception from the
mod ("No data fixer registered for" comes from SB, "No key layers ..." from vanilla's flat world in
`server.properties`).

## Standalone run without Sophisticated Backpacks

`gradlew runSmokeServer -PsmokeNoSb=true --no-daemon` (loader folders with the Sophisticated Backpacks integration)
proves the mod works without Sophisticated Backpacks and Sophisticated Core: `gradle/smoketest.gradle` drops every
dependency of the `localRuntime`, `modLocalRuntime`, `smoketestLocalRuntime`, `modSmoketestLocalRuntime` and
`modSmoketestRuntimeOnly` configurations (Sophisticated Backpacks and Core, their Fabric port libraries, Curios,
Trinkets), leaves the backpack fixture (every file under a `smoketestBackpacks` folder) out of the smoke source set
and passes `-Dsophisticatedbuilding.smoketest.noSb=true` to the run.
The `sb.*` scenarios report "skipped" (and fail instead if the backpack integration is active anyway).
`server.place_line_survival`, `server.undo_redo`, `server.merge_undo_refund`, `server.refused_place_not_charged`
(skipped on Fabric: no place event there) and `server.no_mod_errors` must pass. The main code still compiles against
Sophisticated Backpacks (compile-only), so only the runtime changes. On the hub,
`scripts/test-all-versions.ps1 -SmokeTasks runSmokeServerNoSb` runs it for every loader folder with the integration.
