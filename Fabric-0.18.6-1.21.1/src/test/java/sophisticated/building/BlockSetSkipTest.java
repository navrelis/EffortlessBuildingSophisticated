package sophisticated.building;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSetSkipTest {

	private static BlockSet setWith(boolean skipFirst, BlockPos firstPos, BlockPos... positions) {
		BlockSet blocks = new BlockSet();
		for (BlockPos pos : positions) {
			blocks.put(pos, new BlockEntry(pos));
		}
		blocks.firstPos = firstPos;
		blocks.skipFirst = skipFirst;
		return blocks;
	}

	@Test
	void firstPositionIsSkippedByValueNotByIdentity() {
		// after decoding, the entry position and firstPos are different objects
		BlockSet blocks = setWith(true, new BlockPos(1, 2, 3), new BlockPos(1, 2, 3), new BlockPos(4, 2, 3));
		assertTrue(blocks.isSkipped(blocks.get(new BlockPos(1, 2, 3))));
		assertFalse(blocks.isSkipped(blocks.get(new BlockPos(4, 2, 3))));
	}

	@Test
	void nothingIsSkippedWithoutTheFlag() {
		BlockSet blocks = setWith(false, new BlockPos(1, 2, 3), new BlockPos(1, 2, 3));
		assertFalse(blocks.isSkipped(blocks.get(new BlockPos(1, 2, 3))));
		assertTrue(blocks.hasUnskippedEntries());
	}

	@Test
	void setWithOnlyTheSkippedFirstPositionHasNothingLeft() {
		BlockSet blocks = setWith(true, new BlockPos(1, 2, 3), new BlockPos(1, 2, 3));
		assertFalse(blocks.hasUnskippedEntries());
	}

	@Test
	void setWithMoreThanTheSkippedFirstPositionHasEntriesLeft() {
		BlockSet blocks = setWith(true, new BlockPos(1, 2, 3), new BlockPos(1, 2, 3), new BlockPos(-1, 2, 3));
		assertTrue(blocks.hasUnskippedEntries());
	}

	@Test
	void undoSetsWithoutFirstPositionSkipNothing() {
		BlockSet blocks = setWith(false, null, new BlockPos(0, 0, 0));
		assertFalse(blocks.isSkipped(blocks.get(new BlockPos(0, 0, 0))));
		assertTrue(blocks.hasUnskippedEntries());
	}
}
