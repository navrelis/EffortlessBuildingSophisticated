# R2-GUI port notes (reference: mc/1.21.1, commits 664805a..71eae62, base fb10ef2)

Port the six commits below onto each branch (1.16.3 ... 26.2). `git -C versions/1.21.1 show <hash>` shows every file.
Verified on 1.21.1: build (Fabric 90 tests = 77 + 13, NeoForge/Forge/Forge 1.21 76 = 65 + 11), Fabric runGametest
17/17, runSmokeServer 9/9 fabric+neoforge, 3/3 forge+forge-1.21, runSmokeClient 21/21 fabric+neoforge (8 sb.*),
13/13 forge+forge-1.21.

## Commits and files

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

## Behaviour to keep identical on every branch

- Settings (key = config key, lang `sophisticatedbuilding.player_settings.<key>` + `.tooltip`): Visuals
  showBlockPreviews, onlyShowBlockPreviewsWhenBuilding, showMiniBlockPreview (buttons); maxBlockPreviews (step 64,
  exponent 3), appearAnimationLength, breakAnimationLength (step 1), previewScale (step 0.05); Performance
  previewRenderDistance (step 8), enableUpdateThrottling (button), maxMiniBlockPreviews (step 64, exponent 3). If a
  branch's ClientConfig differs, list the difference.
- Changes apply at once (`ConfigValue#set`), `removed()` saves when something changed (Done, Escape, the key, any
  other screen). `isPauseScreen()` false (the smoke harness needs the integrated server running while it is open).
- Radial: button directly above Modifier Settings; tooltip keybind only when bound. Key unbound by default.
- Bag title: available width = imageWidth - 16, scale down to 0.6, then ellipsis + tooltip on hover.

## APIs that differ by Minecraft / loader version (check with javap on each branch)

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

## Follow-up r2gui-b (commit 8facc98): terrain option icons

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

## Follow-up r2gui-c (commit 0d22b14): radial side buttons inside the window, power level tooltip

- New pure class `gui/buildmode/RadialButtonLayout.java` + `common/src/test/.../RadialButtonLayoutTest.java` (4 tests;
  common tests become 69 + 11 = 80, Fabric 94). Copy both unchanged (no Minecraft classes).
- `RadialMenu`: the hard-coded side button positions are replaced by two `RadialButtonLayout.layout(...)` calls (left:
  rows {player settings at column 2}, {redo, undo, modifier settings, mini preview[, protect]}, [{offhand, only
  blocks, blocks and air, only air}], first row y -39, pitch 26; right: one row per build mode option, first row y -13,
  pitch 39, 14 px label headroom, 24 px reserved at the bottom for the power level text), `minSideButtonInner() =
  ringOuterEdge + 20`, option labels at `optionLayout.inner() - 9` / `rowFirstY - 24` clamped to the window,
  `SideButton` record + `sideButtons()` (for the harness), power level tooltip only when `doAction == null &&
  switchTo == null`. At wide windows the positions are exactly the old ones (unit test).
- Branches whose RadialMenu differs (26.x rendering rewrite, 1.16-1.19 PoseStack) only need the button-list block and
  the label block adapted; the layout class is version-independent.
- Harness: `RadialMenuDriver#hoverButton(action)` (uses `RadialMenu#sideButtons()`, replaces `hoverLeftButton` /
  `hoverOptionButton`), `GuiScenarios#sideButtonsInsideForEveryMode` (switches `BUILD_MODES.setBuildMode` with the
  menu open, asserts inside window / clear of the ring / no overlaps). Fabric smoke: 123 side buttons of 15 modes
  inside 427x240.

## MUST GO FIRST on every branch before any client runs there: smoke client never touches the OS cursor (commit 7ee7d88)

Cause: `MouseHandler#grabMouse` (world join, every `setScreen(null)`, a click with no screen) -> `InputConstants.grabOrReleaseMouse`
-> `glfwSetInputMode(GLFW_CURSOR_DISABLED)` + `glfwSetCursorPos(window centre)`; `releaseMouse` (every screen open) warps it
again. `grabMouse` only acts while `Minecraft#isWindowActive()`; a freshly created GLFW window takes the focus.

Change (copy from mc/1.21.1):
1. `common/src/smoketest/.../client/ClientWindow.java` (whole file): `detachInput` also removes
   `glfwSetCursorEnterCallback` and `glfwSetWindowFocusCallback`; new `keepOffTheCursor(Minecraft)` called from
   `muteAndMoveAside`: reflection `Minecraft#windowActive = false`; if `mouseHandler.isMouseGrabbed()`: reflection
   `MouseHandler#mouseGrabbed = false` + `glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)` (no cursor pos);
   `glfwSetWindowAttrib(window, GLFW_FOCUS_ON_SHOW, GLFW_FALSE)`; on Windows `User32.SetWindowLongPtr(hwnd, GWL_EXSTYLE,
   style | WS_EX_NOACTIVATE)` with `hwnd = GLFWNativeWin32.glfwGetWin32Window(window)` (LWJGL core/glfw, no extra dependency).
   Keep `options.pauseOnLostFocus = false` (already in `ClientScenarios#joinFreshWorld`).
