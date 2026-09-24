package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.InventoryHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackpackContributionTest {

	@Test
	void clampsToMaxBlocksWhenBackpackCountExceedsIt() {
		assertEquals(64, InventoryHelper.clampedBackpackContribution(500, 64));
	}

	@Test
	void returnsBackpackCountWhenBelowMaxBlocks() {
		assertEquals(10, InventoryHelper.clampedBackpackContribution(10, 64));
	}

	@Test
	void returnsZeroWhenMaxBlocksIsZero() {
		assertEquals(0, InventoryHelper.clampedBackpackContribution(500, 0));
	}

	@Test
	void reservesHeldBlockWhenUpgradeActiveAndBackpackHasItem() {
		assertEquals(1, InventoryHelper.reservedHeld(true, 5, true));
	}

	@Test
	void doesNotReserveWhenBackpacksHaveNoneOfTheItem() {
		// Enabled upgrade, but no backpack contains the held item (e.g. cobblestone upgrade,
		// player holding their last dirt): the anchor must not block an item no backpack supplies.
		assertEquals(0, InventoryHelper.reservedHeld(true, 0, true));
	}

	@Test
	void doesNotReserveWhenNoUpgradeIsActive() {
		assertEquals(0, InventoryHelper.reservedHeld(false, 5, true));
	}

	@Test
	void doesNotReserveWhenSelectedSlotDoesNotHoldTheItem() {
		assertEquals(0, InventoryHelper.reservedHeld(true, 5, false));
	}
}
