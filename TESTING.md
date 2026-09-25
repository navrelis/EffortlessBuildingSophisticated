# Testing Sophisticated Building 1.17.1

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (117 tests on Fabric incl. its config tests, 103 on Forge) |
| Fabric GameTests | `gradlew runGametest` | 37 server-side building rules (`fabric/src/gametest`) |
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
  window cannot disturb a run. It never touches the OS mouse cursor or the focus: the window is created unfocused
  (harness mixin `SmokeWindowMixin` on both loaders, GLFW hints FOCUSED/FOCUS_ON_SHOW off right before the window is
  created: before `glfwCreateWindow` in `Window.<init>` on Fabric, before FML's
  `EarlyProgressVisualization#handOffWindow` on Forge, which has no early loading window on this Minecraft version, so
  no `fml.toml` setting is needed), is marked inactive for the whole run so the game never grabs, hides or warps the
  cursor (`ClientWindow#keepOffTheCursor`: focus callback removed, `Minecraft#windowActive` false, Windows
  `WS_EX_NOACTIVATE`), and the harness moves only the game's own pointer (`MouseHandler#xpos/ypos`) and calls the input
  handlers directly. On the title screen the harness creates a fresh superflat world with a unique name
  (`sb-smoketest-<time>`, older ones are deleted) through `Minecraft#createLevel` (no quick play), runs the client
  scenarios, writes the result and stops the game. It takes about a minute after the game has loaded. The game
  directory is `<loader>/build/smoketest/client-run`; putting `soundCategory_master:0.0` and `pauseOnLostFocus:false`
  into its `options.txt` beforehand also silences the title screen before the harness mutes the game.
