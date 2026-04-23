package sophisticated.building.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.network.message.TranslatedLogPacket;

public final class PacketHandlerClient {

    private PacketHandlerClient() {
    }

    public static void setupClient() {
        ClientPlayNetworking.registerGlobalReceiver(BackpackItemCountPacket.ID, BackpackItemCountPacket.Handler::handle);
        ClientPlayNetworking.registerGlobalReceiver(ModifierSettingsPacket.ID, ModifierSettingsPacket.ClientHandler::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(PowerLevelPacket.ID, PowerLevelPacket.Handler::handle);
        ClientPlayNetworking.registerGlobalReceiver(TranslatedLogPacket.ID, TranslatedLogPacket.Handler::handle);
        SophisticatedBuilding.log("Registered client networking receivers");
    }
}
