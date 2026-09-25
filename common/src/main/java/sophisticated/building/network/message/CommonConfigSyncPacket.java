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
	/** At most this many values are read (the synced list has 20: four limits of five levels). */
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
		int[] values = new int[this.values.size()];
		for (int i = 0; i < values.length; i++) values[i] = this.values.get(i);
		buf.writeVarIntArray(values);
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
