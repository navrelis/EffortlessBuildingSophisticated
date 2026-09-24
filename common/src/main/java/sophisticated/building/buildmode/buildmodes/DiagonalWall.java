package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ThreeClicksBuildMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DiagonalWall extends ThreeClicksBuildMode {

	//Add diagonal wall from first to second, filled (F4 default/legacy behaviour)
	public static List<BlockPos> getDiagonalWallBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		return getDiagonalWallBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3, false);
	}

	/**
	 * Diagonal wall between the diagonal line (first-&gt;second) and the third-click height.
	 * FULL (hollow = false) is the original behaviour: every y level between lowest and highest
	 * copies the whole diagonal line. HOLLOW (F4) keeps only the diagonal line at the lowest and
	 * highest y plus the vertical columns at the two ends of the line for the y values in between;
	 * when lowest == highest both reduce to the plain line.
	 */
	public static List<BlockPos> getDiagonalWallBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3, boolean hollow) {
		//Get diagonal line blocks (always 1 block thick, regardless of the Line Thickness option;
		//uses the pure overload directly so this never depends on client ModeOptions state)
		List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(x1, y1, z1, x2, y2, z2, 1, 1);

		int lowest = Math.min(y1, y3);
		int highest = Math.max(y1, y3);

		if (!hollow || lowest == highest) {
			List<BlockPos> list = new ArrayList<>();
			for (int y = lowest; y <= highest; y++) {
				for (BlockPos blockPos : diagonalLineBlocks) {
					list.add(new BlockPos(blockPos.getX(), y, blockPos.getZ()));
				}
			}
			return list;
		}

		Set<BlockPos> set = new LinkedHashSet<>();
		BlockPos firstEnd = diagonalLineBlocks.get(0);
		BlockPos secondEnd = diagonalLineBlocks.get(diagonalLineBlocks.size() - 1);

		for (int y = lowest; y <= highest; y++) {
			if (y == lowest || y == highest) {
				for (BlockPos blockPos : diagonalLineBlocks) {
					set.add(new BlockPos(blockPos.getX(), y, blockPos.getZ()));
				}
			} else {
				set.add(new BlockPos(firstEnd.getX(), y, firstEnd.getZ()));
				set.add(new BlockPos(secondEnd.getX(), y, secondEnd.getZ()));
			}
		}

		return new ArrayList<>(set);
	}

	@Override
	protected BlockPos findSecondPos(Player player, BlockPos firstPos, boolean skipRaytrace) {
		return Floor.findFloor(player, firstPos, skipRaytrace);
	}

	@Override
	protected BlockPos findThirdPos(Player player, BlockPos firstPos, BlockPos secondPos, boolean skipRaytrace) {
		return findHeight(player, secondPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getIntermediateBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return DiagonalLine.getDiagonalLineBlocks(x1, y1, z1, x2, y2, z2, 1, 1);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		boolean hollow = ModeOptions.getFill() == ModeOptions.ActionEnum.HOLLOW;
		return getDiagonalWallBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3, hollow);
	}
}
