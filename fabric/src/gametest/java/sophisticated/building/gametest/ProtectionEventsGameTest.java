package sophisticated.building.gametest;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Claim and protection mods get their say on Fabric: the mod's breaks fire Fabric API's player block break events
 * (a listener that cancels BEFORE keeps the block, AFTER follows every broken block). Minecraft 1.16.x has no Common
 * Protection API (it needs Java 17), so there is no placement check to test here.
 */
public class ProtectionEventsGameTest {

    private static final Set<BlockPos> REFUSE_BREAK = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<BlockPos> BROKEN_AFTER = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static boolean registered;

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !REFUSE_BREAK.contains(pos));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> BROKEN_AFTER.add(pos.immutable()));
    }

    @ServerTest
    public void cancelledBreakEventKeepsTheBlock(ServerTestHelper helper) {
        register();
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        BlockPos refused = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos allowed = helper.absolutePos(new BlockPos(3, 1, 1));
        try (ConfigScope config = ConfigScope.baseline()) {
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
            REFUSE_BREAK.add(refused);

            //Creative breaks are applied at once
            SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, set(breaking(refused), breaking(allowed)));

            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 1));
            assertTrue(BROKEN_AFTER.contains(allowed), "PlayerBlockBreakEvents.AFTER should follow the broken block");
            assertTrue(!BROKEN_AFTER.contains(refused), "PlayerBlockBreakEvents.AFTER must not fire for the refused block");
        } finally {
            REFUSE_BREAK.remove(refused);
            removePlayer(player);
        }
        helper.succeed();
    }
}
