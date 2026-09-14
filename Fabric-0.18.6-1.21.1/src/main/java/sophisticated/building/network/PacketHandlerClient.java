package sophisticated.building.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BackpackToolsPacket;
import sophisticated.building.network.message.BreakCountdownPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
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
        ClientPlayNetworking.registerGlobalReceiver(BuildingUpgradeStatePacket.ID, BuildingUpgradeStatePacket.Handler::handle);
        ClientPlayNetworking.registerGlobalReceiver(BackpackToolsPacket.ID, BackpackToolsPacket.Handler::handle);
        ClientPlayNetworking.registerGlobalReceiver(BreakCountdownPacket.ID, BreakCountdownPacket.Handler::handle);
        SophisticatedBuilding.log("Registered client networking receivers");
    }
}
