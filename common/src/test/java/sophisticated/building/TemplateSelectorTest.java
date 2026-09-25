package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.systems.ItemUsageTracker;
import sophisticated.building.utilities.TemplateSelector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateSelectorTest {

	private static final class FakeStack {
		int count;
		final boolean hasData;

		FakeStack(int count, boolean hasData) {
			this.count = count;
			this.hasData = hasData;
		}
	}

	private static TemplateSelector<FakeStack> selector() {
		return new TemplateSelector<>(s -> s.count, s -> s.hasData);
	}

	// Mimics ServerBlockPlacer: stacks with data are consumed right after a successful placement
	private static FakeStack placeOne(TemplateSelector<FakeStack> selector, List<FakeStack> candidates, FakeStack anchor, int anchorCount) {
		FakeStack selected = selector.select(candidates, anchor, anchorCount);
		if (selected != null && selected.hasData) selected.count--;
		return selected;
	}

	@Test
	void threeFilledBoxesInDifferentSlotsAreEachUsedOnce() {
		FakeStack held = new FakeStack(1, true);
		FakeStack slot5 = new FakeStack(1, true);
		FakeStack slot20 = new FakeStack(1, true);
		List<FakeStack> candidates = Arrays.asList(held, slot5, slot20);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(held, placeOne(selector, candidates, held, 0));
		assertSame(slot5, placeOne(selector, candidates, held, 0));
		assertSame(slot20, placeOne(selector, candidates, held, 0));
		assertNull(placeOne(selector, candidates, held, 0));

		assertEquals(0, held.count + slot5.count + slot20.count);
	}

	@Test
	void plainStackIsReservedUntilUsedUpThenStackWithDataFollows() {
		FakeStack plain = new FakeStack(2, false);
		FakeStack filled = new FakeStack(1, true);
		List<FakeStack> candidates = Arrays.asList(plain, filled);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(plain, placeOne(selector, candidates, null, 0));
		assertSame(plain, placeOne(selector, candidates, null, 0));
		assertSame(filled, placeOne(selector, candidates, null, 0));
		// Plain stacks are removed later in bulk, so their count is untouched here
		assertEquals(2, plain.count);
		assertEquals(0, filled.count);
	}

	@Test
	void heldStackWithDataComesBeforePlainStacks() {
		FakeStack held = new FakeStack(1, true);
		FakeStack plain = new FakeStack(64, false);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(held, placeOne(selector, Arrays.asList(held, plain), held, 0));
		assertSame(plain, placeOne(selector, Arrays.asList(held, plain), held, 0));
	}

	@Test
	void anchorKeepsOneHeldPlainItemOutOfSelection() {
		FakeStack held = new FakeStack(2, false);
		FakeStack filled = new FakeStack(1, true);
		List<FakeStack> candidates = Arrays.asList(held, filled);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(held, placeOne(selector, candidates, held, 1));
		assertSame(filled, placeOne(selector, candidates, held, 1));
		assertNull(placeOne(selector, candidates, held, 1));
	}

	@Test
	void anchorDoesNotApplyToStackWithData() {
		FakeStack held = new FakeStack(1, true);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(held, placeOne(selector, Arrays.asList(held), held, 1));
	}

	@Test
	void returnsNullWhenNothingIsLeftSoTheBackpackPathIsUsed() {
		FakeStack plain = new FakeStack(1, false);
		TemplateSelector<FakeStack> selector = selector();

		assertSame(plain, placeOne(selector, Arrays.asList(plain), null, 0));
		assertNull(placeOne(selector, Arrays.asList(plain), null, 0));
		assertNull(selector().select(Collections.emptyList(), null, 0));
	}

	@Test
	void failedPlacementWithDataStackReusesTheSameStack() {
		FakeStack filled = new FakeStack(1, true);
		TemplateSelector<FakeStack> selector = selector();

		// Nothing consumed when placing failed, so the same stack is still the template
		assertSame(filled, selector.select(Arrays.asList(filled), null, 0));
		assertSame(filled, selector.select(Arrays.asList(filled), null, 0));
	}

	@Test
	void bulkRemovalSubtractsIndividuallyConsumedItems() {
		Map<String, Integer> placed = counts("shulker", 3, "stone", 10, "sack", 2);
		Map<String, Integer> consumed = counts("shulker", 3, "sack", 1);

		Map<String, Integer> bulk = ItemUsageTracker.subtractCounts(placed, consumed);

		assertEquals(counts("stone", 10, "sack", 1), bulk);
	}

	@Test
	void bulkRemovalNeverGoesNegative() {
		Map<String, Integer> bulk = ItemUsageTracker.subtractCounts(counts("shulker", 1), counts("shulker", 2, "other", 5));

		assertTrue(bulk.isEmpty());
	}

	/** Map.of for String counts (Java 8). */
	private static Map<String, Integer> counts(Object... keysAndValues) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put((String) keysAndValues[i], (Integer) keysAndValues[i + 1]);
		}
		return map;
	}
}
