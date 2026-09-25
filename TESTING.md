# Testing Sophisticated Building 1.21.1

Three layers, from fast to real:

| Layer | Command (in a loader folder) | What it proves |
|---|---|---|
| Unit tests | `gradlew build` | Pure logic in `common/src/test` (77 tests on Fabric incl. its config tests, 65 on NeoForge, Forge and Forge 1.21) |
| Fabric GameTests | `gradlew runGametest` | 17 server-side building rules (`fabric/src/gametest`) |
| **In-game smoke tests** | `gradlew runSmokeClient` / `gradlew runSmokeServer` | The mod works in a real game on this loader, including the Sophisticated Backpacks (SB) integration |

`gradlew build` compiles the smoke harness (so it cannot rot) but never runs it. The harness is dev-only: it lives in
its own source set, is loaded only by the smoke runs, and never ends up in the mod jar.

## Running the smoke tests

```
cd fabric   && gradlew runSmokeClient -PsmoketestOut=<absolute dir> --no-daemon
cd fabric   && gradlew runSmokeServer -PsmoketestOut=<absolute dir> --no-daemon
```

Same for `neoforge` and `forge`. Without `-PsmoketestOut` the result goes to `<loader>/build/smoketest/client` or
`.../server`.

- **runSmokeClient** starts a real client: muted, moved to a secondary monitor if there is one, and deaf to real
  keyboard and mouse input (its GLFW input callbacks are removed; the harness does not need them), so clicking into the
  window cannot disturb a run. On the title screen the
  harness creates a fresh superflat world with a unique name (`sb-smoketest-<time>`, older ones are deleted) through
  the vanilla world creation flow (no quick play), runs the client scenarios, writes the result and stops the game. It
  takes about a minute after the game has loaded.
- **runSmokeServer** is headless (no GPU needed, for CI): a game test server runs the server scenarios with fake
  survival players and real backpacks, writes the same result file and exits.

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
  Forge 1.21.1 has no SB and reports none.
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

### Server (`runSmokeServer`, all loaders)

