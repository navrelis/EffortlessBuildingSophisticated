# Testing Sophisticated Building 1.21.10

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (104 tests on Fabric incl. its config tests, 90 on NeoForge and Forge) |
| Fabric GameTests | `gradlew runGametest` | 24 server-side building rules (`fabric/src/gametest`; the run reports 25 with vanilla's `minecraft:always_pass`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration on NeoForge |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

On Minecraft 1.21.10 only NeoForge has Sophisticated Backpacks (official build); Fabric and Forge have none, so their
smoke runs have no `sb.*` checks and do not compile the SB fixture.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `neoforge` and `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or
`.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. It never touches the OS mouse cursor or the focus: the window is created unfocused
  (harness mixin `SmokeWindowMixin` on every loader, GLFW hints FOCUSED/FOCUS_ON_SHOW off right before the window is
  created; on NeoForge and Forge the smoke client task first writes `earlyWindowControl = false` into the run's
  `config/fml.toml`, so FML's early loading window, which is created before any mod code and takes the focus, is not
  used and the game creates its window itself), is marked inactive for the whole
  run so the game never grabs, hides or warps the cursor (`ClientWindow#keepOffTheCursor`: focus callback removed,
  `Minecraft#windowActive` false, Windows `WS_EX_NOACTIVATE`), and the harness moves only the game's own pointer
  (`MouseHandler#xpos/ypos`) and calls the input handlers directly. On the title screen the
  harness creates a fresh superflat world with a unique name (`sb-smoketest-<time>`, older ones are deleted) through
  the vanilla world creation flow (no quick play), runs the client scenarios, writes the result and stops the game. It
  takes well under a minute after the game has loaded.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players (and real backpacks on NeoForge), writes the same result file and exits.

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
  on 1.21.10 that is NeoForge only. Fabric and Forge 1.21.10 have no SB and report none.
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
| `sb.worn_backpack` | The backpack worn in the Curios `back` slot supplies a line. Skipped with the reason if no accessory mod is in the runtime |
| `sb.upgrade_settings_tab` | Using a backpack with an enabled tier 1 Building Upgrade (looking at the sky) opens the SB backpack screen; a click on the upgrade's tab icon opens `BuildingUpgradeSettingsTab`, a click on its toggle disables the upgrade on the server (stored on the upgrade), and after Escape the client's `ClientBuildingUpgradeState` follows. Screenshot `sb_upgrade_settings_tab` |
| `client.no_mod_errors` | No ERROR line from the mod's loggers and no WARN/ERROR carrying an exception thrown from the mod's code during the whole run |

The `sb.*` rows run on NeoForge only.

### Server (`runSmokeServer`, all loaders)

Game tests with a fake survival player (the loader's fake player, or `VanillaFakePlayers` on Forge, which has none).
The block sets are encoded and decoded with the packets' stream codecs and handed to the packets' server handlers,
exactly what arrives from a client.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `server.merge_undo_refund` | Survival merges (+1 snow layer, +1 candle) cost one item each; undo puts both blocks back without mining and gives the items back, redo charges them again |
| `server.refused_place_not_charged` | The loader's block place event refuses 2 of a 5 block line (as a protection mod would): only the 3 placed planks are charged and undo gives back exactly those. Skipped on Fabric (no place event; `ChargeGameTest` covers refused placements) |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side (NeoForge only) |
| `server.no_mod_errors` | As on the client |

Since Minecraft 1.21.5 a game test is a test function (code) plus a test instance (data). The scenarios are registered
as test functions `sophisticatedbuilding_smoketest:<name>` by the loader glue (`SmokeServerTests#functions`); the
instances that run them are data in the harness resources: `data/sophisticatedbuilding/test_instance/<name>.json`
(template `sophisticatedbuilding:smoketest_empty`, 400 ticks) with one `test_environment` `smoke_<n>` each, so every
test is its own batch and they run one after the other, as on 1.21.1. The `sb_` instances live in
`common/src/smoketestBackpacks/resources`, so only loaders with SB run them. The reporter turns only this mod's
instances into checks (the game test server also runs vanilla's optional `minecraft:always_pass`).

On NeoForge `sb.worn_backpack` is skipped by `runSmokeServer` since 1.21.8: the NeoForge fake player has no Curios
`back` slot with Curios 12.0.0+1.21.8 and 13.0.0+1.21.10 (it had one with Curios for 1.21.1). The client run, with a
real player, checks it.

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerTests, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structure/smoketest_empty.nbt (empty 8x8x8 game test template),
                                       test_instance/server_*.json, test_environment/smoke_1..2.json
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture; SophisticatedBackpacksScreens, client only;
                                       net.p3pp3rf1y API) and the sb_* test
                                       instances (smoke_3..8), only for loaders with SB (NeoForge)
<loader>/src/smoketest                 loader glue: mod metadata, entry points, test function registration, fake players, accessory slots
```

The harness is enabled only by the system properties the smoke tasks set (`sophisticatedbuilding.smoketest.out`,
`sophisticatedbuilding.smoketest.mode`); its classes do nothing in any other run.

How it drives the client: `SmokeClient.init()` starts a harness thread, which first waits for the `Minecraft` instance
(NeoForge 21.5 constructs mods before it exists); `ClientDriver` submits every action to the client thread
(`Minecraft#submit`) or the integrated server thread and waits for it, and counts client ticks through
`SmokeClient.onClientTickEnd()`, so scenarios read as linear scripts. Key presses use `KeyMapping.set/click` like
`MouseHandler`; aiming sets the player's rotation. The radial menu is steered by writing its accumulated mouse offset
(it tracks the mouse as a delta from the screen centre), the hit-testing, highlighting and selection are the menu's
own. In every other screen (`GuiScenarios`) the harness moves the pointer by writing `MouseHandler`'s `xpos`/`ypos`
(what the detached cursor callback would do) and clicks, turns the wheel and presses keys by calling `MouseHandler`'s
`onButton`/`onScroll` and `KeyboardHandler#keyPress` (private since the 1.21.9 input records; reflection) with the
window handle, the code GLFW's callbacks call, so the screens receive `mouseClicked`/`mouseReleased`/`mouseScrolled`/
`keyPressed` (loader screen events included) at real GUI coordinates, and hover state and tooltips come from their
own render pass.

Loader glue per build:

| | Fabric | NeoForge | Forge |
|---|---|---|---|
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client` | `neoforge.mods.toml`, `@Mod` | `mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 7 creates `runSmoketestClient`/`runSmoketestGameTestServer`; `runSmokeClient`/`runSmokeServer` depend on them |
| Test functions | `Registry.register(BuiltInRegistries.TEST_FUNCTION, ...)` | `DeferredRegister` on `Registries.TEST_FUNCTION` | `DeferredRegister` on `Registries.TEST_FUNCTION` |
| Test runner | Fabric API game test server (`-Dfabric-api.gametest`) | NeoForge game test server | Forge game test server (Forge 58+; Forge 55 had none) |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` |
| SB fixture | - | `common/src/smoketestBackpacks` | - |
| Accessory slot | - | Curios (smoke runtime only) | - |

Forge's world previews and outlines are drawn in a frame pass added through `AddFramePassEvent` (Forge 58+). The GUI
quads need the common mixin config (`GuiGraphicsAccessor`); the Forge smoke runs inherit the
`--mixin.config=sophisticatedbuilding.mixins.json` argument from `minecraft.runs.configureEach` (log: "Mixing
GuiGraphicsAccessor from sophisticatedbuilding.mixins.json into net.minecraft.client.gui.GuiGraphics"); the
`line_preview` and `radial_menu` screenshots are the runtime proof that the previews and the radial menu render.

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch, and the `smoketest` source set, run and `check` wiring from each
   `<loader>/build.gradle` (search for "smoke"). A loader without SB leaves `smoketestBackpacks` out of its source set
   and has no accessory slot glue (as Fabric and Forge here); its `sb_` test instances then simply do not exist.
2. Compile (`gradlew smoketestClasses`). What is version-specific and may need adapting:
   - World creation: `WorldOpenFlows#createFreshLevel(name, LevelSettings, WorldOptions, dimensions getter, screen)`,
     `LevelSettings` / `WorldDataConfiguration` / `GameRules` constructors, `WorldPresets.FLAT` (`ClientScenarios#joinFreshWorld`).
   - Game test API (1.21.5+ shape): test functions in `Registries.TEST_FUNCTION`, data-driven `test_instance` /
     `test_environment`, `GameTestInfo#id()`, `GameTestHelper#fail/assertTrue` taking a `Component`,
     `GlobalTestReporter`/`TestReporter`, and whether the loader's game test server works at all (Forge 55 does not,
     Forge 58 and 60 do).
     The NBT `DataVersion` of `smoketest_empty.nbt` is 3955 (1.21.1); old templates are upgraded by DataFixer (1.21.4 =
     4189, 1.21.5 = 4325, 1.21.8 = 4440, 1.21.10 = 4556).
   - Packets: `StreamCodec` round trip in `ServerScenarios#roundTrip` (1.20.5+; older versions use `FriendlyByteBuf`
     write/read methods).
   - Client: `Screenshot.takeScreenshot` (asynchronous since 1.21.5), `KeyMapping.set/click`, `Minecraft#submit`, the
     toast manager, `Inventory#getSelectedSlot/setSelectedSlot` (1.21.5; the `selected` field before),
     `CommonListenerCookie` (1.20.2+), the mod's `RadialMenu` field names (`accumulatedMouseX/Y`, `mouseInitialized`,
     `ringInnerEdge/OuterEdge`) and `ModifiersScreen#addMirrorButton`.
   - Registries: `Registry#getValue` (1.21.2+; `get` returns an `Optional<Holder.Reference>`).
   - SB: the `IBackpackWrapper` / `UpgradeHandler` API (`SophisticatedBackpacksFixture`), block/item ids.
   - Accessory mods: the Curios / Trinkets glue and their versions.
   - Forge: the `pack.mcmeta` formats of the harness mod (and of the mod itself) and the `loaderVersion` in its `mods.toml`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers `pointAt`/`clickAt`/`scrollAt`/`pressKey`
