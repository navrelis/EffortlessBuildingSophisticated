package sophisticated.building.network.message;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.BlockSet;

/**
 * Sends a message to the server to break multiple blocks
 */
public record ServerBreakBlocksPacket(BlockSet blocks) implements CustomPacketPayload {

	public static final StreamCodec<FriendlyByteBuf, ServerBreakBlocksPacket> CODEC = CustomPacketPayload.codec(
			ServerBreakBlocksPacket::write,
			ServerBreakBlocksPacket::new);
	public static final Type<ServerBreakBlocksPacket> ID = new Type<>(SophisticatedBuilding.asResource("server_break_blocks"));

	public ServerBreakBlocksPacket(FriendlyByteBuf buf) {
		this(BlockSet.decode(buf));
	}

	public void write(FriendlyByteBuf buf) {
		BlockSet.encode(buf, blocks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerBreakBlocksPacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				if (context.player() instanceof ServerPlayer player) {
					SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, packet.blocks());
				}
			});
		}
	}
}
