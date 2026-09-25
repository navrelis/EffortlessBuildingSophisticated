# R2 combined port guide (GUI + gameplay), reference branch mc/1.21.1

Reference = `wip/r2play-1.21.1` (worktree `local/wt/r2play-1.21.1`), which the lead fast-forwards `mc/1.21.1` to.
It holds both R2 parts on top of fb10ef2 (`git -C local/wt/r2play-1.21.1 show <hash>` shows every file):
- R2-GUI (r2gui): 664805a, b9fec27, 059934c, f3940e5, 7248698, 71eae62; r2gui-b 8facc98 (terrain option icons)
- R2-play (r2play): fdba1b8 (server fixes), 8cf025a (Disable + Quick Replace preview), f271f6b (docs)
- merges 44cb166 (conflict: TESTING.md test counts), 179222b (8facc98 terrain icons, no conflict), 16b7426 (0d22b14
  radial side button layout; conflicts: TESTING.md counts, release jars), 0533430 (7ee7d88 smoke client never touches
  the OS cursor, no conflict)
- a0ddcab: dead `renderMiniBlockPreviews` deleted, smoke check `client.mini_block_preview`, changelog/README
- final commit: release jars + SHA256SUMS

Port order per branch: the GUI commits (b9fec27 is needed by the harness, which sets config values), 8facc98,
then fdba1b8, 8cf025a, f271f6b, then a0ddcab's code/doc parts; release jars last.

Verified on 1.21.1 with everything merged (up to 0533430): unit tests Fabric 104 / NeoForge, Forge, Forge 1.21 90 (0 failures);
Fabric runGametest "All 24 required tests passed"; runSmokeServer Fabric 11 (1 skip: refused_place_not_charged, no
place event), NeoForge 11/11, Forge 5/5, Forge 1.21 5/5; runSmokeClient (final code) Fabric 24/24, Forge 16/16, Forge 1.21 16/16; NeoForge 23/23 before the 8facc98 merge (its final-code run was stopped by the lead: no clients while the harness moves the OS cursor), so NeoForge still needs one runSmokeClient.
Release jars: no smoketest/gametest classes, no nested jars.

Check counts after porting everything (SB loaders / no-SB loaders): client 24 / 16 (incl. radial_option_icons,
mini_block_preview, disable_quick_replace_preview), server 11 / 5, GameTests 24, unit tests +25 on common
(65 -> 90; Fabric +12 ConfigSpecTest +2 r2gui = 104).

## GUI: Commits and files

| Commit | What | Files |
|---|---|---|
| 664805a | delete 6 dead widgets | `common/.../gui/elements/{GuiCheckBoxFixed,GuiCollapsibleScrollEntry,GuiIconButton,GuiNumberField,GuiScrollPane,SlotGui}.java` (nothing else used only by them; branches 1.21.10+ already deleted 3 of them) |
| b9fec27 | config set/save API | common: `config/ConfigValue.java` (+`set`, `getDefault`, no longer @FunctionalInterface), new `config/NumberConfigValue.java` (`getMin/getMax`), `config/IConfigBuilder.java` (`defineInRange` returns `NumberConfigValue`), `platform/services/IConfigHelper.java` (+`save(Object spec)`), `ClientConfig.java` (number fields typed `NumberConfigValue`, `save()`); fabric: `config/SimpleConfigValue.java` (IntValue/DoubleValue implement NumberConfigValue, boxed getMin/getMax), `config/ConfigSpec.java` (defineInRange returns IntValue/DoubleValue), `config/ConfigFile.java` (+`save`), `config/ModConfigs.java` (+`save`), `fabric/platform/FabricConfigHelper.java` (+`save`), `src/test/.../ConfigSpecTest.java` (+2 tests); neoforge `neoforge/platform/NeoForgeConfigHelper.java`, forge `forge/platform/ForgeConfigHelper.java` (Value/NumberValue adapters, `save` = `spec.save()`) |
| 059934c | PlayerSettingsGui + opening | common: `gui/buildmode/PlayerSettingsGui.java` (rewritten), new `gui/SliderValues.java`, `ClientEvents.java` (`PLAYER_SETTINGS_KEY = 6`, array size 7, key handler), `gui/buildmode/RadialMenu.java` (button at `-buttonDistance - 52, -39`; `findKeybind` index 6 and skip unbound keys), `buildmode/ModeOptions.java` (icon `I_PLAYER_SETTINGS`), `AllIcons.java` (`I_PLAYER_SETTINGS = next()` after `I_TERRAIN_WALL` = row 4 col 8), `textures/gui/icons.png` (slider icon at x=128,y=64), `lang/en_us.json` (key, action name/description, `sophisticatedbuilding.player_settings.*`), `render/BlockPreviews.java` (mini preview reads/writes the config, `onConfigChanged()`, cache refresh before the limit check), new test `SliderValuesTest.java` |
| f3940e5 | bag titles | new `gui/TitleFit.java` (pure), new `gui/BagTitle.java`, the 4 `gui/*RandomizerBagScreen.java` (`renderLabels` -> `BagTitle.draw`, `render` -> `BagTitle.renderTooltip`), the 4 `item/*RandomizerBagItem.java` (`ContainerProvider#getDisplayName` returns `bag.getHoverName()`), `gui/elements/LabeledScrollInput.java` (scale down numbers wider than the field), new test `TitleFitTest.java` |
| 7248698 | smoke harness | `smoketest/client/GuiScenarios.java` (new `playerSettingsGui`, `titleFits`, `renamedBagTitle`, config file probe), `ClientDriver.java` (+`dragTo`), `RadialMenuDriver.java` (+`hoverLeftButton`), `ClientScenarios.java` (`new GuiScenarios(driver, radial)`) |
| 71eae62 | docs + release | `README.md` (section "Player settings (client config)"), `TESTING.md` (check rows, "Since R2" port list, findings), `changelog/PATCH_NOTES_4.3.0.md` (Player Settings section, Fixes section, Forge placeholder removed - adapt the fixes list to what the branch really has), `<loader>/release/*` via `release.ps1` |