in `ClientDriver`, `SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with
its SB implementation `SophisticatedBackpacksScreens` + `META-INF/services` entry in `common/src/smoketestBackpacks`).
They came from mc/1.21.1 (H5a). The calls that depend on the Minecraft, loader or SB version, as on this branch:

- Input (`ClientDriver`): `MouseHandler` private fields `xpos`/`ypos` and private methods
  `onButton(long, MouseButtonInfo, int action)` and `onScroll(long, double, double)`, private
  `KeyboardHandler#keyPress(long, int action, KeyEvent)` (reflection, Mojang names; before 1.21.9 `onPress(long, int,
  int, int)` and a public `keyPress(long, int, int, int, int)`), `Window#handle()`,
  `Window#getScreenWidth/getScreenHeight/getGuiScaledWidth/getGuiScaledHeight`.
- Screens (`GuiScenarios`): `Screen#children()`, `Screen#renderables` (reflection), `AbstractContainerScreen` fields
  `leftPos`/`topPos` (reflection), `AbstractContainerScreen#getMenu`, `AbstractContainerMenu#slots/getSlot/getCarried/containerId`,
  `Slot#x/y/index/container/getContainerSlot`, `AbstractWidget#getX/getY/getWidth/getHeight/isHovered`,
  `Button#getMessage`, `Player#containerMenu/inventoryMenu/closeContainer`, `Entity#isShiftKeyDown` on the server.
