package sophisticated.building.smoketest.server;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import sophisticated.building.platform.services.IBackpackIntegration;
import sophisticated.building.platform.Services;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.network.message.PerformRedoPacket;
import sophisticated.building.network.message.PerformUndoPacket;
import sophisticated.building.network.message.ServerBreakBlocksPacket;
import sophisticated.building.network.message.ServerPlaceBlocksPacket;
import sophisticated.building.smoketest.backpack.SmokeAccessorySlots;
import sophisticated.building.smoketest.backpack.SmokeBackpacks;
import sophisticated.building.smoketest.servertest.ServerTestAssertException;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Server smoke scenarios as server test bodies ({@link ServerTestHelper}, the Minecraft 1.16.3 stand-in for vanilla's
 * game test helper, loader-neutral). A survival server player without a client sends exactly what the client sends: the
 * block sets are written with the packets' write methods, read back with their FriendlyByteBuf constructors (encoded and
 * decoded like on the wire) and handed to the packets' server handlers. {@link SmokeServer} runs one test per method,
 * named like the method, and reports e.g. {@code sb_tier_cap} as {@code sb.tier_cap}.
 */
public final class ServerScenarios {

    /** Success details and skip reasons by check name, read by the reporter. */
    static final Map<String, String> DETAILS = new ConcurrentHashMap<>();
    /** System property of the standalone smoke run (set by gradle/smoketest.gradle for -PsmokeNoSb=true). */
    static final String NO_SB_PROPERTY = "sophisticatedbuilding.smoketest.noSb";
    static final Map<String, String> SKIPPED = new ConcurrentHashMap<>();

    public static final int TIMEOUT_TICKS = 400;
    private static final int LINE = 5;
    private static final int TIER1_CAP = 32;

    private ServerScenarios() {
    }

    //region Building without backpacks

    public static void server_place_line_survival(ServerTestHelper helper) {
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        List<BlockPos> line = row(helper, LINE);
        sendPlace(player, placeSet(line, Blocks.OAK_PLANKS.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.OAK_PLANKS))
                .thenIdle(5)
                .thenExecute(() -> {
                    expectEquals(helper, "oak planks left", 64 - LINE, count(player.inventory, Items.OAK_PLANKS));
                    DETAILS.put("server.place_line_survival", "Survival player placed a " + LINE + " block line through ServerPlaceBlocksPacket, "
                            + LINE + " planks consumed");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    public static void server_undo_redo(ServerTestHelper helper) {
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        player.inventory.setItem(8, new ItemStack(Items.IRON_AXE));
        List<BlockPos> line = row(helper, LINE);
        sendPlace(player, placeSet(line, Blocks.OAK_PLANKS.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.OAK_PLANKS))
                .thenExecute(() -> PerformUndoPacket.Handler.handle(roundTrip(new PerformUndoPacket(), PerformUndoPacket::new), player))
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.AIR))
                .thenExecute(() -> expectEquals(helper, "oak planks after undo", 64, count(player.inventory, Items.OAK_PLANKS)))
                .thenExecute(() -> PerformRedoPacket.Handler.handle(roundTrip(new PerformRedoPacket(), PerformRedoPacket::new), player))
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.OAK_PLANKS))
                .thenExecute(() -> {
                    expectEquals(helper, "oak planks after redo", 64 - LINE, count(player.inventory, Items.OAK_PLANKS));
                    DETAILS.put("server.undo_redo", "Undo mined the line back into the inventory (64 planks), redo placed it again (" + (64 - LINE) + ")");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    /** Undo of merges (+1 snow layer, +1 sea pickle; Minecraft 1.16.3 has no candles) gives back the merged items without mining; redo charges them again. */
    public static void server_merge_undo_refund(ServerTestHelper helper) {
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.SNOW, 1));
        player.inventory.setItem(1, new ItemStack(Items.SEA_PICKLE, 1));
        BlockPos snowPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos picklePos = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockState snow = Blocks.SNOW.defaultBlockState();
        BlockState pickle = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        BlockState snow2 = snow.setValue(BlockStateProperties.LAYERS, 2);
        BlockState pickle2 = pickle.setValue(BlockStateProperties.PICKLES, 2);
        for (BlockPos pos : Arrays.asList(snowPos, picklePos)) {
            helper.getLevel().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3 /* Block.UPDATE_ALL */);
        }
        helper.getLevel().setBlock(snowPos, snow, 3 /* Block.UPDATE_ALL */);
        helper.getLevel().setBlock(picklePos, pickle, 3 /* Block.UPDATE_ALL */);
        List<BlockEntry> entries = Arrays.asList(new BlockEntry(snowPos, snow2, Items.SNOW), new BlockEntry(picklePos, pickle2, Items.SEA_PICKLE));
        sendPlace(player, new BlockSet(entries, snowPos, picklePos, false));

