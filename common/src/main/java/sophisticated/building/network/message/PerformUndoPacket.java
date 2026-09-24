package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;

public record PerformUndoPacket() implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("perform_undo");

	public PerformUndoPacket(FriendlyByteBuf buf) {
		this();
	}

	@Override
	public void write(FriendlyByteBuf buf) {}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final PerformUndoPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer player) {
				SophisticatedBuilding.UNDO_REDO.undo(player);
			}
		}
	}
}
