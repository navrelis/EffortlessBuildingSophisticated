package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.systems.ServerBuildState;

public record IsUsingBuildModePacket(boolean isUsingBuildMode) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("is_using_build_mode");

	public IsUsingBuildModePacket(FriendlyByteBuf buf) {
		this(buf.readBoolean());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(isUsingBuildMode);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final IsUsingBuildModePacket packet, final Player sender) {
			if (sender instanceof ServerPlayer player) {
				ServerBuildState.setIsUsingBuildMode(player, packet.isUsingBuildMode());
			}
		}
	}
}
