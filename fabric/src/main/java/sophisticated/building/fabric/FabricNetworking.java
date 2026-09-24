package sophisticated.building.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.network.message.ServerConfigSyncPacket;

/**
 * Registers the payload types of both directions (see {@link PacketHandler}) plus the Fabric-only
 * server config sync, and the server receivers; the handlers run on the server thread with the
 * sending player.
 */
public final class FabricNetworking {

    private FabricNetworking() {
    }

    public static void setupCommon() {
        for (PacketHandler.Payload<?> payload : PacketHandler.SERVERBOUND) {
            registerType(PayloadTypeRegistry.serverboundPlay(), payload);
        }
        for (PacketHandler.Payload<?> payload : PacketHandler.CLIENTBOUND) {
            registerType(PayloadTypeRegistry.clientboundPlay(), payload);
        }
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigSyncPacket.ID, ServerConfigSyncPacket.CODEC);

        for (PacketHandler.Payload<?> payload : PacketHandler.SERVERBOUND) {
            registerReceiver(payload);
        }
        SophisticatedBuilding.log("Registered networking payloads and server receivers");
    }

    private static <T extends CustomPacketPayload> void registerType(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry, PacketHandler.Payload<T> payload) {
        registry.register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerReceiver(PacketHandler.Payload<T> payload) {
        ServerPlayNetworking.registerGlobalReceiver(payload.type(), (packet, context) ->
                context.server().execute(() -> payload.handler().accept(packet, context.player())));
    }
}
