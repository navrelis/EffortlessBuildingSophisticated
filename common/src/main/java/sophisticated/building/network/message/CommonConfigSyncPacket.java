package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;

/**
 * The server's power level limits (reach, blocks at once, blocks per axis, mirror radius of every level; the common
 * config, which the loaders do not sync) sent to the client on join, so the client builds with the limits the server
 * checks (ServerBlockPlacer). The client drops them when it leaves the world or server.
 */
public record CommonConfigSyncPacket(List<Integer> values) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, CommonConfigSyncPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)),
			CommonConfigSyncPacket::values,
			CommonConfigSyncPacket::new);

	public static final Type<CommonConfigSyncPacket> ID = new Type<>(SophisticatedBuilding.asResource("common_config_sync"));

	public static CommonConfigSyncPacket fromCurrent() {
		int[] values = CommonConfig.syncedValues();
		Integer[] boxed = new Integer[values.length];
		for (int i = 0; i < values.length; i++) boxed[i] = values[i];
		return new CommonConfigSyncPacket(List.of(boxed));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
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
