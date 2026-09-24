package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;

import static sophisticated.building.gametest.GameTestSupport.*;

/** #4: storage blocks placed with build modes keep the contents and name of the inventory stack. */
public class StorageDataGameTest {

    private static final String NAME = "Build Loot";

    private static ItemStack namedShulker() {
        ItemStack stack = new ItemStack(Items.SHULKER_BOX);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(NAME));
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 7))));
        return stack;
    }

    private static boolean hasLoot(ShulkerBoxBlockEntity box) {
        ItemStack first = box.getItem(0);
        return first.is(Items.DIAMOND) && first.getCount() == 7
                && box.getCustomName() != null && NAME.equals(box.getCustomName().getString());
    }

    private static void expectLoot(GameTestHelper helper, BlockPos rel) {
        helper.assertBlockPresent(Blocks.SHULKER_BOX, rel);
        ShulkerBoxBlockEntity box = helper.getBlockEntity(rel, ShulkerBoxBlockEntity.class);
        expectTrue(helper, hasLoot(box), "Placed shulker box should have 7 diamonds and the name '" + NAME
                + "', has " + box.getItem(0) + " named " + box.getCustomName());
    }

    @GameTest
    public void survivalKeepsContentsAndName(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (var config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, namedShulker());
            BlockPos rel = new BlockPos(2, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.SHULKER_BOX.defaultBlockState())));

            expectLoot(helper, rel);
            expectTrue(helper, player.getMainHandItem().isEmpty(), "The survival player's shulker box should be used up, main hand has " + player.getMainHandItem());
            expectEquals(helper, "shulker boxes left", 0, count(player, Items.SHULKER_BOX));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //A plain stack in hand and a stack with data elsewhere: two placements use one of each, both are consumed once
    @GameTest
    public void survivalPlainAndNamedStacksBothConsumedOnce(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (var config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.SHULKER_BOX));
            player.getInventory().setItem(5, namedShulker());
            BlockPos relA = new BlockPos(1, 1, 2);
            BlockPos relB = new BlockPos(4, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(
                    place(helper.absolutePos(relA), Blocks.SHULKER_BOX.defaultBlockState()),
                    place(helper.absolutePos(relB), Blocks.SHULKER_BOX.defaultBlockState())));

            helper.assertBlockPresent(Blocks.SHULKER_BOX, relA);
            helper.assertBlockPresent(Blocks.SHULKER_BOX, relB);
            ShulkerBoxBlockEntity a = helper.getBlockEntity(relA, ShulkerBoxBlockEntity.class);
            ShulkerBoxBlockEntity b = helper.getBlockEntity(relB, ShulkerBoxBlockEntity.class);
            int withLoot = (hasLoot(a) ? 1 : 0) + (hasLoot(b) ? 1 : 0);
            int empty = (a.isEmpty() && a.getCustomName() == null ? 1 : 0) + (b.isEmpty() && b.getCustomName() == null ? 1 : 0);
            expectEquals(helper, "placed boxes with the named stack's data", 1, withLoot);
            expectEquals(helper, "placed plain boxes", 1, empty);
            expectEquals(helper, "shulker boxes left", 0, count(player, Items.SHULKER_BOX));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest
    public void creativeCopiesDataAndKeepsStack(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        try (var config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, namedShulker());
            BlockPos rel = new BlockPos(2, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.SHULKER_BOX.defaultBlockState())));

            expectLoot(helper, rel);
            ItemStack held = player.getMainHandItem();
            expectTrue(helper, held.is(Items.SHULKER_BOX) && held.getCount() == 1 && ItemStack.isSameItemSameComponents(held, namedShulker()),
                    "The creative player's stack should be kept unchanged, main hand has " + held);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
