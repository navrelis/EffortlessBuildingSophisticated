package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.config.ModConfigs;

/**
 * Sends all {@link ServerConfig} values (compact values-only JSON) to the client on join, so
 * client-side decisions (e.g. {@code PowerLevel.canBreakFar}) use the server's settings.
 */
public record ServerConfigSyncPacket(String json) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, ServerConfigSyncPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(1 << 20),
			ServerConfigSyncPacket::json,
			ServerConfigSyncPacket::new);

	public static final Type<ServerConfigSyncPacket> ID = new Type<>(SophisticatedBuilding.asResource("server_config_sync"));

	public static ServerConfigSyncPacket fromCurrent() {
		return new ServerConfigSyncPacket(ModConfigs.spec(ServerConfig.spec).toSyncJson());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerConfigSyncPacket packet, final Player player) {
			ModConfigs.applyServerSync(packet.json());
		}
	}
}
