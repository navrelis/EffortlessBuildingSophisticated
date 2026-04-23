package sophisticated.building.network.message;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.systems.ServerBuildState;

public record IsUsingBuildModePacket(boolean isUsingBuildMode) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, IsUsingBuildModePacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			IsUsingBuildModePacket::isUsingBuildMode,
			IsUsingBuildModePacket::new);
	public static final Type<IsUsingBuildModePacket> ID = new Type<>(SophisticatedBuilding.asResource("is_using_build_mode"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final IsUsingBuildModePacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ServerBuildState.setIsUsingBuildMode(player, packet.isUsingBuildMode());
				}
			});
		}
	}
}