Game tests with a fake survival player (the loader's fake player, or `VanillaFakePlayers` on Forge 52 which has none).
The block sets are encoded and decoded with the packets' stream codecs and handed to the packets' server handlers,
exactly what arrives from a client.

| Check | Asserts |
|---|---|
| `server.place_line_survival` | 5 planks placed and consumed |
| `server.undo_redo` | Undo/redo packets restore the inventory counts |
| `sb.upgrade_supplies_blocks`, `sb.disabled_upgrade_ignored`, `sb.tier_cap`, `sb.tool_swapper_tools`, `sb.worn_backpack_chest`, `sb.worn_backpack` | As on the client, server side (Fabric, NeoForge) |
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
| Harness mod | `fabric.mod.json`, entrypoints `main`/`client`/`fabric-gametest` | `neoforge.mods.toml`, `@Mod` | `mods.toml` + `pack.mcmeta`, `@Mod` |
| Source set wiring | Loom runs `smokeClient`/`smokeServer` (`source sourceSets.smoketest`) | MDG runs `smokeClient`/`smokeServer` (`loadedMods` main + harness) | ForgeGradle 7 creates `runSmoketestClient`/`runSmoketestGameTestServer`; `runSmokeClient`/`runSmokeServer` depend on them |
| Game tests | `FabricGameTest`, `EMPTY_STRUCTURE` | `@GameTestHolder`, template `smoketest_empty` | `@GameTestHolder`, template `sophisticatedbuilding:smoketest_empty` |
| Fake player | Fabric API `FakePlayer` | `FakePlayerFactory` | `VanillaFakePlayers` |
| Held key in screens | nothing | `NeoForgeSmokeClientPlatform` (key conflict context) | `ForgeSmokeClientPlatform` |
| Accessory slot | Trinkets (smoke runtime only) | Curios (smoke runtime only) | - |

## Adopting the harness in another Minecraft version (port)

1. Copy `gradle/smoketest.gradle`, `common/src/smoketest`, `common/src/smoketestBackpacks` (if that version has SB)
   and `<loader>/src/smoketest` from this branch, and the `smoketest` source set, run and `check` wiring from each
   `<loader>/build.gradle` (search for "smoke").
2. Compile (`gradlew smoketestClasses`). What is 1.21.1-specific and may need adapting:
   - World creation: `WorldOpenFlows#createFreshLevel(name, LevelSettings, WorldOptions, dimensions getter, screen)`,
     `LevelSettings` / `WorldDataConfiguration` / `GameRules` constructors, `WorldPresets.FLAT` (`ClientScenarios#joinFreshWorld`).
   - Game test API: `GameTestHelper`, `GlobalTestReporter`/`TestReporter` (the framework was rewritten in 1.21.5), the annotations and
     template lookup of each loader, and the NBT `DataVersion` of `smoketest_empty.nbt` (3955 = 1.21.1; old templates
     are upgraded by DataFixer).
   - Packets: `StreamCodec` round trip in `ServerScenarios#roundTrip` (1.20.5+; older versions use `FriendlyByteBuf`
     write/read methods).
   - Client: `Screenshot.takeScreenshot`, `KeyMapping.set/click`, `Minecraft#submit`, `CommonListenerCookie` (1.20.2+),
     the mod's `RadialMenu` field names (`accumulatedMouseX/Y`, `mouseInitialized`, `ringInnerEdge/OuterEdge`) and
     `ModifiersScreen#addMirrorButton`.
   - SB: the `IBackpackWrapper` / `UpgradeHandler` API (`SophisticatedBackpacksFixture`), block/item ids.
   - Accessory mods: the Curios / Trinkets glue and their versions.
3. Run `runSmokeServer` first (headless), then `runSmokeClient`.

### Porting the H5 checks (GUI screens)

`client.randomizer_bag_screens`, `client.player_settings_gui`, `client.modifier_entry_widgets` and
`sb.upgrade_settings_tab` live in `client/GuiScenarios` (plus the input helpers `pointAt`/`clickAt`/`scrollAt`/`pressKey`
in `ClientDriver`, `SmokeBackpacks#isBuildingUpgradeEnabled`, and the client-only service `SmokeBackpackScreens` with
its SB implementation `SophisticatedBackpacksScreens` + `META-INF/services` entry in `common/src/smoketestBackpacks`).
They add about 6 s to a client run on 1.21.1. The calls that depend on the Minecraft, loader or SB version:

- Input (`ClientDriver`): `MouseHandler` private fields `xpos`/`ypos` and private methods `onPress(long, int, int, int)`
  and `onScroll(long, double, double)` (reflection, Mojang names), `KeyboardHandler#keyPress(long, int, int, int, int)`
  (public), `Window#getScreenWidth/getScreenHeight/getGuiScaledWidth/getGuiScaledHeight`. Minecraft 1.21.9 reworked
  input into event records (`MouseButtonInfo`/`KeyEvent`, `Screen#mouseClicked(MouseButtonEvent, boolean)`): check these
  signatures with javap there. A branch whose dev runtime is not on Mojang names needs that mapping's field/method names.
- Screens (`GuiScenarios`): `Screen#children()`, `Screen#renderables` (private in vanilla, public on NeoForge: read by
  reflection), `AbstractContainerScreen` fields `leftPos`/`topPos` (reflection; Forge/NeoForge also have `getGuiLeft()`),
  `AbstractContainerScreen#getMenu`, `AbstractContainerMenu#slots/getSlot/getCarried/containerId`, `Slot#x/y/index/container/getContainerSlot`,
  `AbstractWidget#getX/getY/getWidth/getHeight/isHovered` (before 1.19.4 `x`/`y` are public fields; `isHovered()` was
  `isHoveredOrFocused()` in some versions), `Button#getMessage`, `LocalPlayer#getInventory()` (1.17+, `player.inventory`
  before), `Player#containerMenu/inventoryMenu/closeContainer`, `Entity#isShiftKeyDown` on the server (the client sends
  the sneak state when `keyShift` is held). The harness never renders itself, so `GuiGraphics` (1.20+) vs `PoseStack`
  only matters in the mod's own screens.
- Menu opening: a randomizer bag opens with sneak + use (`AbstractRandomizerBagItem#use` -> `player.openMenu`, the
  harness looks at the sky so no block is targeted); a backpack opens with use (`BackpackItem#use`). Screen classes
  `RandomizerBagScreen`, `GoldenRandomizerBagScreen`, `DiamondRandomizerBagScreen`, `OmegaRandomizerBagScreen` (registered
  per loader via `MenuScreens`/`RegisterMenuScreensEvent`), items `SophisticatedBuilding.*RANDOMIZER_BAG_ITEM`.
- Mod internals read by the checks: `AbstractRandomizerBagItem#getBagInventory`, `OmegaRandomizerBagItem#getSlotWeight`
  (storage differs per version, the method hides it), the Omega screen's `Reset` button text,
  `ModeOptions.ActionEnum.OPEN_PLAYER_SETTINGS`, `ModifiersScreen` fields `addArrayButton`/`closeButton`/`list`,
  `BaseModifierEntry#modifier` and field `enableButton`, `ArrayEntry` field `countInput`, `Array#enabled/count`, the
  modifier NBT keys (`modifierSettingsList`, `type`, `enabled`, `count`) and the player data key
  `sophisticatedbuilding:buildModifiers` (`ModifierSettingsPacket`, read through `IPlatformHelper#getPersistentData`).
- SB (`SophisticatedBackpacksScreens`): `StorageScreenBase#getUpgradeSettingsControl()`, `SettingsTabControl#getOpenTab()`,
  `CompositeWidgetBase#children()`, `WidgetBase#getX/getY/getWidth/getHeight`, `ButtonBase` (tab icon), `ToggleButton`
  (the mod's toggle), the mod's `sophisticated.building.client.gui.BuildingUpgradeSettingsTab`, and `IUpgradeWrapper#isEnabled`
  in the fixture. Sophisticated Backpacks builds from before Sophisticated Core have these classes in
  `net.p3pp3rf1y.sophisticatedbackpacks.client.gui`; adapt the imports there. The same source compiles against the
  official NeoForge builds and the Fabric port on 1.21.1.

## Findings of the first runs (1.21.1)

- Fabric: the server cancelled the vanilla placement of a build-mode click but never corrected the client's predicted
  item use, so a player holding exactly the blocks a build needs (e.g. 1 stone + a backpack) lost the held block
  client side and the build was cancelled. Fixed in `FabricCommonEvents` (resend the slot, as NeoForge/Forge already do);
  caught by `sb.upgrade_supplies_blocks`, `sb.tier_cap` and `sb.worn_backpack_chest`.
- Forge: `pack.mcmeta` declared only `pack_format` 34 (resource packs), so the mod's data pack was flagged incompatible
  (data packs are 48 in 1.21.1). Now `supported_formats: [34, 48]`; checked by `client.mod_data_pack_compatible`.
- The Fabric SB port rescans Trinkets slots for backpacks only every 100 ticks: a backpack put into a Trinkets slot
  supplies blocks after up to 5 s.
- GUI checks (H5): every screen works on all four loader builds. `PlayerSettingsGui` is a stub that no key or radial
  button opens (only `ModeOptions` action `OPEN_PLAYER_SETTINGS`); its button, slider and "Done" are render-only and it
  saves nothing. The widget classes `GuiCheckBoxFixed`, `GuiNumberField`, `GuiIconButton`, `GuiScrollPane`,
  `GuiCollapsibleScrollEntry` and `SlotGui` (`gui/elements`) are used by no screen, so no player can reach them; the
  checkbox and number widgets players do use are the modifier entries' `MiniButton` and `LabeledScrollInput`
  (`client.modifier_entry_widgets`). Cosmetic: the translated titles of the leather, golden and diamond bags are wider
  than their GUI texture and run past its right edge.

## Minecraft 1.21 check (one jar for 1.21 and 1.21.1)

The release jars built from this branch (compiled against 1.21.1) were run in a Minecraft 1.21 runtime: a scratch copy
of the branch switched to the 1.21 loader/game versions, with the mod sources replaced by the release jar (its
`fabric.mod.json` / `neoforge.mods.toml` / `mods.toml` already declaring the 1.21 range) and the smoke harness
compiled against that jar.

| Loader | Runtime | Result |
|---|---|---|
| NeoForge | NeoForge 21.0.167, Sophisticated Backpacks 1.21-3.20.26.1151 + Core 1.21-0.7.13.797, no Curios (no build for NeoForge 21.0) | `runSmokeServer` 9 checks passed (1 skipped: `sb.worn_backpack`, no Curios), `runSmokeClient` 17 checks passed (same skip); every other `sb.*` check passes against the old Backpacks API |
| Fabric | Fabric Loader 0.19.5, Fabric API 0.108.0+1.21.1 (runs on 1.21), no Sophisticated Backpacks (the Fabric port needs exactly 1.21.1; the `sb_*` game tests were left out) | `runSmokeServer` 3 checks passed, `runSmokeClient` 10 checks passed |
| Forge | Forge 51.0.33 | The 1.21.1 Forge jar does not load: `NoSuchMethodException: SophisticatedBuildingForge.<init>()` (constructor injection of `FMLJavaModLoadingContext` is Forge 52+), and `AddGuiOverlayLayersEvent`/`ForgeLayeredDraw` do not exist in Forge 51. Minecraft 1.21 gets its own Forge jar from `forge-1.21/` (below) |

Hence `minecraft_version_range=[1.21,1.21.1]` (Fabric: `>=1.21 <=1.21.1`), Forge `forge_minecraft_version_range=[1.21.1]`,
NeoForge floor 21.0.167, NeoForge optional Backpacks `[3.20.26,)` / Core `[0.7.13,)` (the last 1.21 builds), Fabric
`"fabric-api": ">=0.108.0"`: the newest Fabric API tagged for 1.21 (0.102.0+1.21) has no `ClientWorldEvents` and the jar
crashed at client start with it (`NoClassDefFoundError`); with the floor Fabric Loader reports the missing update instead.
The Fabric API 1.21.1 builds before 0.108.0 lack it as well, so the floor also fixes those on 1.21.1.

Running Forge 51.0.33 in dev needs `jopt-simple` forced to 5.0.4 (its `bootstrap-api` pulls 6.0-alpha-3, module
`joptsimple`, while modlauncher requires `jopt.simple`); with that ForgeGradle 7 runs it (`forge-1.21/build.gradle`).

### Forge 1.21 (`forge-1.21/`)

`forge-1.21/` builds `sophisticatedbuilding-forge-1.21-4.3.0.jar` (Minecraft `[1.21]`, Forge `[51.0.33,)`) from
`../forge` with the classes Forge 51 cannot run replaced (README.md, "Forge 1.21"). Its smoke runs use the same
scenarios and check names as `forge/` (no `sb.*`: no Sophisticated Backpacks for Forge 1.21):

- `runSmokeServer`: 3 checks passed (`server.place_line_survival`, `server.undo_redo`, `server.no_mod_errors`).
  Forge 51's game test server never runs the server start hooks, so the mod's SERVER config stayed unloaded and
  both scenarios failed with "Cannot get config value before config is loaded"; the harness glue
  (`forge-1.21/src/smoketest/.../ForgeSmokeTest`) now runs `ServerLifecycleHooks.handleServerAboutToStart` for the game
  test server before its first tick, as the dedicated server does.
- `runSmokeClient`: 10 checks passed, the client checks of `forge/`. The HUD (build hints, block counts, power level)
  is drawn by `GuiMixin` after vanilla's HUD; the screenshots show it.
- `runServer`: "Done", no Sophisticated Backpacks, no warning or error from the mod.
