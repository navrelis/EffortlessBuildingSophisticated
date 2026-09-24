package sophisticated.building.neoforge.platform;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.platform.services.INetworkHelper;

import java.util.List;

public final class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.SERVER.noArg().send(copy(payload, PacketHandler.SERVERBOUND));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.PLAYER.with(player).send(copy(payload, PacketHandler.CLIENTBOUND));
    }

    /**
     * Minecraft 1.20.4 hands packets to the other side of an in-memory (singleplayer) connection without encoding
     * them, so the receiver would get the sender's own object, e.g. the BuilderChain's live BlockSet, which the client
     * clears on its next tick before the integrated server places it (nothing was placed). The receiver gets a decoded
     * copy instead, as over a network connection and as in Minecraft 1.20.5+, which encodes in memory too.
     */
    private static CustomPacketPayload copy(CustomPacketPayload payload, List<PacketHandler.Payload<?>> direction) {
        for (PacketHandler.Payload<?> type : direction) {
            if (type.id().equals(payload.id())) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    payload.write(buffer);
                    return type.reader().apply(buffer);
                } finally {
                    buffer.release();
                }
            }
        }
        return payload;
    }
}
