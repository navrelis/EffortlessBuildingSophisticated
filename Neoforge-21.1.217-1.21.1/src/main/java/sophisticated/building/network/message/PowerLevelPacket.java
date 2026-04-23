package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import sophisticated.building.SophisticatedBuilding;
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
		public static void handle(final PowerLevelPacket packet, final IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() != null) {
					Player player = context.player();
					PowerLevel currentLevel = player.getData(SophisticatedBuilding.POWER_LEVEL.get());
					currentLevel.setPowerLevel(packet.powerLevel);
					player.setData(SophisticatedBuilding.POWER_LEVEL.get(), currentLevel);
				}
			}).exceptionally(e -> {
				// Handle exception
				context.disconnect(Component.translatable("sophisticatedbuilding.networking.power_level.failed", e.getMessage()));
				return null;
			});
		}
	}
}
