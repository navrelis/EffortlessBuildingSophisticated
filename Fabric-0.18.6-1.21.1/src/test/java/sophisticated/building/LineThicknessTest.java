package sophisticated.building;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import sophisticated.building.buildmode.buildmodes.Line;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineThicknessTest {

	@Test
	void thickness1GivesThePlainXLine() {
		List<BlockPos> blocks = Line.getLineBlocks(0, 0, 0, 3, 0, 0, 1);
		assertEquals(4, blocks.size());
		assertEquals(Set.of(
				new BlockPos(0, 0, 0), new BlockPos(1, 0, 0),
				new BlockPos(2, 0, 0), new BlockPos(3, 0, 0)
		), Set.copyOf(blocks));
	}

	@Test
	void thickness3OnXLineOfLength4Gives4Times9Blocks() {
		List<BlockPos> blocks = Line.getLineBlocks(0, 0, 0, 3, 0, 0, 3);
		Set<BlockPos> unique = Set.copyOf(blocks);
		assertEquals(4 * 9, unique.size());

		// square cross-section: every block within +/-1 in Y and Z of the X axis
		assertTrue(unique.stream().allMatch(pos -> Math.abs(pos.getY()) <= 1 && Math.abs(pos.getZ()) <= 1));
		for (int x = 0; x <= 3; x++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					assertTrue(unique.contains(new BlockPos(x, dy, dz)),
							"expected block at x=" + x + " dy=" + dy + " dz=" + dz);
				}
			}
		}
	}

	@Test
	void thickness5OnXLineOfLength4Gives4Times25Blocks() {
		List<BlockPos> blocks = Line.getLineBlocks(0, 0, 0, 3, 0, 0, 5);
		Set<BlockPos> unique = Set.copyOf(blocks);
		assertEquals(4 * 25, unique.size());
		assertTrue(unique.stream().allMatch(pos -> Math.abs(pos.getY()) <= 2 && Math.abs(pos.getZ()) <= 2));
	}

	@Test
	void thickness3OnYLineOfLength4Gives4Times9BlocksWithinXAndZ() {
		List<BlockPos> blocks = Line.getLineBlocks(0, 0, 0, 0, 3, 0, 3);
		Set<BlockPos> unique = Set.copyOf(blocks);
		assertEquals(4 * 9, unique.size());
		assertTrue(unique.stream().allMatch(pos -> Math.abs(pos.getX()) <= 1 && Math.abs(pos.getZ()) <= 1));
		for (int y = 0; y <= 3; y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					assertTrue(unique.contains(new BlockPos(dx, y, dz)));
				}
			}
		}
	}

	@Test
	void thickness3OnZLineOfLength4Gives4Times9BlocksWithinXAndY() {
		List<BlockPos> blocks = Line.getLineBlocks(0, 0, 0, 0, 0, 3, 3);
		Set<BlockPos> unique = Set.copyOf(blocks);
		assertEquals(4 * 9, unique.size());
		assertTrue(unique.stream().allMatch(pos -> Math.abs(pos.getX()) <= 1 && Math.abs(pos.getY()) <= 1));
		for (int z = 0; z <= 3; z++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					assertTrue(unique.contains(new BlockPos(dx, dy, z)));
				}
			}
		}
	}

	// Note: Line.thicknessOf(ModeOptions.ActionEnum) is intentionally not covered here.
	// ModeOptions.ActionEnum's <clinit> references AllIcons, which references
	// SophisticatedBuilding's registerItem() -> BuiltInRegistries, which requires Minecraft's
	// Bootstrap.bootStrap() to have run first ("Not bootstrapped" IllegalArgumentException) -
	// unavailable in a plain JUnit unit test. The pure block-generation overloads above (which take
	// an int thickness directly, per the T6/T7 "expose pure overloads" instruction) are what's
	// unit-tested; thicknessOf's int mapping is a one-line lookup exercised manually/in-game.
}
