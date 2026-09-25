package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.network.ModPayload;

/**
 * Sent right after the server enqueues a survival break as a {@code DelayedEntry}, so the client
 * can show an on-screen countdown until the blocks actually break (T-S10). Creative breaking is
 * instant and sends nothing. {@code placing} marks a survival placement that mines the blocks it
 * replaces; {@code blockCount} is then the number of replaced blocks.
 */
public final class BreakCountdownPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("break_countdown");

	private final int delayTicks;
	private final int blockCount;
	private final boolean placing;

	public BreakCountdownPacket(int delayTicks, int blockCount, boolean placing) {
		this.delayTicks = delayTicks;
		this.blockCount = blockCount;
		this.placing = placing;
	}

	public int delayTicks() {
		return delayTicks;
	}

	public int blockCount() {
		return blockCount;
	}

	public boolean placing() {
		return placing;
	}

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
