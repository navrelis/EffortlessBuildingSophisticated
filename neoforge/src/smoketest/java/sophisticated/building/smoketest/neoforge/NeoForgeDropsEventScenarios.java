package sophisticated.building.smoketest.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import sophisticated.building.create.foundation.utility.BlockHelper;
import sophisticated.building.smoketest.server.ServerScenarios;
import sophisticated.building.smoketest.server.SmokeServerPlatform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * NeoForge's block drops event during a build-mode break ({@link BlockHelper#destroyBlockAs} as a survival player, the
 * path of ServerBlockPlacer): listeners get the real drops as a mutable list, and what they add, remove or cancel is
 * what the drop callback receives. A listener adding an item entity used to crash with an immutable list
 * (Apothic Enchanting's Boon enchantment). The listener only acts while a check runs, at that check's position.
 */
final class NeoForgeDropsEventScenarios {

    private enum Mode { ADD_DIAMOND, REMOVE_ALL, CANCEL }

    private static boolean listening;
    private static volatile Mode mode;
    private static volatile BlockPos target;
    private static final List<ItemStack> SEEN = new ArrayList<>();

    private NeoForgeDropsEventScenarios() {
    }

    /** The test functions by name; only the NeoForge glue registers them (the test instances live in the NeoForge smoketest resources). */
    static Map<String, Consumer<GameTestHelper>> functions() {
        Map<String, Consumer<GameTestHelper>> functions = new LinkedHashMap<>();
        functions.put("server_drops_event_add", NeoForgeDropsEventScenarios::server_drops_event_add);
        functions.put("server_drops_event_remove", NeoForgeDropsEventScenarios::server_drops_event_remove);
        functions.put("server_drops_event_cancel", NeoForgeDropsEventScenarios::server_drops_event_cancel);
        return functions;
    }

    static void server_drops_event_add(GameTestHelper helper) {
        List<ItemStack> received = breakStone(helper, Mode.ADD_DIAMOND);
        expectCount(helper, "cobblestone the listener saw", 1, SEEN, Items.COBBLESTONE);
        expectCount(helper, "cobblestone received", 1, received, Items.COBBLESTONE);
        expectCount(helper, "diamonds received", 1, received, Items.DIAMOND);
        expectCount(helper, "stacks received", 2, received.size());
        ServerScenarios.DETAILS.put("server.drops_event_add", "A BlockDropsEvent listener saw the cobblestone of a broken stone and added a diamond "
                + "item entity: no exception, the drop callback got " + describe(received));
        helper.succeed();
    }

    static void server_drops_event_remove(GameTestHelper helper) {
        List<ItemStack> received = breakStone(helper, Mode.REMOVE_ALL);
        expectCount(helper, "cobblestone the listener saw", 1, SEEN, Items.COBBLESTONE);
        expectCount(helper, "stacks received", 0, received.size());
        ServerScenarios.DETAILS.put("server.drops_event_remove", "A BlockDropsEvent listener removed the cobblestone of a broken stone: "
                + "the drop callback got nothing");
        helper.succeed();
    }

    static void server_drops_event_cancel(GameTestHelper helper) {
        List<ItemStack> received = breakStone(helper, Mode.CANCEL);
        expectCount(helper, "cobblestone the listener saw", 1, SEEN, Items.COBBLESTONE);
        expectCount(helper, "stacks received", 0, received.size());
        ServerScenarios.DETAILS.put("server.drops_event_cancel", "A BlockDropsEvent listener cancelled the drops of a broken stone: "
                + "the drop callback got nothing, the block was still broken");
        helper.succeed();
    }

    /** Breaks a stone block with an iron pickaxe as a survival player while the listener acts in {@code checkMode}. */
    private static List<ItemStack> breakStone(GameTestHelper helper, Mode checkMode) {
        listen();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer player = SmokeServerPlatform.get().createPlayer(level, GameType.SURVIVAL);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        player.getInventory().setItem(0, tool);
        List<ItemStack> received = new ArrayList<>();
        SEEN.clear();
        target = pos.immutable();
        mode = checkMode;
        try {
            if (!BlockHelper.destroyBlockAs(level, pos, player, tool, 0, received::add)) {
                helper.fail(Component.literal("destroyBlockAs did not break the stone"));
            }
        } finally {
            mode = null;
            target = null;
            player.getInventory().clearContent();
        }
        if (!level.getBlockState(pos).isAir()) {
            helper.fail(Component.literal("Expected air at " + pos.toShortString() + " but was " + level.getBlockState(pos)));
        }
        return received;
    }

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener((BlockDropsEvent event) -> {
            Mode current = mode;
            if (current == null || !event.getPos().equals(target)) return;
            for (ItemEntity itemEntity : event.getDrops()) SEEN.add(itemEntity.getItem().copy());
            BlockPos pos = event.getPos();
            switch (current) {
                case ADD_DIAMOND -> event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, new ItemStack(Items.DIAMOND)));
                case REMOVE_ALL -> event.getDrops().clear();
                case CANCEL -> event.setCanceled(true);
            }
        });
    }

    private static void expectCount(GameTestHelper helper, String what, int expected, List<ItemStack> stacks, Item item) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(item)) count += stack.getCount();
        }
        expectCount(helper, what + " (" + describe(stacks) + ")", expected, count);
    }

    private static void expectCount(GameTestHelper helper, String what, int expected, int actual) {
        helper.assertTrue(expected == actual, Component.literal(what + ": expected " + expected + " but was " + actual));
    }

    private static String describe(List<ItemStack> stacks) {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : stacks) parts.add(stack.getCount() + " " + stack.getItem());
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }
}
