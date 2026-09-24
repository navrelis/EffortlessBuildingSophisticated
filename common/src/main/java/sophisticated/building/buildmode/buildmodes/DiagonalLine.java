package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ThreeClicksBuildMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DiagonalLine extends ThreeClicksBuildMode {

	//Add diagonal line from first to second
	public static List<BlockPos> getDiagonalLineBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, float sampleMultiplier) {
		return getDiagonalLineBlocks(x1, y1, z1, x2, y2, z2, sampleMultiplier, Line.thicknessOf(ModeOptions.getLineThickness()));
	}

	/**
	 * Diagonal line thickened into a cube neighbourhood (radius r = (t-1)/2 in every axis) around
	 * each sampled centre block. Thickness 1 returns exactly the plain sampled line. Duplicate
	 * blocks across samples are naturally skipped since callers (TwoClicks/ThreeClicksBuildMode)
	 * key their preview/placement map by BlockPos.
	 */
	public static List<BlockPos> getDiagonalLineBlocks(int x1, int y1, int z1, int x2, int y2, int z2, float sampleMultiplier, int thickness) {
		List<BlockPos> centerLine = new ArrayList<>();

		Vec3 first = new Vec3(x1, y1, z1).add(0.5, 0.5, 0.5);
		Vec3 second = new Vec3(x2, y2, z2).add(0.5, 0.5, 0.5);

		int iterations = (int) Math.ceil(first.distanceTo(second) * sampleMultiplier);
		for (double t = 0; t <= 1.0; t += 1.0 / iterations) {
			Vec3 lerp = first.add(second.subtract(first).scale(t));
			BlockPos candidate = BlockPos.containing(lerp);
			//Only add if not equal to the last in the list
			if (centerLine.isEmpty() || !centerLine.get(centerLine.size() - 1).equals(candidate))
				centerLine.add(candidate);
		}

		if (thickness <= 1) {
			return centerLine;
		}

		int radius = (thickness - 1) / 2;
		Set<BlockPos> thickened = new LinkedHashSet<>();
		for (BlockPos center : centerLine) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						thickened.add(center.offset(dx, dy, dz));
					}
				}
			}
		}

		return new ArrayList<>(thickened);
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
		//Add diagonal line from first to second
		return getDiagonalLineBlocks(player, x1, y1, z1, x2, y2, z2, 10);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		//Add diagonal line from first to third
		return getDiagonalLineBlocks(player, x1, y1, z1, x3, y3, z3, 10);
	}
}
