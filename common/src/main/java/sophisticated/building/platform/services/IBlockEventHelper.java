package sophisticated.building.platform.services;

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

import javax.annotation.Nullable;

/**
 * Block placing/breaking hooks whose behaviour depends on the loader: NeoForge fires its block
 * events and uses its block extension methods, Fabric uses plain vanilla behaviour (it fires no
 * events for these server-side operations).
 */
public interface IBlockEventHelper {

    /**
     * Places a block through {@code placement} on behalf of {@code player}. NeoForge captures the
     * block snapshots and fires the (multi) place event, reverting the placement when it is cancelled;
     * Fabric just runs the placement.
     *
     * @return false if a listener cancelled the placement (always true on Fabric, which has no such event); whether
     * the placement set a block at all is reported by the placement itself
     */
    boolean placeBlock(Player player, Level level, BlockPos pos, Runnable placement);

    /** Fires the loader's block break event for a build-mode break; false if a listener cancelled it. */
    boolean fireBlockBreakEvent(Level level, BlockPos pos, BlockState state, Player player);

    /** After a build-mode break removed the block: Fabric fires its player break AFTER event; nothing on NeoForge/Forge. */
    default void afterBlockBroken(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Player player) {
    }

    /** True while the loader is restoring captured block snapshots (no drops may be produced). */
    boolean isRestoringBlockSnapshots(Level level);

    /**
     * Called after the drops of a player-broken block were collected. Forge pops the experience of the
     * block break event (Forge moved it there from spawnAfterBreak); Fabric does nothing (experience comes from
     * {@link BlockState#spawnAfterBreak}, see {@link #getSpawnAfterBreakTool}).
     */
    void onBlockDropsCollected(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Player player, ItemStack tool);

    /**
     * The tool passed to {@link BlockState#spawnAfterBreak}: the used tool on both loaders (on Forge it drops no
     * experience, {@link #onBlockDropsCollected} pops it).
     */
    ItemStack getSpawnAfterBreakTool(ItemStack tool);

    /**
     * Whether breaking ice with {@code tool} turns it into water like vanilla mining does. Forge:
     * without Silk Touch. Fabric: never (4.2.1 behaviour).
     */
    boolean doesBrokenIceTurnIntoWater(Level level, ItemStack tool);

    /** Forge's post-placement update when a rail is placed without updates; nothing on Fabric. */
    void markAndNotifyBlock(Level level, BlockPos pos, LevelChunk chunk, BlockState oldState, BlockState newState, int flags);

    /**
     * Places a block that plants itself with custom placement (NeoForge 1.21+ {@code SpecialPlantable}).
     *
     * @return true if the block is such a plantable and was handled; always false on Fabric and on Forge
     * 1.19.2, which have no such interface
     */
    boolean placeSpecialPlantable(Level level, BlockState state, BlockPos pos, ItemStack stack);

    /** Whether the player can harvest the block (NeoForge fires its harvest check event). */
    boolean canHarvestBlock(Player player, BlockState state, Level level, BlockPos pos);

    /** The block's sound type (NeoForge's position/entity aware variant). */
    SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity);

    /** Rotates a block state (NeoForge's level/position aware variant). */
    BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation);
}
