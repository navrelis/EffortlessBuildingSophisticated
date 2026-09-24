package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.ReplaceRules;
import sophisticated.building.utilities.ReplaceRules.Action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceRulesTest {

	@Test
	void onlyRealBlocksNeedMining() {
		assertFalse(ReplaceRules.needsMining(true, true));   // air
		assertFalse(ReplaceRules.needsMining(false, true));  // grass, water, snow layer
		assertTrue(ReplaceRules.needsMining(false, false));  // stone
	}

	@Test
	void creativeAlwaysOverwrites() {
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(false, true, false, false, false));
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(false, true, true, false, true));
	}

	@Test
	void survivalPlacingOnReplaceableTargetDoesNotMine() {
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(true, false, false, false, false));
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(true, false, false, false, true));
	}

	@Test
	void survivalRealReplaceIsRejectedWhenDisabled() {
		assertEquals(Action.SKIP, ReplaceRules.forPlacement(true, true, false, false, false));
	}

	@Test
	void survivalRealReplaceMinesWhenEnabled() {
		assertEquals(Action.REPLACE, ReplaceRules.forPlacement(true, true, false, false, true));
	}

	@Test
	void survivalMergeIsPlacedWithoutMiningWhateverTheConfig() {
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(true, true, true, true, false));
		assertEquals(Action.PLACE, ReplaceRules.forPlacement(true, true, true, true, true));
	}

	@Test
	void survivalSameBlockThatIsNoMergeIsSkippedWhateverTheConfig() {
		assertEquals(Action.SKIP, ReplaceRules.forPlacement(true, true, true, false, false));
		assertEquals(Action.SKIP, ReplaceRules.forPlacement(true, true, true, false, true));
	}

	@Test
	void undoToAirAlwaysBreaks() {
		assertEquals(Action.BREAK, ReplaceRules.forUndo(false, true, false, true, false, false));
		assertEquals(Action.BREAK, ReplaceRules.forUndo(true, true, false, true, false, true));
	}

	@Test
	void creativeUndoOverwrites() {
		assertEquals(Action.PLACE, ReplaceRules.forUndo(false, false, true, true, false, false));
		assertEquals(Action.PLACE, ReplaceRules.forUndo(false, false, false, true, false, false));
	}

	@Test
	void survivalUndoOverAirOrReplaceableKeepsPlacing() {
		assertEquals(Action.PLACE, ReplaceRules.forUndo(true, false, true, false, false, false));
		assertEquals(Action.PLACE, ReplaceRules.forUndo(true, false, false, false, false, false));
	}

	@Test
	void survivalUndoOfAReplacementMinesAndReplacesWhenEnabled() {
		assertEquals(Action.REPLACE, ReplaceRules.forUndo(true, false, true, true, false, true));
	}

	@Test
	void survivalUndoOfAReplacementIsSkippedWhenDisabledOrUnchanged() {
		assertEquals(Action.SKIP, ReplaceRules.forUndo(true, false, true, true, false, false));
		assertEquals(Action.SKIP, ReplaceRules.forUndo(true, false, true, true, true, true));
	}

	@Test
	void survivalUndoWithoutAnItemOnlyMinesTheCurrentBlock() {
		assertEquals(Action.BREAK, ReplaceRules.forUndo(true, false, false, true, false, false));
		assertEquals(Action.BREAK, ReplaceRules.forUndo(true, false, false, true, false, true));
	}

	@Test
	void placeTimeKeepsTheClientTimeWhenMiningIsFaster() {
		assertEquals(105L, ReplaceRules.placeTime(105L, 100L, 3, 40));
	}

	@Test
	void placeTimeWaitsForTheMining() {
		assertEquals(120L, ReplaceRules.placeTime(105L, 100L, 20, 40));
	}

	@Test
	void placeTimeCapsTheMiningDelay() {
		assertEquals(140L, ReplaceRules.placeTime(105L, 100L, 500, 40));
		assertEquals(105L, ReplaceRules.placeTime(105L, 100L, 500, 0));
	}
}
