# Testing Sophisticated Building 1.19 - 1.19.2

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (104 tests on Fabric incl. its config tests, 90 on each Forge folder) |
| Fabric GameTests | `gradlew runGametest` | 24 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `forge` and `forge-1.19`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or
`.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. It never touches the OS mouse cursor or the focus: the window is created unfocused
  (harness mixin `SmokeWindowMixin` in every folder, GLFW hints FOCUSED/FOCUS_ON_SHOW off right before the game's
  `glfwCreateWindow`; Forge 41-43 has no FML early loading window, so the game creates its window itself as on Fabric),
  is marked inactive for the whole run so the game never grabs, hides or warps the cursor (`ClientWindow#keepOffTheCursor`:
  focus callback removed, `Minecraft#windowActive` false, Windows `WS_EX_NOACTIVATE`), and the harness moves only the
  game's own pointer (`MouseHandler#xpos/ypos`) and calls the input handlers directly. On the title screen the harness
  creates a fresh superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through the vanilla world creation flow (no quick play), runs the
  client scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players and real backpacks, writes the same result file and exits. On Forge the task starts every run on a
  fresh superflat world (it writes the game directory's `server.properties` and deletes the old world): Forge's
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
  on this branch fabric, forge and forge-1.19 do.
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
| `client.player_settings_gui` | The radial menu's player settings button (above Modifier Settings) opens `PlayerSettingsGui`; a click flips `onlyShowBlockPreviewsWhenBuilding`, a drag sets the Appear Animation slider 5 -> 20 ticks, Done closes it; the loader's client config holds both values in memory and in its file (`config/sophisticatedbuilding-client.json` on Fabric, `.toml` on NeoForge/Forge). The "Open Player Settings" key (unbound by default, bound to F7 for the test) reopens it showing the saved value, Reset to Defaults and the key again restore and save the defaults. Screenshots `radial_player_settings`, `player_settings` |
| `client.modifier_entry_widgets` | The mod's checkbox and number widgets where a player uses them: in the modifier screen "Add Array" adds an array, a click on the entry's enable checkbox switches it off, the mouse wheel on its Count input raises 5 -> 6, the close button closes the screen, and the server stores the array with these values (`ModifierSettingsPacket`, player data `sophisticatedbuilding:buildModifiers`). Screenshot `modifier_widgets` |
| `client.radial_option_icons` | Every icon the radial menu draws (15 build modes, 33 actions and options) has pixels in `textures/gui/icons.png` (read from the resource manager, cell position from `AllIcons`); Terrain Mound selected in the radial menu shows its Natural Variation and Terrain Shape option buttons (all 7 hovered, the active ones highlighted), the active shape is clicked again and the previous build mode restored. With the menu open every build mode is switched to and all side buttons (actions and that mode's options, as the menu drew them: `RadialMenu#sideButtons()`) must lie fully inside the window, clear of the ring and without overlapping each other. Screenshots `radial_terrain_options`, `radial_terrain_mountain` |
| `sb.hud_count_synced` | The client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | A 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | The backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | The backpack worn in an accessory slot supplies a line: Curios `back` (Forge), Trinkets `chest/back` (Fabric server run). Skipped with the reason if no accessory mod is in the runtime: the Fabric client run has none, see "Adopting" item 5 |
| `sb.upgrade_settings_tab` | Using a backpack with an enabled tier 1 Building Upgrade (looking at the sky) opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (stored on the upgrade), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

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
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side |
| `server.no_mod_errors` | As on the client |

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

| | Fabric | Forge |
|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `META-INF/mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG Legacy runs `smokeClient`/`smokeServer` (`loadedMods` main + harness); SB and Curios through remapping configurations (`modLocalRuntime`, `modSmoketestLocalRuntime`) |
| Client tick hook | `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`, phase `END` |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` |
| Fake player | `VanillaFakePlayers` (Fabric API 0.77 has no fake player) | `FakePlayerFactory` |
| Held key in screens | nothing | `ForgeSmokeClientPlatform` (key conflict context) |
| Accessory slot | Trinkets 3.4.2 + Cardinal Components 5.0.2 (smoke server run only) | Curios 1.19.2-5.1.6.4 (smoke runtime only) |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch (the closest older one), and the `smoketest` source set, run and
   `check` wiring from each `<loader>/build.gradle` (search for "smoke").
2. Compile (`gradlew smoketestClasses`). What was adapted from 1.21.1 via 1.20.4 and 1.20.1 to 1.19.2, and what older
   versions may need:
   - Java 17: no `List#getFirst/getLast` (`get(0)`, `get(size() - 1)`).
   - Packets: no `StreamCodec`; `ServerScenarios#roundTrip` writes with the payload's `write(FriendlyByteBuf)` and
     reads with its `FriendlyByteBuf` constructor. Before 1.20.2 there is no `CustomPacketPayload`: the payloads are
     the mod's `ModPayload`.
   - Fake players: Fabric API 0.77 (1.19.2) has no `FakePlayer`, so `common/.../server/VanillaFakePlayers` is back in
     its 1.19 form (`new ServerPlayer(server, level, profile, null)`, a `ServerGamePacketListenerImpl` whose
     `send(Packet)` drops everything; only that overload is overridden, the one with a listener has another parameter
     type in 1.19 than in 1.19.1+). Forge keeps `FakePlayerFactory`.
   - `GameTestHelper` has no `assertTrue` in 1.19.2: `ServerScenarios` (and the Fabric game tests' `GameTestSupport`)
     have their own that throws `GameTestAssertException`.
   - World creation: `WorldOpenFlows#createFreshLevel(name, settings, registryAccess, worldGenSettings)` with
     `RegistryAccess.builtinCopy().freeze()`, the flat preset's `createWorldGenSettings(seed, false, false)` and
     `DataPackConfig.DEFAULT` in `LevelSettings` (the demo world of 1.19.2's title screen does the same).
   - Muting: `Options#setSoundCategoryVolume` (no `OptionInstance` for sound sources before 1.19.3). Widgets have public
     `x`/`y` fields (`getX()` is 1.19.3+).
   - `ModErrorLogCapture`'s appender uses the `AbstractAppender(name, filter, layout, ignoreExceptions)` constructor:
     the Forge dev compile classpath (1.19.2 as 1.20.1) has a log4j-core without `Property.EMPTY_ARRAY` and the
     properties constructor.
   - Game test template folder `data/<ns>/structures/` (plural before 1.21) and the NBT `DataVersion` of
     `smoketest_empty.nbt` (3120 = 1.19.2; 3465 on 1.20.1, 3700 on 1.20.4, 3955 on 1.21.1). The 1.19 and 1.19.1 runs
     load the 3120 template without complaint.
   - Forge / MDG Legacy: `META-INF/mods.toml` with `mandatory=` dependencies, a `pack.mcmeta` (pack format 9 with
     `forge:resource_pack_format` 9 and `forge:data_pack_format` 10 on 1.19.x), `TickEvent.ClientTickEvent` with a
     phase, the no-argument `@Mod` constructor; mod dependencies are SRG-named and go through MDG Legacy's remapping
     configurations; Curios 5.1 returns a capability `LazyOptional` from
     `CuriosApi.getCuriosHelper().getCuriosHandler` (`resolve()` to an `Optional`).
   - SB fixture: the wrapper lookup differs per loader (`BackpackWrapperLookup.get(stack)` on the Fabric port, the
     `CapabilityBackpackWrapper` capability on Forge; both `LazyOptional`s), so the fixture resolves it by
     reflection; `new ResourceLocation(ns, path)`; the Fabric port's inventory only takes items through the Fabric
     Transfer API, so the fixture puts the contents into empty slots with `setStackInSlot` (both loaders have it)
     instead of `insertItem`. The same fixture compiles and passes against Sophisticated Backpacks 1.19-3.18.9 /
     Core 1.19-0.4.10 (`forge-1.19`).
   - Trinkets for 1.19.2 is 3.4.2 (only on the Modrinth maven: `maven.modrinth:trinkets:3.4.2`) with Cardinal
     Components 5.0.2, group `dev.onyxstudios.cardinal-components-api`.
   - Unchanged and working as on 1.20.1: `GlobalTestReporter`/`TestReporter`, `Screenshot.takeScreenshot`,
     `KeyMapping.set/click`, `Minecraft#submit`, the `RadialMenu` / `ModifiersScreen` field names, Forge
     `FakePlayerFactory`, `GameTestHolder`/`PrefixGameTestTemplate`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.
4. Fabric: when a mod dependency (here Trinkets) is added to a build whose Loom remap cache already holds the
   Sophisticated Backpacks jar, that cached jar keeps its calls into the new dependency in intermediary names
   (`TrinketInventory.method_5439`), which fails with `NoSuchMethodError` in the dev runs (the mod logs "Could not link
   SophisticatedBackpacks' PlayerInventoryProvider.runOnBackpacks", `sb.worn_backpack` fails). Delete
   `fabric/.gradle/loom-cache/remapped_mods` (or run once with `--refresh-dependencies`); a fresh clone is not affected.
5. Fabric: Trinkets 3.4.2 (like 3.7.2 of 1.20.1) was built with Loom 0.11, and its client mixin `ClickableWidgetMixin`
   shadows `AbstractWidget`'s field by its Yarn name (`hovered`), which the Mojang-mapped dev client cannot resolve
   (seen on 1.20.1: the client crashes at startup; players run the intermediary jar and are not affected).
   `fabric/build.gradle` therefore leaves Trinkets and Cardinal Components out of the `runSmokeClient` classpath; the
   client reports `sb.worn_backpack` as skipped, `runSmokeServer` (no client mixins) runs it with Trinkets.
6. Fabric: the dev Minecraft jar gets the transitive access wideners of every mod dependency, here Porting Lib's
   (nested in Sophisticated Core). Code that needs such a widening compiles and runs in the dev runs but crashes for
   players without that mod. Check once with a build that has only Minecraft and Fabric API (no SB) on its classpath;
   that is how `RenderType.create` was found (see below).

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers `pointAt`/`clickAt`/`scrollAt`/`pressKey`
in `ClientDriver`, `SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with
its SB implementation `SophisticatedBackpacksScreens` + `META-INF/services` entry in `common/src/smoketestBackpacks`).
The version-dependent calls are listed in the 1.21.1 branch's `TESTING.md` ("Porting the H5 checks"). From 1.20.1 to
1.19.2:

- `AbstractWidget` has public `x`/`y` fields instead of `getX()`/`getY()` (before 1.19.4); `isHovered()` is used on the
  Count input's own class (`AbstractSimiWidget`, which declares it), vanilla 1.19.2 widgets only have
  `isHoveredOrFocused()`.
- `SettingsTabControl#getOpenTab` is public in Sophisticated Core 0.6 (1.19.2) but protected in Core 0.4 (1.19 and
  1.19.1, `forge-1.19`): `SophisticatedBackpacksScreens` calls it by reflection, so one source serves all three folders.
- Unchanged: `MouseHandler` `xpos`/`ypos`/`onPress`/`onScroll` (Mojang names in every dev runtime of this branch),
  `KeyboardHandler#keyPress`, the mod's screen and widget fields, and the other Sophisticated Core GUI classes
  (`StorageScreenBase`, `ButtonBase`, `ToggleButton`, `WidgetBase`) on Forge 1.19.2, Forge 1.19 and the Fabric port.

Since R2 (player settings editor, bag title fit) the two checks also use:

- `client.player_settings_gui`: `RadialMenuDriver#hoverButton` (the button positions
  the menu drew, `RadialMenu#sideButtons()`), `ClientDriver#dragTo` (press and release through `MouseHandler`,
  the move itself through `Screen#mouseMoved`/`mouseDragged`, because `MouseHandler#handleAccumulatedMovement` only
  forwards moves for the focused window and the harness window never has focus), `PlayerSettingsGui#settingEntries()`
  with `SettingEntry#key/widget()` and `NumberEntry#values` (`SliderValues`), the fields `doneButton`/`resetButton`,
  `AbstractSliderButton` track geometry (`x + 4 .. x + width - 4`), `KeyMapping#setKey` + `KeyMapping.resetMapping()` to
  bind the unbound key for the test, `ClientConfig` values, and the client config file in `<game dir>/config`:
  `sophisticatedbuilding-client.json` (Fabric, section `Visuals`) or `sophisticatedbuilding-client.toml` (NeoForge/Forge).
- `client.randomizer_bag_screens`: `BagTitle.fit`/`availableWidth`/`isHovered` and `TitleFit#drawnWidth` (the screens'
  own layout), `AbstractContainerScreen#imageWidth` (reflection), `Screen#getTitle`, and the anvil name of an item:
  `ItemStack#setHoverName` (1.20.5+: `ItemStack#set(DataComponents.CUSTOM_NAME, ...)`).

## Findings of the first runs (1.19.2)

- Fabric: `RenderType.create` (with the sort flags) and `RenderType.CompositeState` are private/protected in vanilla
  1.19.2 and Fabric API 0.77 does not widen them (its 1.20+ builds do). In the dev runs only Porting Lib's transitive
  access widener (nested in Sophisticated Core) did, so every smoke run with SB passed, while the release jar in a
  1.19.2 runtime without SB crashed with `IllegalAccessError` at `OutlineRenderTypes.<clinit>` on the first world
  render (found in the Minecraft 1.19 check below). Fixed with the mod's own access widener; a compile of the Fabric
  sources (without the SB integration classes) against Minecraft + Fabric API only is clean, and the release jar
  without SB passes `runSmokeClient` 10/10 and `runSmokeServer` 3/3 on 1.19.2.
- Fabric game tests: vanilla's `PlayerList#placeNewPlayer` needs the profile cache, which the 1.19.2 game test server
  does not have (every test failed with "gameProfileCache is null"): `GameTestSupport` adds its player to the level
  instead.
- The Forge client reports the data packs of Sophisticated Backpacks and Core 1.19.x as `TOO_OLD` (their pack format);
  they load anyway, the mod's own pack is compatible.
- GUI checks (H5): every screen works on all three loader folders at the first run (client 21/21 on `forge` and
  `forge-1.19` (Forge 41.1.0 / Minecraft 1.19 with SB 1.19), 21/21 on `fabric` with `sb.worn_backpack` skipped as
  before); the 1.19.2 GUI code of this branch (`PoseStack` screens, the Omega weights badge and tooltip, the Building
  Upgrade settings tab on Forge SB 1.19.2, SB 1.19 and the Fabric SB port) needed no fix. Server runs unchanged (9/9 in
  every folder). Cosmetic, original code: the leather, golden and diamond bag titles are wider than their GUI texture
  and run past its right edge; `PlayerSettingsGui` is a render-only stub.



- R2 (ported from mc/1.21.1 fb10ef2..766d18f): player settings editor (radial button + unbound key), config set/save
  in every folder (Forge 43 and Forge 41 `ForgeConfigSpec.ConfigValue#set`/`getDefault` and `ForgeConfigSpec#save()`
  exist), bag title fit and bag name, the six unused widget classes deleted, Terrain Mound option icons (1.21.1's
  `icons.png`, same atlas layout), radial side button layout, the dead mini preview method deleted, and the gameplay
  fixes (merge-undo refund, failed placements not charged, full item count charged, stuck undo stack, Disable + Quick
  Replace preview). Harness: the smoke client never touches the OS cursor or the focus (`ClientWindow#keepOffTheCursor`,
  `SmokeWindowMixin` in every folder, registered with `--mixin.config` in both MDG Legacy runs; Forge 41-43 has no FML
  early window, so the Forge mixin targets the game's `GLFW.glfwCreateWindow` like the Fabric one and no `fml.toml` is
  written). Version differences to 1.21.1: `PlayerSettingsGui` renders with `PoseStack` (the branch's `GuiGraphics`
  shim for the list background), its buttons use the `Button` constructor, and its tooltips are drawn by the screen
  (no `Tooltip` class before 1.19.3; the row under the pointer, label or control, reports its tooltip); the list uses
  the 6-arg constructor, `x0/y0/x1/y1` and the translucent background of the modifier settings list; widgets have `x`/`y`
  fields; the bag screens keep their `PoseStack` methods and call `BagTitle` through the shim; `LabeledScrollInput`
  draws its label with the `PoseStack` `Label#render`; `ChargeGameTest` uses `GRASS` and `ServerPlayer#setLevel`,
  `GameTestSupport.assertTrue` instead of `GameTestHelper#assertTrue` (1.19.3+); `MergeUndoGameTest` has no pink
  petals (1.19.4+); the harness round-trips packets through their `FriendlyByteBuf` constructors, names the bag with
  `ItemStack#setHoverName` and reads slider/row positions from the `x`/`y` fields; harness mixin configs `JAVA_17`.
  Bug found on the way (Fabric, fixed): `PlayerSettingsGui`'s rows read `Screen#font`, which Porting Lib's transitive
  access widener (nested in the Fabric Sophisticated Core port) makes public in the dev runtime, so javac compiled a
  direct field access instead of an accessor, which a player without Sophisticated Backpacks would get as
  `IllegalAccessError`; the rows now use a private `rowFont()`. Checked by compiling the Fabric sources against
  Minecraft + Fabric API only (`local/h5d-nosb-1.19.2`, git-ignored) and comparing the bytecode of every class with
  the dev build: identical (342 classes). Verified: `gradlew build` Fabric 104 tests, `forge` and `forge-1.19` 90 (also
  with `CI=true`, tests rerun without cache); Fabric `runGametest` "All 24 required tests passed"; `runSmokeServer`
  Fabric 11 (1 skip: `server.refused_place_not_charged`, no place event), `forge` 11/11, `forge-1.19` 11/11 (Forge
  41.1.0). `runSmokeClient` pending (no game clients until the lead allows them).

## Minecraft 1.19 and 1.19.1

The release jars were run in dev runtimes of the older versions (scratch builds under `local/pb3-119x`, git-ignored:
the smoke harness compiled against that version, the release jar on the runtime classpath instead of the sources, the
Sophisticated Backpacks builds of that version where they exist):

| Jar | Runtime | runSmokeServer | runSmokeClient |
|---|---|---|---|
| `forge-1.19` | Forge 41.1.0 (1.19), SB 1.19-3.18.9.661 + Core 1.19-0.4.10.87, Curios | 9/9 incl. 6 `sb.*` | 17/17 incl. 7 `sb.*` |
| `forge-1.19` | Forge 42.0.9 (1.19.1), same SB/Core/Curios | 9/9 incl. 6 `sb.*` | - |
| `forge` (1.19.2), ranges widened | Forge 42.0.9 (1.19.1), SB 1.19 | 4/9: the Building Upgrade items cannot be created against Core 0.4.10 (`NoClassDefFoundError: IUpgradeCountLimitConfig`), 5 `sb.*` fail | - |
| `fabric` | Fabric API 0.58.0+1.19 (1.19), no SB (no Fabric port) | 3/3 | 10/10 |
| `fabric` | Fabric API 0.58.5+1.19.1 (1.19.1), no SB | 3/3 | - |
| `fabric` | Fabric API 0.77.0+1.19.2 (1.19.2), no SB | 3/3 | 10/10 |

Hence the two Forge jars and one Fabric jar. The Fabric jar needed two changes for 1.19 and 1.19.1: the dependency on
the mod id `fabric` (the Fabric API builds for those versions are not `fabric-api` yet) and the access widener above
(the 1.19 client crashed without it). Both Fabric client runs used jars whose classes are identical to the release
jar except two unused `GuiGraphics` scissor methods and one language key that were removed/added afterwards (their
metadata differed only in the Minecraft and Fabric API lines); the server runs used the final release jar. The Forge
1.19 real server (installer 41.1.0, only the `forge-1.19` jar and SB/Core 1.19 in `mods`) and the Forge 1.19.2 real
server (installer 43.5.2, only the `forge` jar and SB/Core 1.19.2) reach "Done" with the backpack integration
registered and no error from the mod.

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
