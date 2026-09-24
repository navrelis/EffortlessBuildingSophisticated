package sophisticated.building.fabric.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.network.ModPayload;
import sophisticated.building.platform.services.INetworkHelper;

/**
 * Sends the payloads on the Fabric channel named by their id, with the body the payload writes.
 */
public final class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(ModPayload payload) {
        ClientPlayNetworking.send(payload.id(), encode(payload));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPayload payload) {
        ServerPlayNetworking.send(player, payload.id(), encode(payload));
    }

    private static FriendlyByteBuf encode(ModPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        return buf;
    }
}
