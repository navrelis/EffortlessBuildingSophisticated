package sophisticated.building.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
import sophisticated.building.smoketest.servertest.ServerTestAssertException;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Shared helpers for the Sophisticated Building game tests. */
public final class GameTestSupport {

    private GameTestSupport() {
    }

    //region Players

    /**
     * A real {@link ServerPlayer} on a fake connection, in the given game mode, added to the test level. Vanilla's
     * {@code makeMockPlayer} is not a ServerPlayer (skips the adventure check and the packets the mod sends), and
     * Minecraft 1.17.1 has no {@code makeMockServerPlayerInLevel}. The player is not put into the player list: 1.17.1's
     * {@code PlayerList#placeNewPlayer} needs the profile cache, which the game test server does not have.
     */
    public static ServerPlayer spawnPlayer(ServerTestHelper helper, GameType gameType) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sb-gametest");
        ServerPlayer player = new ServerPlayer(server, level, profile, new ServerPlayerGameMode(level));
        // As vanilla's makeMockServerPlayerInLevel (1.19+): the embedded channel activates the connection and swallows
        // what the server sends
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player);
        level.addNewPlayer(player);
        player.setGameMode(gameType);
        player.inventory.clearContent();
        player.inventory.selected = 0;
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
        } else {
            player.getLevel().removePlayerImmediately(player);
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
        List<BlockEntry> list = Arrays.asList(entries);
        return new BlockSet(list, entries[0].blockPos, entries[entries.length - 1].blockPos, skipFirst);
    }

    public static BlockSet set(BlockEntry... entries) {
        return set(false, entries);
    }

    //endregion

    //region Inventory

    /** Items of this type in the whole inventory (main, armor, offhand). */
    public static int count(Player player, Item item) {
        Inventory inventory = player.inventory;
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if ((stack.getItem() == item)) total += stack.getCount();
        }
        return total;
    }

    //endregion

    //region Assertions

    /** ServerTestHelper#assertTrue of 1.20+: Minecraft 1.19.2's helper has none. */
    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new ServerTestAssertException(message);
        }
    }

    public static void expectEquals(ServerTestHelper helper, String what, Object expected, Object actual) {
        assertTrue(expected == null ? actual == null : expected.equals(actual),
                what + ": expected " + expected + " but was " + actual);
    }

    public static void expectState(ServerTestHelper helper, BlockPos relativePos, BlockState expected) {
        BlockState actual = helper.getBlockState(relativePos);
        assertTrue(actual == expected, "Block at " + relativePos + ": expected " + expected + " but was " + actual);
    }

    //endregion
}
