package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ThreeClicksBuildMode;

import java.util.ArrayList;
import java.util.List;

public class Cone extends ThreeClicksBuildMode {

	public static List<BlockPos> getConeBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		List<BlockPos> list = new ArrayList<>();

		// Calculate center of base circle
		float centerX = x1;
		float centerZ = z1;

		// Adjust for CIRCLE_START
		if (ModeOptions.getCircleStart() == ModeOptions.ActionEnum.CIRCLE_START_CORNER) {
			centerX = x1 + (x2 - x1) / 2f;
			centerZ = z1 + (z2 - z1) / 2f;
		} else {
			x1 = (int) (centerX - (x2 - centerX));
			z1 = (int) (centerZ - (z2 - centerZ));
		}

		float baseRadiusX = Mth.abs(x2 - centerX);
		float baseRadiusZ = Mth.abs(z2 - centerZ);

		// Height direction
		int baseY = y1;
		int apexY = y3;
		int heightDir = apexY > baseY ? 1 : -1;
		int totalHeight = Math.abs(apexY - baseY) + 1;

		boolean hollow = ModeOptions.getFill() == ModeOptions.ActionEnum.HOLLOW;

		// Build layer by layer
		for (int layer = 0; layer < totalHeight; layer++) {
			int currentY = baseY + layer * heightDir;

			// Calculate radius for this layer (linearly interpolate from base to apex)
			float layerRatio = (totalHeight > 1) ? (float) layer / (totalHeight - 1) : 0;
			float layerRadiusX = baseRadiusX * (1 - layerRatio);
			float layerRadiusZ = baseRadiusZ * (1 - layerRatio);

			// If radius is essentially 0, just add the center point
			if (layerRadiusX < 0.5f && layerRadiusZ < 0.5f) {
				list.add(new BlockPos((int) centerX, currentY, (int) centerZ));
				continue;
			}

			// Calculate bounds for this layer
			int layerMinX = (int) Math.floor(centerX - layerRadiusX);
			int layerMaxX = (int) Math.ceil(centerX + layerRadiusX);
			int layerMinZ = (int) Math.floor(centerZ - layerRadiusZ);
			int layerMaxZ = (int) Math.ceil(centerZ + layerRadiusZ);

			// Add blocks for this layer using circle/ellipse logic
			for (int x = layerMinX; x <= layerMaxX; x++) {
				for (int z = layerMinZ; z <= layerMaxZ; z++) {
					float distance = distance(x, z, centerX, centerZ);
					float radius = calculateEllipseRadius(centerX, centerZ, layerRadiusX, layerRadiusZ, x, z);

					if (hollow && layer > 0 && layer < totalHeight - 1) {
						// Hollow: only add edge blocks
						if (distance < radius + 0.4f && distance > radius - 0.6f) {
							list.add(new BlockPos(x, currentY, z));
						}
					} else {
						// Full: add all blocks within radius
						if (distance < radius + 0.4f) {
							list.add(new BlockPos(x, currentY, z));
						}
					}
				}
			}
		}

		return list;
	}

	private static float distance(float x1, float z1, float x2, float z2) {
		return Mth.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
	}

	private static float calculateEllipseRadius(float centerX, float centerZ, float radiusX, float radiusZ, int x, int z) {
		if (radiusX == 0 || radiusZ == 0) return 0;
		float theta = (float) Mth.atan2(z - centerZ, x - centerX);
		float part1 = radiusX * radiusX * Mth.sin(theta) * Mth.sin(theta);
		float part2 = radiusZ * radiusZ * Mth.cos(theta) * Mth.cos(theta);
		return radiusX * radiusZ / Mth.sqrt(part1 + part2);
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
		return Circle.getCircleBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		return getConeBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3);
	}
}
