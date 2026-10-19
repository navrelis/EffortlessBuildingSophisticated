package sophisticated.building.utilities;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Pure cooldown logic for balanced survival breaking: every accepted mass-break is a "use", and
 * the cooldown escalates with the number of uses inside a rolling real-time window. No Minecraft
 * imports and no config access so this class is directly unit-testable (see
 * {@code BalanceCooldownTest}). All times are epoch milliseconds. Keep Java 8 compatible.
 */
public final class BalanceCooldown {

	/** Hard cap on stored use timestamps (oldest dropped first) so the saved data stays small. */
	public static final int MAX_STORED_USES = 256;

	/** Tunable values, supplied by the caller (normally read from the server config). */
	public static final class Settings {
		public final long baseMillis;
		public final long windowMillis;
		public final long stepMillis;
		public final long maxMillis;
		public final int freeUses;

		public Settings(long baseMillis, long windowMillis, int freeUses, long stepMillis, long maxMillis) {
			this.baseMillis = baseMillis;
			this.windowMillis = windowMillis;
			this.freeUses = freeUses;
			this.stepMillis = stepMillis;
			this.maxMillis = maxMillis;
		}
	}

	/** Use timestamps in ascending order (oldest first). */
	private final ArrayDeque<Long> uses = new ArrayDeque<Long>();
	private long cooldownEndMillis;

	public BalanceCooldown() {
	}

	/** Cooldown for n uses in the window. n &lt;= 0 behaves like n = 1. */
	public static long cooldownFor(int usesInWindow, Settings settings) {
		int n = Math.max(1, usesInWindow);
		long result = settings.baseMillis;
		if (n > settings.freeUses) {
			result = settings.baseMillis + (long) (n - settings.freeUses) * settings.stepMillis;
		}
		if (result > settings.maxMillis) {
			result = settings.maxMillis;
		}
		if (result < settings.baseMillis) {
			result = settings.baseMillis;
		}
		return result;
	}

	/** Removes uses with timestamp &lt;= now - windowMillis, and uses with timestamp &gt; now (clock moved back). */
	public void prune(long nowMillis, long windowMillis) {
		long oldest = nowMillis - windowMillis;
		Iterator<Long> it = uses.iterator();
		while (it.hasNext()) {
			long t = it.next();
			if (t <= oldest || t > nowMillis) {
				it.remove();
			}
		}
	}

	/** Number of uses in the window after pruning. */
	public int usesInWindow(long nowMillis, long windowMillis) {
		prune(nowMillis, windowMillis);
		return uses.size();
	}

	/**
	 * Records a use at now, prunes, sets cooldownEnd = now + cooldownFor(uses in window including
	 * this one); returns the cooldown in ms.
	 */
	public long recordUse(long nowMillis, Settings settings) {
		prune(nowMillis, settings.windowMillis);
		uses.addLast(nowMillis);
		while (uses.size() > MAX_STORED_USES) {
			uses.removeFirst();
		}
		long cooldown = cooldownFor(uses.size(), settings);
		cooldownEndMillis = nowMillis + cooldown;
		return cooldown;
	}

	/**
	 * Remaining cooldown, max(0, cooldownEnd - now), clamped to settings.maxMillis (protects
	 * against a clock moved back).
	 */
	public long remainingMillis(long nowMillis, Settings settings) {
		long remaining = cooldownEndMillis - nowMillis;
		if (remaining <= 0) {
			return 0;
		}
		return Math.min(remaining, settings.maxMillis);
	}

	public boolean isCoolingDown(long nowMillis, Settings settings) {
		return remainingMillis(nowMillis, settings) > 0;
	}

	public long getCooldownEndMillis() {
		return cooldownEndMillis;
	}

	/** Persistence: [cooldownEnd, use1, use2, ...] oldest use first. */
	public long[] toArray() {
		long[] out = new long[uses.size() + 1];
		out[0] = cooldownEndMillis;
		int i = 1;
		for (long t : uses) {
			out[i++] = t;
		}
		return out;
	}

	/**
	 * Inverse of toArray; null or empty gives a fresh instance; ignores timestamps &lt;= 0; keeps at
	 * most MAX_STORED_USES newest; sorts uses ascending.
	 */
	public static BalanceCooldown fromArray(long[] data) {
		BalanceCooldown result = new BalanceCooldown();
		if (data == null || data.length == 0) {
			return result;
		}
		result.cooldownEndMillis = Math.max(0L, data[0]);
		int count = 0;
		long[] tmp = new long[Math.max(0, data.length - 1)];
		for (int i = 1; i < data.length; i++) {
			if (data[i] > 0) {
				tmp[count++] = data[i];
			}
		}
		long[] valid = Arrays.copyOf(tmp, count);
		Arrays.sort(valid);
		int start = Math.max(0, valid.length - MAX_STORED_USES);
		for (int i = start; i < valid.length; i++) {
			result.uses.addLast(valid[i]);
		}
		return result;
	}

	public void clear() {
		uses.clear();
		cooldownEndMillis = 0L;
	}
}
