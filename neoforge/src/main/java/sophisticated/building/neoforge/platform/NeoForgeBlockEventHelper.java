package sophisticated.building.neoforge.platform;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import sophisticated.building.platform.services.IBlockEventHelper;

import java.util.List;

public final class NeoForgeBlockEventHelper implements IBlockEventHelper {

    //ForgeHooks::onPlaceItemIntoWorld, the placement runs with block snapshot capturing
    @Override
    public boolean placeBlock(Player player, Level level, BlockPos pos, Runnable placement) {
        level.captureBlockSnapshots = true;
        placement.run();
        level.captureBlockSnapshots = false;

        //Find out if we get to keep the placed block by sending a forge event
        @SuppressWarnings("unchecked")
        List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>) level.capturedBlockSnapshots.clone();
        level.capturedBlockSnapshots.clear();
        Direction side = Direction.UP;

        boolean eventResult = false;
        if (blockSnapshots.size() > 1) {
            eventResult = EventHooks.onMultiBlockPlace(player, blockSnapshots, side);
        } else if (blockSnapshots.size() == 1) {
            eventResult = EventHooks.onBlockPlace(player, blockSnapshots.get(0), side);
        }

        if (eventResult) {
            // revert back all captured blocks
            for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots)) {
                level.restoringBlockSnapshots = true;
                blocksnapshot.restore(true, false);
                level.restoringBlockSnapshots = false;
            }
        } else {
            for (BlockSnapshot snap : blockSnapshots) {
                int updateFlag = snap.getFlag();
                BlockState oldBlock = snap.getReplacedBlock();
                BlockState newBlock = level.getBlockState(snap.getPos());
                newBlock.onPlace(level, snap.getPos(), oldBlock, false);

                level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
            }
        }
        level.capturedBlockSnapshots.clear();
        return !eventResult;
    }

    @Override
    public boolean fireBlockBreakEvent(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public boolean isRestoringBlockSnapshots(Level level) {
        return level.restoringBlockSnapshots;
    }

    @Override
    public void onBlockDropsCollected(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Player player, ItemStack tool) {
        // 1.20.4 has no block drops event: the experience its break event would compute, for the used tool
        int fortune = tool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
        int silkTouch = tool.getEnchantmentLevel(Enchantments.SILK_TOUCH);
        int experience = state.getExpDrop(level, level.random, pos, fortune, silkTouch);
        if (experience > 0)
            state.getBlock().popExperience(level, pos, experience);
    }

    @Override
    public ItemStack getSpawnAfterBreakTool(ItemStack tool) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean doesBrokenIceTurnIntoWater(Level level, ItemStack tool) {
        return tool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 0;
    }

    @Override
    public void markAndNotifyBlock(Level level, BlockPos pos, LevelChunk chunk, BlockState oldState, BlockState newState, int flags) {
        level.markAndNotifyBlock(pos, chunk, oldState, newState, flags, 512);
    }

    @Override
    public boolean placeSpecialPlantable(Level level, BlockState state, BlockPos pos, ItemStack stack) {
        // NeoForge 20.4 has no SpecialPlantable (its IPlantable, implemented by the vanilla plants, only gives their
        // default state), so no block places itself this way.
        return false;
    }

    @Override
    public boolean canHarvestBlock(Player player, BlockState state, Level level, BlockPos pos) {
        return CommonHooks.isCorrectToolForDrops(state, player);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getBlock().getSoundType(state, level, pos, entity);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(level, pos, rotation);
    }
}
