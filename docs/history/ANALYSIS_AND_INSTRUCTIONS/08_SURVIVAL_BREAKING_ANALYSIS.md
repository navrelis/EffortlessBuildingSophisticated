# 08 – Survival block breaking: analysis and design

Written 2026-09-14 by the orchestrator (Fable) after reading the code. Nothing here is a guess;
every claim names the file it was read from. Companion task list: `09_SURVIVAL_BREAKING_TASKS.md`.

## 1. What users asked for (CurseForge comments)

1. "Can this mod only place blocks? ... still don't know how to break blocks."
2. "The Disable mode prevents me from placing or breaking any blocks, instead of reverting to
   vanilla placing."
3. "Am I able to break blocks in mass using survival mode?" – "they basically block it unless in
   creative mode ... I really wish they would allow that option like the original had."
4. "Please make it also work to mass break blocks in survival."

So three things: (a) mass breaking in survival, balanced; (b) make Disable mode reliably vanilla;
(c) document how breaking works at all.

## 2. How breaking works today (both loaders, identical logic)

Client, `systems/BuilderChain.java`:

- `determineAbilities()` – if the main hand is **not** a block (or block proxy) the state is
  `CAN_BREAK`; with a block it is `CAN_PLACE_AND_BREAK`. Breaking is the left mouse button
  (`ClientEvents.onMouseInput` → `BUILDER_CHAIN.onLeftClick()` every 4 ticks while held; only
  SINGLE mode repeats).
- `onLeftClick()` line 129: `if (!AttachmentHandler.canBreakFar(player)) return;` – **this is the
  survival gate.** `findStartPosition()` line 302 has the same check for far targets.
- `attachment/PowerLevel.canBreakFar(player)` = `player.getAbilities().instabuild` → creative only.
- After the mode's `onClick` returns true the client sends `ServerBreakBlocksPacket(blocks)`.
  `BlockSet.encode` drops entries flagged `invalid` (used today only for missing items when
  placing).

Server:

- `ServerBreakBlocksPacket.Handler` → `ServerBlockPlacer.breakBlocks` → `applyBlockSet` →
  `applyBlockEntry` (breaking when `newBlockState` is null/air) → `BlockPlacerHelper.breakBlock`.
- `utilities/BlockPlacerHelper.breakBlock` calls `BlockHelper.destroyBlockAs(level, pos, player,
  ItemStack.EMPTY, 0f, drop -> giveItemToPlayer)`. **Tool is always EMPTY**: no correct-tool
  check, no durability, no silk touch/fortune, no hunger. Drops go to the player inventory.
- `create/foundation/utility/BlockHelper.destroyBlockAs` already does the right things when given
  a real tool: `usedTool.mineBlock(...)` (vanilla durability incl. Unbreaking), `awardStat`,
  `Block.getDrops(state, level, pos, be, player, usedTool)` (enchantment-aware loot). Fabric's copy
  passes `ItemStack.EMPTY` to `spawnAfterBreak` (XP); NeoForge's copy posts `BlockDropsEvent`.
  Neither checks `requiresCorrectToolForDrops` – vanilla does that in `Block.playerDestroy`, so
  **we must do it ourselves** or a wooden pickaxe would silently delete diamond ore with no drop.
- Vanilla breaking is cancelled while a build mode is active, creative only:
  Fabric `fabric/FabricCommonEvents.java` `PlayerBlockBreakEvents.BEFORE` returns
  `isLikeVanilla(player) || !powerLevel.canBreakFar(player)`; NeoForge `CommonEvents.onBlockBroken`
  cancels `BlockEvent.BreakEvent` when `!isLikeVanilla && canBreakFar`. Both skip when
  `SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()`.
- Server limits that already apply to breaking: `ServerConfig.validation.maxBlocksPlacedAtOnce`
  (10000), `allowInSurvival`, whitelist, `mayBuild`. Per-player limits (blocks per click 128/256/
  512/2048, per axis 8–32, reach 0/8/16/32 by power level, creative 10000/1000/200) are applied on
  the client via `AttachmentHandler` (`BlockSet.ClientSide.isFull`).
- `utilities/SurvivalHelper.canBreak` (correct-tool check for the *held* item) exists but is
  **unused**.
- Delayed application already exists for placing: `ServerBlockPlacer.placeBlocksDelayed` stores a
  `DelayedEntry(player, blocks, placeTime)` and `tick()` applies it when `gameTime >= placeTime`.
  `applyBlockSet` handles break entries too, so a delayed break needs no new machinery.
- Undo/redo of a break re-places blocks from inventory (`undoBlockSet`); redo of a break goes
  through `undoBlockEntry` → `BlockPlacerHelper.breakBlock` again.

