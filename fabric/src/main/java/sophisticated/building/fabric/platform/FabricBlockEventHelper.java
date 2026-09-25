package sophisticated.building.fabric.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import sophisticated.building.platform.services.IBlockEventHelper;

/**
 * Plain vanilla behaviour: Fabric fires no events for these server-side block operations.
 */
public final class FabricBlockEventHelper implements IBlockEventHelper {

    @Override
    public boolean placeBlock(Player player, Level level, BlockPos pos, Runnable placement) {
        //No place event on Fabric: nothing can refuse the placement (whether a block was set is up to the caller)
        placement.run();
        return true;
    }

    @Override
    public boolean fireBlockBreakEvent(Level level, BlockPos pos, BlockState state, Player player) {
        return true;
    }

    @Override
    public boolean isRestoringBlockSnapshots(Level level) {
        return false;
    }

    @Override
    public void onBlockDropsCollected(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Player player, ItemStack tool) {
    }

    @Override
    public ItemStack getSpawnAfterBreakTool(ItemStack tool) {
        return tool;
    }

    @Override
    public boolean doesBrokenIceTurnIntoWater(Level level, ItemStack tool) {
        return false;
    }

    @Override
    public void markAndNotifyBlock(Level level, BlockPos pos, LevelChunk chunk, BlockState oldState, BlockState newState, int flags) {
    }

    @Override
    public boolean placeSpecialPlantable(Level level, BlockState state, BlockPos pos, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canHarvestBlock(Player player, BlockState state, Level level, BlockPos pos) {
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getSoundType();
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(rotation);
    }
}
