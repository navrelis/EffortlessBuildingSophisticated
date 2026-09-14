# 09 – Survival breaking: implementation tasks (contract for the implementing agent)

Read `01`, `02`, `08` first (and `05` for the conventions – they all still apply: build after every
task, one commit per task with the trailer, push, no red build, deviations go into
`07_SESSION_LOG.md` under "Implementer notes – survival breaking"). Version stays **4.1.0**.

Conventions recap
- Repo root `C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated`; Fabric project
  `Fabric-0.18.6-1.21.1` (primary), NeoForge project `Neoforge-21.1.217-1.21.1` (parity).
- Build: `.\gradlew.bat build --no-daemon` in each project dir (PowerShell). Fabric tests run as
  part of `build`.
- Commit trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`; `git push origin main`
  after every commit.
- Classes that import `net.p3pp3rf1y.*` must live in `sophisticated.building.integration` or
  `sophisticated.building.item.upgrade` and may only be touched behind
  `CompatHelper.isSophisticatedBackpacksLoaded()` + `catch (Exception | NoClassDefFoundError)`
  exactly like `BuildingUpgradeHelper` is used from `InventoryHelper`.
- Do the Fabric project first (T-S1 … T-S7), then NeoForge parity (T-S8), then release plumbing
  (T-S9). Do not start the graph rebuild (orchestrator does it).
- Do not touch `graphify-out/` (it has uncommitted changes from the orchestrator; leave them).

---

## T-S1 – Config + pure tool-selection logic (Fabric, common code, unit-tested)

Files: `ServerConfig.java`, new `utilities/ToolSelector.java`, new test
`src/test/java/sophisticated/building/ToolSelectorTest.java`.

1. `ServerConfig`: add
   ```java
   public static final SurvivalBreaking survivalBreaking = new SurvivalBreaking();
   public static class SurvivalBreaking {
       public final SimpleConfigValue<Boolean> enabled = new SimpleConfigValue<>(true);
       public final SimpleConfigValue<Boolean> stopBeforeToolBreaks = new SimpleConfigValue<>(true);
       public final SimpleConfigValue<Integer> maxDelayTicks = new SimpleConfigValue<>(40);
       public final SimpleConfigValue<Double> exhaustionPerBlock = new SimpleConfigValue<>(0.005);
   }
   ```
2. `utilities/ToolSelector` – **no Minecraft imports** so it is testable like `LineThicknessTest`:
   ```java
   public final class ToolSelector {
       /** What the selector needs to know about one candidate tool. */
       public interface Candidate {
           boolean isEffective();        // destroySpeed(state) > 1
           boolean isCorrect();          // isCorrectToolForDrops(state)
           int remainingUses();          // Integer.MAX_VALUE when not damageable
           boolean isMainHand();
       }
       public enum Need { UNBREAKABLE, NO_TOOL, TOOL }
       /** Returns index into candidates, -1 = "use empty hand" (only for NO_TOOL), -2 = impossible. */
       public static int select(List<? extends Candidate> candidates, Need need,
                                boolean requiresCorrectTool, boolean stopBeforeToolBreaks)
       /** ceil(1 / (toolSpeed / hardness / (correct ? 30 : 100))); 0 when hardness <= 0. */
       public static int estimateBreakTicks(float hardness, float toolSpeed, boolean correct)
       public static int capDelay(int totalTicks, int maxDelayTicks)
   }
   ```
   `select` rules (from `08` D1): `UNBREAKABLE` → -2; `NO_TOOL` → -1; `TOOL` → first candidate with
   `isEffective() && (!requiresCorrectTool || isCorrect())` and
   `(!stopBeforeToolBreaks || remainingUses() > 1)`; if none and `!requiresCorrectTool` → the
   main-hand candidate (`isMainHand()`) if present and usable (remaining-uses rule) → its index;
   else -2.
3. `ToolSelectorTest` (JUnit 5, same style as `LineThicknessTest`): at least
   - correct+effective pickaxe chosen over an earlier effective-but-wrong-tier one when
     `requiresCorrectTool`;
   - `stopBeforeToolBreaks` skips a candidate with `remainingUses()==1` and takes the next;
   - `requiresCorrectTool=false` with no effective tool falls back to the main-hand candidate, and
     returns -2 when the main hand is absent;
   - `estimateBreakTicks(1.5f, 6f, true) == 8`, `estimateBreakTicks(0.5f, 1f, true) == 15`,
     `estimateBreakTicks(1.5f, 1f, false) == 150`, `estimateBreakTicks(0f, 1f, true) == 0`;
   - `capDelay(300, 40) == 40`, `capDelay(12, 40) == 12`.

Check: `.\gradlew.bat test --no-daemon` green. Commit `feat(survival-break): config and pure tool selector (T-S1)`.

## T-S2 – `BreakToolHelper`: candidate collection, planning, server execution (Fabric)

Files: new `utilities/BreakToolHelper.java`, new `integration/ToolSwapperIntegration.java`,
`utilities/BlockPlacerHelper.java`, `create/foundation/utility/BlockHelper.java`.

1. `utilities/BreakToolHelper` (common; may be called on client and server):
   ```java
   public interface ToolSlot { ItemStack get(); void set(ItemStack stack); boolean isMainHand(); String describe(); }
   public static boolean isTool(ItemStack s)   // DiggerItem || ShearsItem || tag pickaxes/axes/shovels/hoes
   public static List<ToolSlot> collectCandidates(Player player)
   public static ToolSelector.Need needFor(Level level, BlockPos pos, BlockState state)
   public static ToolSlot selectTool(Player p, Level l, BlockPos pos, BlockState state, List<ToolSlot> cands) // null = empty hand, throws nothing; returns NONE sentinel when impossible
   public static int estimateBreakTicks(Level l, BlockPos pos, BlockState state, ItemStack tool)
   ```
   - `collectCandidates` order: main hand (`player.getInventory().selected`), hotbar 0–8 (skip the
     selected), 9–35, offhand (`Inventory.SLOT_OFFHAND` index 40 via `player.getOffhandItem()`),
     then – **server only** and only when `CompatHelper.isSophisticatedBackpacksLoaded()` –
     `ToolSwapperIntegration.collectBackpackTools(player)` inside the usual guard; **client only** –
     `ClientBackpackToolCache.snapshot()` (T-S4) wrapped as read-only slots whose `set` is a no-op.
     Only stacks with `isTool` are candidates.
   - Player-inventory `ToolSlot.set(stack)` → `player.getInventory().setItem(index, stack)`.
   - `needFor`: `destroySpeed = state.getDestroySpeed(level, pos)`; `< 0` → UNBREAKABLE; `== 0` →
     NO_TOOL; else TOOL.
   - Candidate adapter for `ToolSelector.Candidate`: `isEffective = stack.getDestroySpeed(state) >
     1.0F`; `isCorrect = stack.isCorrectToolForDrops(state)`; `remainingUses = stack.isDamageableItem()
     ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE`.
   - `estimateBreakTicks` → `ToolSelector.estimateBreakTicks(hardness, tool.isEmpty() ? 1f :
     tool.getDestroySpeed(state), !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state))`.
   - A client-side *plan* helper `planClient(Player, BlockSet)` that iterates the set in insertion
     order, works on **copies** of the candidate stacks (apply `copy.setDamageValue(+1)` per
     assignment when hardness > 0 so the "remaining uses" budget drains), sets
     `entry.invalid = true` where the selector returns impossible, and returns a
     `BreakPlan { Map<ToolSlot,Integer> usesPerTool; int unbreakable; int delayTicks }` for the HUD.
2. `integration/ToolSwapperIntegration` (imports `net.p3pp3rf1y.*`; server only):
   `List<BreakToolHelper.ToolSlot> collectBackpackTools(Player player)` using
   `PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, invName, identifier, slot) -> {...; return false;})`:
   `IStorageWrapper w = BackpackWrapper.fromStack(backpack)`; find in
   `w.getUpgradeHandler().getSlotWrappers().values()` a `ToolSwapperUpgradeWrapper ts` with
   `ts.isEnabled() && ts.getToolSwapMode() != ToolSwapMode.NO_SWAP`; if found iterate
   `inv = w.getInventoryHandler()`, `for i < inv.getSlotCount()` (**Fabric name**), stack =
   `inv.getStackInSlot(i)`; keep when `BreakToolHelper.isTool(stack)` and
   (`ts.hideSettingsTab() || matches(ts.getFilterLogic(), stack)`) where `matches` returns true when
   `matchesFilter` throws. `ToolSlot.get` returns `inv.getStackInSlot(i)`, `set(stack)` calls
   `inv.setStackInSlot(i, stack)`, `describe()` = `"backpack:" + i`.
   Both `ToolSwapperUpgradeItem` variants (basic/advanced) share the wrapper class, nothing else
   to distinguish.
3. `BlockPlacerHelper.breakBlock(Player player, BlockEntry entry)` → new overload
   `breakBlock(Player player, BlockEntry entry, @Nullable List<ToolSlot> candidates)`; keep the old
   signature delegating with `null` (creative behaviour unchanged: tool EMPTY). Non-creative path:
   `state = level.getBlockState(pos)`; `slot = BreakToolHelper.selectTool(...)`; impossible → return
   false; else `tool = slot == null ? ItemStack.EMPTY : slot.get().copy()`; call
   `BlockHelper.destroyBlockAs(level, pos, player, tool, 0f, drops -> giveItemToPlayer)`; afterwards
   if `slot != null` → `slot.set(tool)` (writes back durability; if `tool.isEmpty()` after breaking
   the slot is cleared – this cannot happen with `stopBeforeToolBreaks=true`); then
   `player.causeFoodExhaustion(ServerConfig.survivalBreaking.exhaustionPerBlock.get().floatValue())`.
4. `BlockHelper.destroyBlockAs` (Fabric copy): pass `usedTool` instead of `ItemStack.EMPTY` to
   `state.spawnAfterBreak(...)` so Silk Touch suppresses ore XP like vanilla. Also add a guard at
   the top: `if (state.isAir()) return false;`.

Check: Fabric `build` green. Commit `feat(survival-break): tool-aware server breaking with Tool Swapper backpack support (T-S2)`.

## T-S3 – Server gate, delay and validation (Fabric)

Files: `attachment/PowerLevel.java`, `systems/ServerBlockPlacer.java`,
`network/message/ServerBreakBlocksPacket.java`, `fabric/FabricCommonEvents.java`.

1. `PowerLevel.canBreakFar(player)` → `player.getAbilities().instabuild ||
   ServerConfig.survivalBreaking.enabled.get()`. Update the javadoc: "may use build-mode breaking".
2. `FabricCommonEvents` `PlayerBlockBreakEvents.BEFORE`: unchanged expression (it now cancels
   vanilla for survival players with a build mode active too – decision D5). Add a comment saying so.
3. `ServerBlockPlacer`:
   - `breakBlocks(player, blocks)`: creative → `applyBlockSet` immediately (unchanged). Survival:
     if `!ServerConfig.survivalBreaking.enabled.get()` → `log(player, RED + "Survival breaking is disabled on this server.")` and return.
     Otherwise run `checkAndNotifyAllowedToUseMod` + `validateBlockSet`, compute
     `candidates = BreakToolHelper.collectCandidates(player)` and
     `delay = ToolSelector.capDelay(sum over entries (skip skipFirst) of
     estimateBreakTicks(level, pos, existingState, selectedToolOrEmpty), maxDelayTicks)`; enqueue
     `delayedEntries.add(new DelayedEntry(player, blocks, gameTime + delay))`. (Do **not** pre-damage
     tools here; selection happens again at apply time with live stacks.)
   - `applyBlockSet`: compute `candidates` once per set when `!player.isCreative()` and pass it to
     `applyBlockEntry` → `BlockPlacerHelper.breakBlock(player, block, candidates)`. Same for
     `undoBlockEntry` (redo of a break).
   - `validateBlockEntry(player, block, breaking)`: when `breaking && !player.isCreative()` also
     require `player.level().mayInteract(player, block.blockPos)` and
     `!(player instanceof ServerPlayer sp && sp.blockActionRestricted(sp.level(), block.blockPos, sp.gameMode.getGameModeForPlayer()))`.
   - If after applying a survival break set zero entries succeeded, send
     `SophisticatedBuilding.logTranslate(player, "", "sophisticatedbuilding.message.survival_break_nothing", "", true)`.
4. `ServerBreakBlocksPacket.Handler` unchanged (still calls `breakBlocks`).

Check: `build` green; `grep -n "instabuild" attachment/PowerLevel.java` shows only
`canReplaceBlocks` and the new expression. Commit `feat(survival-break): server gate, mining delay and validation (T-S3)`.

## T-S4 – Backpack tool sync packet (Fabric)

Files: new `network/message/BackpackToolsPacket.java`, new `client/ClientBackpackToolCache.java`,
`network/PacketHandler.java`, `fabric/FabricCommonEvents.java`, `fabric/FabricClientEvents.java`,
`SophisticatedBuildingClient` (wherever S2C receivers are registered – find with
`grep -rn "registerGlobalReceiver(BuildingUpgradeStatePacket" src/main/java`).

1. `BackpackToolsPacket(List<ItemStack> tools)` S2C, `StreamCodec<RegistryFriendlyByteBuf,
   BackpackToolsPacket>` built with `ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(BackpackToolsPacket::new,
   BackpackToolsPacket::tools)`; id `backpack_tools`. Handler → `ClientBackpackToolCache.set(list)`
   on the client thread.
2. `ClientBackpackToolCache`: `set(List<ItemStack>)` (stores copies), `List<ItemStack> snapshot()`
   (copies), `clear()`. Clear in the existing `ClientPlayConnectionEvents.DISCONNECT` handler.
3. Server (`FabricCommonEvents`): `sendBackpackTools(ServerPlayer, boolean force)` – only when SB
   loaded, inside the same try/catch as the other helpers: `tools = ToolSwapperIntegration
   .collectBackpackTools(player)` → list of `slot.get().copy()`; fingerprint =
   `List<String>` of `ITEM.getKey(item) + "#" + damage + "#" + count`; keep `LAST_BACKPACK_TOOLS`
   map per UUID (clear on disconnect / server stop like the other maps); send when forced or when
   fingerprint differs. Call with `force=true` on JOIN, AFTER_RESPAWN, AFTER_PLAYER_CHANGE_WORLD and
   every 10 ticks (`player.tickCount % 10 == 0`) with `force=false`.
4. Register the payload type and client receiver next to `BuildingUpgradeStatePacket`.

Check: `build` green. Commit `feat(survival-break): sync Tool Swapper backpack tools to the client (T-S4)`.

## T-S5 – Client: planning, preview, HUD, messages (Fabric)

Files: `systems/BuilderChain.java`, `render/BlockPreviews.java`, `render/RenderHandler.java`,
`assets/sophisticatedbuilding/lang/en_us.json`.

1. `BuilderChain.onTick`: after `filterOnExistingBlockStates`, when
   `getPretendBuildingState() == BREAKING && !player.isCreative()` call
   `lastBreakPlan = BreakToolHelper.planClient(player, blocks)` (flags `invalid`). Store
   `lastBreakPlan` (nullable) with a getter; set it to null otherwise.
2. `BuilderChain.onLeftClick`: after `onClick` returns true and `!blocks.isEmpty()`, in survival run
   `planClient` again on the final set (the set may have changed since the tick), then count valid
   entries (`!invalid`, excluding the skipped first). If 0 → `logTranslate(player, "",
   "sophisticatedbuilding.message.survival_break_nothing", "", true)`, `cancel()` and return without
   sending. Otherwise proceed exactly as today (previews, sound, swing, send). If some entries are
   invalid, additionally `logTranslate(..., "sophisticatedbuilding.message.survival_break_partial",
   String.valueOf(invalidCount), true)`.
3. `BlockPreviews.drawLookAtPreview` breaking branch: split coordinates into valid and invalid;
   valid → existing red cluster; invalid → second cluster id `firstPos + "-invalid"` (or `"single-invalid"`),
   texture `thin_checkered`, colour `0.35f, 0.35f, 0.35f, 1f`.
4. `RenderHandler.drawStacks`: also handle `BREAKING` when `!player.isCreative()` and
   `BUILDER_CHAIN.getBreakPlan() != null`: draw one `drawItemStack(stack.copy() with count = uses)`
   per tool in `usesPerTool` (iterate in candidate order), and if `unbreakable > 0` draw
   `new ItemStack(Items.BARRIER, unbreakable)` with `missing=true`. Skip entirely when the plan is
   empty (`usesPerTool` empty and `unbreakable == 0`).
5. Lang (`en_us.json`): 
   ```
   "sophisticatedbuilding.message.survival_break_nothing": "No suitable tool for the selected blocks (switch to Disable mode for vanilla mining)",
   "sophisticatedbuilding.message.survival_break_partial": "%s block(s) skipped: no suitable tool",
   ```
   Check how `logTranslate` formats its middle argument (`SophisticatedBuilding.logTranslate`) and
   adapt so the count is shown; if it cannot take a format argument, use a plain
   `SophisticatedBuilding.log(player, I18n…, true)` on the client side.

Check: `build` green; then a manual sanity read of `BlockSet.encode` confirming invalid entries are
dropped (no code change expected). Commit `feat(survival-break): client plan, preview and HUD (T-S5)`.

## T-S6 – Disable mode: resync build state on join (Fabric)

Files: `buildmode/BuildModes.java`, `fabric/FabricClientEvents.java`, `systems/BuildSettings.java`
(read only unless a getter is missing).

1. `BuildModes.resyncToServer()`: sends `IsUsingBuildModePacket(buildMode != DISABLED)`; if
   `BuildSettings` exposes the quick-replace flag that `IsQuickReplacingPacket` carries, send that
   too with the current value.
2. `FabricClientEvents.register`: `ClientPlayConnectionEvents.JOIN.register((handler, sender, client)
   -> client.execute(() -> { try { BUILD_MODES.resyncToServer(); } catch (Exception e) { logger.warn(...); } }))`.
3. Server side stays as is (`handleNewPlayer` resets on join). Add a one-line comment in
   `ServerBuildState` next to the unused `IS_USING_BUILD_MODE_KEY` constants: "legacy NBT keys, no
   longer persisted; state is session-only and re-sent by the client on join".

Check: `build` green. Commit `fix: resync build mode state to the server on join so Disable mode is always vanilla (T-S6)`.

## T-S7 – Fabric smoke test

Same procedure as T8 in `05` (`runServer`, wait for `Done`, grep the log for
`NoClassDefFoundError|NoSuchMethodError|Failed to|Exception` mentioning `sophisticated`, stop the
process). Paste the relevant excerpt into the session log. `runClient` is not required.

## T-S8 – NeoForge parity

Apply T-S1 … T-S6 to `Neoforge-21.1.217-1.21.1` with these substitutions:
- `InventoryHandler` slot count is `getSlots()`.
- Vanilla-cancel hook is `CommonEvents.onBlockBroken(BlockEvent.BreakEvent)` – expression stays
  `!isLikeVanilla && canBreakFar`.
- NeoForge `BlockHelper.destroyBlockAs` already handles XP via `BlockDropsEvent`; only add the
  `state.isAir()` guard.
- Packets: `registrar.playToClient(BackpackToolsPacket.ID, CODEC, Handler::handle)`; send with
  `PacketDistributor.sendToPlayer`. Server hooks live in `CommonEvents.onPlayerTick` (every 10
  ticks), `onPlayerLoggedIn`, `onPlayerRespawn`, `onPlayerChangedDimension`; clear caches in
  `onPlayerLoggedOut`.
- Client resync: `ClientEvents` gets `@SubscribeEvent onLoggingIn(ClientPlayerNetworkEvent.LoggingIn)`
  → `BuildModes.resyncToServer()`; `ClientBackpackToolCache.clear()` in the existing `onLoggingOut`.
- `PowerLevel` there implements `INBTSerializable`; only the `canBreakFar` body changes.
- Unit tests: NeoForge has no test source set; do not add one. `ToolSelector` must be byte-for-byte
  the same file in both projects (copy it).
- NeoForge's `ServerPlayer.gameMode` is public as on Fabric (vanilla field); `blockActionRestricted`
  is vanilla.

Check: NeoForge `build` green, jar produced. Commit `feat(neoforge): survival breaking parity (T-S8)`.

## T-S9 – Release plumbing (still 4.1.0)

1. `PATCH_NOTES_4.1.0.md` (root): keep everything that is there and add, under "Major changes",
   a section **"Survival mass breaking (Fabric + NeoForge)"** explaining, for players: how to break
   at all (hold a non-block item – a tool or nothing –, the outline turns red, left-click; multi-
   click modes work the same as for placing), that survival is now supported, the rules (needs a
   suitable tool in hand/hotbar/inventory or in a backpack with an enabled Tool Swapper / Advanced
   Tool Swapper upgrade – Advanced filters are respected –, durability, drops and hunger exactly
   like vanilla, Silk Touch/Fortune apply, wrong-tier tools skip the block instead of destroying it,
   tools are never broken (they stop at 1 durability), unbreakable blocks are skipped, a short
   mining delay of up to 2 seconds per operation, spawn protection/adventure mode respected, power-
   level limits unchanged), the HUD (tools with counts, red barrier count for blocks that cannot be
   broken, grey outline for those blocks), and that build-mode breaking replaces vanilla mining
   while a build mode is active – switch to Disable for vanilla mining.
   Add a bullet under the radial/Disable fixes: "Disable mode is now re-synced to the server every
   time you join a world, so it can no longer get stuck blocking vanilla placing/breaking after a
   rejoin."
   Add the four config keys under a short "Server config" note with the caveat that config files
   are not read yet.
2. Run `rebuild_all_and_export_jar.ps1` from the repo root (it copies the newest patch notes) and
   copy the Fabric jar into `DevInstance_Fabric/run/mods/` replacing the old 4.1.0 jar.
3. Append "Implementer notes – survival breaking" to `07_SESSION_LOG.md` (what was done per task,
   command outputs summarised, every deviation with reason).
4. Commit `release: 4.1.0 patch notes – survival breaking (T-S9)`, push, and verify
   `git log origin/main..main` is empty.
