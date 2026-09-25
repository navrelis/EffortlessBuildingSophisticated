# Testing Sophisticated Building 1.21.4

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (104 tests on Fabric incl. its config tests, 90 on NeoForge and Forge) |
| Fabric GameTests | `gradlew runGametest` | 24 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration on NeoForge |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

On Minecraft 1.21.4 only NeoForge has Sophisticated Backpacks (official build); Fabric and Forge have none, so their
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
  on 1.21.4 that is NeoForge only. Fabric and Forge 1.21.4 have no SB and report none.
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

Game tests with a fake survival player (the loader's fake player, or `VanillaFakePlayers` on Forge 54 which has none).
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

## Layout

```
gradle/smoketest.gradle                shared by every loader build: output dir, result verification, task timeout
common/src/smoketest/java              loader-neutral harness (vanilla + mod API only)
  sophisticated/building/smoketest/
    SmokeTest, SmokeReport, SmokeWatchdog, ModErrorLogCapture     switches, JSON result, watchdog, log capture
    client/SmokeClient, ClientDriver, ClientScenarios, GuiScenarios, RadialMenuDriver, ClientWindow, SmokeClientPlatform
    server/SmokeServer, ServerScenarios, SmokeServerPlatform, VanillaFakePlayers
    backpack/SmokeBackpacks, SmokeBackpackScreens, SmokeAccessorySlots   service interfaces for the SB fixture
common/src/smoketest/resources         data/sophisticatedbuilding/structure/smoketest_empty.nbt (empty game test template)
common/src/smoketestBackpacks          SB fixture (SophisticatedBackpacksFixture; SophisticatedBackpacksScreens, client only;
                                       net.p3pp3rf1y API), only for loaders with SB (NeoForge)
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
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `neoforge.mods.toml`, `@Mod` | `mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 7 creates `runSmoketestClient`/`runSmoketestGameTestServer`; `runSmokeClient`/`runSmokeServer` depend on them |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` | `@GameTestHolder`, template `sophisticatedbuilding:smoketest_empty` |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` |
| SB fixture | - | `common/src/smoketestBackpacks` | - |
| Accessory slot | - | Curios (smoke runtime only) | - |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch, and the `smoketest` source set, run and `check` wiring from each
   `<loader>/build.gradle` (search for "smoke"). A loader without SB leaves `smoketestBackpacks` out of its source set
   and has no `sb_` game tests and no accessory slot glue (as Fabric and Forge here).
2. Compile (`gradlew smoketestClasses`). What is version-specific and may need adapting:
   - World creation: `WorldOpenFlows#createFreshLevel(name, LevelSettings, WorldOptions, dimensions getter, screen)`,
     `LevelSettings` / `WorldDataConfiguration` / `GameRules` constructors, `WorldPresets.FLAT` (`ClientScenarios#joinFreshWorld`).
   - Game test API: `GameTestHelper`, `GlobalTestReporter`/`TestReporter` (the framework was rewritten in 1.21.5), the annotations and
     template lookup of each loader, and the NBT `DataVersion` of `smoketest_empty.nbt` (3955 = 1.21.1; old templates
     are upgraded by DataFixer, which is why the 1.21.1 file works unchanged on 1.21.4 = 4189).
   - Packets: `StreamCodec` round trip in `ServerScenarios#roundTrip` (1.20.5+; older versions use `FriendlyByteBuf`
     write/read methods).
   - Client: `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, the toast manager,
     `CommonListenerCookie` (1.20.2+), the mod's `RadialMenu` field names (`accumulatedMouseX/Y`, `mouseInitialized`,
     `ringInnerEdge/OuterEdge`) and `ModifiersScreen#addMirrorButton`.
   - Registries: `Registry#getValue` (1.21.2+; `get` returns an `Optional<Holder.Reference>`).
   - SB: the `IBackpackWrapper` / `UpgradeHandler` API (`SophisticatedBackpacksFixture`), block/item ids.
   - Accessory mods: the Curios / Trinkets glue and their versions.
   - Forge: the `pack.mcmeta` formats of the harness mod (and of the mod itself) and the `loaderVersion` in its `mods.toml`.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

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

### Porting the H5 checks (GUI screens, 1.21.1 -> 1.21.4)

`GuiScenarios`, its input helpers in `ClientDriver` (`pointAt`/`clickAt`/`scrollAt`/`pressKey`), `SmokeBackpacks#isBuildingUpgradeEnabled`
and the client-only `SmokeBackpackScreens`/`SophisticatedBackpacksScreens` service ported unchanged from 1.21.1: `MouseHandler`'s
private `xpos`/`ypos` fields and `onPress`/`onScroll` methods, `KeyboardHandler#keyPress`, the mod's screen/widget field names
(`PlayerSettingsGui`, `ModifiersScreen`, `BaseModifierEntry`, `ArrayEntry`, the four randomizer bag screens, `OmegaRandomizerBagItem#getSlotWeight`,
`ModeOptions.ActionEnum.OPEN_PLAYER_SETTINGS`) and the Sophisticated Core GUI API (`StorageScreenBase#getUpgradeSettingsControl`,
`SettingsTabControl#getOpenTab`, `CompositeWidgetBase#children`, `WidgetBase`/`ButtonBase`/`ToggleButton`) are identical to 1.21.1
(checked with `javap` against `upstream/neoforge/1.21.4/sophisticatedcore-1.21.4-1.5.0.2336.jar`). NeoForge only (Fabric and Forge
have no SB on this branch, so no `sb.upgrade_settings_tab`).

Verified (H5b, 2026-09-25): `runSmokeClient` NeoForge 21/21 (8 `sb.*` incl. `sb.upgrade_settings_tab`), Fabric 13/13,
Forge 13/13; `runSmokeServer` unchanged (NeoForge 9/9, Fabric 3/3, Forge 3/3); `gradlew build` 65 / 77 / 65 tests.

Since R2 (player settings editor, bag title fit) the two checks also use:

- `client.player_settings_gui`: `RadialMenuDriver#hoverLeftButton` (the menu's `buttonDistance` field; the player
  settings button sits at `-buttonDistance - 52, -39`), `ClientDriver#dragTo` (press and release through `MouseHandler`,
  the move itself through `Screen#mouseMoved`/`mouseDragged`, because `MouseHandler#handleAccumulatedMovement` only
  forwards moves for the focused window and the harness window never has focus), `PlayerSettingsGui#settingEntries()`
  with `SettingEntry#key/widget()` and `NumberEntry#values` (`SliderValues`), the fields `doneButton`/`resetButton`,
  `AbstractSliderButton` track geometry (`x + 4 .. x + width - 4`), `KeyMapping#setKey` + `KeyMapping.resetMapping()` to
  bind the unbound key for the test, `ClientConfig` values, and the client config file in `<game dir>/config`:
  `sophisticatedbuilding-client.json` (Fabric, section `Visuals`) or `sophisticatedbuilding-client.toml` (NeoForge/Forge).
- `client.randomizer_bag_screens`: `BagTitle.fit`/`availableWidth`/`isHovered` and `TitleFit#drawnWidth` (the screens'
  own layout), `AbstractContainerScreen#imageWidth` (reflection), `Screen#getTitle`, and the anvil name of an item:
  `ItemStack#set(DataComponents.CUSTOM_NAME, ...)` (1.20.5+; before: `ItemStack#setHoverName`).

## Findings (1.21.4)

- The two fixes found by the first 1.21.1 runs are applied here too: Fabric `FabricCommonEvents` resends the held slot
  when a build-mode click cancels the vanilla placement (the client had predicted the item use; a player holding
  exactly the blocks a build needs lost the held block client side), and Forge `pack.mcmeta` declares
  `supported_formats: [46, 61]` (1.21.4 resource packs are 46, data packs 61), so the mod's data pack is not flagged
  incompatible; checked by `client.mod_data_pack_compatible`.
- GUI checks (H5): every screen works on all three loader builds. They found `PlayerSettingsGui` to be a stub, six
  unused widget classes in `gui/elements` and bag titles wider than their texture; all three are resolved by round 2.
- Round 2 (ported from mc/1.21.1 fb10ef2..766d18f): client config set/save API and the `PlayerSettingsGui` editor
  (radial button, unbound "Open Player Settings" key), bag title fit + bag name, dead widgets removed, Terrain Mound
  icons + highlight, `RadialButtonLayout`, dead mini preview method removed, gameplay fixes (merge-undo refund, no charge
  for failed placements, full-count charge, stuck undo stack, Disable + Quick Replace preview), cursor/focus-safe smoke
  client. 1.21.4 differences: `AbstractScrollArea` names in `PlayerSettingsGui` (`scrollAmount()`, `scrollBarX()`),
  `NativeImage#getPixel` (ARGB) in `GuiScenarios#iconPixels`. Smoke client focus: NeoForge 21.4 (FML 6) and Forge 54
  create the window through `ImmediateWindowHandler.setupMinecraftWindow` (mixin target as on 1.21.1), `earlyWindowControl`
  exists in both FML configs. Verified headless (2026-09-25): `gradlew build` Fabric 104 / NeoForge 90 / Forge 90 tests,
  Fabric `runGametest` "All 24 required tests passed", `runSmokeServer` Fabric 5/5 (`server.refused_place_not_charged`
  skipped: no place event on Fabric), NeoForge 11/11, Forge 5/5. `runSmokeClient` not run yet (no game clients until the
  lead allows them).

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
