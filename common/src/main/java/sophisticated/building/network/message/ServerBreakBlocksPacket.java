package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.utilities.BlockSet;

/**
 * Sends a message to the server to break multiple blocks
 */
public record ServerBreakBlocksPacket(BlockSet blocks) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("server_break_blocks");

	public ServerBreakBlocksPacket(FriendlyByteBuf buf) {
		this(BlockSet.decode(buf));
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		BlockSet.encode(buf, blocks);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerBreakBlocksPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer player) {
				SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, packet.blocks());
			}
		}
	}
}
