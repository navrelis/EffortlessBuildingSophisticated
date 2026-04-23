package sophisticated.building.network.message;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.systems.ServerBuildState;

public record IsQuickReplacingPacket(boolean isQuickReplacing) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, IsQuickReplacingPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			IsQuickReplacingPacket::isQuickReplacing,
			IsQuickReplacingPacket::new);
	public static final Type<IsQuickReplacingPacket> ID = new Type<>(SophisticatedBuilding.asResource("is_quick_replacing"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final IsQuickReplacingPacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ServerBuildState.setIsQuickReplacing(player, packet.isQuickReplacing());
				}
			});
		}
	}
}