Rendering: `render/BlockPreviews.drawLookAtPreview` draws breaking sets as one red
`thin_checkered` cluster; `RenderHandler.drawStacks` only draws the item HUD while PLACING.

## 3. Why "Disable" can look like it blocks vanilla

`ServerBuildState` (server, per-UUID map) is the only thing that can block vanilla placing/breaking
(`UseBlockCallback` FAIL / `PlayerBlockBreakEvents.BEFORE` false when `!isLikeVanilla`). It is set
**only** by `IsUsingBuildModePacket`, sent from `BuildModes.setBuildMode`, and reset to `false` by
`ServerBuildState.handleNewPlayer` on join/respawn. The client's `BuildModes.buildMode` is a static
field that survives leaving a world, but the client never re-sends it on join. So client and server
can disagree after a rejoin (client LINE / server "vanilla" → double placement; or in older builds
that persisted the flag in player NBT, the reverse → "Disable does nothing, vanilla blocked"). The
unused `IS_USING_BUILD_MODE_KEY` NBT constants in `ServerBuildState` are the fossil of that older
persistence. Fix: the client re-sends its build-mode and quick-replace state on every join
(T-S6), and the server keeps resetting on join. Also, once survival breaking exists, a survival
player with a build mode active will have vanilla breaking cancelled exactly like creative does
today; the actionbar message from T-S3 tells them why nothing breaks when no tool fits, and
Disable mode (radial menu / keybind) restores vanilla.

## 4. Upstream API facts verified with javap (2026-09-14)

Both the Fabric port (`other_mods/sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar`, core
`1.2.9.21.168`) and the official NeoForge port (gradle cache `sophisticated-backpacks-1.21.1-
3.25.44.1736.jar`, core `1.4.38.1847`) expose the **same** signatures:

- `net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperUpgradeWrapper extends
  UpgradeWrapperBase<...>`: `boolean isEnabled()` (inherited), `ToolSwapMode getToolSwapMode()`,
  `FilterLogic getFilterLogic()`, `boolean hideSettingsTab()` (true for the basic upgrade, false
  for the Advanced one), `boolean shouldSwapWeapon()`.
- `ToolSwapMode` enum: `ANY`, `ONLY_TOOLS`, `NO_SWAP`.
- `ToolSwapperUpgradeItem.TYPE` is **private static** → `UpgradeHandler.getTypeWrappers(TYPE)` is
  not usable from our code. Enumerate `upgradeHandler.getSlotWrappers().values()` and filter
  `instanceof ToolSwapperUpgradeWrapper`.
- `net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.TOOL_SWAPPER_UPGRADE` /
  `ADVANCED_TOOL_SWAPPER_UPGRADE` exist (DeferredHolder) – not needed.
- `IStorageWrapper.getInventoryHandler()` → `InventoryHandler` (extends porting_lib
  `ItemStackHandler` on Fabric, NeoForge `ItemStackHandler` on NeoForge): `getStackInSlot(int)`,
  `setStackInSlot(int, ItemStack)`, `extractItem(int,int,boolean)`, `insertItem`. Slot count:
  **Fabric `getSlotCount()`** (see existing `BuildingUpgradeWrapper.java` line 33), **NeoForge
  `getSlots()`**.
- `FilterLogic.matchesFilter(ItemStack)` exists on both.
- Backpack enumeration: `PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, invName,
  identifier, slot) -> false)` (already used by `BuildingUpgradeHelper`); wrapper via
  `BackpackWrapper.fromStack(stack)`. Server only – the client copy has no reliable contents.

## 5. Design decisions (the "not too OP, not too bad" balance)

Goal: survival players can mass-break with build modes, paying what vanilla would charge and
never getting more than vanilla would give.

