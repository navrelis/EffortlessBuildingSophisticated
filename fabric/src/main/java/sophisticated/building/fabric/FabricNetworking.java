package sophisticated.building.fabric;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.network.PacketHandler;

/**
 * Registers the server receivers of the serverbound payloads (see {@link PacketHandler}); each payload
 * travels on the channel named by its id. The payload is read on the network thread and its handler
 * runs on the server thread with the sending player. Fabric API 0.25.0 (the last one for Minecraft 1.16.3) only has
 * the networking v0 packet registries.
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
        ServerSidePacketRegistry.INSTANCE.register(payload.id(), (context, buf) -> {
            T packet = payload.reader().apply(buf);
            context.getTaskQueue().execute(() -> payload.handler().accept(packet, context.getPlayer()));
        });
    }
}
