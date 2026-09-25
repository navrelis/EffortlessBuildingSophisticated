package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.config.ModConfigs;
import sophisticated.building.network.ModPayload;

/**
 * Sends all {@link ServerConfig} values (compact values-only JSON) to the client on join, so
 * client-side decisions (e.g. {@code PowerLevel.canBreakFar}) use the server's settings.
 */
public final class ServerConfigSyncPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("server_config_sync");

	private static final int MAX_JSON_LENGTH = 1 << 20;

	private final String json;

	public ServerConfigSyncPacket(String json) {
		this.json = json;
	}

	public String json() {
		return json;
	}

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
