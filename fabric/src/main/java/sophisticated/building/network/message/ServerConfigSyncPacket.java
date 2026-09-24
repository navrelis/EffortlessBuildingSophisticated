package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.config.ModConfigs;

/**
 * Sends all {@link ServerConfig} values (compact values-only JSON) to the client on join, so
 * client-side decisions (e.g. {@code PowerLevel.canBreakFar}) use the server's settings.
 */
public record ServerConfigSyncPacket(String json) implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("server_config_sync");

	private static final int MAX_JSON_LENGTH = 1 << 20;

	public ServerConfigSyncPacket(FriendlyByteBuf buf) {
		this(buf.readUtf(MAX_JSON_LENGTH));
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(json, MAX_JSON_LENGTH);
	}

	public static ServerConfigSyncPacket fromCurrent() {
		return new ServerConfigSyncPacket(ModConfigs.spec(ServerConfig.spec).toSyncJson());
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerConfigSyncPacket packet, final Player player) {
			ModConfigs.applyServerSync(packet.json());
		}
	}
}
