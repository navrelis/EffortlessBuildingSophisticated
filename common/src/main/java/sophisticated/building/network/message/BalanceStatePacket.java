package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBalanceState;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.network.ModPayload;

/**
 * The player's balanced breaking state (see {@code BalancedBreaking}): whether it is on for this server, the remaining
 * cooldown and the operations in the cooldown window. Sent on login, respawn and dimension change, after every
 * accepted or refused balanced operation and when the server's effective setting changes. {@code refused} is
 * {@link #REFUSED_BREAK} or {@link #REFUSED_PLACEMENT} when this packet answers a refused request: the client then
 * drops the pending outline of that request at once instead of waiting for its timeout.
 */
public record BalanceStatePacket(boolean active, long cooldownRemainingMillis, int usesInWindow, int refused) implements ModPayload {
	public static final int REFUSED_NONE = 0;
	public static final int REFUSED_BREAK = 1;
	public static final int REFUSED_PLACEMENT = 2;

	public static final ResourceLocation ID = SophisticatedBuilding.asResource("balance_state");

	public BalanceStatePacket(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(active);
		buf.writeVarLong(cooldownRemainingMillis);
		buf.writeVarInt(usesInWindow);
		buf.writeVarInt(refused);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final BalanceStatePacket packet, final Player player) {
			ClientBalanceState.set(packet.active(), packet.cooldownRemainingMillis(), packet.usesInWindow());
			if (packet.refused() != REFUSED_NONE) {
				ClientBreakCountdown.dropPending(packet.refused() == REFUSED_PLACEMENT);
			}
		}
	}
}