- NBT: `CompoundTag#getCompoundOrEmpty/getListOrEmpty/getStringOr/getBooleanOr/getIntOr` and
  `ListTag#getCompoundOrEmpty` (1.21.5+; the getters return `Optional` without a default).
- Mod internals read by the checks: `AbstractRandomizerBagItem#getBagInventory`, `OmegaRandomizerBagItem#getSlotWeight`,
  the Omega screen's `Reset` button text, `ModeOptions.ActionEnum.OPEN_PLAYER_SETTINGS`, `ModifiersScreen` fields
  `addArrayButton`/`closeButton`/`list`, `BaseModifierEntry#modifier` and field `enableButton`, `ArrayEntry` field
  `countInput`, `Array#enabled/count`, the modifier NBT keys (`modifierSettingsList`, `type`, `enabled`, `count`) and the
  player data key `sophisticatedbuilding:buildModifiers`.
- SB (`SophisticatedBackpacksScreens`): `StorageScreenBase#getUpgradeSettingsControl()`, `SettingsTabControl#getOpenTab()`,
  `CompositeWidgetBase#children()`, `WidgetBase#getX/getY/getWidth/getHeight`, `ButtonBase` (tab icon), `ToggleButton`,
  the mod's `BuildingUpgradeSettingsTab`, and `IUpgradeWrapper#isEnabled` in the fixture.

