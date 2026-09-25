package sophisticated.building.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.ServerConfig;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.inventory.ItemHandlerHelper;
import sophisticated.building.network.message.BreakCountdownPacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockPlacerHelper;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.BlockUtilities;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.BuildLimits;
import sophisticated.building.utilities.InventoryHelper;
import sophisticated.building.utilities.ModifierLimits;
import sophisticated.building.utilities.PlacementTemplates;
import sophisticated.building.utilities.ReplaceRules;
import sophisticated.building.utilities.ToolSelector;
import sophisticated.building.platform.Services;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {
    private boolean isPlacingOrBreakingBlocks = false;

//region Delays
    private final Set<DelayedEntry> delayedEntries = ConcurrentHashMap.newKeySet();
    private final Set<DelayedEntry> delayedEntriesView = Collections.unmodifiableSet(delayedEntries);

    public void placeBlocksDelayed(Player player, BlockSet blocks, long placeTime) {

        resolveSkipFirst(player, blocks);
        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;
        if (!validateRequest(player, blocks)) return;

        if (!player.isCreative() && ServerConfig.survivalReplace.enabled.get()) {
            placeTime = scheduleReplaceMining(player, blocks, placeTime);
        }

        delayedEntries.add(new DelayedEntry(player, blocks, placeTime));
    }

    //Survival replace: the blocks in the way are mined, so wait for the mining delay like survival breaking
    private long scheduleReplaceMining(Player player, BlockSet blocks, long clientPlaceTime) {
        Level level = player.level();
        List<BreakToolHelper.ToolSlot> candidates = BreakToolHelper.collectCandidates(player);
        int totalTicks = 0;
        int replaceCount = 0;
        for (BlockEntry block : blocks) {
            if (blocks.isSkipped(block)) continue;
            if (BlockUtilities.isNullOrAir(block.newBlockState)) continue;

            BlockState state = level.getBlockState(block.blockPos);
            if (!BlockUtilities.needsMining(state) || state.is(block.newBlockState.getBlock())) continue;

            var selected = BreakToolHelper.selectTool(player, level, block.blockPos, state, candidates);
            if (BreakToolHelper.isImpossible(selected)) continue;

            replaceCount++;
            var tool = selected == null ? ItemStack.EMPTY : selected.get();
            totalTicks += BreakToolHelper.estimateBreakTicks(level, block.blockPos, state, tool);
        }
        if (replaceCount == 0) return clientPlaceTime;

        long now = level.getGameTime();
        long placeTime = ReplaceRules.placeTime(clientPlaceTime, now, totalTicks, ServerConfig.survivalBreaking.maxDelayTicks.get());
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToPlayer(serverPlayer, new BreakCountdownPacket((int) Math.max(0, placeTime - now), replaceCount, true));
        }
        return placeTime;
    }
    
    public void tick() {

        //Iterator to prevent concurrent modification exception
        for (var iterator = delayedEntries.iterator(); iterator.hasNext(); ) {
            DelayedEntry entry = iterator.next();
            // Check if player is still valid/online to avoid crashes
            if (entry.player.isRemoved()) {
                iterator.remove();
                continue;
            }
            
            long gameTime = entry.player.level().getGameTime();
            if (gameTime >= entry.placeTime) {
                applyBlockSet(entry.player, entry.blocks);
                iterator.remove();
            }
        }
    }

    public Set<DelayedEntry> getDelayedEntries() {
        return delayedEntriesView;
    }

    public record DelayedEntry(Player player, BlockSet blocks, long placeTime) {}
