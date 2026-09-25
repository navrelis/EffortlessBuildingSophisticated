package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import sophisticated.building.SophisticatedBuilding;

import static sophisticated.building.gametest.GameTestSupport.*;

/** #4: storage blocks placed with build modes keep the contents and name of the inventory stack. */
public class StorageDataGameTest implements FabricGameTest {

    private static final String NAME = "Build Loot";

    private static ItemStack namedShulker() {
        ItemStack stack = new ItemStack(Items.SHULKER_BOX);
        stack.setHoverName(new TextComponent(NAME));
        CompoundTag contents = new CompoundTag();
        ContainerHelper.saveAllItems(contents, NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 7)));
        // Minecraft 1.17.1 has no BlockItem.setBlockEntityData: the same "BlockEntityTag" it would write
        stack.addTagElement(BlockItem.BLOCK_ENTITY_TAG, contents);
        return stack;
    }

    private static boolean hasLoot(ShulkerBoxBlockEntity box) {
        ItemStack first = box.getItem(0);
        return (first.getItem() == Items.DIAMOND) && first.getCount() == 7
                && box.getCustomName() != null && NAME.equals(box.getCustomName().getString());
    }

    private static void expectLoot(GameTestHelper helper, BlockPos rel) {
        helper.assertBlockPresent(Blocks.SHULKER_BOX, rel);
        ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) helper.getBlockEntity(rel);
        assertTrue(hasLoot(box), "Placed shulker box should have 7 diamonds and the name '" + NAME
                + "', has " + box.getItem(0) + " named " + box.getCustomName());
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void survivalKeepsContentsAndName(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.inventory.setItem(0, namedShulker());
            BlockPos rel = new BlockPos(2, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.SHULKER_BOX.defaultBlockState())));

            expectLoot(helper, rel);
            assertTrue(player.getMainHandItem().isEmpty(), "The survival player's shulker box should be used up, main hand has " + player.getMainHandItem());
            expectEquals(helper, "shulker boxes left", 0, count(player, Items.SHULKER_BOX));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //A plain stack in hand and a stack with data elsewhere: two placements use one of each, both are consumed once
    @GameTest(template = EMPTY_STRUCTURE)
    public void survivalPlainAndNamedStacksBothConsumedOnce(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.inventory.setItem(0, new ItemStack(Items.SHULKER_BOX));
            player.inventory.setItem(5, namedShulker());
            BlockPos relA = new BlockPos(1, 1, 2);
            BlockPos relB = new BlockPos(4, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(
                    place(helper.absolutePos(relA), Blocks.SHULKER_BOX.defaultBlockState()),
                    place(helper.absolutePos(relB), Blocks.SHULKER_BOX.defaultBlockState())));

            helper.assertBlockPresent(Blocks.SHULKER_BOX, relA);
            helper.assertBlockPresent(Blocks.SHULKER_BOX, relB);
            ShulkerBoxBlockEntity a = (ShulkerBoxBlockEntity) helper.getBlockEntity(relA);
            ShulkerBoxBlockEntity b = (ShulkerBoxBlockEntity) helper.getBlockEntity(relB);
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

    @GameTest(template = EMPTY_STRUCTURE)
    public void creativeCopiesDataAndKeepsStack(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.inventory.setItem(0, namedShulker());
            BlockPos rel = new BlockPos(2, 1, 2);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.SHULKER_BOX.defaultBlockState())));

            expectLoot(helper, rel);
            ItemStack held = player.getMainHandItem();
            assertTrue((held.getItem() == Items.SHULKER_BOX) && held.getCount() == 1 && ItemStack.isSameItemSameTags(held, namedShulker()),
                    "The creative player's stack should be kept unchanged, main hand has " + held);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
