package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;

/**
 * The server's power level limits (reach, blocks at once, blocks per axis, mirror radius of every level; the common
 * config, which the loaders do not sync) sent to the client on join, so the client builds with the limits the server
 * checks (ServerBlockPlacer). The client drops them when it leaves the world or server.
 */
public record CommonConfigSyncPacket(int[] values) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("common_config_sync");

	/** At most this many values are read (as the list codec of the newer branches). */
	private static final int MAX_VALUES = 64;

	public CommonConfigSyncPacket(FriendlyByteBuf buf) {
		this(buf.readVarIntArray(MAX_VALUES));
	}

	public static CommonConfigSyncPacket fromCurrent() {
		return new CommonConfigSyncPacket(CommonConfig.syncedValues());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarIntArray(values);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final CommonConfigSyncPacket packet, final Player player) {
			CommonConfig.SERVER_VALUES.apply(packet.values().clone());
		}
	}
}
