package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.systems.ServerBuildState;

public record IsQuickReplacingPacket(boolean isQuickReplacing) implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("is_quick_replacing");

	public IsQuickReplacingPacket(FriendlyByteBuf buf) {
		this(buf.readBoolean());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(isQuickReplacing);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final IsQuickReplacingPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer player) {
				ServerBuildState.setIsQuickReplacing(player, packet.isQuickReplacing());
			}
		}
	}
}
