package sophisticated.building.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BackpackToolsPacket;
import sophisticated.building.network.message.BreakCountdownPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
import sophisticated.building.network.message.CommonConfigSyncPacket;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The mod's payloads, their readers and handlers. The loader projects register every entry with their
 * networking API and run the handler on the main thread with the context player (the sender on the
 * server, the local player on the client). A payload type in both lists is bidirectional.
 */
public final class PacketHandler {

	public static final List<Payload<?>> SERVERBOUND = List.of(
			new Payload<>(IsUsingBuildModePacket.ID, IsUsingBuildModePacket::new, IsUsingBuildModePacket.Handler::handle, "is_using_build_mode"),
			new Payload<>(IsQuickReplacingPacket.ID, IsQuickReplacingPacket::new, IsQuickReplacingPacket.Handler::handle, "is_quick_replacing"),
			new Payload<>(ServerPlaceBlocksPacket.ID, ServerPlaceBlocksPacket::new, ServerPlaceBlocksPacket.Handler::handle, "server_place_blocks"),
			new Payload<>(ServerBreakBlocksPacket.ID, ServerBreakBlocksPacket::new, ServerBreakBlocksPacket.Handler::handle, "server_break_blocks"),
			new Payload<>(PerformUndoPacket.ID, PerformUndoPacket::new, PerformUndoPacket.Handler::handle, "perform_undo"),
			// Shares the undo failure key, as in 4.2.1
			new Payload<>(PerformRedoPacket.ID, PerformRedoPacket::new, PerformRedoPacket.Handler::handle, "perform_undo"),
			new Payload<>(OmegaBagWeightPacket.ID, OmegaBagWeightPacket::new, OmegaBagWeightPacket.Handler::handle, null),
			new Payload<>(ModifierSettingsPacket.ID, ModifierSettingsPacket::new, ModifierSettingsPacket.ServerHandler::handleServer, "modifier_settings")
	);

	public static final List<Payload<?>> CLIENTBOUND = List.of(
			new Payload<>(BackpackItemCountPacket.ID, BackpackItemCountPacket::new, BackpackItemCountPacket.Handler::handle, null),
			new Payload<>(ModifierSettingsPacket.ID, ModifierSettingsPacket::new, ModifierSettingsPacket.ClientHandler::handleClient, "modifier_settings"),
			new Payload<>(PowerLevelPacket.ID, PowerLevelPacket::new, PowerLevelPacket.Handler::handle, "power_level"),
			new Payload<>(TranslatedLogPacket.ID, TranslatedLogPacket::new, TranslatedLogPacket.Handler::handle, "translated_log"),
			new Payload<>(BuildingUpgradeStatePacket.ID, BuildingUpgradeStatePacket::new, BuildingUpgradeStatePacket.Handler::handle, "building_upgrade_state"),
			new Payload<>(BackpackToolsPacket.ID, BackpackToolsPacket::new, BackpackToolsPacket.Handler::handle, "backpack_tools"),
			new Payload<>(BreakCountdownPacket.ID, BreakCountdownPacket::new, BreakCountdownPacket.Handler::handle, "break_countdown"),
			new Payload<>(CommonConfigSyncPacket.ID, CommonConfigSyncPacket::new, CommonConfigSyncPacket.Handler::handle, "common_config_sync")
	);

	private PacketHandler() {
	}

	/**
	 * One payload type in one direction.
	 *
	 * @param failureKey Forge disconnects with the translation key
	 *                   {@code sophisticatedbuilding.networking.<failureKey>.failed} when the handler throws;
	 *                   null to only drop the failure. On Fabric the game's task queue logs it.
	 */
	public record Payload<T extends ModPayload>(ResourceLocation id,
												FriendlyByteBuf.Reader<T> reader,
												BiConsumer<T, Player> handler,
												@Nullable String failureKey) {
	}
}
