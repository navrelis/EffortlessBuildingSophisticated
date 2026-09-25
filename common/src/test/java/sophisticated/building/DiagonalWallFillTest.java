package sophisticated.building;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import sophisticated.building.buildmode.buildmodes.DiagonalLine;
import sophisticated.building.buildmode.buildmodes.DiagonalWall;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagonalWallFillTest {

	@Test
	void filledWallCopiesTheWholeLineAtEveryHeight() {
		// 5-block line (x: 0..4), height from y=0 (first click) to y=3 (third click) -> 4 levels
		List<BlockPos> blocks = DiagonalWall.getDiagonalWallBlocks(null, 0, 0, 0, 4, 0, 0, 0, 3, 0, false);
		Set<BlockPos> unique = new HashSet<>(blocks);

		List<BlockPos> line = DiagonalLine.getDiagonalLineBlocks(0, 0, 0, 4, 0, 0, 1, 1);
		assertEquals(line.size() * 4, unique.size());

		for (int y = 0; y <= 3; y++) {
			for (BlockPos p : line) {
				assertEquals(true, unique.contains(new BlockPos(p.getX(), y, p.getZ())),
						"expected filled wall to contain " + p.getX() + "," + y + "," + p.getZ());
			}
		}
	}

	@Test
	void hollowWallOnFiveBlockLineWithHeightFourContainsOnlyTheTwoRowsAndTwoEndColumns() {
		// Line from (0,0,0) to (4,0,0): 5 blocks. Third click y=3 -> lowest=0, highest=3.
		List<BlockPos> blocks = DiagonalWall.getDiagonalWallBlocks(null, 0, 0, 0, 4, 0, 0, 0, 3, 0, true);
		Set<BlockPos> unique = new HashSet<>(blocks);

		Set<BlockPos> expected = new HashSet<>(Arrays.asList(
				// bottom row (y=0)
				new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(3, 0, 0), new BlockPos(4, 0, 0),
				// top row (y=3)
				new BlockPos(0, 3, 0), new BlockPos(1, 3, 0), new BlockPos(2, 3, 0), new BlockPos(3, 3, 0), new BlockPos(4, 3, 0),
				// end columns (y=1,2 at x=0 and x=4)
				new BlockPos(0, 1, 0), new BlockPos(0, 2, 0),
				new BlockPos(4, 1, 0), new BlockPos(4, 2, 0)
		));

		assertEquals(14, unique.size());
		assertEquals(expected, unique);
	}

	@Test
	void hollowWallWithEqualHeightsReducesToThePlainLine() {
		List<BlockPos> blocks = DiagonalWall.getDiagonalWallBlocks(null, 0, 0, 0, 4, 0, 0, 0, 0, 0, true);
		List<BlockPos> line = DiagonalLine.getDiagonalLineBlocks(0, 0, 0, 4, 0, 0, 1, 1);

		assertEquals(new HashSet<>(line), new HashSet<>(blocks));
	}
}
