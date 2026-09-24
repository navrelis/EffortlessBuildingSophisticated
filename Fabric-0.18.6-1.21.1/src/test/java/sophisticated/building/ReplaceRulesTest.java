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

	@Test
	void singleItemStatesCountOne() {
		assertEquals(1, ReplaceRules.itemCount(false, 0)); // stone, a single slab
		assertEquals(1, ReplaceRules.itemCount(false, 1)); // one candle
	}

	@Test
	void doubleSlabCountsTwo() {
		assertEquals(2, ReplaceRules.itemCount(true, 0));
	}

	@Test
	void countPropertyGivesTheItemCount() {
		assertEquals(4, ReplaceRules.itemCount(false, 4)); // four candles, pickles, eggs or petals
		assertEquals(8, ReplaceRules.itemCount(false, 8)); // eight snow layers
	}

	@Test
	void restoringOverAirOrAnotherBlockCostsTheWholeState() {
		assertEquals(2, ReplaceRules.restoreCost(2, 0)); // double slab after a survival break
		assertEquals(3, ReplaceRules.restoreCost(3, 0)); // three candles
	}

	@Test
	void restoringOverTheSameBlockCostsOnlyTheDifference() {
		assertEquals(1, ReplaceRules.restoreCost(2, 1)); // redo of a slab merge
		assertEquals(2, ReplaceRules.restoreCost(5, 3)); // snow layers placed over
	}

	@Test
	void restoringFewerItemsIsFree() {
		assertEquals(0, ReplaceRules.restoreCost(3, 4));
		assertEquals(0, ReplaceRules.restoreCost(8, 8));
	}

	@Test
	void vanillaHandlesTheFirstBlockOnlyInDisableModeWithoutQuickReplace() {
		assertTrue(ReplaceRules.vanillaHandlesFirst(true, false));
		assertFalse(ReplaceRules.vanillaHandlesFirst(true, true));   // vanilla is cancelled, the mod replaces it
		assertFalse(ReplaceRules.vanillaHandlesFirst(false, false)); // build mode
		assertFalse(ReplaceRules.vanillaHandlesFirst(false, true));
	}

	@Test
	void serverHonoursSkipFirstOnlyWhileLikeVanilla() {
		assertTrue(ReplaceRules.shouldSkipFirst(true, true));
		assertFalse(ReplaceRules.shouldSkipFirst(true, false)); // vanilla was cancelled, skipping would lose the block
		assertFalse(ReplaceRules.shouldSkipFirst(false, true));
		assertFalse(ReplaceRules.shouldSkipFirst(false, false));
	}
}