        helper.startSequence()
                .thenWaitUntil(() -> expectStates(helper, snowPos, snow2, picklePos, pickle2))
                .thenIdle(5)
                .thenExecute(() -> expectItems(helper, player, "after the merges", 0))
                .thenExecute(() -> PerformUndoPacket.Handler.handle(roundTrip(new PerformUndoPacket(), PerformUndoPacket::new), player))
                .thenWaitUntil(() -> expectStates(helper, snowPos, snow, picklePos, pickle))
                .thenExecute(() -> expectItems(helper, player, "after undo", 1))
                .thenExecute(() -> PerformRedoPacket.Handler.handle(roundTrip(new PerformRedoPacket(), PerformRedoPacket::new), player))
                .thenWaitUntil(() -> expectStates(helper, snowPos, snow2, picklePos, pickle2))
                .thenExecute(() -> {
                    expectItems(helper, player, "after redo", 0);
                    DETAILS.put("server.merge_undo_refund", "Survival merges (+1 snow layer, +1 sea pickle) cost one item each, undo gave "
                            + "both back without mining, redo charged them again");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    /**
     * The loader's block place event refuses 2 of a 5 block line (a protection mod): only the 3 placed blocks are charged,
     * and undo only takes those back.
     */
    public static void server_refused_place_not_charged(ServerTestHelper helper) {
        String check = "server.refused_place_not_charged";
        ServerPlayer player = player(helper);
        List<BlockPos> line = row(helper, LINE);
        List<BlockPos> refused = Arrays.asList(line.get(1), line.get(3));
        List<BlockPos> placed = Arrays.asList(line.get(0), line.get(2), line.get(4));
        if (!SmokeServerPlatform.get().refusePlacementsAt(new HashSet<>(refused))) {
            SKIPPED.put(check, "This loader fires no block place event for the mod's placements (Fabric); "
                    + "the Fabric GameTest ChargeGameTest covers refused placements");
            cleanup(player);
            helper.succeed();
            return;
        }
        player.inventory.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        player.inventory.setItem(8, new ItemStack(Items.IRON_AXE));
        sendPlace(player, placeSet(line, Blocks.OAK_PLANKS.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, placed, Blocks.OAK_PLANKS))
                .thenIdle(5)
                .thenExecute(() -> {
                    SmokeServerPlatform.get().refusePlacementsAt(Collections.emptySet());
                    expectAll(helper, refused, Blocks.AIR);
                    expectEquals(helper, "oak planks left (only the 3 placed blocks charged)", 64 - placed.size(),
                            count(player.inventory, Items.OAK_PLANKS));
                })
                .thenExecute(() -> PerformUndoPacket.Handler.handle(roundTrip(new PerformUndoPacket(), PerformUndoPacket::new), player))
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.AIR))
                .thenExecute(() -> {
                    expectEquals(helper, "oak planks after undo", 64, count(player.inventory, Items.OAK_PLANKS));
                    DETAILS.put(check, "The place event refused 2 of a " + LINE + " block line: " + placed.size()
                            + " placed and charged (64 -> " + (64 - placed.size()) + "), undo gave back exactly those");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    /**
     * A crafted request (as a modified client could send) the server must refuse with the player's power level limits:
     * a start position 60 blocks above the player (survival power level 0), and a line whose clicks are 20 blocks apart
     * (8 per axis). Nothing is placed or charged; a normal line right after is placed.
     */
    public static void server_request_limits(ServerTestHelper helper) {
        String check = "server.request_limits";
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        BlockPos far = player.blockPosition().above(60);
        sendPlace(player, new BlockSet(Collections.singletonList(new BlockEntry(far, Blocks.OAK_PLANKS.defaultBlockState(), Items.OAK_PLANKS)), far, far, false));
        List<BlockPos> line = row(helper, LINE);
        sendPlace(player, new BlockSet(new ArrayList<>(placeSet(line, Blocks.OAK_PLANKS.defaultBlockState()).values()), line.get(0),
                line.get(0).east(19), false));

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    if (helper.getLevel().getBlockState(far).is(Blocks.OAK_PLANKS)) {
                        helper.fail("The block 60 blocks away (out of reach) was placed");
                    }
                    expectAll(helper, line, Blocks.AIR);
                    expectEquals(helper, "oak planks after two refused requests", 64, count(player.inventory, Items.OAK_PLANKS));
                })
                .thenExecute(() -> sendPlace(player, placeSet(line, Blocks.OAK_PLANKS.defaultBlockState())))
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.OAK_PLANKS))
                .thenExecute(() -> {
                    expectEquals(helper, "oak planks after the normal line", 64 - LINE, count(player.inventory, Items.OAK_PLANKS));
                    DETAILS.put(check, "Refused a start 60 blocks away and a 20 block extent (survival limits: reach, 8 per axis), "
                            + "nothing placed or charged; a normal " + LINE + " block line after them was placed");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    //endregion

    //region Sophisticated Backpacks

    public static void sb_upgrade_supplies_blocks(ServerTestHelper helper) {
        if (standalone(helper, "sb.upgrade_supplies_blocks")) return;
        SmokeBackpacks backpacks = backpacks(helper);
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.STONE, 1));
        ItemStack backpack = backpacks.createBackpack(1, true, false, Collections.singletonList(new ItemStack(Items.STONE, 64)));
        player.inventory.setItem(1, backpack);
        List<BlockPos> line = row(helper, LINE);
        sendPlace(player, placeSet(line, Blocks.STONE.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.STONE))
                .thenIdle(5)
                .thenExecute(() -> {
                    expectEquals(helper, "stone held", 1, count(player.inventory, Items.STONE));
                    expectEquals(helper, "stone in the backpack", 64 - LINE, backpacks.count(backpack, Items.STONE));
                    DETAILS.put("sb.upgrade_supplies_blocks", "Holding 1 stone, the Building Upgrade supplied a " + LINE
                            + " block line from the backpack (64 -> " + (64 - LINE) + "), held stone kept");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    public static void sb_disabled_upgrade_ignored(ServerTestHelper helper) {
        if (standalone(helper, "sb.disabled_upgrade_ignored")) return;
        SmokeBackpacks backpacks = backpacks(helper);
        ServerPlayer player = player(helper);
        int held = 3;
        player.inventory.setItem(0, new ItemStack(Items.STONE, held));
        ItemStack backpack = backpacks.createBackpack(1, false, false, Collections.singletonList(new ItemStack(Items.STONE, 64)));
        player.inventory.setItem(1, backpack);
        List<BlockPos> line = row(helper, LINE);
        sendPlace(player, placeSet(line, Blocks.STONE.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> assertTrue(countBlocks(helper, line, Blocks.STONE) >= held, "waiting for the held stone"))
                .thenIdle(10)
                .thenExecute(() -> {
                    expectEquals(helper, "blocks placed", held, countBlocks(helper, line, Blocks.STONE));
                    expectEquals(helper, "stone held", 0, count(player.inventory, Items.STONE));
                    expectEquals(helper, "stone in the disabled upgrade's backpack", 64, backpacks.count(backpack, Items.STONE));
                    DETAILS.put("sb.disabled_upgrade_ignored", "Disabled upgrade: only the " + held + " held stone of a " + LINE
                            + " block line were placed, the backpack kept its 64");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    public static void sb_tier_cap(ServerTestHelper helper) {
        if (standalone(helper, "sb.tier_cap")) return;
        SmokeBackpacks backpacks = backpacks(helper);
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.STONE, 1));
        ItemStack backpack = backpacks.createBackpack(1, true, false, Collections.singletonList(new ItemStack(Items.STONE, 64)));
        player.inventory.setItem(1, backpack);
        List<BlockPos> floor = new ArrayList<>();
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                floor.add(helper.absolutePos(new BlockPos(1 + x, 1, 1 + z)));
            }
        }
        sendPlace(player, placeSet(floor, Blocks.STONE.defaultBlockState()));

        helper.startSequence()
                .thenWaitUntil(() -> assertTrue(countBlocks(helper, floor, Blocks.STONE) >= TIER1_CAP, "waiting for the floor"))
                .thenIdle(10)
                .thenExecute(() -> {
                    expectEquals(helper, "blocks placed of a 36 block floor (tier 1 cap)", TIER1_CAP, countBlocks(helper, floor, Blocks.STONE));
                    expectEquals(helper, "stone in the backpack", 64 - TIER1_CAP, backpacks.count(backpack, Items.STONE));
                    expectEquals(helper, "stone held", 1, count(player.inventory, Items.STONE));
                    DETAILS.put("sb.tier_cap", "Tier 1 upgrade (max " + TIER1_CAP + ") capped a 36 block floor at " + TIER1_CAP
                            + " blocks from a backpack with 64 stone");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    public static void sb_tool_swapper_tools(ServerTestHelper helper) {
        if (standalone(helper, "sb.tool_swapper_tools")) return;
        SmokeBackpacks backpacks = backpacks(helper);
        String noToolSwapper = backpacks.whyNoToolSwapper();
        if (noToolSwapper != null) {
            SKIPPED.put("sb.tool_swapper_tools", noToolSwapper);
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.STICK));
        ItemStack backpack = backpacks.createBackpack(0, false, true, Collections.singletonList(new ItemStack(Items.DIAMOND_PICKAXE)));
        player.inventory.setItem(1, backpack);
        List<BlockPos> line = row(helper, LINE);
        line.forEach(pos -> helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3 /* Block.UPDATE_ALL */));
        sendBreak(player, breakSet(line));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.AIR))
                .thenIdle(5)
                .thenExecute(() -> {
                    expectEquals(helper, "damage of the pickaxe in the backpack", LINE, backpacks.find(backpack, Items.DIAMOND_PICKAXE).getDamageValue());
                    expectEquals(helper, "cobblestone in the inventory", LINE, count(player.inventory, Items.COBBLESTONE));
                    DETAILS.put("sb.tool_swapper_tools", "Survival break of " + LINE + " stone (stick in hand) used the Tool Swapper backpack's diamond pickaxe (damage "
                            + LINE + "), drops in the inventory");
                })
                .thenExecute(() -> cleanup(player))
                .thenSucceed();
    }

    public static void sb_worn_backpack_chest(ServerTestHelper helper) {
        wornBackpack(helper, "sb.worn_backpack_chest", false);
    }

    public static void sb_worn_backpack(ServerTestHelper helper) {
        wornBackpack(helper, "sb.worn_backpack", true);
    }

    private static void wornBackpack(ServerTestHelper helper, String check, boolean accessory) {
        if (standalone(helper, check)) return;
        SmokeBackpacks backpacks = backpacks(helper);
        ServerPlayer player = player(helper);
        player.inventory.setItem(0, new ItemStack(Items.STONE, 1));
        ItemStack backpack = backpacks.createBackpack(1, true, false, Collections.singletonList(new ItemStack(Items.STONE, 64)));
        String slot;
        if (accessory) {
            String reason = backpacks.equipInAccessorySlot(player, backpack);
            if (reason != null) {
                SKIPPED.put(check, reason + "; the worn case is covered by sb.worn_backpack_chest (chest armor slot)");
                cleanup(player);
                helper.succeed();
                return;
            }
            slot = SmokeAccessorySlots.find().map(SmokeAccessorySlots::describe).orElse("accessory slot");
        } else {
            player.setItemSlot(EquipmentSlot.CHEST, backpack);
            slot = "chest armor slot";
        }
        List<BlockPos> line = row(helper, LINE);

        helper.startSequence()
                // The Fabric port rescans Trinkets slots for backpacks only every 100 ticks
                .thenIdle(accessory ? 110 : 1)
                .thenExecute(() -> sendPlace(player, placeSet(line, Blocks.STONE.defaultBlockState())))
                .thenWaitUntil(() -> expectAll(helper, line, Blocks.STONE))
                .thenIdle(5)
                .thenExecute(() -> {
                    ItemStack worn = accessory ? backpacks.getFromAccessorySlot(player) : player.getItemBySlot(EquipmentSlot.CHEST);
                    expectEquals(helper, "stone in the worn backpack", 64 - LINE, backpacks.count(worn, Items.STONE));
                    expectEquals(helper, "stone held", 1, count(player.inventory, Items.STONE));
                    DETAILS.put(check, "Backpack worn in the " + slot + " supplied a " + LINE + " block line (64 -> " + (64 - LINE) + ")");
                })
                .thenExecute(() -> {
                    if (accessory) backpacks.equipInAccessorySlot(player, ItemStack.EMPTY);
                    cleanup(player);
                })
                .thenSucceed();
    }

    //endregion

    //region Helpers

    /**
     * Standalone smoke run ({@code gradlew runSmokeServer -PsmokeNoSb=true}: Sophisticated Backpacks, Sophisticated Core
     * and the accessory mods are left out of the runtime and the backpack fixture is not compiled in): the sb.*
     * scenarios are skipped, the building scenarios run as usual. Fails instead when the backpack integration is
     * active anyway, so the switch cannot silently test the wrong setup.
     */
    private static boolean standalone(ServerTestHelper helper, String check) {
        if (!Boolean.getBoolean(NO_SB_PROPERTY)) {
            return false;
        }
        if (Services.backpacks() != IBackpackIntegration.NONE) {
            throw new IllegalStateException("Standalone run (" + NO_SB_PROPERTY + ") but the Sophisticated Backpacks integration is active");
        }
        SKIPPED.put(check, "Standalone run without Sophisticated Backpacks (-PsmokeNoSb=true): the mod runs with IBackpackIntegration.NONE");
        helper.succeed();
        return true;
    }
    private static SmokeBackpacks backpacks(ServerTestHelper helper) {
        return SmokeBackpacks.find().orElseThrow(() -> new IllegalStateException("No SmokeBackpacks fixture registered"));
    }

    private static ServerPlayer player(ServerTestHelper helper) {
        ServerPlayer player = SmokeServerPlatform.get().createPlayer(helper.getLevel(), GameType.SURVIVAL);
        // At the test structure, like a player building there (the server checks the reach of build requests)
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.moveTo(origin.getX() + 3.5, origin.getY() + 1, origin.getZ() + 3.5, 0, 0);
        ServerBuildState.setIsUsingBuildMode(player, true);
        ServerBuildState.setIsQuickReplacing(player, false);
        return player;
    }

    private static void cleanup(ServerPlayer player) {
        SophisticatedBuilding.UNDO_REDO.clear(player);
        ServerBuildState.setIsUsingBuildMode(player, false);
        player.inventory.clearContent();
    }

    /** Positions (1..count, 1, 1) of the test structure, absolute. */
    private static List<BlockPos> row(ServerTestHelper helper, int count) {
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(helper.absolutePos(new BlockPos(1 + i, 1, 1)));
        return list;
    }

    private static BlockSet placeSet(List<BlockPos> positions, BlockState state) {
        List<BlockEntry> entries = new ArrayList<>();
        for (BlockPos pos : positions) entries.add(new BlockEntry(pos, state, state.getBlock().asItem()));
        return new BlockSet(entries, positions.get(0), positions.get(positions.size() - 1), false);
    }

    private static BlockSet breakSet(List<BlockPos> positions) {
        List<BlockEntry> entries = new ArrayList<>();
        for (BlockPos pos : positions) entries.add(new BlockEntry(pos, null, null));
        return new BlockSet(entries, positions.get(0), positions.get(positions.size() - 1), false);
    }

    /** What the client's second click sends, through the packet's encoding and server handler. */
    private static void sendPlace(ServerPlayer player, BlockSet blocks) {
        ServerPlaceBlocksPacket packet = roundTrip(new ServerPlaceBlocksPacket(blocks, player.level.getGameTime()), ServerPlaceBlocksPacket::new);
        ServerPlaceBlocksPacket.Handler.handle(packet, player);
    }

    private static void sendBreak(ServerPlayer player, BlockSet blocks) {
        ServerBreakBlocksPacket packet = roundTrip(new ServerBreakBlocksPacket(blocks), ServerBreakBlocksPacket::new);
        ServerBreakBlocksPacket.Handler.handle(packet, player);
    }

    private static <T extends ModPayload> T roundTrip(T packet, Function<FriendlyByteBuf, T> reader) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            return reader.apply(buffer);
        } finally {
            buffer.release();
        }
    }

