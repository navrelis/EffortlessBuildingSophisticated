package sophisticated.building.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.IsQuickReplacingPacket;
import sophisticated.building.network.message.IsUsingBuildModePacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.OmegaBagWeightPacket;
import sophisticated.building.network.message.PerformRedoPacket;
import sophisticated.building.network.message.PerformUndoPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.network.message.ServerBreakBlocksPacket;
import sophisticated.building.network.message.ServerPlaceBlocksPacket;
import sophisticated.building.network.message.TranslatedLogPacket;

public class PacketHandler {

	public static void setupCommon() {
		registerPayloadTypes();
		registerServerReceivers();
		SophisticatedBuilding.log("Registered networking payloads and server receivers");
	}

	private static void registerPayloadTypes() {
		PayloadTypeRegistry.playC2S().register(IsUsingBuildModePacket.ID, IsUsingBuildModePacket.CODEC);
		PayloadTypeRegistry.playC2S().register(IsQuickReplacingPacket.ID, IsQuickReplacingPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(ServerPlaceBlocksPacket.ID, ServerPlaceBlocksPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(ServerBreakBlocksPacket.ID, ServerBreakBlocksPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(PerformUndoPacket.ID, PerformUndoPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(PerformRedoPacket.ID, PerformRedoPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(OmegaBagWeightPacket.ID, OmegaBagWeightPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(ModifierSettingsPacket.ID, ModifierSettingsPacket.CODEC);

		PayloadTypeRegistry.playS2C().register(BackpackItemCountPacket.ID, BackpackItemCountPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(ModifierSettingsPacket.ID, ModifierSettingsPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(PowerLevelPacket.ID, PowerLevelPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(TranslatedLogPacket.ID, TranslatedLogPacket.CODEC);
	}

	private static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(IsUsingBuildModePacket.ID, IsUsingBuildModePacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(IsQuickReplacingPacket.ID, IsQuickReplacingPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(ServerPlaceBlocksPacket.ID, ServerPlaceBlocksPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(ServerBreakBlocksPacket.ID, ServerBreakBlocksPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(PerformUndoPacket.ID, PerformUndoPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(PerformRedoPacket.ID, PerformRedoPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(OmegaBagWeightPacket.ID, OmegaBagWeightPacket.Handler::handle);
		ServerPlayNetworking.registerGlobalReceiver(ModifierSettingsPacket.ID, ModifierSettingsPacket.ServerHandler::handleServer);
	}
}
