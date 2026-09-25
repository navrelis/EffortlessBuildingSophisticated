package sophisticated.building.fabric;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.network.message.ServerConfigSyncPacket;

/**
 * Client receivers of the clientbound payloads (see {@link PacketHandler}) plus the Fabric-only
 * server config sync. The payload is read on the network thread and its handler runs on the client
 * thread with the local player (Fabric API networking v0, see {@link FabricNetworking}).
 */
public final class FabricClientNetworking {

    private FabricClientNetworking() {
    }

    public static void setupClient() {
        for (PacketHandler.Payload<?> payload : PacketHandler.CLIENTBOUND) {
            registerReceiver(payload);
        }
        registerReceiver(new PacketHandler.Payload<>(ServerConfigSyncPacket.ID, ServerConfigSyncPacket::new, ServerConfigSyncPacket.Handler::handle, null));
        SophisticatedBuilding.log("Registered client networking receivers");
    }

    private static <T extends ModPayload> void registerReceiver(PacketHandler.Payload<T> payload) {
        ClientSidePacketRegistry.INSTANCE.register(payload.id(), (context, buf) -> {
            T packet = payload.reader().apply(buf);
            context.getTaskQueue().execute(() -> payload.handler().accept(packet, context.getPlayer()));
        });
    }
}
