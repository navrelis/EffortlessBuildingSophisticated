package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.BalanceCooldown;
import sophisticated.building.utilities.BalanceRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceRulesTest {

	@Test
	void dedicatedServerOnlyFollowsItsServerConfig() {
		assertFalse(BalanceRules.isEnabled(false, true, false));
		assertFalse(BalanceRules.isEnabled(false, true, true), "a host toggle never reaches a dedicated server");
		assertTrue(BalanceRules.isEnabled(true, true, false));
		assertTrue(BalanceRules.isEnabled(true, true, true));
	}

	@Test
	void integratedServerFollowsServerConfigOrHostToggle() {
		assertFalse(BalanceRules.isEnabled(false, false, false));
		assertTrue(BalanceRules.isEnabled(false, false, true));
		assertTrue(BalanceRules.isEnabled(true, false, false));
		assertTrue(BalanceRules.isEnabled(true, false, true));
	}

	@Test
	void oneBlockOperationsAreExempt() {
		assertFalse(BalanceRules.applies(true, false, 0));
		assertFalse(BalanceRules.applies(true, false, 1));
		assertTrue(BalanceRules.applies(true, false, 2));
		assertTrue(BalanceRules.applies(true, false, 500));
	}

	@Test
	void creativeAndOffAreExempt() {
		assertFalse(BalanceRules.applies(true, true, 10));
		assertFalse(BalanceRules.applies(false, false, 10));
	}

	@Test
	void usesPerBlockIsTheMultiplierOnlyWhenBalanced() {
		assertEquals(1, BalanceRules.usesPerBlock(false, 5));
		assertEquals(5, BalanceRules.usesPerBlock(true, 5));
		assertEquals(20, BalanceRules.usesPerBlock(true, 20));
		assertEquals(1, BalanceRules.usesPerBlock(true, 0), "never below one use");
	}

	@Test
	void hungerRefusesBelowTheMinimumFoodLevel() {
		assertTrue(BalanceRules.tooHungry(6, 7));
		assertFalse(BalanceRules.tooHungry(7, 7));
		assertFalse(BalanceRules.tooHungry(0, 0), "0 never refuses");
	}

	@Test
	void cooldownIsCheckedBeforeHunger() {
		assertEquals(BalanceRules.Refusal.COOLDOWN, BalanceRules.check(1, 0, 7));
		assertEquals(BalanceRules.Refusal.HUNGER, BalanceRules.check(0, 6, 7));
		assertEquals(BalanceRules.Refusal.NONE, BalanceRules.check(0, 20, 7));
	}

	@Test
	void displaySecondsRoundsUp() {
		assertEquals(0, BalanceRules.displaySeconds(0));
		assertEquals(0, BalanceRules.displaySeconds(-50));
		assertEquals(1, BalanceRules.displaySeconds(1));
		assertEquals(1, BalanceRules.displaySeconds(1000));
		assertEquals(42, BalanceRules.displaySeconds(41_200));
	}

	@Test
	void settingsConvertConfigUnits() {
		BalanceCooldown.Settings settings = BalanceRules.settings(15, 30, 10, 5, 120);
		assertEquals(15_000L, settings.baseMillis);
		assertEquals(30 * 60_000L, settings.windowMillis);
		assertEquals(10, settings.freeUses);
		assertEquals(5_000L, settings.stepMillis);
		assertEquals(120_000L, settings.maxMillis);
	}

	@Test
	void maximumIsNeverBelowTheBase() {
		BalanceCooldown.Settings settings = BalanceRules.settings(60, 30, 10, 5, 10);
		assertEquals(60_000L, settings.maxMillis);
		BalanceCooldown cooldown = new BalanceCooldown();
		cooldown.recordUse(1_000_000L, settings);
		assertEquals(60_000L, cooldown.remainingMillis(1_000_000L, settings));
	}
}
