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
}
