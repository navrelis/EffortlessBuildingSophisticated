package sophisticated.building.utilities;

/**
 * Pure decisions for survival replace: placing onto a block that has to be mined first.
 * No Minecraft imports so this class is directly unit-testable (see {@code ReplaceRulesTest}).
 */
public final class ReplaceRules {

	private ReplaceRules() {
	}

	public enum Action {
		PLACE,   // place over air or a replaceable block (grass, water, snow layer...), no mining
		BREAK,   // mine the current block, place nothing
		REPLACE, // survival: pay the item, mine the current block, then place
		SKIP     // leave the entry alone, nothing mined or consumed
	}

	/** A block that is neither air nor replaceable has to be mined before something can be placed there. */
	public static boolean needsMining(boolean isAir, boolean canBeReplaced) {
		return !isAir && !canBeReplaced;
	}

	/**
	 * How to apply one entry that places a block.
	 *
	 * @param survival       false for creative, which always overwrites instantly
	 * @param sameBlock      the target is the block to place, in any state
	 * @param isMerge        the new state adds one to the target (slab to double slab, one more candle...), like vanilla
	 * @param replaceEnabled the survival replace server config switch
	 */
	public static Action forPlacement(boolean survival, boolean targetNeedsMining, boolean sameBlock, boolean isMerge,
									  boolean replaceEnabled) {
		if (!survival || !targetNeedsMining) return Action.PLACE;
		//Survival never mines a block to place the same block: a merge is placed over it for one item like vanilla,
		//anything else (another orientation, the same state) is skipped
		if (sameBlock) return isMerge ? Action.PLACE : Action.SKIP;
		if (!replaceEnabled) return Action.SKIP;
		return Action.REPLACE;
	}

	/**
	 * How to undo one entry, i.e. restore the old state over the current block.
	 *
	 * @param oldIsAir   the old state is air, so the current block is broken
	 * @param oldHasItem the old state has an item that pays for re-placing it
	 */
	public static Action forUndo(boolean survival, boolean oldIsAir, boolean oldHasItem, boolean currentNeedsMining,
								 boolean sameState, boolean replaceEnabled) {
		if (oldIsAir) return Action.BREAK;
		if (!survival || !currentNeedsMining) return Action.PLACE;
		//Survival never overwrites for free: without an item to pay with, only mine the current block
		if (!oldHasItem) return Action.BREAK;
		if (sameState || !replaceEnabled) return Action.SKIP;
		return Action.REPLACE;
	}

	/** Game time to apply a survival set with replacements: the client's time, or later while mining takes longer. */
	public static long placeTime(long clientPlaceTime, long now, int miningTicks, int maxDelayTicks) {
		return Math.max(clientPlaceTime, now + ToolSelector.capDelay(miningTicks, maxDelayTicks));
	}
}