Merge note: the gameplay agent (R2-play) also touches 1.21.1; overlap risk only in `render/BlockPreviews.java`
(my hunks: fields/onTick top, line ~113 `isMiniBlockPreviewEnabled()`, renderBlockPreviews cache refresh, the
toggle/is methods at the end) and possibly the item classes.

## GUI: Behaviour to keep identical on every branch

- Settings (key = config key, lang `sophisticatedbuilding.player_settings.<key>` + `.tooltip`): Visuals
  showBlockPreviews, onlyShowBlockPreviewsWhenBuilding, showMiniBlockPreview (buttons); maxBlockPreviews (step 64,
  exponent 3), appearAnimationLength, breakAnimationLength (step 1), previewScale (step 0.05); Performance
  previewRenderDistance (step 8), enableUpdateThrottling (button), maxMiniBlockPreviews (step 64, exponent 3). If a
  branch's ClientConfig differs, list the difference.
- Changes apply at once (`ConfigValue#set`), `removed()` saves when something changed (Done, Escape, the key, any
  other screen). `isPauseScreen()` false (the smoke harness needs the integrated server running while it is open).
- Radial: button directly above Modifier Settings; tooltip keybind only when bound. Key unbound by default.
- Bag title: available width = imageWidth - 16, scale down to 0.6, then ellipsis + tooltip on hover.

## GUI: APIs that differ by Minecraft / loader version (check with javap on each branch)

GUI rendering
- 1.20+: `GuiGraphics` (`drawString`, `drawCenteredString`, `pose()`, `renderTooltip(Font, Component, x, y)`).
  1.16-1.19.4: `PoseStack` + `GuiComponent.drawString/drawCenteredString(PoseStack, Font, ...)` or `font.draw/drawShadow`,
  `Screen#renderTooltip(PoseStack, Component, x, y)`; `renderLabels(PoseStack, int, int)`.
