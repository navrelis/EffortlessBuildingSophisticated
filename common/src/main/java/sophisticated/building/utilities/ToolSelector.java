package sophisticated.building.utilities;

import java.util.List;

/**
 * Pure tool-selection logic for survival block breaking. No Minecraft imports so this class is
 * directly unit-testable (see {@code ToolSelectorTest}). Must be byte-identical in the Fabric and
 * NeoForge projects.
 */
public final class ToolSelector {

	private ToolSelector() {
	}

	/** What the selector needs to know about one candidate tool. */
	public interface Candidate {
		boolean isEffective();        // destroySpeed(state) > 1

		boolean isCorrect();          // isCorrectToolForDrops(state)

		int remainingUses();          // Integer.MAX_VALUE when not damageable

		boolean isMainHand();
	}

	public enum Need {
		UNBREAKABLE, NO_TOOL, TOOL
	}

	/**
	 * Returns an index into {@code candidates}, or -1 to mean "use the empty hand" (only possible
	 * for {@link Need#NO_TOOL}), or -2 to mean "impossible with what is available".
	 */
	public static int select(List<? extends Candidate> candidates, Need need,
			boolean requiresCorrectTool, boolean stopBeforeToolBreaks) {
		if (need == Need.UNBREAKABLE) {
			return -2;
		}
		if (need == Need.NO_TOOL) {
			return -1;
		}

		for (int i = 0; i < candidates.size(); i++) {
			Candidate candidate = candidates.get(i);
			if (!candidate.isEffective()) {
				continue;
			}
			if (requiresCorrectTool && !candidate.isCorrect()) {
				continue;
			}
			if (stopBeforeToolBreaks && candidate.remainingUses() <= 1) {
				continue;
			}
			return i;
		}

		if (!requiresCorrectTool) {
			for (int i = 0; i < candidates.size(); i++) {
				Candidate candidate = candidates.get(i);
				if (!candidate.isMainHand()) {
					continue;
				}
				if (stopBeforeToolBreaks && candidate.remainingUses() <= 1) {
					continue;
				}
				return i;
			}
		}

		return -2;
	}

	/** ceil(1 / (toolSpeed / hardness / (correct ? 30 : 100))); 0 when hardness &lt;= 0. */
	public static int estimateBreakTicks(float hardness, float toolSpeed, boolean correct) {
		if (hardness <= 0f) {
			return 0;
		}
		double speedMultiplier = correct ? 30d : 100d;
		double progressPerTick = (toolSpeed / hardness) / speedMultiplier;
		return (int) Math.ceil(1d / progressPerTick);
	}

	public static int capDelay(int totalTicks, int maxDelayTicks) {
		return Math.min(totalTicks, maxDelayTicks);
	}
}