What the port from 1.21.1 changed:

- `ClientDriver`: the 1.21.9 input records. A click calls `MouseHandler#onButton(handle, new MouseButtonInfo(button, 0),
  action)` (was `onPress(handle, button, action, 0)`), a key press the now private
  `KeyboardHandler#keyPress(handle, action, new KeyEvent(key, scancode, 0))` by reflection, and the GLFW handle is
  `Window#handle()`.
- `GuiScenarios#modifierEntryWidgets`: the stored modifier is read with the 1.21.5+ NBT getters with defaults
  (`getCompoundOrEmpty`, `getListOrEmpty`, `getStringOr`, `getBooleanOr`, `getIntOr`).
- Unchanged and working: the rest of `GuiScenarios` (the reworked modifier list still exposes its entries through
  `ModifiersScreenList#children()` and lays them out while rendering), `SophisticatedBackpacksScreens` and the fixture's
  `isBuildingUpgradeEnabled` (Sophisticated Core 1.21.10 GUI API identical; NeoForge only).

Since R2 (player settings editor, bag title fit, radial layout) the checks also use:

- `client.player_settings_gui`: `RadialMenuDriver#hoverButton(action)` (the side buttons as the menu last drew them,
  `RadialMenu#sideButtons()`), `ClientDriver#dragTo` (press and release through `MouseHandler`, the move itself through
  `Screen#mouseMoved` and `Screen#mouseDragged(MouseButtonEvent, double, double)` (1.21.9 input records), because
  `MouseHandler#handleAccumulatedMovement` only forwards moves for the focused window and the harness window never has
  focus), `PlayerSettingsGui#settingEntries()` with `SettingEntry#key/widget()` and `NumberEntry#values`
  (`SliderValues`), the fields `doneButton`/`resetButton`, `AbstractSliderButton` track geometry
  (`x + 4 .. x + width - 4`), `KeyMapping#setKey` + `KeyMapping.resetMapping()` to bind the unbound key for the test,
  `ClientConfig` values, and the client config file in `<game dir>/config`: `sophisticatedbuilding-client.json`
  (Fabric, section `Visuals`) or `sophisticatedbuilding-client.toml` (NeoForge/Forge).
- `client.randomizer_bag_screens`: `BagTitle.fit`/`availableWidth`/`isHovered` and `TitleFit#drawnWidth` (the screens'
  own layout), `AbstractContainerScreen#imageWidth` (reflection), `Screen#getTitle`, and the anvil name of an item:
  `ItemStack#set(DataComponents.CUSTOM_NAME, ...)`.
- `client.radial_option_icons`: `NativeImage#getPixel` (ARGB since 1.21.2), `ResourceManager#open`, the `AllIcons`
  fields `iconX/iconY`, `RadialMenu#sideButtons()`.

### What the 1.21.1 -> 1.21.4 adoption changed

- `ClientScenarios#joinFreshWorld`: `new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())` (1.21.2+ takes the
  feature flags); the dimensions getter receives a `HolderLookup.Provider`:
  `registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)` instead of
  `registryOrThrow(...).getHolderOrThrow(...)`.