    private static Map.Entry<BlockPos, BlockState> entry(BlockPos pos, BlockState state) {
        return new AbstractMap.SimpleEntry<>(pos, state);
    }

    private static void expectStates(ServerTestHelper helper, BlockPos posA, BlockState stateA, BlockPos posB, BlockState stateB) {
        for (Map.Entry<BlockPos, BlockState> expected : Arrays.asList(entry(posA, stateA), entry(posB, stateB))) {
            BlockState state = helper.getLevel().getBlockState(expected.getKey());
            if (state != expected.getValue()) {
                helper.fail("Expected " + expected.getValue() + " at " + ServerTestHelper.shortString(expected.getKey()) + " but was " + state);
            }
        }
    }

    /** Snow layers and sea pickles held: both the same count. */
    private static void expectItems(ServerTestHelper helper, ServerPlayer player, String when, int each) {
        expectEquals(helper, "snow layers " + when, each, count(player.inventory, Items.SNOW));
        expectEquals(helper, "sea pickles " + when, each, count(player.inventory, Items.SEA_PICKLE));
    }

    private static void expectAll(ServerTestHelper helper, List<BlockPos> positions, Block block) {
        for (BlockPos pos : positions) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (!state.is(block)) {
                helper.fail("Expected " + block + " at " + ServerTestHelper.shortString(pos) + " but was " + state);
            }
        }
    }

    private static int countBlocks(ServerTestHelper helper, List<BlockPos> positions, Block block) {
        int count = 0;
        for (BlockPos pos : positions) {
            if (helper.getLevel().getBlockState(pos).is(block)) count++;
        }
        return count;
    }

    private static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if ((stack.getItem() == item)) total += stack.getCount();
        }
        return total;
    }

    private static void expectEquals(ServerTestHelper helper, String what, Object expected, Object actual) {
        assertTrue(expected.equals(actual), what + ": expected " + expected + " but was " + actual);
    }

    /** GameTestHelper#assertTrue of 1.20+. */
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new ServerTestAssertException(message);
        }
    }

    //endregion
}
