package sophisticated.building.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.IsQuickReplacingPacket;
import sophisticated.building.network.message.IsUsingBuildModePacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.OmegaBagWeightPacket;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.PerformRedoPacket;
import sophisticated.building.network.message.PerformUndoPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.network.message.ServerBreakBlocksPacket;
import sophisticated.building.network.message.ServerPlaceBlocksPacket;
import sophisticated.building.network.message.TranslatedLogPacket;

public class PacketHandler {

	public static void setupPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedBuilding.MODID);

		registrar.playToServer(IsUsingBuildModePacket.ID, IsUsingBuildModePacket.CODEC, IsUsingBuildModePacket.Handler::handle);
		registrar.playToServer(IsQuickReplacingPacket.ID, IsQuickReplacingPacket.CODEC, IsQuickReplacingPacket.Handler::handle);
		registrar.playToServer(ServerPlaceBlocksPacket.ID, ServerPlaceBlocksPacket.CODEC, ServerPlaceBlocksPacket.Handler::handle);
		registrar.playToServer(ServerBreakBlocksPacket.ID, ServerBreakBlocksPacket.CODEC, ServerBreakBlocksPacket.Handler::handle);
		registrar.playToServer(PerformUndoPacket.ID, PerformUndoPacket.CODEC, PerformUndoPacket.Handler::handle);
		registrar.playToServer(PerformRedoPacket.ID, PerformRedoPacket.CODEC, PerformRedoPacket.Handler::handle);
		registrar.playToServer(OmegaBagWeightPacket.ID, OmegaBagWeightPacket.CODEC, OmegaBagWeightPacket.Handler::handle);
		registrar.playToClient(BackpackItemCountPacket.ID, BackpackItemCountPacket.CODEC, BackpackItemCountPacket.Handler::handle);

		registrar.playBidirectional(ModifierSettingsPacket.ID, ModifierSettingsPacket.CODEC,
				new DirectionalPayloadHandler<>(
						ModifierSettingsPacket.ClientHandler::handleClient,
						ModifierSettingsPacket.ServerHandler::handleServer
				)
		);

		registrar.playToClient(PowerLevelPacket.ID, PowerLevelPacket.CODEC, PowerLevelPacket.Handler::handle);
		registrar.playToClient(TranslatedLogPacket.ID, TranslatedLogPacket.CODEC, TranslatedLogPacket.Handler::handle);
	}
}
