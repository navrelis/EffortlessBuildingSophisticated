package sophisticated.building.network.message;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBreakCountdown;

/**
 * Sent right after the server enqueues a survival break as a {@code DelayedEntry}, so the client
 * can show an on-screen countdown until the blocks actually break (T-S10). Creative breaking is
 * instant and sends nothing. {@code placing} marks a survival placement that mines the blocks it
 * replaces; {@code blockCount} is then the number of replaced blocks.
 */
public record BreakCountdownPacket(int delayTicks, int blockCount, boolean placing) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, BreakCountdownPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			BreakCountdownPacket::delayTicks,
			ByteBufCodecs.INT,
			BreakCountdownPacket::blockCount,
			ByteBufCodecs.BOOL,
			BreakCountdownPacket::placing,
			BreakCountdownPacket::new);

	public static final Type<BreakCountdownPacket> ID = new Type<>(SophisticatedBuilding.asResource("break_countdown"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final BreakCountdownPacket packet, final ClientPlayNetworking.Context context) {
			context.client().execute(() -> ClientBreakCountdown.onPacket(packet.delayTicks(), packet.blockCount(), packet.placing()));
		}
	}
}