D1. **Server-authoritative tool rules, mirrored on the client for the preview.** A common helper
    (`utilities/BreakToolHelper` + pure `ToolSelector`) decides, per block, which tool breaks it:
    1. Unbreakable blocks (`state.getDestroySpeed(level,pos) < 0`) → never.
    2. Hardness 0 blocks (grass, flowers, torches, redstone dust...) → broken with the empty hand,
       no tool, no durability (vanilla charges nothing either).
    3. Otherwise the first *candidate* in priority order that is **effective**
       (`stack.getDestroySpeed(state) > 1.0F`) and, if `state.requiresCorrectToolForDrops()`,
       **correct** (`stack.isCorrectToolForDrops(state)`). A wrong-tier pickaxe on obsidian is
       skipped – we never destroy a block without its drops.
    4. If no candidate is effective and the block does not require a tool (glass, glowstone,
       carpet...) the **main-hand item** is used as long as it is a tool (see "tool" below); it
       pays 1 durability per block like vanilla would.
    5. Candidate order: main hand, hotbar 0–8, main inventory 9–35, offhand, then tools inside
       Sophisticated Backpacks that carry an **enabled Tool Swapper / Advanced Tool Swapper
       upgrade** whose mode is not `NO_SWAP` (Advanced: the stack must also pass
       `getFilterLogic().matchesFilter`).
    6. "Tool" = `DiggerItem` or `ShearsItem` or in item tags `minecraft:pickaxes/axes/shovels/hoes`.
       Swords, bows, armor are never consumed.
    7. `stopBeforeToolBreaks` (default true): a damageable tool with <= 1 use left is skipped, so
       the mod never destroys your tool. The next candidate takes over mid-set.
D2. **Durability, drops and hunger exactly like vanilla**: `destroyBlockAs` gets the selected
    stack (`mineBlock` → `hurtAndBreak`, Unbreaking respected; `Block.getDrops` with the tool →
    Silk Touch / Fortune); plus `player.causeFoodExhaustion(0.005F)` per block (vanilla
    `Block.playerDestroy`). Backpack tools are damaged in place and written back with
    `setStackInSlot`.
D3. **Not instant: a short server-computed mining delay.** Creative stays instant. Survival break
    sets are applied through the existing delayed queue after
    `delay = min(maxDelayTicks, sum of estimateBreakTicks(block, tool))` where
    `estimateBreakTicks = ceil(1 / (toolSpeed / hardness / (correct ? 30 : 100)))` (vanilla's
    per-tick progress formula without efficiency/haste modifiers). Default cap 40 ticks (2 s).
    Effect: one stone block with an iron pickaxe = 8 ticks (close to vanilla), a 128-block wall =
    2 s. This removes the "instant mining" exploit without making the feature feel slow.
D4. **Existing survival limits stay in force**: blocks per click / per axis / reach by power level,
    `maxBlocksPlacedAtOnce`, `allowInSurvival`, whitelist, tile-entity protection. Additionally
    survival breaking respects spawn protection and adventure-mode restrictions
    (`level.mayInteract(player,pos)`, `player.blockActionRestricted`).
D5. **Vanilla breaking is cancelled while a build mode is active, in survival as in creative.**
    Consistent mental model: build mode on = the mod breaks (with the rules above); Disable = pure
    vanilla (modifiers still add their extra blocks, `skipFirst` as today). When the mod cannot
    break anything of the selection the client shows an actionbar message naming the reason.
D6. **Client preview**: entries the client cannot find a tool for are flagged `invalid` (dropped by
    `BlockSet.encode`) and drawn as a grey cluster instead of red; the HUD next to the crosshair
    shows the tools that will be used with the block count each will take, plus a red barrier
    icon with the count of blocks that cannot be broken. Backpack tools are known on the client
    only through a new `BackpackToolsPacket` (server → client, list of ItemStack copies, sent on
    join/respawn/dimension change and every 10 ticks when the fingerprint changes) – same RC2
    pattern as `BuildingUpgradeStatePacket`.
D7. **Config** (`ServerConfig.survivalBreaking`): `enabled=true`, `stopBeforeToolBreaks=true`,
    `maxDelayTicks=40`, `exhaustionPerBlock=0.005`. Note: `SimpleConfigValue` is still an in-memory
    placeholder on both loaders (no file is read), so these are defaults until config loading is
    implemented; they exist so the switch is in one place.
D8. **Out of scope** (deliberately): automatic tool *swapping* into the hand (SB does that itself
    on vanilla clicks), inserting drops into backpacks (existing `giveItemToPlayer` stays),
    XP for delayed breaks beyond what `destroyBlockAs` already spawns, breaking in Disable mode
    beyond the existing modifier behaviour, config file loading.

## 6. Risks the reviewer will look at

- Client/server plan divergence (client sees a stale backpack tool list) is harmless: the server
  simply skips what it cannot break; the client HUD may briefly be optimistic.
- `hurtAndBreak(1, player, EquipmentSlot.MAINHAND)` inside `Item.mineBlock` broadcasts the break
  animation for the main hand even when the damaged stack sits in the hotbar or a backpack. Cosmetic.
- `ClientPlayNetworking.send` from `ClientPlayConnectionEvents.JOIN` must happen on the client
  thread; wrap in `client.execute`.
- Backpack write-back: modify a **copy** of `getStackInSlot(i)` and call `setStackInSlot(i, copy)`
  so `InventoryHandler.onContentsChanged` persists it.
