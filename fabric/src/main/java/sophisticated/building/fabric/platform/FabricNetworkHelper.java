package sophisticated.building.fabric.platform;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.network.ModPayload;
import sophisticated.building.platform.services.INetworkHelper;

/**
 * Sends the payloads on the Fabric channel named by their id, with the body the payload writes (Fabric API
 * networking v0, the only networking API of Fabric API 0.25.0 for Minecraft 1.16.3).
 */
public final class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(ModPayload payload) {
        ClientSidePacketRegistry.INSTANCE.sendToServer(payload.id(), encode(payload));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPayload payload) {
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, payload.id(), encode(payload));
    }

    private static FriendlyByteBuf encode(ModPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        return buf;
    }
}
