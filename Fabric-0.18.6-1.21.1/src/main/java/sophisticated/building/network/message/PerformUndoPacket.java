package sophisticated.building.network.message;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.SophisticatedBuilding;

public record PerformUndoPacket() implements CustomPacketPayload {

	public static final StreamCodec<FriendlyByteBuf, PerformUndoPacket> CODEC = CustomPacketPayload.codec(
			PerformUndoPacket::write,
			PerformUndoPacket::new);
	public static final Type<PerformUndoPacket> ID = new Type<>(SophisticatedBuilding.asResource("perform_undo"));

	public PerformUndoPacket(FriendlyByteBuf buf) {
		this();
	}

	public void write(FriendlyByteBuf buf) {}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final PerformUndoPacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				if (context.player() instanceof ServerPlayer player) {
					SophisticatedBuilding.UNDO_REDO.undo(player);
				}
			});
		}
	}
}
