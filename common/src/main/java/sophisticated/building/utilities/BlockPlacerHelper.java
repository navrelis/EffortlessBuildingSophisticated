package sophisticated.building.utilities;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.ServerConfig;
import sophisticated.building.create.foundation.utility.BlockHelper;
import sophisticated.building.inventory.ItemHandlerHelper;
import sophisticated.building.platform.Services;

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

    public static boolean placeBlock(Player player, BlockEntry blockEntry) {
        return placeBlock(player, blockEntry, ItemStack.EMPTY);
    }

    //ForgeHooks::onPlaceItemIntoWorld, removed itemstack usage (consumption is up to the caller)
    //The template's data components are applied to the placed block; an empty template means a plain stack.
    public static boolean placeBlock(Player player, BlockEntry blockEntry, ItemStack template) {

        Level level = player.level();
        ItemStack itemStack;
        if (!template.isEmpty()) {
            itemStack = template.copyWithCount(1);
        } else {
            //Undo re-placements have no item
            Item item = blockEntry.item != null ? blockEntry.item : blockEntry.newBlockState.getBlock().asItem();
            itemStack = new ItemStack(item);
        }

        // The loader fires its block place events around the placement (NeoForge reverts a cancelled placement).
        return Services.BLOCK_EVENTS.placeBlock(player, level, blockEntry.blockPos,
                () -> BlockHelper.placeSchematicBlock(level, blockEntry.newBlockState, blockEntry.blockPos, itemStack, null, player));
    }
}
