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

## Process
- [ ] One commit per task group with the trailer; all pushed (`git log origin/main..main` empty).
- [ ] `07_SESSION_LOG.md` has the implementer notes; deviations are explained.
- [ ] Knowledge graph rebuilt after the code changes.
