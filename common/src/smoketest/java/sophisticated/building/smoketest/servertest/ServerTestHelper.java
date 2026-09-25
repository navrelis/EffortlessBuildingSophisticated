package sophisticated.building.smoketest.servertest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What a server test sees of the world (the subset of vanilla's {@code GameTestHelper} the tests use, same method
 * names): positions are relative to the test's origin, whose layer y = 0 and everything above it is air when the test
 * starts, with stone below it.
 */
public final class ServerTestHelper {

    private final ServerLevel level;
    private final BlockPos origin;
    private final List<ServerTestSequence> sequences = new ArrayList<>();
    private boolean succeeded;

    ServerTestHelper(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos absolutePos(BlockPos relativePos) {
        return origin.offset(relativePos);
    }

    public BlockState getBlockState(BlockPos relativePos) {
        return level.getBlockState(absolutePos(relativePos));
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos relativePos) {
        return level.getBlockEntity(absolutePos(relativePos));
    }

    public void setBlock(BlockPos relativePos, BlockState state) {
        level.setBlock(absolutePos(relativePos), state, 3 /* Block.UPDATE_ALL */);
    }

    public void setBlock(BlockPos relativePos, Block block) {
        setBlock(relativePos, block.defaultBlockState());
    }

    public void assertBlockPresent(Block block, BlockPos relativePos) {
        BlockState state = getBlockState(relativePos);
        if (!state.is(block)) {
            throw new ServerTestAssertException("Expected " + Registry.BLOCK.getKey(block) + ", got "
                    + Registry.BLOCK.getKey(state.getBlock()) + " at " + shortString(relativePos));
        }
    }

    public ServerTestSequence startSequence() {
        ServerTestSequence sequence = new ServerTestSequence(this);
        sequences.add(sequence);
        return sequence;
    }

    public void succeed() {
        succeeded = true;
    }

    public void fail(String message) {
        throw new ServerTestAssertException(message);
    }

    /** "x, y, z" like {@code BlockPos.toShortString()}, which is client only in Minecraft 1.16.3. */
    public static String shortString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    boolean hasSucceeded() {
        return succeeded;
    }

    /** Runs the sequences of this tick; a waiting step's assertion is rethrown for the timeout message. */
    void tick(long tick) {
        ServerTestAssertException waiting = null;
        for (ServerTestSequence sequence : new ArrayList<>(sequences)) {
            try {
                sequence.tick(tick);
            } catch (ServerTestAssertException e) {
                waiting = e;
            }
        }
        if (waiting != null) {
            throw waiting;
        }
    }
}
