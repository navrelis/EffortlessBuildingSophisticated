package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.BuildLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildLimitsTest {

	@Test
	void startReachIsAtLeastTheVanillaInteractionRange() {
		assertEquals(6, BuildLimits.startReach(0, 4.5));   // survival power level 0: reach 0, looks at nearby blocks
		assertEquals(32, BuildLimits.startReach(32, 4.5));
		assertEquals(200, BuildLimits.startReach(200, 5));  // creative
	}

	@Test
	void startWithinReachAllowsTheTolerance() {
		double reach = BuildLimits.startReach(8, 4.5);
		assertTrue(BuildLimits.startWithinReach(8 * 8, reach));
		assertTrue(BuildLimits.startWithinReach(11 * 11, reach));
		assertFalse(BuildLimits.startWithinReach(12 * 12, reach));
		assertFalse(BuildLimits.startWithinReach(60 * 60, BuildLimits.startReach(0, 4.5)));
	}

	@Test
	void extentCountsBothEnds() {
		assertTrue(BuildLimits.extentWithinAxisLimit(7, 0, 0, 8));   // 8 blocks
		assertFalse(BuildLimits.extentWithinAxisLimit(8, 0, 0, 8));  // 9 blocks
		assertFalse(BuildLimits.extentWithinAxisLimit(0, -19, 0, 8));
		assertTrue(BuildLimits.extentWithinAxisLimit(-7, 7, -7, 8));
	}

	@Test
	void maxBlockDistanceAddsShapeModifiersAndTolerance() {
		// build mode reach 6, start reach 6, 8 per axis, no modifiers: 6 + 8 + 0 + 3
		assertEquals(17, BuildLimits.maxBlockDistance(6, 6, 8, 0));
		// a mirror of radius 16 adds 32
		assertEquals(49, BuildLimits.maxBlockDistance(6, 6, 8, 32));
	}

	@Test
	void countIsCutToTheLimit() {
		assertEquals(128, BuildLimits.allowedCount(200, 128));
		assertEquals(5, BuildLimits.allowedCount(5, 128));
		assertEquals(0, BuildLimits.allowedCount(5, 0));
	}

	@Test
	void arrayCountKeepsTheExtentWithinTheAxisLimit() {
		assertEquals(8, BuildLimits.arrayCount(20, 1, 8));
		assertEquals(4, BuildLimits.arrayCount(20, 2, 8));
		assertEquals(3, BuildLimits.arrayCount(3, 2, 8));
		assertEquals(0, BuildLimits.arrayCount(5, 9, 8));
		assertEquals(5, BuildLimits.arrayCount(5, 0, 8));   // no offset: the array does nothing anyway
	}
}
