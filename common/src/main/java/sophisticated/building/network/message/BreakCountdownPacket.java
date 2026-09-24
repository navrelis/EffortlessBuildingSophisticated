package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBreakCountdown;

/**
 * Sent right after the server enqueues a survival break as a {@code DelayedEntry}, so the client
 * can show an on-screen countdown until the blocks actually break (T-S10). Creative breaking is
 * instant and sends nothing. {@code placing} marks a survival placement that mines the blocks it
 * replaces; {@code blockCount} is then the number of replaced blocks.
 */
public record BreakCountdownPacket(int delayTicks, int blockCount, boolean placing) implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("break_countdown");

	public BreakCountdownPacket(FriendlyByteBuf buf) {
		this(buf.readInt(), buf.readInt(), buf.readBoolean());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(delayTicks);
		buf.writeInt(blockCount);
		buf.writeBoolean(placing);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final BreakCountdownPacket packet, final Player player) {
			ClientBreakCountdown.onPacket(packet.delayTicks(), packet.blockCount(), packet.placing());
		}
	}
}
