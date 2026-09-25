package sophisticated.building.client;

import net.minecraft.core.BlockPos;
import sophisticated.building.ClientConfig;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.utilities.BlockSet;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tracks pending survival break sets between the click (when the client sends
 * {@code ServerBreakBlocksPacket}) and the server's {@code BreakCountdownPacket} response, then
 * counts down until the blocks actually break, so the HUD can show an on-screen countdown (T-S10).
 * Survival placements that replace blocks are delayed by their mining the same way.
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
		final boolean placing;
		//Outlined while waiting: every block of a break, only the mined blocks of a placement
		final Set<BlockPos> outline;
		int ageTicks = 0;

		PendingSet(BlockSet blocks, boolean placing, Set<BlockPos> outline) {
			this.blocks = blocks;
			this.placing = placing;
			this.outline = outline;
		}
	}

	private static final class Countdown {
		int remainingTicks;
		final int totalTicks;
		final int blockCount;
		final boolean placing;
		@Nullable
		final PendingSet pending;
		//Ticks before the end at which the animation starts (placing: the appear animation ends when the blocks are placed)
		final int animationLead;
		boolean animated = false;

		Countdown(int remainingTicks, int totalTicks, int blockCount, boolean placing, @Nullable PendingSet pending, int animationLead) {
			this.remainingTicks = remainingTicks;
			this.totalTicks = totalTicks;
			this.blockCount = blockCount;
			this.placing = placing;
			this.pending = pending;
			this.animationLead = animationLead;
		}
	}

	/** Registers a just-sent survival break set, to be matched with the server's countdown packet. */
	public static void addPending(BlockSet copy) {
		PENDING.addLast(new PendingSet(copy, false, copy.getCoordinates()));
	}

	/** Registers a just-sent survival placement that mines {@code minedPositions}; its appear animation waits for the mining. */
	public static void addPendingPlacement(BlockSet copy, Set<BlockPos> minedPositions) {
		PENDING.addLast(new PendingSet(copy, true, minedPositions));
	}

	/** Handles the server's {@code BreakCountdownPacket}: starts a new countdown for the oldest pending set of that kind. */
	public static void onPacket(int delayTicks, int blockCount, boolean placing) {
		PendingSet pending = null;
		for (Iterator<PendingSet> iterator = PENDING.iterator(); iterator.hasNext(); ) {
			PendingSet candidate = iterator.next();
			if (candidate.placing == placing) {
				iterator.remove();
				pending = candidate;
				break;
			}
		}
		int animationLead = placing ? ClientConfig.visuals.appearAnimationLength.get() : 0;
		ACTIVE.add(new Countdown(delayTicks, delayTicks, blockCount, placing, pending, animationLead));
	}

	/**
	 * Ticks every active countdown and ages pending sets; call once per client tick. Break
	 * countdowns that reach 0 fire {@code onBlocksBroken} for their set (if any) so the dissolve
	 * animation starts when the blocks actually vanish, then are removed. Placement countdowns fire
	 * {@code onBlocksPlaced} early enough for the appear animation to end when the blocks are placed.
	 * Pending sets older than {@link #PENDING_TIMEOUT_TICKS} ticks without a matching packet are
	 * dropped (the server refused the break).
	 */
	public static void tick() {
		for (Iterator<Countdown> iterator = ACTIVE.iterator(); iterator.hasNext(); ) {
			Countdown countdown = iterator.next();
			countdown.remainingTicks--;
			if (!countdown.animated && countdown.remainingTicks <= countdown.animationLead) {
				countdown.animated = true;
				if (countdown.pending != null) {
					if (countdown.placing) {
						SophisticatedBuildingClient.BLOCK_PREVIEWS.onBlocksPlaced(countdown.pending.blocks);
					} else {
						SophisticatedBuildingClient.BLOCK_PREVIEWS.onBlocksBroken(countdown.pending.blocks);
					}
				}
			}
			if (countdown.remainingTicks <= 0) {
				iterator.remove();
			}
		}

		for (Iterator<PendingSet> iterator = PENDING.iterator(); iterator.hasNext(); ) {
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

	/** True when every active countdown belongs to a placement (the HUD then says replacing instead of breaking). */
	public static boolean onlyPlacing() {
		for (Countdown countdown : ACTIVE) {
			if (!countdown.placing) return false;
		}
		return true;
	}

	public static int totalBlockCount() {
		int total = 0;
		for (Countdown countdown : ACTIVE) {
			total += countdown.blockCount;
		}
		return total;
	}

	/** Coordinates of every block still pending a break or a replacing placement's mining (awaiting
	 * the server's countdown packet, or already counting down), for the "pending-break" preview cluster. */
	public static HashSet<BlockPos> pendingCoordinates() {
		HashSet<BlockPos> coordinates = new HashSet<>();
		for (PendingSet pending : PENDING) {
			coordinates.addAll(pending.outline);
		}
		for (Countdown countdown : ACTIVE) {
			if (countdown.pending == null || countdown.animated) continue;
			coordinates.addAll(countdown.pending.outline);
		}
		return coordinates;
	}
}
