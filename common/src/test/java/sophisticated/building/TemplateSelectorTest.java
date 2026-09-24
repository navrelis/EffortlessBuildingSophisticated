package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.systems.ItemUsageTracker;
import sophisticated.building.utilities.TemplateSelector;

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
		var held = new FakeStack(1, true);
		var slot5 = new FakeStack(1, true);
		var slot20 = new FakeStack(1, true);
		var candidates = List.of(held, slot5, slot20);
		var selector = selector();

		assertSame(held, placeOne(selector, candidates, held, 0));
		assertSame(slot5, placeOne(selector, candidates, held, 0));
		assertSame(slot20, placeOne(selector, candidates, held, 0));
		assertNull(placeOne(selector, candidates, held, 0));

		assertEquals(0, held.count + slot5.count + slot20.count);
	}

	@Test
	void plainStackIsReservedUntilUsedUpThenStackWithDataFollows() {
		var plain = new FakeStack(2, false);
		var filled = new FakeStack(1, true);
		var candidates = List.of(plain, filled);
		var selector = selector();

		assertSame(plain, placeOne(selector, candidates, null, 0));
		assertSame(plain, placeOne(selector, candidates, null, 0));
		assertSame(filled, placeOne(selector, candidates, null, 0));
		// Plain stacks are removed later in bulk, so their count is untouched here
		assertEquals(2, plain.count);
		assertEquals(0, filled.count);
	}

	@Test
	void heldStackWithDataComesBeforePlainStacks() {
		var held = new FakeStack(1, true);
		var plain = new FakeStack(64, false);
		var selector = selector();

		assertSame(held, placeOne(selector, List.of(held, plain), held, 0));
		assertSame(plain, placeOne(selector, List.of(held, plain), held, 0));
	}

	@Test
	void anchorKeepsOneHeldPlainItemOutOfSelection() {
		var held = new FakeStack(2, false);
		var filled = new FakeStack(1, true);
		var candidates = List.of(held, filled);
		var selector = selector();

		assertSame(held, placeOne(selector, candidates, held, 1));
		assertSame(filled, placeOne(selector, candidates, held, 1));
		assertNull(placeOne(selector, candidates, held, 1));
	}

	@Test
	void anchorDoesNotApplyToStackWithData() {
		var held = new FakeStack(1, true);
		var selector = selector();

		assertSame(held, placeOne(selector, List.of(held), held, 1));
	}

	@Test
	void returnsNullWhenNothingIsLeftSoTheBackpackPathIsUsed() {
		var plain = new FakeStack(1, false);
		var selector = selector();

		assertSame(plain, placeOne(selector, List.of(plain), null, 0));
		assertNull(placeOne(selector, List.of(plain), null, 0));
		assertNull(selector().select(List.of(), null, 0));
	}

	@Test
	void failedPlacementWithDataStackReusesTheSameStack() {
		var filled = new FakeStack(1, true);
		var selector = selector();

		// Nothing consumed when placing failed, so the same stack is still the template
		assertSame(filled, selector.select(List.of(filled), null, 0));
		assertSame(filled, selector.select(List.of(filled), null, 0));
	}

	@Test
	void bulkRemovalSubtractsIndividuallyConsumedItems() {
		Map<String, Integer> placed = Map.of("shulker", 3, "stone", 10, "sack", 2);
		Map<String, Integer> consumed = Map.of("shulker", 3, "sack", 1);

		var bulk = ItemUsageTracker.subtractCounts(placed, consumed);

		assertEquals(Map.of("stone", 10, "sack", 1), bulk);
	}

	@Test
	void bulkRemovalNeverGoesNegative() {
		var bulk = ItemUsageTracker.subtractCounts(Map.of("shulker", 1), Map.of("shulker", 2, "other", 5));

		assertTrue(bulk.isEmpty());
	}
}