//endregion

    public void breakBlocks(Player player, BlockSet blocks) {
        resolveSkipFirst(player, blocks);
        if (player.isCreative()) {
            if (blocks.isEmpty() || !validateRequest(player, blocks)) return;
            applyBlockSet(player, blocks);
            return;
        }

        if (!ServerConfig.survivalBreaking.enabled.get()) {
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.survival_breaking_disabled");
            return;
        }

        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;
        if (!validateRequest(player, blocks)) return;

        List<BreakToolHelper.ToolSlot> candidates = BreakToolHelper.collectCandidates(player);
        int totalTicks = 0;
        int blockCount = 0;
        for (BlockEntry block : blocks) {
            if (blocks.isSkipped(block)) continue;
            blockCount++;

            var state = player.level().getBlockState(block.blockPos);
            var selected = BreakToolHelper.selectTool(player, player.level(), block.blockPos, state, candidates);
            if (BreakToolHelper.isImpossible(selected)) continue;

            var tool = selected == null ? net.minecraft.world.item.ItemStack.EMPTY : selected.get();
            totalTicks += BreakToolHelper.estimateBreakTicks(player.level(), block.blockPos, state, tool);
        }
        int delay = ToolSelector.capDelay(totalTicks, ServerConfig.survivalBreaking.maxDelayTicks.get());

        // Tools are not pre-damaged here; selection (and the actual durability cost) happens again
        // at apply time in applyBlockSet, against the live stacks.
        delayedEntries.add(new DelayedEntry(player, blocks, player.level().getGameTime() + delay));

        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToPlayer(serverPlayer, new BreakCountdownPacket(delay, blockCount, false));
        }
    }

    public void applyBlockSet(Player player, BlockSet blocks) {
        applyBlockSet(player, blocks, false);
    }

    //Redo restores whole states, so multi-item states (double slabs, candles...) cost all their items
    //Returns the entries that could not be redone (missing items, unminable block...), for the caller to keep
    //on the redo stack
    public BlockSet redoBlockSet(Player player, BlockSet blocks) {
        return applyBlockSet(player, blocks, true);
    }

    //Returns the entries that were not applied (empty unless restoring, see redoBlockSet)
    private BlockSet applyBlockSet(Player player, BlockSet blocks, boolean restoring) {

        if (!checkAndNotifyAllowedToUseMod(player)) return blocks;
        if (!validateBlockSet(player, blocks)) return blocks;

        SophisticatedBuilding.ITEM_USAGE_TRACKER.initialize();
        List<BreakToolHelper.ToolSlot> candidates = player.isCreative() ? null : BreakToolHelper.collectCandidates(player);
        var templates = new PlacementTemplates(player);
        //Only the blocks the mod itself changed (never the skipped first block, which vanilla handled)
        var undoSet = new BlockSet();
        var notAppliedSet = new BlockSet();
        int survivalBreaksAttempted = 0;
        int survivalBreaksSucceeded = 0;
        for (BlockEntry block : blocks) {
            if (blocks.isSkipped(block)) continue;

            boolean breaking = BlockUtilities.isNullOrAir(block.newBlockState);
            if (breaking && candidates != null) survivalBreaksAttempted++;

            if (applyBlockEntry(player, block, candidates, templates, restoring)) {
                undoSet.add(block);
                if (breaking && candidates != null) survivalBreaksSucceeded++;
            } else {
                notAppliedSet.add(block);
            }
        }

        //Remove items from inventory, except those already consumed individually during placement
        //(Adding items is done during BlockPlacerHelper.breakBlock)
        SophisticatedBuilding.ITEM_USAGE_TRACKER.calculateMissingItems(player);
        if (!player.isCreative()) {
            InventoryHelper.removeFromInventory(player, SophisticatedBuilding.ITEM_USAGE_TRACKER.getBulkRemovalCounts());
        }

        if (survivalBreaksAttempted > 0 && survivalBreaksSucceeded == 0) {
            SophisticatedBuilding.logTranslate(player, "", "sophisticatedbuilding.message.survival_break_nothing", "", true);
        }

        SophisticatedBuilding.UNDO_REDO.addUndo(player, undoSet);

        //Redo: nothing at all could be restored (as opposed to a partial success)
        if (restoring && undoSet.isEmpty() && !notAppliedSet.isEmpty()) {
            SophisticatedBuilding.logTranslate(player, "", "sophisticatedbuilding.message.redo_nothing", "", true);
        }

        return notAppliedSet;
    }

    //Returns the entries that could not be undone (missing items, unminable block...), for the caller to keep
    //on the undo stack
    public BlockSet undoBlockSet(Player player, BlockSet blocks) {

        if (!SophisticatedBuilding.UNDO_REDO.isAllowedToUndo(player)) return blocks;

        SophisticatedBuilding.ITEM_USAGE_TRACKER.initialize();
        List<BreakToolHelper.ToolSlot> candidates = player.isCreative() ? null : BreakToolHelper.collectCandidates(player);
        var templates = new PlacementTemplates(player);
        var redoSet = new BlockSet();
        var notUndoneSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.isSkipped(block)) continue;

            if (undoBlockEntry(player, block, candidates, templates)) {
                redoSet.add(block);
            } else {
                notUndoneSet.add(block);
            }
        }

        //Remove items from inventory
        //(Adding items is done during BlockPlacerHelper.breakBlock)
        SophisticatedBuilding.ITEM_USAGE_TRACKER.calculateMissingItems(player);
        if (!player.isCreative()) {
            InventoryHelper.removeFromInventory(player, SophisticatedBuilding.ITEM_USAGE_TRACKER.getBulkRemovalCounts());
        }

        SophisticatedBuilding.UNDO_REDO.addRedo(player, redoSet);

        //Nothing at all could be undone (as opposed to a partial success)
        if (redoSet.isEmpty() && !notUndoneSet.isEmpty()) {
            SophisticatedBuilding.logTranslate(player, "", "sophisticatedbuilding.message.undo_nothing", "", true);
        }

        return notUndoneSet;
    }

    //Build and redo charge the whole state (see restoreCost): a merge one item, three candles onto air three candles
    //restoring: redo, where a state that is already there counts as redone (nothing to place or charge)
    private boolean applyBlockEntry(Player player, BlockEntry block, @Nullable List<BreakToolHelper.ToolSlot> candidates,
                                    PlacementTemplates templates, boolean restoring) {

        block.existingBlockState = player.level().getBlockState(block.blockPos);
        if (restoring && isAlreadyThere(block.existingBlockState, block.newBlockState)) return true;
        boolean breaking = BlockUtilities.isNullOrAir(block.newBlockState);
        //Survival may only overwrite a real block by mining it, and only with survival replace enabled (merges excepted)
        ReplaceRules.Action action = breaking ? ReplaceRules.Action.BREAK : ReplaceRules.forPlacement(candidates != null,
                BlockUtilities.needsMining(block.existingBlockState), block.existingBlockState.is(block.newBlockState.getBlock()),
                BlockUtilities.isOneStepMerge(block.existingBlockState, block.newBlockState),
                ServerConfig.survivalReplace.enabled.get());
        if (action == ReplaceRules.Action.SKIP) return false;
        if (!validateBlockEntry(player, block, action != ReplaceRules.Action.PLACE)) return false;

        int count = breaking ? 1 : restoreCost(action, block.existingBlockState, block.newBlockState);

        isPlacingOrBreakingBlocks = true;
        try {
            return switch (action) {
                case BREAK -> BlockPlacerHelper.breakBlock(player, block, candidates);
                case REPLACE -> mineAndPlace(player, block, candidates, templates, count);
                default -> placeIfAvailable(player, block, templates, count);
            };
        } finally {
            isPlacingOrBreakingBlocks = false;
        }
    }

    //A placed state costs all its items: a double slab two slabs, three candles three candles. The same block placed
    //over without mining (a merge, snow layers) keeps its items, so only the difference is charged.
    private static int restoreCost(ReplaceRules.Action action, BlockState current, BlockState target) {
        boolean kept = action == ReplaceRules.Action.PLACE && current.is(target.getBlock());
        return ReplaceRules.restoreCost(BlockUtilities.itemCountForState(target), kept ? BlockUtilities.itemCountForState(current) : 0);
    }

    //count: items the entry costs, all or nothing
    private boolean placeIfAvailable(Player player, BlockEntry block, PlacementTemplates templates, int count) {
        //If we have the items in our inventory, place it
        if (SophisticatedBuilding.ITEM_USAGE_TRACKER.tryIncreaseUsageCount(block.item, count, player)) {
            return placeWithTemplate(player, block, templates, count);
        }
        //Not having the item at this point would be a bit weird
        //It could mean the client/server are out of sync, or the inventory changed during the short delay period
        //HUD already shows inventory counts visually, no need to spam chat
        return false;
    }

    //Survival replace: the items are checked first, then the block in the way is mined like a survival break
    //(tool durability, drops to the inventory, exhaustion). Nothing is mined or charged if either fails.
    private boolean mineAndPlace(Player player, BlockEntry block, List<BreakToolHelper.ToolSlot> candidates,
                                 PlacementTemplates templates, int count) {
        var tracker = SophisticatedBuilding.ITEM_USAGE_TRACKER;
        if (!tracker.tryIncreaseUsageCount(block.item, count, player)) return false;
        if (!BlockPlacerHelper.breakBlock(player, block, candidates)) {
            tracker.decreaseUsageCount(block.item, count);
            return false;
        }
        return placeWithTemplate(player, block, templates, count);
    }

    //Places the entry with the data of a real inventory stack; a stack with data is consumed right here in survival.
    //Only a block that was really placed is charged: a placement the server refused (a cancelled place event, water in
    //the nether, nothing changed) is taken back from the usage count.
    private boolean placeWithTemplate(Player player, BlockEntry block, PlacementTemplates templates, int count) {
        var tracker = SophisticatedBuilding.ITEM_USAGE_TRACKER;
        var template = block.item == null ? null : templates.find(block.item);
        if (!BlockPlacerHelper.placeBlock(player, block, template == null ? ItemStack.EMPTY : template.stack())) {
            tracker.decreaseUsageCount(block.item, count);
            return false;
        }
        //Each counted item comes from a template; the first one gives the placed block its data
        for (int i = 0; i < count && template != null; i++) {
            if (i > 0) template = templates.find(block.item);
            if (template.individual()) {
                //Taken from the exact stack and left out of the bulk removal
                tracker.addConsumedIndividually(block.item, 1);
                template.stack().shrink(1);
            }
        }
        return true;
    }

    //Undo of a merge: the block gets its old state back without mining, and survival gets back exactly the item the
    //merge charged (a turtle egg mined would drop nothing, and mining needs survival replace and a tool)
    private boolean unmerge(Player player, BlockEntry block, BlockState merged, boolean survival) {
        if (!BlockPlacerHelper.placeBlock(player, block)) return false;
        if (survival) {
            int refund = ReplaceRules.unmergeRefund(BlockUtilities.itemCountForState(merged), BlockUtilities.itemCountForState(block.newBlockState));
            Item item = merged.getBlock().asItem();
            if (refund > 0 && item != Items.AIR) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item, refund));
            }
        }
        return true;
    }

    private boolean undoBlockEntry(Player player, BlockEntry block, @Nullable List<BreakToolHelper.ToolSlot> candidates,
                                   PlacementTemplates templates) {

        boolean breaking = BlockUtilities.isNullOrAir(block.existingBlockState);

        var tempBlockEntry = new BlockEntry(block.blockPos);
        var temp = block.existingBlockState;
        tempBlockEntry.existingBlockState = block.newBlockState;
        tempBlockEntry.newBlockState = temp;
        if (!breaking) {
            //Re-placing a broken block costs its items like any placement; blocks without an item stay free
            Item item = temp.getBlock().asItem();
            tempBlockEntry.item = item == Items.AIR ? null : item;
        }

        //Survival: a real block in the way is mined (replace), never overwritten
        BlockState current = player.level().getBlockState(block.blockPos);
        //Already back in the old state (e.g. someone mined the placed block): done, instead of failing on every undo
        if (isAlreadyThere(current, temp)) {
            block.newBlockState = current;
            return true;
        }
        ReplaceRules.Action action = ReplaceRules.forUndo(candidates != null, breaking, tempBlockEntry.item != null,
                BlockUtilities.needsMining(current), current == temp, ServerConfig.survivalReplace.enabled.get(),
                !breaking && BlockUtilities.isOneStepMerge(temp, current));
        if (action == ReplaceRules.Action.SKIP) return false;
        boolean mining = action == ReplaceRules.Action.BREAK || action == ReplaceRules.Action.REPLACE;
        if (!validateBlockEntry(player, tempBlockEntry, mining)) return false;

        //Update newBlockState for future redo's
        block.newBlockState = current;

        int count = breaking ? 1 : restoreCost(action, current, temp);

        isPlacingOrBreakingBlocks = true;
        try {
            return switch (action) {
                case BREAK -> BlockPlacerHelper.breakBlock(player, tempBlockEntry, candidates);
                case UNMERGE -> unmerge(player, tempBlockEntry, current, candidates != null);
                case REPLACE -> mineAndPlace(player, tempBlockEntry, candidates, templates, count);
                default -> placeIfAvailable(player, tempBlockEntry, templates, count);
            };
        } finally {
            isPlacingOrBreakingBlocks = false;
        }
    }

    //The block already has the state an undo or redo would restore (a break restores air)
    private static boolean isAlreadyThere(BlockState current, @Nullable BlockState target) {
        return BlockUtilities.isNullOrAir(target) ? current.isAir() : current == target;
    }

    private boolean checkAndNotifyAllowedToUseMod(Player player) {

        if (!player.getAbilities().mayBuild) {
            SophisticatedBuilding.message(player, false, "sophisticatedbuilding.message.not_allowed_to_build");
            return false;
        }

        if (!isAllowedToUseMod(player)) {
            SophisticatedBuilding.message(player, false, "sophisticatedbuilding.message.not_allowed_to_use_mod");
            return false;
        }
        return true;
    }

    private boolean isAllowedToUseMod(Player player) {

        if (!ServerConfig.validation.allowInSurvival.get() && !player.isCreative()) return false;

        if (ServerConfig.validation.useWhitelist.get()) {
            return ServerConfig.validation.whitelist.get().contains(player.getGameProfile().getName());
        }

        return true;
    }

    //The client's skipFirst is decided when the click arrives: vanilla handled the first block only if it was not cancelled
    private static void resolveSkipFirst(Player player, BlockSet blocks) {
        blocks.skipFirst = ReplaceRules.shouldSkipFirst(blocks.skipFirst, ServerBuildState.isLikeVanilla(player));
    }

    //A build or break request from a client: the limits of the player's power level apply here too, since a modified
    //client could send anything (see BuildLimits). An out-of-reach start, an extent over the blocks per axis or a block
    //farther than the build mode and the player's modifiers reach reject the request; more blocks than may be placed at
    //once are cut to that limit.
    private boolean validateRequest(Player player, BlockSet blocks) {
        if (blocks.firstPos == null) return false;
        int maxPerAxis = AttachmentHandler.getMaxBlocksPerAxis(player, false);
        double startReach = BuildLimits.startReach(AttachmentHandler.getPlacementReach(player, false), player.blockInteractionRange());
        BlockPos center = player.blockPosition();
        if (!BuildLimits.startWithinReach(center.distSqr(blocks.firstPos), startReach)) {
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.request_out_of_reach");
            return false;
        }
        BlockPos last = blocks.lastPos != null ? blocks.lastPos : blocks.firstPos;
        if (!BuildLimits.extentWithinAxisLimit(last.getX() - blocks.firstPos.getX(), last.getY() - blocks.firstPos.getY(),
                last.getZ() - blocks.firstPos.getZ(), maxPerAxis)) {
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.request_over_axis_limit", maxPerAxis);
            return false;
        }
        CompoundTag modifiers = Services.PLATFORM.getPersistentData(player).getCompound(ModifierSettingsPacket.DATA_KEY);
        int modifierReach = ModifierLimits.reach(modifiers, maxPerAxis, AttachmentHandler.getMaxMirrorRadius(player, false));
        int maxDistance = BuildLimits.maxBlockDistance(AttachmentHandler.getBuildModeReach(player), startReach, maxPerAxis, modifierReach);
        for (BlockEntry block : blocks) {
            BlockPos pos = block.blockPos;
            int distance = Math.max(Math.abs(pos.getX() - center.getX()),
                    Math.max(Math.abs(pos.getY() - center.getY()), Math.abs(pos.getZ() - center.getZ())));
            if (distance > maxDistance) {
                SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.request_out_of_reach");
                return false;
            }
        }

        int maxAtOnce = AttachmentHandler.getMaxBlocksPlacedAtOnce(player, false);
        List<BlockEntry> unskipped = new ArrayList<>();
        for (BlockEntry block : blocks) {
            if (!blocks.isSkipped(block)) unskipped.add(block);
        }
        int allowed = BuildLimits.allowedCount(unskipped.size(), maxAtOnce);
        if (allowed <= 0) return false;
        if (allowed < unskipped.size()) {
            //The start stays, the rest is cut
            unskipped.sort((a, b) -> Boolean.compare(!a.blockPos.equals(blocks.firstPos), !b.blockPos.equals(blocks.firstPos)));
            for (BlockEntry block : unskipped.subList(allowed, unskipped.size())) {
                blocks.remove(block.blockPos);
            }
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.request_capped", maxAtOnce);
        }
        return true;
    }

    private boolean validateBlockSet(Player player, BlockSet blocks) {

        if (blocks.isEmpty()) {
            SophisticatedBuilding.message(player, false, "sophisticatedbuilding.message.no_blocks");
            return false;
        }
        //Vanilla already handled the only block
        if (!blocks.hasUnskippedEntries()) return false;
        if (blocks.size() > ServerConfig.validation.maxBlocksPlacedAtOnce.get()) {
            SophisticatedBuilding.message(player, false, "sophisticatedbuilding.message.too_many_blocks", ServerConfig.validation.maxBlocksPlacedAtOnce.get());
            return false;
        }

        //Dont allow mixing breaking and placing blocks
        if (isMixedPlacingAndBreaking(blocks)) {
            SophisticatedBuilding.message(player, false, "sophisticatedbuilding.message.mixed_place_break");
            return false;
        }

        return true;
    }

    private boolean isMixedPlacingAndBreaking(BlockSet blocks) {

        //The first entry that is not skipped determines if we are breaking or placing
        Boolean breaking = null;
        for (BlockEntry block : blocks) {
            if (blocks.isSkipped(block)) continue;

            boolean blockBreaking = BlockUtilities.isNullOrAir(block.newBlockState);
            if (breaking == null) {
                breaking = blockBreaking;
            } else if (breaking != blockBreaking) {
                return true;
            }
        }

        return false;
    }

    private boolean validateBlockEntry(Player player, BlockEntry block, boolean breaking) {

        if (!player.level().isLoaded(block.blockPos)) return false;

        if (breaking && BlockUtilities.isNullOrAir(block.existingBlockState)) return false;

        //Like vanilla for every block use and break, in any game mode: spawn protection and world border
        //(operators bypass spawn protection) and adventure mode restrictions
        if (!player.level().mayInteract(player, block.blockPos)) return false;
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.blockActionRestricted(serverPlayer.level(), block.blockPos, serverPlayer.gameMode.getGameModeForPlayer())) {
            return false;
        }

        return true;
    }
    
    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
}
