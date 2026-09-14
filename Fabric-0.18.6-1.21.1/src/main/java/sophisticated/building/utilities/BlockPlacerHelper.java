package sophisticated.building.utilities;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.ServerConfig;
import sophisticated.building.create.foundation.utility.BlockHelper;
import sophisticated.building.inventory.ItemHandlerHelper;

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

        BlockState previous = level.getBlockState(blockEntry.blockPos);
        BlockHelper.placeSchematicBlock(level, blockEntry.newBlockState, blockEntry.blockPos, itemStack, null);
        return level.getBlockState(blockEntry.blockPos) != previous;
    }
}
