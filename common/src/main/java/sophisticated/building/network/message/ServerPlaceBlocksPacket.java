package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.utilities.BlockSet;

/**
 * Sends a message to the server to place multiple blocks
 */
public final class ServerPlaceBlocksPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("server_place_blocks");

	private final BlockSet blocks;
	private final long placeTime;

	public ServerPlaceBlocksPacket(BlockSet blocks, long placeTime) {
		this.blocks = blocks;
		this.placeTime = placeTime;
	}

	public BlockSet blocks() {
		return blocks;
	}

	public long placeTime() {
		return placeTime;
	}

	public ServerPlaceBlocksPacket(FriendlyByteBuf buf) {
		this(BlockSet.decode(buf), buf.readLong());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		BlockSet.encode(buf, blocks);
		buf.writeLong(placeTime);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final ServerPlaceBlocksPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) sender;
				SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, packet.blocks(), packet.placeTime());
			}
		}
	}
}
