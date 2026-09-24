package sophisticated.building.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BackpackToolsPacket;
import sophisticated.building.network.message.BreakCountdownPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
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
 * The mod's payloads, their codecs and handlers. The loader projects register every entry with their
 * networking API and run the handler on the main thread with the context player (the sender on the
 * server, the local player on the client). A payload type in both lists is bidirectional.
 */
public final class PacketHandler {

	public static final List<Payload<?>> SERVERBOUND = List.of(
			new Payload<>(IsUsingBuildModePacket.ID, IsUsingBuildModePacket.CODEC, IsUsingBuildModePacket.Handler::handle, "is_using_build_mode"),
			new Payload<>(IsQuickReplacingPacket.ID, IsQuickReplacingPacket.CODEC, IsQuickReplacingPacket.Handler::handle, "is_quick_replacing"),
			new Payload<>(ServerPlaceBlocksPacket.ID, ServerPlaceBlocksPacket.CODEC, ServerPlaceBlocksPacket.Handler::handle, "server_place_blocks"),
			new Payload<>(ServerBreakBlocksPacket.ID, ServerBreakBlocksPacket.CODEC, ServerBreakBlocksPacket.Handler::handle, "server_break_blocks"),
			new Payload<>(PerformUndoPacket.ID, PerformUndoPacket.CODEC, PerformUndoPacket.Handler::handle, "perform_undo"),
			// Shares the undo failure key, as in 4.2.1
			new Payload<>(PerformRedoPacket.ID, PerformRedoPacket.CODEC, PerformRedoPacket.Handler::handle, "perform_undo"),
			new Payload<>(OmegaBagWeightPacket.ID, OmegaBagWeightPacket.CODEC, OmegaBagWeightPacket.Handler::handle, null),
			new Payload<>(ModifierSettingsPacket.ID, ModifierSettingsPacket.CODEC, ModifierSettingsPacket.ServerHandler::handleServer, "modifier_settings")
	);

	public static final List<Payload<?>> CLIENTBOUND = List.of(
			new Payload<>(BackpackItemCountPacket.ID, BackpackItemCountPacket.CODEC, BackpackItemCountPacket.Handler::handle, null),
			new Payload<>(ModifierSettingsPacket.ID, ModifierSettingsPacket.CODEC, ModifierSettingsPacket.ClientHandler::handleClient, "modifier_settings"),
			new Payload<>(PowerLevelPacket.ID, PowerLevelPacket.CODEC, PowerLevelPacket.Handler::handle, "power_level"),
			new Payload<>(TranslatedLogPacket.ID, TranslatedLogPacket.CODEC, TranslatedLogPacket.Handler::handle, "translated_log"),
			new Payload<>(BuildingUpgradeStatePacket.ID, BuildingUpgradeStatePacket.CODEC, BuildingUpgradeStatePacket.Handler::handle, "building_upgrade_state"),
			new Payload<>(BackpackToolsPacket.ID, BackpackToolsPacket.CODEC, BackpackToolsPacket.Handler::handle, "backpack_tools"),
			new Payload<>(BreakCountdownPacket.ID, BreakCountdownPacket.CODEC, BreakCountdownPacket.Handler::handle, "break_countdown")
	);

	private PacketHandler() {
	}

	/**
	 * One payload type in one direction.
	 *
	 * @param failureKey NeoForge disconnects with the translation key
	 *                   {@code sophisticatedbuilding.networking.<failureKey>.failed} when the handler throws;
	 *                   null to only drop the failure. On Fabric the game's task queue logs it.
	 */
	public record Payload<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
														 StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
														 BiConsumer<T, Player> handler,
														 @Nullable String failureKey) {
	}
}