- 1.21.6+: `GuiGraphics#pose()` is a 2D `Matrix3x2fStack` (`pushMatrix/popMatrix/translate(x, y)/scale(x, y)`, no
  `pushPose`); `drawString` colors are ARGB (a 0x404040 title is invisible - use 0xFF404040); tooltips from screens
  go through `GuiGraphics#setTooltipForNextFrame` (Screen#setTooltipForNextRenderPass is gone - check).
- Before 1.20.2 `Screen#render` does not draw the background: call `renderBackground(...)` first in
  `PlayerSettingsGui#render`. 1.20.5+ lists draw the menu list background/separators; older lists draw dirt unless
  `setRenderBackground(false)`/`setRenderTopAndBottom(false)` (1.16-1.20.4).
- Label tooltip: 1.21.1 `setTooltipForNextRenderPass(Tooltip, DefaultTooltipPositioner.INSTANCE, true)` (1.20+);
  1.19.4: `setTooltipForNextRenderPass(List<FormattedCharSequence>)`; older: draw it in `render` after super.

Widgets
- `Button.builder(...).bounds(...).tooltip(...).build()`, `Tooltip.create`, `AbstractWidget#setTooltip`: 1.19.3+. Older:
  `new Button(x, y, w, h, Component, OnPress[, OnTooltip])` and tooltip rendering by hand.
- `AbstractSliderButton(int x, int y, int w, int h, Component, double value)` with protected `value`,
  `updateMessage()`, `applyValue()` exists 1.16+ (1.16 name `AbstractSliderButton` in Mojang mappings). Track =
  `x + 4 .. x + width - 4`. 1.21.9+: `keyPressed(KeyEvent)`, `mouseClicked(MouseButtonEvent, boolean)`,
  `onDrag(MouseButtonEvent, double, double)` - adapt the overrides.
- `ContainerObjectSelectionList`: 1.20.3+ ctor `(Minecraft, width, height, y, itemHeight)`; 1.17-1.20.2
  `(Minecraft, width, height, y0, y1, itemHeight)`; 1.16 `ContainerObjectSelectionList` exists with the same 6-arg ctor
  (no `narratables()` before 1.17). `getScrollbarPosition()` is `scrollBarX()` from 1.21.4 (AbstractScrollArea),
  `getRowWidth()` stays. 1.21.9+: `Entry#renderContent(GuiGraphics, mouseX, mouseY, hovered, partialTick)` with
  `getContentX/Y/Width/Height()` instead of `render(..., index, top, left, width, height, ...)`.
- `AbstractWidget#setWidth/setX/setY/getX/getY`: 1.19.4+; before `x`/`y` public fields, `setWidth` exists.
- `CommonComponents.optionStatus(boolean)`, `OPTION_OFF`, `GUI_DONE`: 1.16+. `Component.translatable/literal`: 1.19+;
  1.16-1.18 `new TranslatableComponent(...)`, `new TextComponent(...)`.
- `Font#plainSubstrByWidth(String, int)`: 1.16+. `Font#width(String)`: all.

Input, keys (smoke harness + key)
- Key registration: nothing per loader - every loader registers the whole `ClientEvents.keyBindings` array; only the
  array size and index 6 change. `KeyMapping#isUnbound()`, `setKey`, `KeyMapping.resetMapping()`: 1.16+.
- NeoForge/Forge `matchesKey` compares the key code, so the screen's own key closes it even though the
  mapping's conflict context is IN_GAME.
- `MouseHandler#onPress/onScroll` (private, reflection) until 1.21.8; 1.21.9+ input records. Drag: 1.21.1 routes
  cursor moves through `handleAccumulatedMovement` (focused window only), so `ClientDriver#dragTo` calls
  `Screen#mouseMoved/mouseDragged` itself; older versions called them from `onMove` - `dragTo` works unchanged there.
  1.21.9+: `Screen#mouseDragged(MouseButtonEvent, double, double)`.
- Renamed bag in the harness: `ItemStack#set(DataComponents.CUSTOM_NAME, Component)` 1.20.5+; before
  `ItemStack#setHoverName(Component)`.

Config set/save (per loader)
- NeoForge: `ModConfigSpec.ConfigValue#set/getDefault`, `ModConfigSpec#save()` (checked on 21.0.167 and 21.1.251).
  NeoForge 20.2-20.4 branches: check the class (`ModConfigSpec` from 20.4, older NeoForge still `ForgeConfigSpec` in
  `net.neoforged.neoforge.common`) and that `save()` exists; else `ConfigValue#save()` per value.
- Forge: `ForgeConfigSpec.ConfigValue#set/getDefault`, `ForgeConfigSpec#save()` (checked on 51.0.33, 52.1.2); verify on
  Forge 36-47 with javap (FG6/MDG Legacy caches) - `ConfigValue#save()` is the fallback.
- Fabric: the branch's own `ConfigSpec` JSON backend (`ConfigFile.save`). Branches whose Fabric config backend differs
  (e.g. Forge Config API Port) must implement `save` with that backend.
- ClientConfig must be loaded when the screen opens (it is, client init on every loader).

Other
- `AllIcons.render` uses `guiGraphics.blit(ATLAS, ...)` (1.21.1); the icon only needs the atlas cell. Compare the
  branch's `icons.png` with 1.21.1's before copying it (1.21.1's differs only in the new cell); otherwise redraw with
  `scratchpad/r2gui/draw_icon.ps1 -Atlas <png> -Col 8 -Row 4`. Row 4 cols 1-7 (terrain icons) are empty in the 1.21.1
  atlas already - pre-existing, not part of R2.
- `AbstractContainerScreen` fields `imageWidth/leftPos/topPos`, `Screen#getTitle`: all versions (Mojang names).
- Bag menus: `MenuProvider#getDisplayName` -> `bag.getHoverName()` works on all versions.

## GUI: Follow-up r2gui-b (commit 8facc98): terrain option icons

- `icons.png` row 4, columns 1-7 (`I_TERRAIN_NOISE_OFF/ON`, `I_TERRAIN_MOUND/SLOPE/FLAT/MOUNTAIN/WALL`) were empty in every
  version of the file in the history (blob 20e0fb8 since v1.0.0) and showed as blank radial option buttons for Terrain
  Mound. Drawn with `scratchpad/r2gui/draw_terrain.ps1` (row 4 cols 1-7), the player settings icon with `draw_icon.ps1`
  (row 4 col 8).
- Every other branch (mc/1.16.3 ... mc/26.2, incl. mc/1.19.4) still has the original blob 20e0fb868d8f and the same
  `AllIcons` layout, so 1.21.1's `icons.png` (blob 74e46be56a31) can be copied over unchanged; add
  `I_PLAYER_SETTINGS = next()` after `I_TERRAIN_WALL` in `AllIcons` as in 059934c.
- `RadialMenu#drawSideButtonBackgrounds`: `isSelected` now also compares with `ModeOptions.getTerrainNoise()` and
  `getTerrainType()` (the active terrain options were never highlighted).
- Harness: new check `client.radial_option_icons` (`GuiScenarios#radialOptionIcons`, `RadialMenuDriver#hoverOptionButton`,
  `hoverNothing`, pointer follows the menu's tracked mouse). Version-dependent: `NativeImage.read(InputStream)`,
  `NativeImage#getPixelRGBA` (renamed `getPixel` in 1.21.2+, ARGB there), `ResourceManager#open(ResourceLocation)`
  (1.19+; before `getResource(rl).getInputStream()`), the `AllIcons` private fields `iconX/iconY`, `BuildModeEnum#options/icon`,
  `ModeOptions.OptionEnum#actions`, the option button layout `buttonDistance + column * 26, -13 + row * 39`.
  Client check counts become 22 (SB loaders) / 14.

## Gameplay: What changed, file by file

common/src/main
- utilities/ReplaceRules.java: new `Action.UNMERGE`; `forUndo(...)` has a 7th parameter `currentIsMerged`
  (returns UNMERGE right after the old-is-air BREAK check, for survival and creative, any replace config);
  new `unmergeRefund(currentCount, oldCount)`; restoreCost javadoc now says it prices every placement.
- systems/ServerBlockPlacer.java
  - applyBlockEntry: `count = breaking ? 1 : restoreCost(action, existing, new)` for builds too (was 1 unless redo).
    Param `restoring` now only means "redo": an entry whose block already has the target state returns true (done).
  - placeIfAvailable/placeWithTemplate: a failed placement is ALWAYS taken back from the usage count
    (`uncountOnFailure` parameter removed); on success the individual template is shrunk as before.
  - new `unmerge(player, entry, merged, survival)`: BlockPlacerHelper.placeBlock(old state), then survival gets
    `unmergeRefund(...)` items of `merged.getBlock().asItem()` via ItemHandlerHelper.giveItemToPlayer.
  - undoBlockEntry: `isAlreadyThere(current, old)` -> done (block.newBlockState = current); forUndo gets
    `!breaking && BlockUtilities.isOneStepMerge(old, current)`; UNMERGE case in the switch; the "breaking"
    flag for validateBlockEntry is now `action == BREAK || action == REPLACE`.
  - new static `isAlreadyThere(current, target)` (target null/air -> current.isAir(), else identity).
  - import sophisticated.building.inventory.ItemHandlerHelper.
- utilities/BlockPlacerHelper.placeBlock: returns `allowed && placed` where `placed` is the boolean the
  placement lambda gets from BlockHelper.placeSchematicBlock (captured in a boolean[1]).
- create/foundation/utility/BlockHelper.java: the 6-arg `placeSchematicBlock` returns boolean (placed);
  the ultra-warm water branch returns false and NO LONGER calls `Block.dropResources` (it would dupe now that
  the item is not charged); `placeRailWithoutUpdate` returns `old != state`; special plantables compare the
  state before/after; `if (!placed) return false;` before data/setPlacedBy.
- platform/services/IBlockEventHelper.java: javadoc of placeBlock only.
- render/PreviewRules.java (new, pure) + render/BlockPreviews.drawLookAtPreview uses it instead of the two
  single-block `return`s.

fabric/src/main: FabricBlockEventHelper.placeBlock just runs the placement and returns true (it used to
compare states; the "placed" result now comes from placeSchematicBlock). Forge/NeoForge helpers unchanged.

Tests
- common/src/test: ReplaceRulesTest (+5 tests; every existing forUndo call gets `, false`), PreviewRulesTest (5).
  Common unit tests 65 -> 75 (Fabric 87 with ConfigSpecTest).
- fabric/src/gametest: MergeUndoGameTest (3), ChargeGameTest (3), UndoGameTest.undoOfAnAlreadyRemovedBlockIsDone,
  both new classes listed in src/gametest/resources/fabric.mod.json. 17 -> 24 GameTests.
- common/src/smoketest: ServerScenarios.server_merge_undo_refund / server_refused_place_not_charged
  (+ helpers expectStates/expectItems), SmokeServerPlatform.refusePlacementsAt(Set<BlockPos>) default false,
  ClientScenarios.disableQuickReplacePreview (uses lane(9), Outliner "single" entry, BuildSettings.setReplaceMode).
- Loader smoke glue: Forge/NeoForge SmokeServerPlatform implement refusePlacementsAt with a static set + one
  lazily added EntityPlaceEvent listener that cancels at those positions; the three *SmokeServerTests classes
  register the two new server tests (batches smoke_merge, smoke_refused).

## Gameplay: Version-specific APIs to watch

- Count properties (BlockUtilities.COUNT_PROPERTIES, unchanged here): LAYERS (snow, all versions), PICKLES and
  EGGS (1.13+), CANDLES (1.17+, drop on 1.16.x), FLOWER_AMOUNT (pink petals 1.19.4/1.20+; wildflowers on 1.21.5+
  also use it). 1.21.5+ leaf litter uses SEGMENT_AMOUNT, which is NOT in COUNT_PROPERTIES on any branch (pre-existing;
  its merge is not recognised as a merge). MergeUndoGameTest: drop candles before 1.17, pink petals before 1.20.
- SlabType / BlockStateProperties.SLAB_TYPE: same since 1.13.
- Ultra warm water check: `world.dimensionType().ultraWarm()` up to 1.21.10;
  26.x uses `world.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, target)`. Only remove
  the `Block.dropResources(...)` line and return false; keep the branch's condition.
- `Level.setBlock(...)` returns boolean on every version (Forge/NeoForge snapshot capture also returns true when
  a snapshot was taken). ItemStack/BlockState identity (`==`) is canonical everywhere.
- Place events used by the smoke hook: Forge `BlockEvent.EntityPlaceEvent` (`MinecraftForge.EVENT_BUS.addListener`,
  `setCanceled(true)`); on Forge 1.21.6+ (EventBus 7) use the event's own BUS and a cancelling listener
  (`BlockEvent.EntityPlaceEvent.BUS.addListener(true, event -> REFUSED.contains(event.getPos()))` or whatever
  that branch's ForgeCommonEvents does). NeoForge (1.20.2+) `net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent`
  on `NeoForge.EVENT_BUS`; NeoForge 1.20.1 is Forge-style (`net.neoforged.neoforge` does not exist there).
  Forge 1.16-1.18 package: `net.minecraftforge.event.world.BlockEvent` (renamed to `event.level` in 1.19).
- Fabric has no block place event: the smoke check is skipped there by design.
- ChargeGameTest (Fabric): `ServerPlayer.setServerLevel(ServerLevel)` (1.20+; 1.18/1.19 `setLevel`, 1.16/1.17
  `setLevel` too); `Blocks.SHORT_GRASS`/`Items.SHORT_GRASS` are `GRASS` before 1.20.3; `Level.NETHER` is
  `World.NETHER`-style only in yarn/older names (Mojmap has Level.NETHER everywhere).
- GameTests on 1.21.5+: the new framework (test instances): add the two server smoke scenarios to
  `SmokeServerTests.functions()` and create `data/sophisticatedbuilding/test_instance/server_merge_undo_refund.json`
  and `server_refused_place_not_charged.json` (+ one `test_environment` each, like smoke_1/smoke_2). The 1.21.5+
  test structures are NOT cleared between runs: the scenarios place their own dirt below the snow/candle, which is
  fine, but a kept world (Forge 1.21.5 before commit 2961a48) would already have the merged blocks.
- GameTestSequence.thenExecute in 1.21.1 catches an assertion, fails the test and CONTINUES with the next event,
  so the reported failure of a sequence is the LAST failing assertion (the refused scenario before the fix reported
  "after undo 62" although "left 61" had failed first).
- Client check: `Outliner.getInstance().getOutlines()` / `OutlineEntry.isFading()` come from the vendored Catnip
  outliner (same on every branch). `RadialMenuDriver.select(BuildModeEnum.DISABLED)` must exist on the branch.

## Gameplay: Behaviour decisions (for the lead)
- Undo of a merge never mines, in any game mode or replace setting; creative gets no refund.
- A build now charges the full item count of the state it places (restoreCost) instead of 1 per entry. Normal
  merges still cost 1; a multi-item state onto air/another block costs all its items (all or nothing).
- Undo/redo of an entry whose block is already in the target state counts as done (no charge) and leaves the stack.
- Disable + Quick Replace shows the single-block preview regardless of `onlyShowBlockPreviewsWhenBuilding`
  (a Disable click is always idle); the action bar then shows "1 blocks (1x1x1)" like other modes. Single mode +
  Quick Replace is unchanged (still follows the config).
- Not changed: the server does not check entity collision (vanilla refuses a block where an entity stands); undo in
  survival of blocks built in creative mines them with drops (only relevant for players who switch game modes).

## Follow-up after the merge (a0ddcab, touches both parts)
- `render/BlockPreviews.java`: the dead `renderMiniBlockPreviews` (0.5-scale ghosts, never called) is deleted. The
  mini block previews ARE the ghosts `renderBlockPreviews` draws at `previewScale` inside each outlined block;
  `drawLookAtPreview` draws them only with `showMiniBlockPreview` (r2gui: read from the config each frame) and
  `renderBlockPreviews` skips them when the set has more blocks than `maxMiniBlockPreviews` (0 = no limit; cached,
  `onConfigChanged()` refreshes). Delete the dead method on every branch that still has it.
- Smoke check `client.mini_block_preview` (ClientScenarios, right after `client.buildmode_line_preview`): counts the
  ghosts at the 5 line positions through reflection on `GhostBlocks.ghosts` (package-private `Map<Object, Entry>`,
  keys `BlockPos#toShortString()`): 5 by default, 0 with showMiniBlockPreview off, 0 with maxMiniBlockPreviews 4.
  Needs the config `set` API of b9fec27. Screenshots `mini_preview_on`, `mini_preview_off`.
- Changelog: the Fixes section lists the R2-play fixes after r2gui's entries; adapt to what the branch has (no
  candles before 1.17, no pink petals before 1.20, no place event on Fabric).
- Merge conflict to expect when both parts land on a branch separately: only the TESTING.md test-count lines.

## Later mc/1.21.1 commits by h5b (merged, not described in r2gui's notes; see `git show`)
- 0d22b14 fix(gui): radial side buttons always inside the window (new pure `gui/buildmode/RadialButtonLayout` +
  `RadialButtonLayoutTest` (+4 common unit tests), `RadialMenu` uses it and `sideButtons()` for the harness; power level
  tooltip only when nothing is hovered; `client.radial_option_icons` asserts the layout for every mode).
- 7ee7d88 test(smoke): the smoke client never touches the OS cursor or the focus (`ClientWindow`; Fabric smoke
  source set gets `SmokeWindowMixin` + `sophisticatedbuilding_smoketest.mixins.json`). Port before running any
  client on a branch (user requirement).
