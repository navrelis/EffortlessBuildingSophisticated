package sophisticated.building.utilities;

import com.google.common.collect.Lists;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import sophisticated.building.ServerConfig;
import sophisticated.building.create.foundation.utility.BlockHelper;

import javax.annotation.Nullable;
import java.util.List;

//Server only
public class BlockPlacerHelper {

    public static boolean breakBlock(Player player, BlockEntry blockEntry) {
        return breakBlock(player, blockEntry, null);
    }

    /**
     * @param candidates survival tool candidates (see {@link BreakToolHelper#collectCandidates}),
     *                    or {@code null} for creative behaviour (always breaks with the empty hand).
     */
    public static boolean breakBlock(Player player, BlockEntry blockEntry, @Nullable List<BreakToolHelper.ToolSlot> candidates) {
        if (candidates == null || player.isCreative()) {
            // In Creative mode, use empty hand (no tool needed)
            ItemStack usedTool = ItemStack.EMPTY;

            return BlockHelper.destroyBlockAs(player.level(), blockEntry.blockPos, player, usedTool, 0f, stack -> {
                if (!player.isCreative()) {
                    ItemHandlerHelper.giveItemToPlayer(player, stack);
                }
            });
        }

        Level level = player.level();
        BlockState state = level.getBlockState(blockEntry.blockPos);
        BreakToolHelper.ToolSlot selectedSlot = BreakToolHelper.selectTool(player, level, blockEntry.blockPos, state, candidates);
        if (BreakToolHelper.isImpossible(selectedSlot)) {
            return false;
        }

        ItemStack tool = selectedSlot == null ? ItemStack.EMPTY : selectedSlot.get().copy();

        boolean brokeBlock = BlockHelper.destroyBlockAs(level, blockEntry.blockPos, player, tool, 0f,
                stack -> ItemHandlerHelper.giveItemToPlayer(player, stack));

        if (brokeBlock) {
            if (selectedSlot != null) {
                selectedSlot.set(tool);
            }
            player.causeFoodExhaustion(ServerConfig.survivalBreaking.exhaustionPerBlock.get().floatValue());
        }

        return brokeBlock;
    }

    //ForgeHooks::onPlaceItemIntoWorld, removed itemstack usage
    public static boolean placeBlock(Player player, BlockEntry blockEntry) {

        Level level = player.level();
        var itemStack = new ItemStack(blockEntry.item);

        level.captureBlockSnapshots = true;
        BlockHelper.placeSchematicBlock(level, blockEntry.newBlockState, blockEntry.blockPos, itemStack, null);
        level.captureBlockSnapshots = false;

        //Find out if we get to keep the placed block by sending a forge event
        @SuppressWarnings("unchecked")
        List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>)level.capturedBlockSnapshots.clone();
        level.capturedBlockSnapshots.clear();
        Direction side = Direction.UP;

        boolean eventResult = false;
        if (blockSnapshots.size() > 1)
        {
            eventResult = EventHooks.onMultiBlockPlace(player, blockSnapshots, side);
        }
        else if (blockSnapshots.size() == 1)
        {
            eventResult = EventHooks.onBlockPlace(player, blockSnapshots.get(0), side);
        }

        if (eventResult)
        {
            // revert back all captured blocks
            for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots))
            {
                level.restoringBlockSnapshots = true;
                blocksnapshot.restore(Block.UPDATE_NONE);
                level.restoringBlockSnapshots = false;
            }
        }
        else
        {
            for (BlockSnapshot snap : blockSnapshots)
            {
                int updateFlag = snap.getFlags();
                BlockState oldBlock = snap.getState();
                BlockState newBlock = level.getBlockState(snap.getPos());
                newBlock.onPlace(level, snap.getPos(), oldBlock, false);

                level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
            }
        }
        level.capturedBlockSnapshots.clear();
        return !eventResult;
    }
}
