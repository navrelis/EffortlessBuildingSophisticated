package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.ToolSelector;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolSelectorTest {

	private static final class FakeCandidate implements ToolSelector.Candidate {
		private final boolean effective;
		private final boolean correct;
		private final int remainingUses;
		private final boolean mainHand;

		FakeCandidate(boolean effective, boolean correct, int remainingUses, boolean mainHand) {
			this.effective = effective;
			this.correct = correct;
			this.remainingUses = remainingUses;
			this.mainHand = mainHand;
		}

		@Override
		public boolean isEffective() {
			return effective;
		}

		@Override
		public boolean isCorrect() {
			return correct;
		}

		@Override
		public int remainingUses() {
			return remainingUses;
		}

		@Override
		public boolean isMainHand() {
			return mainHand;
		}
	}

	@Test
	void correctAndEffectiveToolChosenOverEarlierEffectiveButWrongTierTool() {
		FakeCandidate wrongTier = new FakeCandidate(true, false, Integer.MAX_VALUE, false);
		FakeCandidate correctTier = new FakeCandidate(true, true, Integer.MAX_VALUE, false);
		int index = ToolSelector.select(List.of(wrongTier, correctTier), ToolSelector.Need.TOOL, true, true);
		assertEquals(1, index);
	}

	@Test
	void stopBeforeToolBreaksSkipsCandidateWithOneUseLeftAndTakesTheNextOne() {
		FakeCandidate almostBroken = new FakeCandidate(true, true, 1, false);
		FakeCandidate healthy = new FakeCandidate(true, true, 10, false);
		int index = ToolSelector.select(List.of(almostBroken, healthy), ToolSelector.Need.TOOL, true, true);
		assertEquals(1, index);
	}

	@Test
	void noEffectiveToolFallsBackToMainHandWhenCorrectToolNotRequired() {
		FakeCandidate ineffective = new FakeCandidate(false, false, Integer.MAX_VALUE, false);
		FakeCandidate mainHand = new FakeCandidate(false, false, Integer.MAX_VALUE, true);
		int index = ToolSelector.select(List.of(ineffective, mainHand), ToolSelector.Need.TOOL, false, true);
		assertEquals(1, index);
	}

	@Test
	void returnsImpossibleWhenMainHandAbsentAndNoToolFits() {
		FakeCandidate ineffective = new FakeCandidate(false, false, Integer.MAX_VALUE, false);
		int index = ToolSelector.select(List.of(ineffective), ToolSelector.Need.TOOL, false, true);
		assertEquals(-2, index);
	}

	@Test
	void estimateBreakTicksMatchesTheStatedExamples() {
		assertEquals(8, ToolSelector.estimateBreakTicks(1.5f, 6f, true));
		assertEquals(15, ToolSelector.estimateBreakTicks(0.5f, 1f, true));
		assertEquals(150, ToolSelector.estimateBreakTicks(1.5f, 1f, false));
		assertEquals(0, ToolSelector.estimateBreakTicks(0f, 1f, true));
	}

	@Test
	void capDelayClampsToTheConfiguredMaximum() {
		assertEquals(40, ToolSelector.capDelay(300, 40));
		assertEquals(12, ToolSelector.capDelay(12, 40));
	}
}
