package sophisticated.building.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.SimpleConfigValue;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared helpers for the Sophisticated Building game tests. */
public final class GameTestSupport {

    private GameTestSupport() {
    }

    //region Players

    /**
     * A real {@link ServerPlayer} on a fake connection, in the given game mode. Vanilla's
     * {@code makeMockServerPlayerInLevel} hard-codes {@code isCreative() == true}, so it cannot test survival,
     * and {@code makeMockPlayer} is not a ServerPlayer (skips the adventure check and the packets the mod sends).
     */
    public static ServerPlayer spawnPlayer(GameTestHelper helper, GameType gameType) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sb-gametest");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        // 1.20.4 checks the listener against the protocol stored on the channel
        channel.attr(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.SERVERBOUND));
        channel.attr(Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.CLIENTBOUND));
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        ServerBuildState.setIsUsingBuildMode(player, false);
        ServerBuildState.setIsQuickReplacing(player, false);
        return player;
    }

    /** Removes the player again and forgets the mod's per-player state. */
    public static void removePlayer(ServerPlayer player) {
        ServerBuildState.setIsUsingBuildMode(player, false);
        ServerBuildState.setIsQuickReplacing(player, false);
        SophisticatedBuilding.UNDO_REDO.clear(player);
        MinecraftServer server = player.getServer();
        if (server != null && server.getPlayerList().getPlayer(player.getUUID()) != null) {
            server.getPlayerList().remove(player);
        }
    }

    //endregion

    //region Config

    /**
     * Snapshot of the server config values the building rules read. {@link #baseline()} saves the current values
     * and resets them to their defaults, so every test starts from the same rules; {@link #close()} restores the
     * saved values.
     */
    /** The Fabric config value behind a loader-neutral one, to set it in a test. */
    public static <T> SimpleConfigValue<T> fabricValue(ConfigValue<T> value) {
        return (SimpleConfigValue<T>) value;
    }

    public static final class ConfigScope implements AutoCloseable {
        private final List<Runnable> restorers = new ArrayList<>();
        private boolean closed;

        public static ConfigScope baseline() {
            ConfigScope scope = new ConfigScope();
            scope.reset(ServerConfig.validation.allowInSurvival);
            scope.reset(ServerConfig.validation.useWhitelist);
            scope.reset(ServerConfig.validation.maxBlocksPlacedAtOnce);
            scope.reset(ServerConfig.survivalBreaking.enabled);
            scope.reset(ServerConfig.survivalBreaking.stopBeforeToolBreaks);
            scope.reset(ServerConfig.survivalBreaking.maxDelayTicks);
            scope.reset(ServerConfig.survivalBreaking.exhaustionPerBlock);
            scope.reset(ServerConfig.survivalReplace.enabled);
            return scope;
        }

        private <T> void reset(ConfigValue<T> configValue) {
            SimpleConfigValue<T> value = fabricValue(configValue);
            T old = value.get();
            restorers.add(() -> value.set(old));
            value.set(value.getDefault());
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            for (Runnable restorer : restorers) {
                restorer.run();
            }
        }
    }

    //endregion

    //region Block sets

    public static BlockEntry place(BlockPos absolutePos, BlockState state) {
        return new BlockEntry(absolutePos, state, state.getBlock().asItem());
    }

    /** A breaking entry, as the client sends it (no new state, no item). */
    public static BlockEntry breaking(BlockPos absolutePos) {
        return new BlockEntry(absolutePos, null, null);
    }

    /** A set whose first position is the first entry. */
    public static BlockSet set(boolean skipFirst, BlockEntry... entries) {
        List<BlockEntry> list = List.of(entries);
        return new BlockSet(list, entries[0].blockPos, entries[entries.length - 1].blockPos, skipFirst);
    }

    public static BlockSet set(BlockEntry... entries) {
        return set(false, entries);
    }

    //endregion

    //region Inventory

    /** Items of this type in the whole inventory (main, armor, offhand). */
    public static int count(Player player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    //endregion

    //region Assertions

    public static void expectEquals(GameTestHelper helper, String what, Object expected, Object actual) {
        helper.assertTrue(expected == null ? actual == null : expected.equals(actual),
                what + ": expected " + expected + " but was " + actual);
    }

    public static void expectState(GameTestHelper helper, BlockPos relativePos, BlockState expected) {
        BlockState actual = helper.getBlockState(relativePos);
        helper.assertTrue(actual == expected, "Block at " + relativePos + ": expected " + expected + " but was " + actual);
    }

    //endregion
}