- `ClientDriver#screenshot`: `Minecraft#getToastManager()` instead of `getToasts()`.
- `SophisticatedBackpacksFixture#item`: `BuiltInRegistries.ITEM.getValue(id)` instead of `get(id)`.
- Forge harness mod: `loaderVersion="[54,)"`, `pack.mcmeta` `pack_format` 46 with `supported_formats: [46, 61]`.
- Fabric: no SB on 1.21.4, so no `smoketestBackpacks` in the source set, no Trinkets/Cardinal Components smoke
  dependencies, no `TrinketsAccessorySlots`, no `sb_` game tests.
- Unchanged and working as on 1.21.1: `RadialMenuDriver`, `ServerScenarios`, `VanillaFakePlayers`,
  `ServerGamePacketListenerImpl#teleport(x, y, z, yaw, pitch)`, the game test glue of all three loaders, the
  1.21.1 `smoketest_empty.nbt`.

### What the 1.21.4 -> 1.21.5 adoption changed

- Game test rework: the class/annotation based tests (`FabricSmokeServerTests`, `NeoForgeSmokeServerTests` with
  `@GameTestHolder`, `ForgeSmokeServerTests`) are gone. New `SmokeServerTests` (test functions by name, registered by
  each loader's glue in `Registries.TEST_FUNCTION`) plus data: `test_instance/<name>.json` (`minecraft:function`,
  template `sophisticatedbuilding:smoketest_empty`, `max_ticks` 400) and one `test_environment/smoke_<n>.json`
  (`minecraft:all_of`, empty) per test to keep one batch per test. `ServerScenarios.TIMEOUT_TICKS` moved into the JSON.
  NeoForge 21.5 has no `@GameTestHolder`/`@PrefixGameTestTemplate` and no namespace filter any more; Fabric's own
  `@GameTest` annotation (used by `fabric/src/gametest`) would work for Fabric only, the data approach works on all three.
