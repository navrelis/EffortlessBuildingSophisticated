package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ThreeClicksBuildMode;

import java.util.ArrayList;
import java.util.List;

public class Pyramid extends ThreeClicksBuildMode {

	public static List<BlockPos> getPyramidBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		List<BlockPos> list = new ArrayList<>();

		// Determine bounds
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minZ = Math.min(z1, z2);
		int maxZ = Math.max(z1, z2);

		// Height direction
		int baseY = y1;
		int apexY = y3;
		int heightDir = apexY > baseY ? 1 : -1;
		int totalHeight = Math.abs(apexY - baseY) + 1;

		// Base dimensions
		int baseWidthX = maxX - minX;
		int baseWidthZ = maxZ - minZ;

		boolean hollow = ModeOptions.getFill() == ModeOptions.ActionEnum.HOLLOW;

		// Build layer by layer
		for (int layer = 0; layer < totalHeight; layer++) {
			int currentY = baseY + layer * heightDir;

			// Calculate shrinkage for this layer
			// At layer 0, no shrinkage. At top layer, shrink to 0 or 1 block
			float shrinkRatioX = (totalHeight > 1) ? (float) layer / (totalHeight - 1) : 0;
			float shrinkRatioZ = (totalHeight > 1) ? (float) layer / (totalHeight - 1) : 0;

			int shrinkX = (int) (shrinkRatioX * baseWidthX / 2);
			int shrinkZ = (int) (shrinkRatioZ * baseWidthZ / 2);

			int layerMinX = minX + shrinkX;
			int layerMaxX = maxX - shrinkX;
			int layerMinZ = minZ + shrinkZ;
			int layerMaxZ = maxZ - shrinkZ;

			// If layer has shrunk to nothing, we're done
			if (layerMinX > layerMaxX || layerMinZ > layerMaxZ) {
				break;
			}

			// Add blocks for this layer
			if (hollow && layer > 0 && layer < totalHeight - 1) {
				// Only add edges for hollow pyramid (not bottom or top layer)
				for (int x = layerMinX; x <= layerMaxX; x++) {
					list.add(new BlockPos(x, currentY, layerMinZ));
					if (layerMinZ != layerMaxZ) {
						list.add(new BlockPos(x, currentY, layerMaxZ));
					}
				}
				for (int z = layerMinZ + 1; z < layerMaxZ; z++) {
					list.add(new BlockPos(layerMinX, currentY, z));
					if (layerMinX != layerMaxX) {
						list.add(new BlockPos(layerMaxX, currentY, z));
					}
				}
			} else {
				// Full layer (bottom, top, or full fill)
				for (int x = layerMinX; x <= layerMaxX; x++) {
					for (int z = layerMinZ; z <= layerMaxZ; z++) {
						list.add(new BlockPos(x, currentY, z));
					}
				}
			}
		}

		return list;
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
		return Floor.getFloorBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		return getPyramidBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3);
	}
}
