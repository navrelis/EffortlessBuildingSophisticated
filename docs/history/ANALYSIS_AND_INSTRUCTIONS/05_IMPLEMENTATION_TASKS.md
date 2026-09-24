# 05 – Implementation tasks (contract for the implementing agent)

Read `01`–`04` first. Work strictly in this order. After each task: build, run the checks listed,
commit with the given message prefix, push. Do not start the next task with a red build.
Do not widen scope. If something in these instructions turns out to be impossible against the real
API (compile error), stop that task, write the exact error and the alternative you propose into
`07_SESSION_LOG.md` under "Implementer notes", and continue with the next independent task.

Conventions
- Repo root: `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated`. Fabric project: `Fabric-0.18.6-1.21.1`. NeoForge project: `Neoforge-21.1.217-1.21.1`.
- Build commands (PowerShell, from the project dir): `.\gradlew.bat build --no-daemon` (Fabric also `remapJar`; NeoForge `build`). First Fabric build after the dependency change may need `--refresh-dependencies` once.
- Commit trailer: `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. Push after every commit: `git push origin main`.
- Java 21, official Mojang mappings, tabs vs spaces: follow the file you edit.
- Keep the optional-dependency pattern: classes that import `net.p3pp3rf1y.*` must only be loaded when `CompatHelper.isSophisticatedBackpacksLoaded()` (existing reflection/`NoClassDefFoundError` guards stay).

---

## T0 – Repository hygiene (Fabric + root)

1. `.gitignore` (root): add negations so these are tracked:
   ```
   !/ANALYSIS_AND_INSTRUCTIONS/
   !/ANALYSIS_AND_INSTRUCTIONS/**
   !/PATCH_NOTES_*.md
   !/ExportedJars/PATCH_NOTES_*.md
   !/graphify-out/GRAPH_REPORT.md
   ```
   and ignore machine-specific graphify files: `graphify-out/.graphify_python`, `graphify-out/.graphify_root`, `graphify-out/cache/`. Keep `graphify-out/graph.json`, `graph.html`, `manifest.json`, `cost.json`, `.graphify_labels.json` tracked (they are ~18 MB; acceptable).
2. Delete `Fabric-0.18.6-1.21.1/other_mods/_named_dev_copies_old/` (stale remapped copies).
3. Verify `git status` shows the new folder and graphify files as untracked-but-not-ignored, then commit: `chore: track analysis folder and knowledge graph`.

Check: `git check-ignore -v ANALYSIS_AND_INSTRUCTIONS/README.md` prints nothing.

## T1 – Fix the Fabric dependency setup (RC4)

File: `Fabric-0.18.6-1.21.1/build.gradle`, `gradle.properties`.

1. Replace the Sophisticated dependency block with CurseMaven coordinates for **all three** 1.21.1 files:
   ```
   modCompileOnly "curse.maven:sophisticated-core-unofficial-fabric-port-979317:7344653"
   modCompileOnly "curse.maven:sophisticated-backpacks-unofficial-fabric-port-979322:6844426"
   modCompileOnly "curse.maven:sophisticated-storage-unofficial-fabric-port-979326:7344673"
   ```
   and, for the dev runtime, `modLocalRuntime` of the same three coordinates by default. Keep a property switch `-PuseLocalRuntimeMods=true` that instead uses `modLocalRuntime(files(<each jar in other_mods matching sophisticated*.jar>))`. Remove `compileOnly(compileSupportModsTree)`, the NeoForge fallback tree and the `remappedPortingTransferTree` hack. If Loom cannot remap a `files()` dependency in a `mod*` configuration in this Loom version, keep CurseMaven only and document it in the session log.
2. `gradle.properties`: replace the stale `sophisticatedcore_version` / `sophisticatedbackpacks_version` lines with the three CurseMaven ids as documented comments (`sophisticatedcore_cf=979317:7344653` etc.) and read them from `build.gradle` instead of hard-coding.
3. Confirm the Core jar's own requirements are on the dev runtime classpath: `forgeconfigapiport` (already pulled by Ponder) and `teamreborn:energy:4.1.0` (already `modLocalRuntime`). Add a `modLocalRuntime` for Forge Config API Port only if the `runServer` smoke test in T8 reports it missing.
4. Bump `mod_version` to `4.1.0` in both projects' `gradle.properties`.

Check: `.\gradlew.bat build --refresh-dependencies --no-daemon` passes in the Fabric project **before any Java change** (baseline). Commit: `build(fabric): compile against the real 1.21.1 Sophisticated ports`.

## T2 – Wrapper aligned with Core (RC3)

File: `Fabric-0.18.6-1.21.1/src/main/java/sophisticated/building/item/upgrade/BuildingUpgradeWrapper.java`.

1. `public class BuildingUpgradeWrapper extends UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem>`; constructor `(IStorageWrapper, ItemStack, Consumer<ItemStack>)` calls `super(...)`. Remove the `enabled` field, the CUSTOM_DATA reading/writing and the local `isEnabled/setEnabled/save/getUpgradeStack` overrides (all inherited). Keep `getTier()`, `getMaxBlocks()`, `getStorageWrapper()`, `extractItem`, `countItem`, `hasPlaceableBlocks`, `canBeDisabled() = true`, `hideSettingsTab() = false`. Use the inherited `upgradeItem` field (typed `BuildingUpgradeItem`).
2. `BuildingUpgradeContainer`: unchanged API, but `setEnabled` must only call `upgradeWrapper.setEnabled(enabled)` on the side that owns the change: on the client call `sendBooleanToServer` **and** set locally (current behaviour is fine); on the server `handlePacket` calls `setEnabled` (inherited wrapper method now refreshes Core's cache).
3. `BuildingUpgradeSettingsTab`: keep the toggle (it now shares state with Core's slot switch) and add a tooltip/label showing tier and max blocks if `SettingsTabBase` offers a simple text child; if not, keep the toggle only. Change the tab title key to a new lang key `sophisticatedbuilding.gui.building_upgrade.tab` = "Building Upgrade" (add to `en_us.json`) in `SophisticatedBackpacksClientIntegration`.
4. Document in the patch notes: previously-disabled Building Upgrades become enabled after the update.

Check: build passes. Commit: `fix(upgrade): store enabled state via SophisticatedCore data component and refresh type wrappers`.

## T3 – Find backpacks through PlayerInventoryProvider (RC1)

File: `BuildingUpgradeHelper.java` (Fabric).

1. Replace the inventory loop + `CuriosCompatHelper` calls in `findBestBuildingUpgrade` and `findAllBuildingUpgrades` with
   ```java
   PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, invName, identifier, slot) -> {
       BuildingUpgradeWrapper w = getBuildingUpgradeFromBackpack(backpack);
       ... collect / keep best by tier ...
       return false; // keep iterating
   });
   ```
   Wrap in the same `try/catch (Exception | NoClassDefFoundError)` style as the rest of the file. The import `net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider` is fine because this class is only loaded when SB is present.
2. `getBuildingUpgradeFromBackpack`: call `BackpackWrapper.fromStack(stack)` directly (no reflection, the method exists) and keep the `new BackpackWrapper(stack)` fallback removed; use `getTypeWrappers(BuildingUpgradeItem.TYPE)` as now (it respects enabled state) but additionally fall back to `getSlotWrappers()` filtered by `instanceof BuildingUpgradeWrapper && isEnabled()` so a stale type cache can never hide an enabled upgrade.
3. Delete `CuriosCompatHelper` on Fabric and all its usages (keep NeoForge's).
4. These helper methods must only ever be called on the logical server from now on (see T4). Add a guard at the top of `findBestBuildingUpgrade`/`findAllBuildingUpgrades`: if `player.level().isClientSide()` return null/empty and log once at DEBUG.

Check: build passes. Commit: `fix(upgrade): find worn backpacks via PlayerInventoryProvider (Trinkets/Accessories/Curios)`.

## T4 – Server-authoritative upgrade state and counts (RC2)

Files (Fabric): new `network/message/BuildingUpgradeStatePacket.java`, new `client/ClientBuildingUpgradeState.java`, `network/PacketHandler.java`, `network/PacketHandlerClient.java`, `fabric/FabricCommonEvents.java`, `fabric/FabricClientEvents.java`, `utilities/InventoryHelper.java`, `BuildingUpgradeHelper.java`, `client/ClientBackpackItemCache.java`.

1. Packet `BuildingUpgradeStatePacket(int tier, int maxBlocks)` (S2C, `StreamCodec.composite`, id `sophisticatedbuilding:building_upgrade_state`), modelled on `PowerLevelPacket`. Register type + client receiver. Handler stores into `ClientBuildingUpgradeState` (static `tier`, `maxBlocks`, `hasUpgrade()`, `clear()`).
2. Server sends it: on `ServerPlayConnectionEvents.JOIN`, `AFTER_RESPAWN`, `AFTER_PLAYER_CHANGE_WORLD`, and in `FabricCommonEvents.onServerTick` every 10 ticks **only when the (tier,maxBlocks) pair differs from the last value sent to that player** (keep a `Map<UUID, int[]>`; clear on disconnect / server stop). Compute with `BuildingUpgradeHelper.getBuildingUpgradeTier` / `getMaxBlocksForPlayer`, guarded by `CompatHelper.isSophisticatedBackpacksLoaded()` and `NoClassDefFoundError`.
3. Counts: make every server→client count message carry the **unclamped** total (`countBlockInBackpacksForDisplay`). Update `syncItemCount`, `syncPeriodicBackpackCounts`, `extractBlockFromBackpack` and `removeFromBackpacks` accordingly. Remove `countBlockInBackpack` (deprecated alias) or make it return the unclamped total.
4. `InventoryHelper` (Fabric):
   - `findTotalItemsInInventory`: if `player.level().isClientSide()` → inventory count + `min(ClientBackpackItemCache.getCount(item), ClientBuildingUpgradeState.maxBlocks)` (0 when no upgrade), minus the reserved held block when an upgrade is active (`ClientBuildingUpgradeState.hasUpgrade()`); on the server → inventory count + `min(server count, server max)` exactly as today but through the server helper only.
   - `getReservedHeldCount`: client uses `ClientBuildingUpgradeState`, server uses the helper.
   - `findTotalItemsInBackpacksForDisplay` / `findTotalItemsForDisplay`: unchanged semantics (client cache, server helper).
   - Put the arithmetic `clampedBackpackContribution(int backpackCount, int maxBlocks)` into a small static pure method so it can be unit-tested.
5. Client lifecycle: clear `ClientBackpackItemCache` and `ClientBuildingUpgradeState` on `ClientPlayConnectionEvents.DISCONNECT` (register in `FabricClientEvents`).
6. Nothing on the client may call `BuildingUpgradeHelper` anymore. Grep for `BuildingUpgradeHelper.` outside server-only code paths and prove it.

Check: build passes; unit test from T7 for the pure helper. Commit: `fix(upgrade): make the server authoritative for building-upgrade state and backpack counts`.

## T5 – Radial menu fixes (F1, F2, F8)

Files (Fabric): `ClientEvents.java`, `buildmode/BuildModes.java`, `fabric/FabricClientEvents.java`.

1. `ClientEvents.isKeybindDown(int)`: use `KeyBindingHelper.getBoundKeyOf(keyBindings[keybindIndex])`; if the key type is `KEYSYM`/`SCANCODE` use `InputConstants.isKeyDown(window, key.getValue())`, if `MOUSE` use `GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS`; return `keyMapping.isDown() || physicalDown`. Remove the hard-coded `GLFW_KEY_LEFT_ALT`.
2. `FabricClientEvents`: do not call `ClientEvents.onGuiOpen()` when `currentScreen instanceof RadialMenu`.
3. `BuildModes.setBuildMode(BuildModeEnum)`: if the new mode differs from the current one, call `SophisticatedBuildingClient.BUILDER_CHAIN.cancel()` **before** assigning the new mode.
4. Apply items 2–3 to NeoForge too (`ClientEvents`/event subscriber equivalents) so behaviour matches; NeoForge's `isKeybindDown` already reads the bound key.

Check: build passes both projects. Commit: `fix(radial): respect rebound radial key and keep builds alive while the wheel is open`.

## T6 – Complete the beta options: line thickness and diagonal wall fill (F3, F4)

Files (Fabric, then mirror to NeoForge): `buildmode/BuildModeEnum.java`, `buildmode/buildmodes/Line.java`, `DiagonalLine.java`, `DiagonalWall.java`.

1. `Line`: add `public static List<BlockPos> getLineBlocks(int x1,int y1,int z1,int x2,int y2,int z2,int thickness)` implementing the square cross-section spec from `04 F3`; the existing `getLineBlocks(Player, …)` delegates with `thicknessOf(ModeOptions.getLineThickness())` (1/3/5). Enable `ModeOptions.OptionEnum.LINE_THICKNESS` on `BuildModeEnum.LINE`.
2. `DiagonalLine`: add a `thickness` parameter overload (cube neighbourhood); the mode uses the current option for its own intermediate/final blocks; `DiagonalWall` keeps calling with thickness 1. Enable `LINE_THICKNESS` on `DIAGONAL_LINE`.
3. `DiagonalWall`: add `getDiagonalWallBlocks(..., boolean hollow)`; hollow = bottom line + top line + end columns; enable `OptionEnum.FILL` on `DIAGONAL_WALL`.
4. Update `DOCUMENTATION.md` (Fabric project, untracked but keep it correct) lines for Line/Diagonal Line/Diagonal Wall.
5. Mirror the same changes into the NeoForge project (identical files).

Check: unit tests from T7; both builds pass. Commit: `feat(modes): line thickness for Line/Diagonal Line and hollow Diagonal Wall`.

## T7 – Unit tests (Fabric project)

1. Add JUnit 5 to `Fabric-0.18.6-1.21.1/build.gradle`: `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'`, `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`, `tasks.test { useJUnitPlatform() }`. Loom puts Minecraft on the test classpath; `BlockPos` works without bootstrap.
2. Tests under `src/test/java/sophisticated/building/`:
   - `LineThicknessTest`: thickness 1 gives the plain line; thickness 3 on an X line of length 4 gives 4×9 blocks, all within ±1 in y/z; thickness 5 gives 4×25; Y and Z lines analogous.
   - `DiagonalWallFillTest`: hollow wall on a 5-block line with height 4 contains the two rows and the two end columns and nothing else.
   - `BackpackContributionTest`: `clampedBackpackContribution(500, 64) == 64`, `(10, 64) == 10`, `(500, 0) == 0`.
3. `.\gradlew.bat test` green.

Commit together with T6 or as `test: cover line thickness, diagonal wall fill and backpack clamp`.

## T8 – Runtime smoke test (Fabric)

1. Ensure `Fabric-0.18.6-1.21.1/run/eula.txt` contains `eula=true` (create if missing) and `run/server.properties` has `online-mode=false`.
2. Start `.\gradlew.bat runServer --no-daemon` in the background with a 6-minute cap. Watch `run/logs/latest.log` until it contains `Done (` or an error. Required lines: mod init (`Initializing Sophisticated Building for Fabric`), `Registered Sophisticated Backpacks upgrade containers`, and the SB/SC/SS versions in the mod list; forbidden: any `Failed to register`, `Failed to create BuildingUpgradeItem`, `NoSuchMethodError`, `NoClassDefFoundError` mentioning `sophisticated`.
3. Stop the server process cleanly (PowerShell: find the java process whose command line contains `DevLaunchInjector` or `fabric-server` and `Stop-Process` it), then `.\gradlew.bat --stop`.
4. Do **not** launch `runClient` (it opens a window on the user's desktop and cannot be verified headlessly).
5. Paste the relevant log lines into `07_SESSION_LOG.md` under "Implementer notes".

## T9 – NeoForge parity for RC1–RC3

Apply T2, T3 (via NeoForge's `PlayerInventoryProvider`, same package/API in official SB 3.25.x; delete NeoForge `CuriosCompatHelper` only if `runOnBackpacks` compiles), and T4 to the NeoForge project using NeoForge networking (`PacketDistributor.sendToPlayer`, `PayloadRegistrar`) and NeoForge events (`PlayerEvent.PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent`, `PlayerTickEvent.Post`, `ClientPlayerNetworkEvent.LoggingOut`). Keep NeoForge's non-clamping `findTotalItemsInInventory` semantics but route the client path through the synced state (`ClientBuildingUpgradeState.hasUpgrade()` gates access instead of a client-side wrapper lookup).

Check: `.\gradlew.bat build --no-daemon` in the NeoForge project (needs network for Modrinth Maven). Commit: `fix(neoforge): parity for server-authoritative building upgrade`.

## T10 – Release plumbing

1. Create `PATCH_NOTES_4.1.0.md` (root) listing: Fabric SB/SC/SS 1.21.1 compatibility, worn-backpack support (Trinkets/Accessories/Curios), server-authoritative upgrade state, enabled-state migration note, radial key fix, build-survives-radial-menu, line thickness, diagonal wall fill, dependency fixes. Copy it to `ExportedJars/`.
2. `rebuild_all_and_export_jar.ps1`: reference `PATCH_NOTES_4.1.0.md` (or glob `PATCH_NOTES_*.md`).
3. Run `rebuild_all_and_export_jar.ps1` from the repo root; confirm both jars appear in `ExportedJars/` and copy the Fabric jar to `DevInstance_Fabric/run/mods/` (replace 4.0.0).
4. Commit: `release: 4.1.0 patch notes and export script`. Push.

## T11 – Rebuild the knowledge graph

From the repo root run the graphify incremental update (`graphify update` via the interpreter in `graphify-out/.graphify_python`, or re-run the AST extraction + build steps documented in `07_SESSION_LOG.md`), regenerate `graph.html`, then commit `chore: refresh knowledge graph` and push.

## Final report

Write a concise summary at the end of `07_SESSION_LOG.md` ("Implementer notes"): what was done per task, test results, smoke-test excerpt, anything skipped and why, and every deviation from these instructions.
