package sophisticated.building.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.network.message.ServerConfigSyncPacket;

/**
 * Client receivers of the clientbound payloads (see {@link PacketHandler}); the handlers run on the
 * client thread with the local player.
 */
public final class FabricClientNetworking {

    private FabricClientNetworking() {
    }

    public static void setupClient() {
        for (PacketHandler.Payload<?> payload : PacketHandler.CLIENTBOUND) {
            registerReceiver(payload);
        }
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigSyncPacket.ID, (packet, context) ->
                context.client().execute(() -> ServerConfigSyncPacket.Handler.handle(packet, context.player())));
        SophisticatedBuilding.log("Registered client networking receivers");
    }

    private static <T extends CustomPacketPayload> void registerReceiver(PacketHandler.Payload<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (packet, context) ->
                context.client().execute(() -> payload.handler().accept(packet, context.player())));
    }
}
