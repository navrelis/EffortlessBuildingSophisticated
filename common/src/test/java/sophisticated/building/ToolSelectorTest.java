package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.ToolSelector;

import java.util.Arrays;
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
		int index = ToolSelector.select(Arrays.asList(wrongTier, correctTier), ToolSelector.Need.TOOL, true, true);
		assertEquals(1, index);
	}

	@Test
	void stopBeforeToolBreaksSkipsCandidateWithOneUseLeftAndTakesTheNextOne() {
		FakeCandidate almostBroken = new FakeCandidate(true, true, 1, false);
		FakeCandidate healthy = new FakeCandidate(true, true, 10, false);
		int index = ToolSelector.select(Arrays.asList(almostBroken, healthy), ToolSelector.Need.TOOL, true, true);
		assertEquals(1, index);
	}

	@Test
	void usesNeededSkipsToolsWithoutMoreUsesLeftThanABlockCosts() {
		FakeCandidate fiveLeft = new FakeCandidate(true, true, 5, false);
		FakeCandidate sixLeft = new FakeCandidate(true, true, 6, false);
		assertEquals(1, ToolSelector.select(Arrays.asList(fiveLeft, sixLeft), ToolSelector.Need.TOOL, true, true, 5));
		assertEquals(-2, ToolSelector.select(Arrays.asList(fiveLeft), ToolSelector.Need.TOOL, true, true, 5));
		assertEquals(0, ToolSelector.select(Arrays.asList(fiveLeft, sixLeft), ToolSelector.Need.TOOL, true, true, 4));
	}

	@Test
	void usesNeededAppliesToTheMainHandFallbackToo() {
		FakeCandidate mainHand = new FakeCandidate(false, false, 3, true);
		assertEquals(-2, ToolSelector.select(Arrays.asList(mainHand), ToolSelector.Need.TOOL, false, true, 3));
		assertEquals(0, ToolSelector.select(Arrays.asList(mainHand), ToolSelector.Need.TOOL, false, true, 2));
	}

	@Test
	void usesNeededIsIgnoredWithoutStopBeforeToolBreaks() {
		FakeCandidate oneLeft = new FakeCandidate(true, true, 1, false);
		assertEquals(0, ToolSelector.select(Arrays.asList(oneLeft), ToolSelector.Need.TOOL, true, false, 5));
	}

	@Test
	void oneUseNeededIsTheOldRule() {
		FakeCandidate oneLeft = new FakeCandidate(true, true, 1, false);
		FakeCandidate twoLeft = new FakeCandidate(true, true, 2, false);
		for (List<FakeCandidate> candidates : Arrays.asList(Arrays.asList(oneLeft), Arrays.asList(twoLeft), Arrays.asList(oneLeft, twoLeft))) {
			assertEquals(ToolSelector.select(candidates, ToolSelector.Need.TOOL, true, true),
					ToolSelector.select(candidates, ToolSelector.Need.TOOL, true, true, 1));
		}
	}

	@Test
	void noEffectiveToolFallsBackToMainHandWhenCorrectToolNotRequired() {
		FakeCandidate ineffective = new FakeCandidate(false, false, Integer.MAX_VALUE, false);
		FakeCandidate mainHand = new FakeCandidate(false, false, Integer.MAX_VALUE, true);
		int index = ToolSelector.select(Arrays.asList(ineffective, mainHand), ToolSelector.Need.TOOL, false, true);
		assertEquals(1, index);
	}

	@Test
	void returnsImpossibleWhenMainHandAbsentAndNoToolFits() {
		FakeCandidate ineffective = new FakeCandidate(false, false, Integer.MAX_VALUE, false);
		int index = ToolSelector.select(Arrays.asList(ineffective), ToolSelector.Need.TOOL, false, true);
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