2. Fabric only: `fabric/src/smoketest/java/.../fabric/mixin/SmokeWindowMixin.java` (@Inject into `Window.<init>` at INVOKE
   `GLFW.glfwCreateWindow(IILjava/lang/CharSequence;JJ)J`, remap=false: `glfwWindowHint(GLFW_FOCUSED, FALSE)` and
   `glfwWindowHint(GLFW_FOCUS_ON_SHOW, FALSE)` when `SmokeTest.isClientMode()`), `fabric/src/smoketest/resources/
   sophisticatedbuilding_smoketest.mixins.json` ("client": ["SmokeWindowMixin"]), `"mixins": [...]` in the harness
   `fabric.mod.json`. Dev runtime only (named mappings, no refmap needed); never in the release jar.
3. Version notes: field names `windowActive` (Minecraft) and `mouseGrabbed` (MouseHandler) are Mojang names on every
   version 1.16-26.x; a branch whose dev runtime is not Mojang-named (Forge 1.16-1.20.x with SRG at runtime? check the
   run's mapping) needs the runtime field name (use ObfuscationReflectionHelper / the SRG name). `glfwSetWindowAttrib`
   needs GLFW 3.3 (LWJGL 3.2.2+, all branches). `GLFW_FOCUS_ON_SHOW` is GLFW 3.3 too. The Window constructor on
   1.16-1.20 has other parameters but the same `glfwCreateWindow` call; 26.x may differ (javap).
4. NeoForge/Forge: `keepOffTheCursor` applies (no grab, no warp, not activatable), but their window is created by FML's
   early loading screen before any mod code, so the initial focus is not controlled by the mixin; set
   `earlyWindowControl = false` in `<smoke client run dir>/config/fml.toml` if the initial focus steal must go too
   (not done on 1.21.1; NeoForge/Forge clients were not run after this change - Fabric proof only).

Proof on 1.21.1 fabric: OS cursor recorded every 100 ms (scratchpad/r2gui/cursor-recorder.ps1, 766 samples, cursor.csv):
clip rect never changed, foreground process never the game, position constant from before window creation through
world join and all GUI checks; runSmokeClient 22/22.

### MUST GO FIRST, part 2 (commit e0a6e93): NeoForge/Forge focus (replaces point 4 above)

FML's early loading window (NeoForge FML 4.x, Forge 51/52) is created before any mod code and takes the focus.
- `gradle/smoketest.gradle`: new `disableEarlyWindow(Task, File gameDir)` - doFirst writes `earlyWindowControl = false` into
  `<gameDir>/config/fml.toml` (other keys kept). Key name `earlyWindowControl` in NeoForge FMLConfig 4.0.x and Forge
  51/52 FMLConfig (checked with javap); check older FML versions.
- neoforge/build.gradle: `tasks.named('runSmokeClient') { verifySmokeRun(...); disableEarlyWindow(it, file('build/smoketest/client-run')) }`;
  forge/ and forge-1.21/build.gradle: same inside `tasks.matching { it.name == 'runSmoketestClient' }.configureEach`.
- The game then creates its window in `Window.<init>` via `ImmediateWindowHandler.setupMinecraftWindow` (FML DummyProvider).
  Harness mixin `SmokeWindowMixin` (neoforge/src/smoketest/.../neoforge/mixin, forge/src/smoketest/.../forge/mixin):
  @Inject(method="<init>", at=@At(value="INVOKE", target="L<net/neoforged|net/minecraftforge>/fml/loading/ImmediateWindowHandler;
  setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",
  remap=false)) -> `SmokeWindowHints.beforeWindowCreation()` (new, common/src/smoketest/.../client; the Fabric mixin uses it too).
  Mixin json `sophisticatedbuilding_smoketest.mixins.json` in each loader's src/smoketest/resources ("client" only).
- Registration: NeoForge harness `neoforge.mods.toml`: `[[mixins]] config="sophisticatedbuilding_smoketest.mixins.json"`.
  Forge/Forge 1.21: `args "--mixin.config=sophisticatedbuilding_smoketest.mixins.json"` in the smoke client run's
  `with(sourceSets.smoketest)` block (forge-1.21 shares forge/src/smoketest; Forge 51 has the same Window path).
- Older branches without FML's early window (and Fabric everywhere) create the window with a vanilla `glfwCreateWindow`
  call in `Window.<init>`: use the Fabric-style target. SRG/intermediary runtimes (Forge 1.16-1.20.x) may need a refmap.
- Verified on 1.21.1: builds/tests, runSmokeServer neoforge 9/9 + forge 3/3; NeoForge client log shows "ImmediateWindowProvider
  not loading because splash screen is disabled" and "Mixing SmokeWindowMixin ... into com.mojang.blaze3d.platform.Window",
  window never foreground. Full NeoForge/Forge cursor proof runs not completed (foreign clients, see report).
