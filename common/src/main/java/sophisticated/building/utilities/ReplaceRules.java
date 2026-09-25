package sophisticated.building.utilities;

/**
 * Pure decisions for applying build-mode sets: survival replace (placing onto a block that has to be mined first),
 * the item cost of multi-item states and the first block that vanilla handles itself.
 * No Minecraft imports so this class is directly unit-testable (see {@code ReplaceRulesTest}).
 */
public final class ReplaceRules {

	private ReplaceRules() {
	}

	public enum Action {
		PLACE,   // place over air or a replaceable block (grass, water, snow layer...), no mining
		BREAK,   // mine the current block, place nothing
		REPLACE, // survival: pay the item, mine the current block, then place
		UNMERGE, // undo of a merge: set the old state again without mining, survival gets the merged item back
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
	 * @param oldIsAir        the old state is air, so the current block is broken
	 * @param oldHasItem      the old state has an item that pays for re-placing it
	 * @param currentIsMerged the current state is the old one plus one item (slab to double slab, +1 candle, pickle,
	 *                        egg, snow layer or petal): the undo of a merge, never mined whatever the replace setting
	 */
	public static Action forUndo(boolean survival, boolean oldIsAir, boolean oldHasItem, boolean currentNeedsMining,
								 boolean sameState, boolean replaceEnabled, boolean currentIsMerged) {
		if (oldIsAir) return Action.BREAK;
		if (currentIsMerged) return Action.UNMERGE;
		if (!survival || !currentNeedsMining) return Action.PLACE;
		//Survival never overwrites for free: without an item to pay with, only mine the current block
		if (!oldHasItem) return Action.BREAK;
		if (sameState || !replaceEnabled) return Action.SKIP;
		return Action.REPLACE;
	}

	/**
	 * Items a block state is made of: what breaking it drops, and what placing it costs.
	 *
	 * @param doubleSlab the state is a double slab
	 * @param countValue the value of its count property (candles, pickles, eggs, snow layers, petals), 0 without one
	 */
	public static int itemCount(boolean doubleSlab, int countValue) {
		if (doubleSlab) return 2;
		return Math.max(1, countValue);
	}

	/**
	 * Items to charge for placing a state (a build, undo or redo): all the items it is made of, less those of the same block
	 * that stays in place when it is placed over without mining (so a merge costs one, like vanilla).
	 *
	 * @param targetCount item count of the placed state
	 * @param keptCount   item count of the current block if it is the same block and not mined, else 0
	 */
	public static int restoreCost(int targetCount, int keptCount) {
		return Math.max(0, targetCount - keptCount);
	}

	/**
	 * Items given back when a merge is undone ({@link Action#UNMERGE}): what the current state holds more than the old
	 * one, i.e. exactly the item the merge charged.
	 *
	 * @param currentCount item count of the merged state
	 * @param oldCount     item count of the state before the merge
	 */
	public static int unmergeRefund(int currentCount, int oldCount) {
		return Math.max(0, currentCount - oldCount);
	}

	/**
	 * Client: vanilla places or mines the first block itself only in Disable mode without Quick Replace. Otherwise the
	 * server cancels vanilla and the mod has to handle the first block too.
	 */
	public static boolean vanillaHandlesFirst(boolean buildModeDisabled, boolean quickReplacing) {
		return buildModeDisabled && !quickReplacing;
	}

	/**
	 * Server: whether the first block of a received set is left to vanilla. The client's flag is only honoured while
	 * vanilla really handled the click ({@code ServerBuildState.isLikeVanilla}); if vanilla was cancelled, skipping
	 * would lose the block.
	 */
	public static boolean shouldSkipFirst(boolean skipFirstFlag, boolean isLikeVanilla) {
		return skipFirstFlag && isLikeVanilla;
	}

	/** Game time to apply a survival set with replacements: the client's time, or later while mining takes longer. */
	public static long placeTime(long clientPlaceTime, long now, int miningTicks, int maxDelayTicks) {
		return Math.max(clientPlaceTime, now + ToolSelector.capDelay(miningTicks, maxDelayTicks));
	}
}
