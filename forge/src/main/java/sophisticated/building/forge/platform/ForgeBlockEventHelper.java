package sophisticated.building.forge.platform;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import sophisticated.building.platform.services.IBlockEventHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge's block hooks, the counterparts of NeoForge's. Forge moved the experience of a broken block
 * from {@code spawnAfterBreak} to the break event ({@code getExpToDrop}), so the experience of the last
 * build-mode break event is kept until its drops are collected and popped then, as Forge's
 * {@code ServerPlayerGameMode.destroyBlock} does.
 */
public final class ForgeBlockEventHelper implements IBlockEventHelper {

    // Experience of the last break event that was not cancelled (server thread only)
    private BlockPos pendingExperiencePos;
    private int pendingExperience;

    //ForgeHooks::onPlaceItemIntoWorld, the placement runs with block snapshot capturing
    @Override
    public boolean placeBlock(Player player, Level level, BlockPos pos, Runnable placement) {
        level.captureBlockSnapshots = true;
        placement.run();
        level.captureBlockSnapshots = false;

        //Find out if we get to keep the placed block by sending a forge event
        List<BlockSnapshot> blockSnapshots = new ArrayList<>(level.capturedBlockSnapshots);
        level.capturedBlockSnapshots.clear();
        Direction side = Direction.UP;

        boolean eventResult = false;
        if (blockSnapshots.size() > 1) {
            eventResult = ForgeEventFactory.onMultiBlockPlace(player, blockSnapshots, side);
        } else if (blockSnapshots.size() == 1) {
            eventResult = ForgeEventFactory.onBlockPlace(player, blockSnapshots.get(0), side);
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
        MinecraftForge.EVENT_BUS.post(event);
        pendingExperiencePos = event.isCanceled() ? null : pos.immutable();
        pendingExperience = event.getExpToDrop();
        return !event.isCanceled();
    }

    @Override
    public boolean isRestoringBlockSnapshots(Level level) {
        return level.restoringBlockSnapshots;
    }

    @Override
    public void onBlockDropsCollected(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, Player player, ItemStack tool) {
        // The break event already dropped the experience to 0 when the player cannot harvest the block
        if (pos.equals(pendingExperiencePos) && pendingExperience > 0) {
            state.getBlock().popExperience(level, pos, pendingExperience);
        }
        pendingExperiencePos = null;
        pendingExperience = 0;
    }

    /** The used tool, as vanilla passes it: on Forge {@code spawnAfterBreak} drops no experience. */
    @Override
    public ItemStack getSpawnAfterBreakTool(ItemStack tool) {
        return tool;
    }

    @Override
    public boolean doesBrokenIceTurnIntoWater(Level level, ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0;
    }

    @Override
    public void markAndNotifyBlock(Level level, BlockPos pos, LevelChunk chunk, BlockState oldState, BlockState newState, int flags) {
        level.markAndNotifyBlock(pos, chunk, oldState, newState, flags, 512);
    }

    /** Forge has no counterpart of NeoForge's {@code SpecialPlantable}. */
    @Override
    public boolean placeSpecialPlantable(Level level, BlockState state, BlockPos pos, ItemStack stack) {
        return false;
    }

    /** Forge's harvest check, which also fires its harvest check event. */
    @Override
    public boolean canHarvestBlock(Player player, BlockState state, Level level, BlockPos pos) {
        return state.canHarvestBlock(level, pos, player);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getSoundType(level, pos, entity);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(level, pos, rotation);
    }
}
