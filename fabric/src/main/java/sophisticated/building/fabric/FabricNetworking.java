package sophisticated.building.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.network.PacketHandler;

/**
 * Registers the server receivers of the serverbound payloads (see {@link PacketHandler}); each payload
 * travels on the channel named by its id. The payload is read on the network thread and its handler
 * runs on the server thread with the sending player.
 */
public final class FabricNetworking {

    private FabricNetworking() {
    }

    public static void setupCommon() {
        for (PacketHandler.Payload<?> payload : PacketHandler.SERVERBOUND) {
            registerReceiver(payload);
        }
        SophisticatedBuilding.log("Registered networking server receivers");
    }

    private static <T extends ModPayload> void registerReceiver(PacketHandler.Payload<T> payload) {
        ServerPlayNetworking.registerGlobalReceiver(payload.id(), (server, player, handler, buf, responseSender) -> {
            T packet = payload.reader().apply(buf);
            server.execute(() -> payload.handler().accept(packet, player));
        });
    }
}
