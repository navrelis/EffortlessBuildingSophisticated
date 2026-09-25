package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;

public final class PerformRedoPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("perform_redo");

	public PerformRedoPacket() {
	}

	public PerformRedoPacket(FriendlyByteBuf buf) {
		this();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final PerformRedoPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) sender;
				SophisticatedBuilding.UNDO_REDO.redo(player);
			}
		}
	}
}