- `SmokeServer`: the check name comes from `GameTestInfo#id().getPath()` (was `getTestName()`); tests outside the
  `sophisticatedbuilding` namespace (vanilla's `minecraft:always_pass`) are not checks.
- `ServerScenarios`: `GameTestHelper#fail` and `#assertTrue` take a `Component` (`Component.literal(...)`).
- Forge 55: no game test server; `SmokeServerRunner` runs the instances on the dedicated server the game test launch
  target starts (hooked on `ServerStartedEvent` and `TickEvent.ServerTickEvent.Post`). The dedicated server pauses
  when it has been empty for 60 s; the two tests finish in about a second.
- `ClientDriver#screenshot`: `Screenshot.takeScreenshot(RenderTarget, Consumer<NativeImage>)` reads the frame back
  asynchronously; the callback writes and closes the image and completes a future the harness waits for.
- `Inventory#selected` is private: `getSelectedSlot()` / `setSelectedSlot(int)` (`ClientDriver#selectHotbarSlot`,
  every fake player) and `getSelectedItem()` (Fabric held-slot fix).
- `SmokeClient`: waits for `Minecraft.getInstance()` before creating the `ClientDriver` (NeoForge 21.5 runs the mod
  constructors before the client exists; the first run failed with a NullPointerException in `ClientDriver`).
- Forge harness mod: `loaderVersion="[55,)"`, `pack.mcmeta` `pack_format` 55 with `supported_formats: [55, 71]`.
- Unchanged: `ClientScenarios` (world creation, radial menu, modifier screen), `RadialMenuDriver`,
  `SophisticatedBackpacksFixture` (SB 1.21.5-3.27.2.2152 API identical), Curios glue, `VanillaFakePlayers`
  construction, the 1.21.1 `smoketest_empty.nbt`.

### What the 1.21.5 -> 1.21.8 adoption changed

- `VanillaFakePlayers`: `ServerCommonPacketListenerImpl#send(Packet, ChannelFutureListener)` replaces the
  `PacketSendListener` overload.
- Forge 58 (EventBus 7): the harness glue adds its listeners through `<Event>.BUS.addListener` and registers the test
  functions on `FMLJavaModLoadingContext#getModBusGroup()`. Forge 58 has a working game test server again, so
  `SmokeServerRunner` is gone and `runSmokeServer` runs the tests like on NeoForge (the log ends with "All 3 required
  tests passed", vanilla's `minecraft:always_pass` included).
- Forge harness mod: `loaderVersion="[58,)"`, `pack.mcmeta` `pack_format` 64 with `supported_formats: [64, 81]`
  (1.21.8 resource packs are 64, data packs 81; the mod's own `pack.mcmeta` got the same).
- Unchanged and working: `ClientScenarios`, `ClientDriver` (world creation, asynchronous screenshots), `RadialMenuDriver`,
  `ServerScenarios`, `SophisticatedBackpacksFixture` (SB 1.21.8-3.26.2.2159 API identical), Curios glue (Curios
  12.0.0+1.21.8), the 1.21.1 `smoketest_empty.nbt`.

### What the 1.21.8 -> 1.21.10 adoption changed

- 1.21.9 input records: `RadialMenuDriver` and the "Add Mirror" click in `ClientScenarios` call
  `mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false)`.
- `ClientScenarios#joinFreshWorld`: the spawn point is `ServerLevel#getRespawnData().pos()` (1.21.9 removed
  `getSharedSpawnPos()`); `GameProfile#name()` (authlib records); `ClientWindow`: `Window#handle()` is the GLFW handle.
- `SophisticatedBackpacksFixture`: Sophisticated Core 1.21.10 is on the NeoForge transfer API; the fixture fills the
  backpack with `InventoryHandler#insert(ItemResource, amount, transaction)` in a committed root transaction, and the
  slot count is `size()` (tried before the older `getSlots()` / `getSlotCount()`).
- Forge 60: harness mod `loaderVersion="[60,)"`; `pack.mcmeta` (harness and mod) declares `min_format` 64 ..
  `max_format` 88 plus `pack_format` 64 / `supported_formats: [64, 88]`. 1.21.9+ reads `min_format`/`max_format`
  (resource packs 69, data packs 88 on 1.21.10) and still needs the legacy fields for a range that starts at or below
  64 (resources) / 81 (data). The lower bound 64 is needed because Forge 60.1.15 checks every mod pack, data pack
  included, against the resource pack version 69 (`ResourcePackLoader#findPacks`): the MDK's `min_format` 88 alone makes
  the mod's data pack `TOO_NEW` (the first run failed `client.mod_data_pack_compatible` exactly so; Forge's own pack
  is listed `TOO_NEW` for the same reason).
- Unchanged and working: `ClientDriver` (world creation, asynchronous screenshots), `ServerScenarios`,
  `VanillaFakePlayers`, the Forge/NeoForge/Fabric glue, Curios glue (Curios 13.0.0+1.21.10), the 1.21.1
  `smoketest_empty.nbt`.

## Findings (1.21.10)

- Results of the 1.21.10 runs: `runSmokeServer` 3 / 9 (1 skipped: `sb.worn_backpack`, see above) / 3 checks,
  `runSmokeClient` 10 / 17 (7 `sb.*`) / 10 checks on Fabric / NeoForge / Forge, all passing; Fabric `runGametest`
  "All 18 required tests passed".
- GUI checks (H5, ported from mc/1.21.1): `runSmokeClient` 13 / 21 (8 `sb.*`, `sb.upgrade_settings_tab` included) / 13
  checks on Fabric / NeoForge / Forge, all passing (`runSmokeServer` unchanged: 3 / 9 / 3; unit tests 77 / 65 / 65);
  every screen behaves as on 1.21.1 (screenshots `omega_randomizer_bag_weights`: weight badge 2 and the weight
  tooltip, `modifier_widgets`: the array entry switched off with Count 6 and the Count tooltip,
  `sb_upgrade_settings_tab`: the open Building Upgrade tab with its toggle on "Disabled").
- Round 2 (ported from mc/1.21.1 fb10ef2..766d18f): client config set/save API and the `PlayerSettingsGui` editor
  (radial button, unbound "Open Player Settings" key), bag title fit + bag name, dead widgets removed (`GuiCheckBoxFixed`,
  `GuiIconButton`, `GuiNumberField`; the other three were already gone), Terrain Mound icons + highlight,
  `RadialButtonLayout`, gameplay fixes (merge-undo refund, no charge for failed placements, full-count charge, stuck
  undo stack, Disable + Quick Replace preview), cursor/focus-safe smoke client. 1.21.10 differences: the 1.21.6 GUI
  (`BagTitle` and `LabeledScrollInput` scale with the 2D `Matrix3x2fStack`, the bag title tooltip goes through
  `GuiGraphics#setTooltipForNextFrame`, text colours carry alpha) and the 1.21.9 input records (`PlayerSettingsGui` and
  its sliders override `keyPressed(KeyEvent)`, list rows draw in `renderContent` with `getContentX/Y/Width/Height()`,
  `ClientDriver#dragTo` passes a `MouseButtonEvent`); `AbstractScrollArea` names (`scrollAmount()`, `scrollBarX()`);
  `NativeImage#getPixel` (ARGB) and `Window#handle()` in the harness; the two new server scenarios are test functions
  plus `test_instance`/`test_environment` data (`server_merge_undo_refund`/`smoke_merge`,
  `server_refused_place_not_charged`/`smoke_refused`); Forge's refusal hook is an EventBus 7 predicate listener
  (`BlockEvent.EntityPlaceEvent.BUS.addListener(event -> refused)`). Smoke client focus: Forge 60 creates the window
  through `ImmediateWindowHandler.setupMinecraftWindow` (mixin target as on 1.21.1); NeoForge 21.10 (FML 9) has no
  `ImmediateWindowHandler` call in `Window` any more: it takes over the early loading screen's window when there is one
  and otherwise calls `glfwCreateWindow`, so its `SmokeWindowMixin` targets `glfwCreateWindow` (as on Fabric), which the
  game reaches because `earlyWindowControl = false` leaves FML without an early window provider. Verified headless
  (2026-09-25): `gradlew build` Fabric 104 / NeoForge 90 / Forge 90 tests, Fabric `runGametest` "All 25 required tests
  passed" (24 + `minecraft:always_pass`), `runSmokeServer` Fabric 5 (`server.refused_place_not_charged` skipped: no
  place event on Fabric), NeoForge 11 (`sb.worn_backpack` skipped as before), Forge 5, all passing. `runSmokeClient`
  not run yet (no game clients until the lead allows them).
- Forge 60.1.15 flags mod data packs against the resource pack version, see above; the mod's `pack.mcmeta` works around
  it.
- The Sophisticated Backpacks, Core and Curios data packs of their 1.21.10 builds are listed as `TOO_OLD` by NeoForge
  (their pack format); the mod's own data pack is compatible.

## Findings (1.21.8)

- Results of the 1.21.8 runs: `runSmokeServer` 3 / 9 (1 skipped: `sb.worn_backpack`, see above) / 3 checks,
  `runSmokeClient` 10 / 17 (7 `sb.*`) / 10 checks on Fabric / NeoForge / Forge, all passing.
- The Sophisticated Backpacks and Core data packs of their 1.21.8 builds are listed as `TOO_OLD` by NeoForge (their
  pack format); the mod's own data pack is compatible (`client.mod_data_pack_compatible`).

## Findings (1.21.5)

- The two fixes found by the first 1.21.1 runs are applied here too: Fabric `FabricCommonEvents` resends the held slot
  when a build-mode click cancels the vanilla placement (the client had predicted the item use; a player holding
  exactly the blocks a build needs lost the held block client side), and Forge `pack.mcmeta` declares `pack_format` 55
  with `supported_formats: [55, 71]` (1.21.5 resource packs are 55, data packs 71; the port still had 46 from
  1.21.4), so the mod's data pack is not flagged incompatible; checked by `client.mod_data_pack_compatible`.
- Forge 55 (55.0.24 and the latest 55.1.14) has no working game test server, see above.
