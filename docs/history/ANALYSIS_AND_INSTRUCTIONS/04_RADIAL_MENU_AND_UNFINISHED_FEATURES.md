# 04 – Radial menu (Alt key) and unfinished alpha/beta features

## How the radial menu works today (Fabric 1.21.1)

- `ClientEvents.onKeyPress()` runs at END_CLIENT_TICK. While `keyBindings[0].isDown()` (default Left
  Alt) and no radial menu is open, it calls `Minecraft.setScreen(RadialMenu.instance)` (a singleton
  `Screen`). If `AttachmentHandler.isDisabled(player)` it prints a chat hint instead.
- `RadialMenu.tick()` closes the screen as soon as `ClientEvents.isKeybindDown(0)` is false;
  `onClose()` then performs the highlighted action unless a mouse click already did
  (`performedActionUsingMouse`).
- Selection uses an accumulated mouse delta (patched in 4.0.0 because the OS cursor is warped to
  the window centre when a screen opens on Fabric): the first rendered frame seeds
  `accumulatedMouseX/Y` from the GUI-scaled `mouseX/mouseY`, subsequent `mouseMoved()` events add
  raw-delta × (guiScaled/screen) to it. The wedge and side buttons are hit-tested against that
  offset. The NeoForge copy is identical apart from comments.
- Layout: one wedge per `BuildModeEnum` value (15), category colour ring, left-side action buttons
  (protect tile entities, mini preview, modifier settings, undo, redo; replace-mode buttons when the
  player can replace), right-side option buttons generated from `BuildModeEnum.options`
  (`OptionEnum` rows of `ActionEnum` buttons). Power level info bottom-right with tooltip.

## Findings

### F1 – Keybind fallback hard-codes Left Alt (Fabric only)
`ClientEvents.isKeybindDown(0)` falls back to `InputConstants.isKeyDown(window, GLFW_KEY_LEFT_ALT)`
because `KeyMapping.isDown()` can desync while a screen is open. If the user rebinds the radial
key, the fallback checks the wrong key and the menu closes on the next tick (or never stays open).
The NeoForge version already does it right (`keyBindings[i].getKey().getValue()`). On Fabric the
bound key is available via `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(KeyMapping)`;
for `InputConstants.Type.MOUSE` keys use `GLFW.glfwGetMouseButton(window, value) == GLFW_PRESS`.

### F2 – Opening the radial menu cancels an in-progress build
`FabricClientEvents` calls `ClientEvents.onGuiOpen()` on any screen change, which runs
`BUILDER_CHAIN.cancel()`. Opening the wheel after the first click of a two/three-click mode (to
switch fill/thickness, or to undo) throws the build away. Decision: do not cancel when the opened
screen is the `RadialMenu`; instead cancel in `BuildModes.setBuildMode()` when the mode actually
changes (before assigning the new mode so the old mode instance is re-initialised). Changing an
option (fill, thickness, …) mid-build is then reflected on the next tick because
`findCoordinates` recomputes every tick.

### F3 – Line thickness is advertised but not implemented (beta feature)
`ModeOptions.OptionEnum.LINE_THICKNESS` with actions `THICKNESS_1/3/5`, icons `AllIcons.I_THICKNESS_*`
and lang keys exist and the documentation lists "Line Thickness (1, 3, or 5 blocks wide)" for
Line and Diagonal Line. But `BuildModeEnum.LINE` and `DIAGONAL_LINE` have the option commented
out (`/*, OptionEnum.THICKNESS*/`) and `Line.getLineBlocks` / `DiagonalLine.getDiagonalLineBlocks`
ignore thickness. This was never finished in Effortless Building either.
Specification to complete it:
- thickness t ∈ {1,3,5}, radius r = (t−1)/2.
- Axis-aligned `Line`: for each block on the line along axis A, add all offsets in the two other
  axes within [−r, r] (square cross-section, t×t). Keep `addXLineBlocks/addYLineBlocks/addZLineBlocks`
  one block thick (they are reused by Wall/Floor hollow outlines).
- `DiagonalLine`: for each sampled centre block add all offsets in [−r, r]³ (cube neighbourhood);
  duplicates are already skipped by `TwoClicks/ThreeClicksBuildMode.findCoordinates` (`blocks.containsKey`).
  `DiagonalWall` must keep calling the 1-thick variant.
- Expose pure overloads that take the thickness as a parameter so they are unit-testable
  (`ModeOptions` is client state).

### F4 – Diagonal wall has no Fill option (beta feature)
`BuildModeEnum.DIAGONAL_WALL` has `/*, OptionEnum.FILL*/` commented out; `DiagonalWall.getDiagonalWallBlocks`
always fills. Specification: HOLLOW = the diagonal line at the lowest and highest y plus the
vertical columns at the two ends of the line for the y values in between; FULL = current behaviour;
when lowest == highest both are the plain line.

### F5 – `PlayerSettingsGui` is an abandoned draft (alpha, not to be completed now)
Reachable only through `ActionEnum.OPEN_PLAYER_SETTINGS`, which no radial button uses. It shows a
"Shader type" dropdown (`DISSOLVE_BLUE/ORANGE`) whose list is populated 40× in a loop, a "Speed"
slider that stores nothing, and TODOs for persistence. There is no backend for shader selection in
the ghost-block renderer. Decision: leave the code untouched (it compiles and is unreachable);
completing it needs a product decision on what "player settings" should contain. Documented as
deferred.

### F6 – Terrain Mound (documented as ALPHA) is fully wired
`TerrainMound` (mound/slope/flat/mountain/wall + noise) is implemented, has options, icons and
lang. No functional defect found; large radii are bounded by `getMaxBlocksPerAxis` and the server
`maxBlocksPlacedAtOnce` validation. No change required.

### F7 – Not in 1.21.1 but present in the 1.21.11 draft (informational)
The git-ignored `Fabric-0.19.2-1.21.11` tree contains a `FallbackPreviewRenderer` (previews without
Catnip) and keybinding categories. Not part of this work; noted for a future backport.

### F8 – Minor
- `SophisticatedBackpacksClientIntegration` labels the upgrade tab with the lang key
  `sophisticatedbuilding.screen.modifier_settings` ("Modifier Settings"). It should have its own key
  (e.g. "Building Upgrade").
- `AbstractRandomizerBagItem` carries a TODO to re-enable an item handler capability; out of scope.
