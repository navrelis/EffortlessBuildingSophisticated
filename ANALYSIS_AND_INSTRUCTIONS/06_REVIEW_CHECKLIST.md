# 06 – Reviewer checklist (Fable, after the implementer reports)

Evidence before assertions. For each item, read the diff and the command output; do not trust the summary.

## Build and tests
- [ ] Fabric: `gradlew build` and `gradlew test` green; `remapJar` produced `build/libs/sophisticatedbuilding-fabric-4.1.0.jar`.
- [ ] NeoForge: `gradlew build` green, `sophisticatedbuilding-neoforge-4.1.0.jar` produced.
- [ ] `ExportedJars/` contains both 4.1.0 jars and `PATCH_NOTES_4.1.0.md`.
- [ ] Smoke test log excerpt shows SB/SC/SS loaded, `Registered Sophisticated Backpacks upgrade containers`, no `NoSuchMethodError`/`NoClassDefFoundError`/`Failed to`.

## RC4 – dependencies
- [ ] `build.gradle` has the three correct CurseMaven ids (979317:7344653, 979322:6844426, 979326:7344673); no 7147929 anywhere.
- [ ] No `compileOnly(fileTree(...))` of un-remapped jars remains.
- [ ] Compiled `BuildingUpgradeHelper.class` in the remapped jar references `PlayerInventoryProvider` (javap check).

## RC3 – wrapper
- [ ] `BuildingUpgradeWrapper extends UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem>`; no `CUSTOM_DATA`/`"enabled"` handling left.
- [ ] `BuildingUpgradeContainer.handlePacket` still handles `"enabled"` and calls the inherited `setEnabled`.

## RC1 – enumeration
- [ ] `findBestBuildingUpgrade`/`findAllBuildingUpgrades` use `PlayerInventoryProvider.get().runOnBackpacks`; Fabric `CuriosCompatHelper` deleted; no `player.getInventory()` scan for backpacks remains.
- [ ] Fallback from `getTypeWrappers` to `getSlotWrappers` filtered by `isEnabled()` present.
- [ ] Client-side guard returns empty on `level().isClientSide()`.

## RC2 – sync
- [ ] `BuildingUpgradeStatePacket` registered S2C, handler stores into `ClientBuildingUpgradeState`.
- [ ] Sent on join, respawn, dimension change, and on change every 10 ticks (per-player last-sent map cleared on disconnect).
- [ ] All count packets carry unclamped totals; client clamps with synced `maxBlocks`.
- [ ] `grep -rn "BuildingUpgradeHelper\." src/main/java` shows no call reachable on the client (InventoryHelper client branches use the caches only).
- [ ] Caches cleared on `ClientPlayConnectionEvents.DISCONNECT`.

## Radial menu
- [ ] `isKeybindDown` uses `KeyBindingHelper.getBoundKeyOf`; no `GLFW_KEY_LEFT_ALT` literal in `ClientEvents`.
- [ ] `onGuiOpen` skipped for `RadialMenu`; `BuildModes.setBuildMode` cancels the chain when the mode changes (before assignment).

## Beta options
- [ ] `BuildModeEnum.LINE`/`DIAGONAL_LINE` carry `LINE_THICKNESS`; `DIAGONAL_WALL` carries `FILL`.
- [ ] `Wall`/`Floor` hollow helpers unchanged (still 1 thick); `DiagonalWall` calls the 1-thick diagonal line.
- [ ] Unit tests exist and assert the block counts stated in T7.

## Survival breaking (T-S1 … T-S9, see 08/09)
- [ ] `ToolSelector` has no Minecraft imports; `ToolSelectorTest` covers the six cases listed in T-S1 and is green; the file is identical in both projects.
- [ ] `PowerLevel.canBreakFar` = `instabuild || ServerConfig.survivalBreaking.enabled`; nothing else in the client gate changed.
- [ ] Server break path: `breakBlocks` enqueues survival sets with a capped delay; `applyBlockEntry`/`undoBlockEntry` pass candidates; `BlockPlacerHelper.breakBlock` selects the tool, passes it to `destroyBlockAs`, writes the stack back, adds exhaustion; creative path still uses `ItemStack.EMPTY`.
- [ ] Correct-tool rule: a block with `requiresCorrectToolForDrops` is never broken by a non-correct tool (read the selector call, not the summary).
- [ ] `validateBlockEntry` checks `mayInteract` + `blockActionRestricted` for survival breaking.
- [ ] `ToolSwapperIntegration` uses `getSlotWrappers().values()` + `instanceof ToolSwapperUpgradeWrapper`, `isEnabled()`, `getToolSwapMode() != NO_SWAP`, filter only when `!hideSettingsTab()`; Fabric `getSlotCount()`, NeoForge `getSlots()`; write-back via `setStackInSlot` on a copy.
- [ ] `BackpackToolsPacket` registered S2C on both loaders; sent on join/respawn/dimension change and on fingerprint change every 10 ticks; cache cleared on disconnect; client never constructs a backpack wrapper.
- [ ] Client: invalid entries flagged in BREAKING state, grey cluster rendered, HUD shows tools + barrier count, "nothing breakable" cancels without sending.
- [ ] `BuildModes.resyncToServer()` called from Fabric `ClientPlayConnectionEvents.JOIN` and NeoForge `ClientPlayerNetworkEvent.LoggingIn`.
- [ ] Vanilla-cancel hooks unchanged in expression on both loaders (survival now included by design D5).
- [ ] Patch notes still say 4.1.0 and contain the survival-breaking section, the Disable resync bullet and the config note; exported copy matches the root copy.

## Process
- [ ] One commit per task group with the trailer; all pushed (`git log origin/main..main` empty).
- [ ] `07_SESSION_LOG.md` has the implementer notes; deviations are explained.
- [ ] Knowledge graph rebuilt after the code changes.
