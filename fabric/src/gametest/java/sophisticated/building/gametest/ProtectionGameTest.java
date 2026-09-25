package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import sophisticated.building.SophisticatedBuilding;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Vanilla protections apply to every build-mode placement. Spawn protection cannot be tested here: the game test
 * server (GameTestServer) never reports spawn protection (MinecraftServer.isUnderSpawnProtection returns false and
 * only DedicatedServer overrides it), so the world border stands in for Level.mayInteract.
 */
public class ProtectionGameTest implements FabricGameTest {

    private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
    private static final BlockPos OUTSIDE = new BlockPos(6, 1, 6);

    //Own batch: the world border is global, so no other test may run while it is shrunk
    @GameTest(template = EMPTY_STRUCTURE, batch = "world_border")
    public void worldBorderSkipsOutside(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        WorldBorder border = helper.getLevel().getWorldBorder();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
            BlockPos inside = helper.absolutePos(INSIDE);
            BlockPos outside = helper.absolutePos(OUTSIDE);
            //A 3x3 border around INSIDE; OUTSIDE is 5 blocks away on both axes
            border.setCenter(inside.getX() + 0.5, inside.getZ() + 0.5);
            border.setSize(3);
            assertTrue(border.isWithinBounds(inside) && !border.isWithinBounds(outside), "Border setup failed");

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(
                    place(inside, Blocks.STONE.defaultBlockState()),
                    place(outside, Blocks.STONE.defaultBlockState())));

            helper.assertBlockPresent(Blocks.STONE, INSIDE);
            helper.assertBlockPresent(Blocks.AIR, OUTSIDE);
            expectEquals(helper, "stone left (only the block inside the border is charged)", 1, count(player, Items.STONE));
        } finally {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
            removePlayer(player);
        }
        helper.succeed();
    }

    //Adventure players may not build (Abilities.mayBuild is false), so the whole set is rejected
    @GameTest(template = EMPTY_STRUCTURE)
    public void adventureModeRejected(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.ADVENTURE);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(
                    place(helper.absolutePos(INSIDE), Blocks.STONE.defaultBlockState()),
                    place(helper.absolutePos(OUTSIDE), Blocks.STONE.defaultBlockState())));

            helper.assertBlockPresent(Blocks.AIR, INSIDE);
            helper.assertBlockPresent(Blocks.AIR, OUTSIDE);
            expectEquals(helper, "stone left", 2, count(player, Items.STONE));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
