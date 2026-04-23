package sophisticated.building.utilities;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.create.foundation.utility.BlockHelper;
import sophisticated.building.inventory.ItemHandlerHelper;

//Server only
public class BlockPlacerHelper {

    public static boolean breakBlock(Player player, BlockEntry blockEntry) {
        // In Creative mode, use empty hand (no tool needed)
        ItemStack usedTool = ItemStack.EMPTY;

        boolean brokeBlock = BlockHelper.destroyBlockAs(player.level(), blockEntry.blockPos, player, usedTool, 0f, stack -> {
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        });
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
