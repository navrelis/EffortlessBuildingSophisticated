package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.BlockSet;

/**
 * Sends a message to the server to place multiple blocks
 */
public record ServerPlaceBlocksPacket(BlockSet blocks, long placeTime) implements CustomPacketPayload {

	public static final StreamCodec<FriendlyByteBuf, ServerPlaceBlocksPacket> CODEC = CustomPacketPayload.codec(
			ServerPlaceBlocksPacket::write,
			ServerPlaceBlocksPacket::new);
	public static final Type<ServerPlaceBlocksPacket> ID = new Type<>(SophisticatedBuilding.asResource("server_place_blocks"));

	public ServerPlaceBlocksPacket(FriendlyByteBuf buf) {
		this(BlockSet.decode(buf), buf.readLong());
	}

	public void write(FriendlyByteBuf buf) {
		BlockSet.encode(buf, blocks);
		buf.writeLong(placeTime);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerPlaceBlocksPacket packet, final IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, packet.blocks(), packet.placeTime());
				}
			}).exceptionally(e -> {
				// Handle exception
				context.disconnect(Component.translatable("sophisticatedbuilding.networking.server_place_blocks.failed", e.getMessage()));
				return null;
			});
		}
	}
}