- **runSmokeServer** is headless (no GPU needed, for CI) and runs the server scenarios as game tests with fake
  survival players (and real backpacks on Forge), writes the same result file and exits.
  - Fabric: Fabric API's game test server.
  - Forge: Forge 1.17.1 has no game test server launch target (it arrives with Forge 38 for 1.18), so the task starts
    a plain dedicated dev server and the harness runs the scenarios itself once the server has started
    (`ForgeSmokeServerTests`: one vanilla `TestFunction` per scenario, each in its own `GameTestBatch`, started with
    `GameTestRunner.runTestBatches` next to the world spawn and ticked through `GameTestTicker`, as the vanilla game test
    server does), then stops the server. Every run starts on a fresh superflat world: the task deletes the old world
    and writes `server.properties` (`level-type=flat`, its own `server-port=25731` so it can run next to other dev
    servers) and `eula.txt` (dev-only server) into `forge/build/smoketest/server-run`. Minecraft 1.17.1 cannot read
    flat `generator-settings` from `server.properties` (it logs `ERROR ... Not a registry ops`, a vanilla bug, not the
    mod's) and uses the default superflat layers.

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
| `sb.hud_count_synced` | Forge: the client caches (`ClientBuildingUpgradeState`, `ClientBackpackItemCache` via `BuildingUpgradeStatePacket` / `BackpackItemCountPacket`) show tier 1 / 32 blocks and the backpack's 64 stone |
| `sb.upgrade_supplies_blocks` | Forge: holding 1 stone with a tier 1 Building Upgrade backpack: a 5 block line is placed from the backpack (64 -> 59), the held stone stays, the HUD count follows |
| `sb.tier_cap` | Forge: a 6x6 floor (36) in survival: the preview shows 32 valid / 4 invalid and exactly 32 are placed, all from the backpack (tier 1 cap = 32) |
| `sb.disabled_upgrade_ignored` | Forge: upgrade disabled, holding 3 stone: only 3 of a 5 block line are placed, the backpack is untouched |
| `sb.tool_swapper_tools` | Forge: survival mass break of 5 stone with a stick in hand uses the diamond pickaxe from a Tool Swapper backpack (damage 5, cobblestone in the inventory); the client first learns the tool through `BackpackToolsPacket` |
| `sb.worn_backpack_chest` | Forge: the backpack worn in the chest armor slot supplies a line |
| `sb.worn_backpack` | Forge: the backpack worn in the Curios `back` slot supplies a line |
| `sb.upgrade_settings_tab` | Forge: using a backpack with an enabled tier 1 Building Upgrade (looking at the sky) opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (stored on the upgrade), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

Fabric reports 16 checks (the `client.*` ones), Forge 24.

### Server (`runSmokeServer`, both loaders)

Game tests with a fake survival player (Forge `FakePlayerFactory`; on Fabric, whose API 0.46 has no fake player,
`VanillaFakePlayers`: a vanilla `ServerPlayer` outside the player list whose connection drops every packet). The block
sets are written with the packets' `write` methods and read back with their `FriendlyByteBuf` constructors, exactly
what arrives from a client, and handed to the packets' server handlers.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `server.merge_undo_refund` | Survival merges (+1 snow layer, +1 candle) cost one item each; undo puts both blocks back without mining and gives the items back, redo charges them again |
| `server.refused_place_not_charged` | The loader's block place event refuses 2 of a 5 block line (as a protection mod would): only the 3 placed planks are charged and undo gives back exactly those. Skipped on Fabric (no place event; `ChargeGameTest` covers refused placements) |
| `server.request_limits` | A survival player's build requests over the power level limits (start 60 blocks away, a 20 block extent over the 8 blocks per axis) are refused, nothing placed or charged; a normal 5 block line after them is placed |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | Forge only: as on the client, server side |
| `server.no_mod_errors` | As on the client |

Fabric reports 6 checks (1 of them skipped: `server.refused_place_not_charged`), Forge 12.
Game tests of other mods in the runtime are not checks: they are only logged when they pass; if one fails, the run
fails with a `server.foreign_game_test` check (on 1.17.1 no other mod in the dev runtime registers one; the Forge smoke
server only runs the harness's own tests).

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeWindowHints,
           SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structures/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture; SophisticatedBackpacksScreens, client only;
                                       net.p3pp3rf1y API), only for loaders with SB (Forge)
<loader>/src/smoketest                 loader glue: mod metadata, entry points, fake players, accessory slots,
                                       SmokeWindowMixin (window created without the focus)
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

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers `pointAt`/`clickAt`/`scrollAt`/`pressKey`
in `ClientDriver`, `SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with
its SB implementation `SophisticatedBackpacksScreens` + `META-INF/services` entry in `common/src/smoketestBackpacks`).
The loader-neutral part is the one of 1.18.x (no `getX()`/`getY()` on vanilla widgets before 1.19.4: the public
fields `x`/`y`; the Count input's hover test through the mod's `LabeledScrollInput`; `get(size() - 1)`; records and
pattern matching compile on Java 16). What 1.17.1 needed on top:

- Sophisticated Backpacks 1.17.1 has no Sophisticated Core: `SophisticatedBackpacksScreens` uses `BackpackScreen`,
  `BackpackWidget`, `SettingsTabControl`/`ButtonBase`/`ToggleButton` under
  `net.p3pp3rf1y.sophisticatedbackpacks.client.gui`, and reads the protected `SettingsTabControl#getOpenTab()` by
  reflection (as `forge-1.18/` on mc/1.18.1).
- Unchanged: `MouseHandler` `xpos`/`ypos`/`onPress(long, int, int, int)`/`onScroll(long, double, double)`,
  `KeyboardHandler#keyPress`, `Screen#renderables` (private, read by reflection), `AbstractContainerScreen`
  `leftPos`/`topPos`, the mod's screen and widget field names, the Omega screen's `Reset` button.

### Porting the R2 checks (player settings, bag titles, radial icons, gameplay fixes, cursor-safe client)

R2 of mc/1.21.1 (`fb10ef2..766d18f`) adds `client.mini_block_preview`, `client.disable_quick_replace_preview`,
`client.radial_option_icons`, `server.merge_undo_refund` and `server.refused_place_not_charged`, rewrites
`client.player_settings_gui` (radial button, drag on a slider, config file probe, key) and extends
`client.randomizer_bag_screens` (title fit, renamed bag). Ported via mc/1.18.2 and mc/1.18.1 (same common code); what
1.17.1 needed on top of the H5 list above:

- As on 1.18.x: renamed bag through `ItemStack#setHoverName(new TextComponent(...))`, the icon atlas through
  `ResourceManager#getResource(...).getInputStream()`, slider and row positions from the public `x`/`y` fields, the
  undo/redo packets round-tripped with their `FriendlyByteBuf` constructors, `refusePlacementsAt` on Forge through
  `net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent` (Fabric has no place event, the check is skipped there),
  Fabric GameTests with `ServerPlayer#setLevel`, `Blocks.GRASS`/`Items.GRASS`, no pink petals and
  `GameTestSupport#assertTrue`.
- Gson of Minecraft 1.17.1 has no static `JsonParser.parseString`: the config file probe uses
  `new JsonParser().parse(...)`.
- Forge 37 has no game test server: `ForgeSmokeServerTests` (the harness's own runner on the dedicated server) lists the
  two new server scenarios in its `scenarios()` map.
- Cursor/focus (`SmokeWindowMixin`): Fabric injects before `GLFW.glfwCreateWindow` in `Window.<init>` like 1.21.1;
  Forge 37 creates the window through `EarlyProgressVisualization#handOffWindow` (its early progress window is
  hard-wired off in FML 37, so no `fml.toml` change), and the mixin config is passed with
  `--mixin.config=sophisticatedbuilding_smoketest.mixins.json` in the `smokeClient` run. The harness mixin configs use
  compatibility level `JAVA_16`.

## Findings of the first runs (1.17.1)

- Fabric: Fabric API 0.46.1+1.17 still has the mod id `fabric` (not `fabric-api`): with `"fabric-api"` in `depends`
  Fabric Loader refused to start ("requires version 0.46.1 or later of fabric-api, which is missing"). The mod and the
  harness depend on `fabric`.
- Fabric GameTests: all 17 failed with the 1.18.2 profile cache `NullPointerException` until `GameTestSupport` took over
  the 1.18.2 fix (test player added to the level, not the player list).
- Forge: no game test server in Forge 37; `runSmokeServer` runs the scenarios on a dedicated dev server (see above).
- All 17 client checks (7 `sb.*`) and 9 server checks (6 `sb.*`) pass on Forge 37.1.1 with Sophisticated Backpacks
  1.17.1-3.12.3.496 and Curios 1.17.1-5.0.2.7; Fabric passes its 10 client and 3 server checks.
- GUI checks (H5): the first runtime test of the 1.17.1 screens (the randomizer bag screens and the modifier entry
  widgets draw through the mod's `GuiGraphics` shim over `GuiComponent`/`PoseStack`): all pass without a change in
  `src/main`. Forge 21 client checks (8 `sb.*`), Fabric 13; the server runs are unchanged (Forge 9, Fabric 3). The SB
  1.17.1 backpack screen is taller than the 854x480 smoke window; its top rows are cut off in `sb_upgrade_settings_tab`.
  Checked once in the same runs with a temporary check (not part of the harness): the mod's creative tab lists all 16
  mod items, the 5 Building Upgrades included (Forge: through the `allowdedIn`/`getCreativeTabs` override of
  `BuildingUpgradeItem`; Fabric: the placeholder items).
- R2 (player settings editor, bag titles, dead widgets, Terrain Mound icons, radial layout, gameplay fixes, cursor-safe
  smoke client), ported from mc/1.21.1 via mc/1.18.x: `gradlew build` Fabric 104 unit tests, Forge 90; Fabric
  `runGametest` "All 24 required tests passed"; `runSmokeServer` Forge 11/11 (6 `sb.*`), Fabric 5 (4 passed,
  `server.refused_place_not_charged` skipped: no place event). Differences to 1.21.1: Forge 37's
  `ForgeConfigSpec.ConfigValue` has no `getDefault()` (the adapter keeps the defined default); no pink petals (1.20+)
  among the merges; the Player Settings screen draws its own background and tooltips (see the README).
- R3 (5.0.1 fixes of `mc/1.21.1` d8ab383..48261e8), ported via mc/1.18.x (same common code): Fabric per-player data
  saved with the player (`PlayerDataMixin`, `ServerPlayerEvents.COPY_FROM`), server checks of build requests with the
  common config synced to the client (`CommonConfigSyncPacket` as a `ModPayload` with `writeVarIntArray`/
  `readVarIntArray`), array limit, Fabric break events, offhand bag filter, material cost list, previous build mode,
  translatable texts (`LangKeysTest`). Differences as on 1.18.x: `new TranslatableComponent(...)`, the start reach uses
  the vanilla reach 5 (creative) / 4.5, `PowerLevel#serializeNBT()` without a registry provider, tooltips with the
  `Level` signature, the Omega bag's Reset button is `new Button(...)`, GameTests without `ClientInformation`. On 1.17.1
  also: no Common Protection API (its only 1.x release needs Java 17; Minecraft 1.17.1 runs on Java 16), so claim mods
  are reached through Fabric API's player break events only, and `ProtectionEventsGameTest` has only its break-event
  test (37 GameTests instead of 38); the Fabric mixin config uses `JAVA_16`; the Forge smoke runner lists
  `server_request_limits` in its `scenarios()` map. Found on the way, as on 1.18.x: Forge 37's `FakePlayer` answers
  `position()`/`blockPosition()` with the origin, so the server's reach check takes the player's coordinates
  (`getX/getY/getZ`) and the smoke players are moved with the five-argument `moveTo`.

## Standalone run without Sophisticated Backpacks

`gradlew runSmokeServer -PsmokeNoSb=true --no-daemon` (loader folders with the Sophisticated Backpacks integration)
proves the mod works without Sophisticated Backpacks and Sophisticated Core: `gradle/smoketest.gradle` drops every
dependency of the `localRuntime`, `modLocalRuntime`, `smoketestLocalRuntime`, `modSmoketestLocalRuntime` and
`modSmoketestRuntimeOnly` configurations (Sophisticated Backpacks and Core, their Fabric port libraries, Curios,
Trinkets), leaves the backpack fixture (every file under a `smoketestBackpacks` folder) out of the smoke source set
and passes `-Dsophisticatedbuilding.smoketest.noSb=true` to the run.
The `sb.*` scenarios report "skipped" (and fail instead if the backpack integration is active anyway).
`server.place_line_survival`, `server.undo_redo`, `server.merge_undo_refund`, `server.refused_place_not_charged`
(skipped on Fabric: no place event there), `server.request_limits` and `server.no_mod_errors` must pass. The main code still compiles against
Sophisticated Backpacks (compile-only), so only the runtime changes. On the hub,
`scripts/test-all-versions.ps1 -SmokeTasks runSmokeServerNoSb` runs it for every loader folder with the integration.
