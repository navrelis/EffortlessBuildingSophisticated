package sophisticated.building.gametest;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import sophisticated.building.SophisticatedBuilding;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Claim and protection mods get their say on Fabric: the mod's breaks fire Fabric API's player block break events
 * (a listener that cancels BEFORE keeps the block, AFTER follows every broken block), and its placements ask the
 * Common Protection API when a claim mod ships it (here a test provider that refuses one position).
 */
public class ProtectionEventsGameTest implements FabricGameTest {

    private static final Set<BlockPos> REFUSE_BREAK = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> BROKEN_AFTER = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> REFUSE_PLACE = ConcurrentHashMap.newKeySet();
    private static boolean registered;

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !REFUSE_BREAK.contains(pos));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> BROKEN_AFTER.add(pos.immutable()));
        CommonProtection.register(ResourceLocation.fromNamespaceAndPath("sophisticatedbuilding_gametest", "refuse_place"), new ProtectionProvider() {
            @Override
            public boolean isProtected(Level level, BlockPos pos) {
                return REFUSE_PLACE.contains(pos);
            }

            @Override
            public boolean isAreaProtected(Level level, AABB area) {
                return false;
            }

            @Override
            public boolean canPlaceBlock(Level level, BlockPos pos, GameProfile profile, Player player) {
                return !REFUSE_PLACE.contains(pos);
            }
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void cancelledBreakEventKeepsTheBlock(GameTestHelper helper) {
        register();
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        BlockPos refused = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos allowed = helper.absolutePos(new BlockPos(3, 1, 1));
        try (var config = ConfigScope.baseline()) {
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
            REFUSE_BREAK.add(refused);

            //Creative breaks are applied at once
            SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, set(breaking(refused), breaking(allowed)));

            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 1));
            helper.assertTrue(BROKEN_AFTER.contains(allowed), "PlayerBlockBreakEvents.AFTER should follow the broken block");
            helper.assertFalse(BROKEN_AFTER.contains(refused), "PlayerBlockBreakEvents.AFTER must not fire for the refused block");
        } finally {
            REFUSE_BREAK.remove(refused);
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void commonProtectionRefusesPlacement(GameTestHelper helper) {
        register();
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        BlockPos refused = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos allowed = helper.absolutePos(new BlockPos(3, 1, 1));
        try (var config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
            REFUSE_PLACE.add(refused);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(
                    place(refused, Blocks.STONE.defaultBlockState()), place(allowed, Blocks.STONE.defaultBlockState())));

            helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 1));
            expectEquals(helper, "stone left (the refused placement is not charged)", 1, count(player, Items.STONE));
        } finally {
            REFUSE_PLACE.remove(refused);
            removePlayer(player);
        }
        helper.succeed();
    }
}
