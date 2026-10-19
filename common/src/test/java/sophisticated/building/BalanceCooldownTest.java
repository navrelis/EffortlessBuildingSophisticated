package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.BalanceCooldown;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceCooldownTest {

	private static final long SECOND = 1000L;
	private static final long MINUTE = 60 * SECOND;

	// base 15 s, window 30 min, 10 free uses, step 5 s, max 120 s
	private static final BalanceCooldown.Settings DEFAULTS =
			new BalanceCooldown.Settings(15 * SECOND, 30 * MINUTE, 10, 5 * SECOND, 120 * SECOND);

	@Test
	void cooldownForFreeUsesIsBase() {
		for (int n = 1; n <= 10; n++) {
			assertEquals(15_000L, BalanceCooldown.cooldownFor(n, DEFAULTS), "n=" + n);
		}
	}

	@Test
	void cooldownEscalatesAfterFreeUses() {
		assertEquals(20_000L, BalanceCooldown.cooldownFor(11, DEFAULTS));
		assertEquals(25_000L, BalanceCooldown.cooldownFor(12, DEFAULTS));
		assertEquals(120_000L, BalanceCooldown.cooldownFor(31, DEFAULTS));
		assertEquals(120_000L, BalanceCooldown.cooldownFor(100, DEFAULTS));
	}

	@Test
	void cooldownForNonPositiveBehavesLikeOne() {
		assertEquals(15_000L, BalanceCooldown.cooldownFor(0, DEFAULTS));
		assertEquals(15_000L, BalanceCooldown.cooldownFor(-5, DEFAULTS));
	}

	@Test
	void maxBelowBaseReturnsBase() {
		BalanceCooldown.Settings s = new BalanceCooldown.Settings(15 * SECOND, 30 * MINUTE, 10, 5 * SECOND, 5 * SECOND);
		assertEquals(15_000L, BalanceCooldown.cooldownFor(1, s));
		assertEquals(15_000L, BalanceCooldown.cooldownFor(50, s));
	}

	@Test
	void recordUseEscalatesAndSetsCooldownEnd() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 1_000_000L;
		for (int i = 1; i <= 10; i++) {
			now += 1000;
			assertEquals(15_000L, c.recordUse(now, DEFAULTS), "use " + i);
			assertEquals(now + 15_000L, c.getCooldownEndMillis());
		}
		now += 1000;
		assertEquals(20_000L, c.recordUse(now, DEFAULTS));
		assertEquals(now + 20_000L, c.getCooldownEndMillis());
		now += 1000;
		assertEquals(25_000L, c.recordUse(now, DEFAULTS));
	}

	@Test
	void usesOlderThanWindowExpire() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 1_000_000L;
		for (int i = 0; i < 20; i++) {
			c.recordUse(now + i, DEFAULTS);
		}
		assertEquals(20, c.usesInWindow(now + 100, DEFAULTS.windowMillis));
		long later = now + 31 * MINUTE;
		assertEquals(0, c.usesInWindow(later, DEFAULTS.windowMillis));
		assertEquals(15_000L, c.recordUse(later, DEFAULTS));
	}

	@Test
	void useExactlyAtWindowEdgeExpires() {
		BalanceCooldown c = new BalanceCooldown();
		long t = 5_000_000L;
		c.recordUse(t, DEFAULTS);
		assertEquals(1, c.usesInWindow(t + DEFAULTS.windowMillis - 1, DEFAULTS.windowMillis));
		assertEquals(0, c.usesInWindow(t + DEFAULTS.windowMillis, DEFAULTS.windowMillis));
	}

	@Test
	void partialExpiryOnlyDropsOldUses() {
		BalanceCooldown c = new BalanceCooldown();
		long start = 10_000_000L;
		// 8 old uses, then 8 recent uses 20 minutes later
		for (int i = 0; i < 8; i++) {
			c.recordUse(start + i * SECOND, DEFAULTS);
		}
		long recent = start + 20 * MINUTE;
		for (int i = 0; i < 8; i++) {
			c.recordUse(recent + i * SECOND, DEFAULTS);
		}
		assertEquals(16, c.usesInWindow(recent + 10 * SECOND, DEFAULTS.windowMillis));
		// 35 minutes after start the old ones are gone, recent ones remain
		long check = start + 35 * MINUTE;
		assertEquals(8, c.usesInWindow(check, DEFAULTS.windowMillis));
		// 9th use in window -> still free
		assertEquals(15_000L, c.recordUse(check, DEFAULTS));
	}

	@Test
	void remainingCountsDownAndReachesZero() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 2_000_000L;
		c.recordUse(now, DEFAULTS);
		assertEquals(15_000L, c.remainingMillis(now, DEFAULTS));
		assertEquals(10_000L, c.remainingMillis(now + 5_000L, DEFAULTS));
		assertEquals(1L, c.remainingMillis(now + 14_999L, DEFAULTS));
		assertEquals(0L, c.remainingMillis(now + 15_000L, DEFAULTS));
		assertEquals(0L, c.remainingMillis(now + 99_000L, DEFAULTS));
	}

	@Test
	void isCoolingDownBoundary() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 2_000_000L;
		assertFalse(c.isCoolingDown(now, DEFAULTS));
		c.recordUse(now, DEFAULTS);
		assertTrue(c.isCoolingDown(now, DEFAULTS));
		assertTrue(c.isCoolingDown(now + 14_999L, DEFAULTS));
		assertFalse(c.isCoolingDown(now + 15_000L, DEFAULTS));
		assertFalse(c.isCoolingDown(now + 15_001L, DEFAULTS));
	}

	@Test
	void clockMovedBackClampsRemainingToMax() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 100_000_000L;
		c.recordUse(now, DEFAULTS);
		long wayBack = now - 10 * 24 * 60 * MINUTE;
		assertEquals(DEFAULTS.maxMillis, c.remainingMillis(wayBack, DEFAULTS));
		assertTrue(c.isCoolingDown(wayBack, DEFAULTS));
	}

	@Test
	void clockMovedBackPrunesFutureUses() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 100_000_000L;
		for (int i = 0; i < 15; i++) {
			c.recordUse(now + i, DEFAULTS);
		}
		long wayBack = now - 60 * MINUTE;
		assertEquals(0, c.usesInWindow(wayBack, DEFAULTS.windowMillis));
		assertEquals(15_000L, c.recordUse(wayBack, DEFAULTS));
	}

	@Test
	void arrayRoundTrip() {
		BalanceCooldown c = new BalanceCooldown();
		long now = 3_000_000L;
		for (int i = 0; i < 12; i++) {
			c.recordUse(now + i * SECOND, DEFAULTS);
		}
		long[] data = c.toArray();
		assertEquals(13, data.length);
		assertEquals(c.getCooldownEndMillis(), data[0]);
		assertEquals(now, data[1]);
		assertEquals(now + 11 * SECOND, data[12]);

		BalanceCooldown copy = BalanceCooldown.fromArray(data);
		assertArrayEquals(data, copy.toArray());
		assertEquals(c.getCooldownEndMillis(), copy.getCooldownEndMillis());
		long next = now + 12 * SECOND;
		assertEquals(c.recordUse(next, DEFAULTS), copy.recordUse(next, DEFAULTS));
	}

	@Test
	void fromArrayNullOrEmptyIsFresh() {
		for (long[] input : new long[][] {null, new long[0]}) {
			BalanceCooldown c = BalanceCooldown.fromArray(input);
			assertEquals(0L, c.getCooldownEndMillis());
			assertArrayEquals(new long[] {0L}, c.toArray());
			assertFalse(c.isCoolingDown(1_000L, DEFAULTS));
		}
	}

	@Test
	void fromArrayOnlyCooldownEnd() {
		BalanceCooldown c = BalanceCooldown.fromArray(new long[] {5_000L});
		assertEquals(5_000L, c.getCooldownEndMillis());
		assertEquals(0, c.usesInWindow(4_000L, DEFAULTS.windowMillis));
	}

	@Test
	void fromArraySortsAndIgnoresNonPositive() {
		long[] data = {9_999L, 300L, 0L, 100L, -50L, 200L};
		BalanceCooldown c = BalanceCooldown.fromArray(data);
		assertArrayEquals(new long[] {9_999L, 100L, 200L, 300L}, c.toArray());
	}

	@Test
	void fromArrayKeepsNewestWhenOverCap() {
		long[] data = new long[1 + MAX_PLUS_TEN()];
		data[0] = 1L;
		for (int i = 1; i < data.length; i++) {
			data[i] = 1000L + i; // 1001 .. 1000 + (cap + 10)
		}
		BalanceCooldown c = BalanceCooldown.fromArray(data);
		long[] out = c.toArray();
		assertEquals(BalanceCooldown.MAX_STORED_USES + 1, out.length);
		assertEquals(1000L + 11, out[1]); // the 10 oldest were dropped
		assertEquals(1000L + MAX_PLUS_TEN(), out[out.length - 1]);
	}

	@Test
	void recordUseCapsStoredUses() {
		// window larger than the test span so nothing expires
		BalanceCooldown.Settings s = new BalanceCooldown.Settings(1000L, 1_000_000_000L, 10, 100L, 5000L);
		BalanceCooldown c = new BalanceCooldown();
		long now = 1_000L;
		for (int i = 0; i < BalanceCooldown.MAX_STORED_USES + 50; i++) {
			c.recordUse(now + i, s);
		}
		long[] out = c.toArray();
		assertEquals(BalanceCooldown.MAX_STORED_USES + 1, out.length);
		assertEquals(now + 50, out[1]); // oldest dropped first
		assertEquals(now + BalanceCooldown.MAX_STORED_USES + 49, out[out.length - 1]);
		assertEquals(BalanceCooldown.MAX_STORED_USES, c.usesInWindow(now + 1000, s.windowMillis));
	}

	@Test
	void clearResetsEverything() {
		BalanceCooldown c = new BalanceCooldown();
		c.recordUse(1_000_000L, DEFAULTS);
		c.clear();
		assertEquals(0L, c.getCooldownEndMillis());
		assertArrayEquals(new long[] {0L}, c.toArray());
		assertFalse(c.isCoolingDown(1_000_001L, DEFAULTS));
	}

	private static int MAX_PLUS_TEN() {
		return BalanceCooldown.MAX_STORED_USES + 10;
	}
}
