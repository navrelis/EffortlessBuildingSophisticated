package sophisticated.building.smoketest.server;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A fake server player from vanilla classes only, for loaders without a fake player API (Forge 52, 54, 55): not in the player
 * list, and its connection drops every packet (like the loaders' fake players), so the packets the mod sends to it go
 * nowhere instead of needing a negotiated client.
 */
public final class VanillaFakePlayers {

    private VanillaFakePlayers() {
    }

    public static ServerPlayer create(ServerLevel level, GameType gameType) {
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sb-smoketest");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
            }
        };
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        return player;
    }
}
