package sophisticated.building.client;

import net.minecraft.core.BlockPos;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

/**
 * Tracks pending survival break sets between the click (when the client sends
 * {@code ServerBreakBlocksPacket}) and the server's {@code BreakCountdownPacket} response, then
 * counts down until the blocks actually break, so the HUD can show an on-screen countdown (T-S10).
 * No loader imports - callable from common client code on either loader.
 */
public class ClientBreakCountdown {

	private static final int PENDING_TIMEOUT_TICKS = 60;

	private static final Deque<PendingSet> PENDING = new ArrayDeque<>();
	private static final List<Countdown> ACTIVE = new ArrayList<>();

	private ClientBreakCountdown() {
	}

	private static final class PendingSet {
		final BlockSet blocks;
		int ageTicks = 0;

		PendingSet(BlockSet blocks) {
			this.blocks = blocks;
		}
	}

	private static final class Countdown {
		int remainingTicks;
		final int totalTicks;
		final int blockCount;
		@Nullable
		final BlockSet blocks;

		Countdown(int remainingTicks, int totalTicks, int blockCount, @Nullable BlockSet blocks) {
			this.remainingTicks = remainingTicks;
			this.totalTicks = totalTicks;
			this.blockCount = blockCount;
			this.blocks = blocks;
		}
	}

	/** Registers a just-sent survival break set, to be matched with the server's countdown packet. */
	public static void addPending(BlockSet copy) {
		PENDING.addLast(new PendingSet(copy));
	}

	/** Handles the server's {@code BreakCountdownPacket}: starts a new countdown for the oldest pending set. */
	public static void onPacket(int delayTicks, int blockCount) {
		BlockSet blocks = null;
		PendingSet pending = PENDING.pollFirst();
		if (pending != null) {
			blocks = pending.blocks;
		}
		ACTIVE.add(new Countdown(delayTicks, delayTicks, blockCount, blocks));
	}

	/**
	 * Ticks every active countdown and ages pending sets; call once per client tick. Countdowns
	 * that reach 0 fire {@code onBlocksBroken} for their set (if any) so the dissolve animation
	 * starts when the blocks actually vanish, then are removed. Pending sets older than
	 * {@link #PENDING_TIMEOUT_TICKS} ticks without a matching packet are dropped (the server
	 * refused the break).
	 */
	public static void tick() {
		for (var iterator = ACTIVE.iterator(); iterator.hasNext(); ) {
			Countdown countdown = iterator.next();
			countdown.remainingTicks--;
			if (countdown.remainingTicks <= 0) {
				if (countdown.blocks != null) {
					SophisticatedBuildingClient.BLOCK_PREVIEWS.onBlocksBroken(countdown.blocks);
				}
				iterator.remove();
			}
		}

		for (var iterator = PENDING.iterator(); iterator.hasNext(); ) {
			PendingSet pending = iterator.next();
			pending.ageTicks++;
			if (pending.ageTicks > PENDING_TIMEOUT_TICKS) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		PENDING.clear();
		ACTIVE.clear();
	}

	public static boolean hasActive() {
		return !ACTIVE.isEmpty();
	}

	/** Remaining ticks of the countdown that finishes last, or 0 when none are active. */
	public static int remainingTicks() {
		int max = 0;
		for (Countdown countdown : ACTIVE) {
			if (countdown.remainingTicks > max) {
				max = countdown.remainingTicks;
			}
		}
		return max;
	}

	/** Total ticks of the countdown that finishes last, or 0 when none are active. */
	public static int totalTicks() {
		int result = 0;
		int max = -1;
		for (Countdown countdown : ACTIVE) {
			if (countdown.remainingTicks > max) {
				max = countdown.remainingTicks;
				result = countdown.totalTicks;
			}
		}
		return result;
	}

	public static int totalBlockCount() {
		int total = 0;
		for (Countdown countdown : ACTIVE) {
			total += countdown.blockCount;
		}
		return total;
	}

	/** Coordinates of every block still pending a break (awaiting the server's countdown packet,
	 * or already counting down), for the "pending-break" preview cluster. */
	public static HashSet<BlockPos> pendingCoordinates() {
		HashSet<BlockPos> coordinates = new HashSet<>();
		for (PendingSet pending : PENDING) {
			for (BlockEntry entry : pending.blocks) {
				coordinates.add(entry.blockPos);
			}
		}
		for (Countdown countdown : ACTIVE) {
			if (countdown.blocks == null) continue;
			for (BlockEntry entry : countdown.blocks) {
				coordinates.add(entry.blockPos);
			}
		}
		return coordinates;
	}
}
