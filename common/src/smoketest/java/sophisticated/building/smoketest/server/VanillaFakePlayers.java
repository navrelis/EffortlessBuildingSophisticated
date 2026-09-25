package sophisticated.building.smoketest.server;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A fake server player from vanilla classes only, for loaders without a fake player API (Fabric API 0.77 for 1.18.2):
 * not in the player list, and its connection drops every packet (like the loaders' fake players), so the packets the
 * mod sends to it go nowhere instead of needing a negotiated client.
 */
public final class VanillaFakePlayers {

    private VanillaFakePlayers() {
    }

    public static ServerPlayer create(ServerLevel level, GameType gameType) {
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sb-smoketest");
        ServerPlayer player = new ServerPlayer(server, level, profile, new ServerPlayerGameMode(level));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player) {
            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {
            }
        };
        player.setGameMode(gameType);
        player.inventory.clearContent();
        player.inventory.selected = 0;
        return player;
    }
}
