package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;

import java.util.List;

/**
 * The server's power level limits (reach, blocks at once, blocks per axis, mirror radius of every level; the common
 * config, which the loaders do not sync) sent to the client on join, so the client builds with the limits the server
 * checks (ServerBlockPlacer). The client drops them when it leaves the world or server.
 */
public record CommonConfigSyncPacket(List<Integer> values) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("common_config_sync");
	/** Upper bound of the value count read from the network. */
	private static final int MAX_VALUES = 64;

	public CommonConfigSyncPacket(FriendlyByteBuf buf) {
		this(boxed(buf.readVarIntArray(MAX_VALUES)));
	}

	public static CommonConfigSyncPacket fromCurrent() {
		return new CommonConfigSyncPacket(boxed(CommonConfig.syncedValues()));
	}

	private static List<Integer> boxed(int[] values) {
		Integer[] boxed = new Integer[values.length];
		for (int i = 0; i < values.length; i++) boxed[i] = values[i];
		return List.of(boxed);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		int[] unboxed = new int[values.size()];
		for (int i = 0; i < unboxed.length; i++) unboxed[i] = values.get(i);
		buf.writeVarIntArray(unboxed);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final CommonConfigSyncPacket packet, final Player player) {
			int[] values = new int[packet.values().size()];
			for (int i = 0; i < values.length; i++) values[i] = packet.values().get(i);
			CommonConfig.SERVER_VALUES.apply(values);
		}
	}
}
