package sophisticated.building.network.message;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;

/**
 * Sync power level from server to client
 */
public record PowerLevelPacket(int powerLevel) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, PowerLevelPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			PowerLevelPacket::powerLevel,
			PowerLevelPacket::new);
	public static final Type<PowerLevelPacket> ID = new Type<>(SophisticatedBuilding.asResource("power_level"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final PowerLevelPacket packet, final ClientPlayNetworking.Context context) {
			context.client().execute(() -> {
				Player player = context.player();
				if (player != null) {
					PowerLevel currentLevel = AttachmentHandler.getOrCreatePowerLevel(player);
					currentLevel.setPowerLevel(packet.powerLevel);
					AttachmentHandler.setPowerLevel(player, currentLevel);
				}
			});
		}
	}
}
