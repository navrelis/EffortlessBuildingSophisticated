package sophisticated.building.systems;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.ServerConfig;
import sophisticated.building.network.message.BreakCountdownPacket;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockPlacerHelper;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.BlockUtilities;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.InventoryHelper;
import sophisticated.building.utilities.PlacementTemplates;
import sophisticated.building.utilities.ToolSelector;

import javax.annotation.Nullable;
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

        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;

        delayedEntries.add(new DelayedEntry(player, blocks, placeTime));
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
        if (player.isCreative()) {
            applyBlockSet(player, blocks);
            return;
        }

        if (!ServerConfig.survivalBreaking.enabled.get()) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "Survival breaking is disabled on this server.", true);
            return;
        }

        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;

        List<BreakToolHelper.ToolSlot> candidates = BreakToolHelper.collectCandidates(player);
        int totalTicks = 0;
        int blockCount = 0;
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;
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
            ServerPlayNetworking.send(serverPlayer, new BreakCountdownPacket(delay, blockCount));
        }
    }

    public void applyBlockSet(Player player, BlockSet blocks) {

        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;

        SophisticatedBuilding.ITEM_USAGE_TRACKER.initialize();
        List<BreakToolHelper.ToolSlot> candidates = player.isCreative() ? null : BreakToolHelper.collectCandidates(player);
        var templates = new PlacementTemplates(player);
        var undoSet = new BlockSet();
        int survivalBreaksAttempted = 0;
        int survivalBreaksSucceeded = 0;
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;

            boolean breaking = BlockUtilities.isNullOrAir(block.newBlockState);
            if (breaking && candidates != null) survivalBreaksAttempted++;

            if (applyBlockEntry(player, block, candidates, templates)) {
                undoSet.add(block);
                if (breaking && candidates != null) survivalBreaksSucceeded++;
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
    }

    public void undoBlockSet(Player player, BlockSet blocks) {

        if (!SophisticatedBuilding.UNDO_REDO.isAllowedToUndo(player)) return;

        SophisticatedBuilding.ITEM_USAGE_TRACKER.initialize();
        List<BreakToolHelper.ToolSlot> candidates = player.isCreative() ? null : BreakToolHelper.collectCandidates(player);
        var templates = new PlacementTemplates(player);
        var redoSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;

            if (undoBlockEntry(player, block, candidates, templates)) {
                redoSet.add(block);
            }
        }

        //Remove items from inventory
        //(Adding items is done during BlockPlacerHelper.breakBlock)
        SophisticatedBuilding.ITEM_USAGE_TRACKER.calculateMissingItems(player);
        if (!player.isCreative()) {
            InventoryHelper.removeFromInventory(player, SophisticatedBuilding.ITEM_USAGE_TRACKER.getBulkRemovalCounts());
        }

        SophisticatedBuilding.UNDO_REDO.addRedo(player, redoSet);
    }

    private boolean applyBlockEntry(Player player, BlockEntry block, @Nullable List<BreakToolHelper.ToolSlot> candidates,
                                    PlacementTemplates templates) {

        block.existingBlockState = player.level().getBlockState(block.blockPos);
        boolean breaking = BlockUtilities.isNullOrAir(block.newBlockState);
        if (!validateBlockEntry(player, block, breaking)) return false;

        boolean success;
        isPlacingOrBreakingBlocks = true;
        if (breaking) {
            success = BlockPlacerHelper.breakBlock(player, block, candidates);
        } else {
            //If we have the item in our inventory, place it
            if (SophisticatedBuilding.ITEM_USAGE_TRACKER.increaseUsageCount(block.item, 1, player)) {
                success = placeWithTemplate(player, block, templates);
            } else {
                success = false;
                //Not having the item at this point would be a bit weird
                //It could mean the client/server are out of sync, or the inventory changed during the short delay period
                //HUD already shows inventory counts visually, no need to spam chat
            }
        }
        isPlacingOrBreakingBlocks = false;
        return success;
    }

    //Places the entry with the data of a real inventory stack; a stack with data is consumed right here in survival
    private boolean placeWithTemplate(Player player, BlockEntry block, PlacementTemplates templates) {
        var template = block.item == null ? null : templates.find(block.item);
        boolean success = BlockPlacerHelper.placeBlock(player, block, template == null ? ItemStack.EMPTY : template.stack());
        if (template != null && template.individual()) {
            //Taken from the exact stack (only if placed); either way it is left out of the bulk removal
            SophisticatedBuilding.ITEM_USAGE_TRACKER.addConsumedIndividually(block.item, 1);
            if (success) template.stack().shrink(1);
        }
        return success;
    }

    private boolean undoBlockEntry(Player player, BlockEntry block, @Nullable List<BreakToolHelper.ToolSlot> candidates,
                                   PlacementTemplates templates) {

        boolean breaking = BlockUtilities.isNullOrAir(block.existingBlockState);

        var tempBlockEntry = new BlockEntry(block.blockPos);
        var temp = block.existingBlockState;
        tempBlockEntry.existingBlockState = block.newBlockState;
        tempBlockEntry.newBlockState = temp;
        if (!breaking) {
            //Re-placing a broken block costs its item like any placement; blocks without an item stay free
            Item item = temp.getBlock().asItem();
            tempBlockEntry.item = item == Items.AIR ? null : item;
        }

        if (!validateBlockEntry(player, tempBlockEntry, breaking)) return false;

        //Update newBlockState for future redo's
        block.newBlockState = player.level().getBlockState(block.blockPos);

        boolean success;
        isPlacingOrBreakingBlocks = true;
        if (breaking) {
            success = BlockPlacerHelper.breakBlock(player, tempBlockEntry, candidates);
        } else {
            //If we have the item in our inventory, place it
            if (SophisticatedBuilding.ITEM_USAGE_TRACKER.increaseUsageCount(tempBlockEntry.item, 1, player)) {
                success = placeWithTemplate(player, tempBlockEntry, templates);
            } else {
                success = false;
                //Not having the item at this point would be a bit weird
                //HUD already shows inventory counts visually, no need to spam chat
            }
        }
        isPlacingOrBreakingBlocks = false;

        return success;
    }

    private boolean checkAndNotifyAllowedToUseMod(Player player) {

        if (!player.getAbilities().mayBuild) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "You are not allowed to build.");
            return false;
        }

        if (!isAllowedToUseMod(player)) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "You are not allowed to use Sophisticated Building.");
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

    private boolean validateBlockSet(Player player, BlockSet blocks) {

        if (blocks.isEmpty()) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "No blocks to place.");
            return false;
        }
        if (blocks.skipFirst && blocks.size() == 1 && blocks.iterator().next().blockPos == blocks.firstPos) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "No blocks to place because the first block was skipped.");
            return false;
        }
        if (blocks.size() > ServerConfig.validation.maxBlocksPlacedAtOnce.get()) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "Too many blocks to place. Max: " + ServerConfig.validation.maxBlocksPlacedAtOnce.get());
            return false;
        }

        //Dont allow mixing breaking and placing blocks
        if (isMixedPlacingAndBreaking(player, blocks)) {
            SophisticatedBuilding.log(player, ChatFormatting.RED + "Cannot mix breaking and placing blocks.");
            return false;
        }

        return true;
    }

    private boolean isMixedPlacingAndBreaking(Player player, BlockSet blocks) {

        //First determine if we are breaking or placing
        var iterator = blocks.iterator();

        //Get any block from the set, skip first if we have to
        var anyBlock = iterator.next();
        if (blocks.skipFirst && anyBlock.blockPos == blocks.firstPos) {
            anyBlock = iterator.next();
        }

        boolean breaking = anyBlock.newBlockState == null || anyBlock.newBlockState.isAir();

        while (iterator.hasNext()) {
            var block = iterator.next();
            if (block.newBlockState == null || block.newBlockState.isAir()) {
                if (!breaking) return true;
            } else {
                if (breaking) return true;
            }
        }

        return false;
    }

    private boolean validateBlockEntry(Player player, BlockEntry block, boolean breaking) {

        if (!player.level().isLoaded(block.blockPos)) return false;

        if (breaking && BlockUtilities.isNullOrAir(block.existingBlockState)) return false;

        if (breaking && !player.isCreative()) {
            if (!player.level().mayInteract(player, block.blockPos)) return false;
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.blockActionRestricted(serverPlayer.level(), block.blockPos, serverPlayer.gameMode.getGameModeForPlayer())) {
                return false;
            }
        }

        return true;
    }
    
    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
}
