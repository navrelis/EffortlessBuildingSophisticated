package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModes;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.TwoClicksBuildMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Line extends TwoClicksBuildMode {

	public static BlockPos findLine(Player player, BlockPos firstPos, boolean skipRaytrace) {
		Vec3 look = BuildModes.getPlayerLookVec(player);
		Vec3 start = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

		List<Criteria> criteriaList = new ArrayList<>(3);

		//X
		Vec3 xBound = BuildModes.findXBound(firstPos.getX(), start, look);
		criteriaList.add(new Criteria(xBound, firstPos, start));

		//Y
		Vec3 yBound = BuildModes.findYBound(firstPos.getY(), start, look);
		criteriaList.add(new Criteria(yBound, firstPos, start));

		//Z
		Vec3 zBound = BuildModes.findZBound(firstPos.getZ(), start, look);
		criteriaList.add(new Criteria(zBound, firstPos, start));

		//Remove invalid criteria
		int reach = AttachmentHandler.getBuildModeReach(player);
		criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

		//If none are valid, return empty list of blocks
		if (criteriaList.isEmpty()) return null;

		//If only 1 is valid, choose that one
		Criteria selected = criteriaList.get(0);

		//If multiple are valid, choose based on criteria
		if (criteriaList.size() > 1) {
			//Select the one that is closest (from wall position to its line counterpart)
			for (int i = 1; i < criteriaList.size(); i++) {
				Criteria criteria = criteriaList.get(i);
				if (criteria.distToLineSq < 2.0 && selected.distToLineSq < 2.0) {
					//Both very close to line, choose closest to player
					if (criteria.distToPlayerSq < selected.distToPlayerSq)
						selected = criteria;
				} else {
					//Pick closest to line
					if (criteria.distToLineSq < selected.distToLineSq)
						selected = criteria;
				}
			}

		}

		return new BlockPos(selected.lineBound);
	}

	public static List<BlockPos> getLineBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return getLineBlocks(x1, y1, z1, x2, y2, z2, thicknessOf(ModeOptions.getLineThickness()));
	}

	/**
	 * Converts the {@link ModeOptions.ActionEnum} thickness selection (THICKNESS_1/3/5) to the
	 * integer thickness used by the pure block-generation methods below.
	 */
	public static int thicknessOf(ModeOptions.ActionEnum thicknessAction) {
		if (thicknessAction == ModeOptions.ActionEnum.THICKNESS_3) {
			return 3;
		}
		if (thicknessAction == ModeOptions.ActionEnum.THICKNESS_5) {
			return 5;
		}
		return 1;
	}

	/**
	 * Axis-aligned line with a square t&#215;t cross-section (radius r = (t-1)/2 in the two axes
	 * perpendicular to the line's own axis). Thickness 1 behaves exactly like the plain
	 * {@code addXLineBlocks}/{@code addYLineBlocks}/{@code addZLineBlocks} helpers, which stay
	 * 1-thick since Wall/Floor reuse them directly for their hollow outlines (F3).
	 */
	public static List<BlockPos> getLineBlocks(int x1, int y1, int z1, int x2, int y2, int z2, int thickness) {
		List<BlockPos> centerLine = new ArrayList<>();

		if (x1 != x2) {
			addXLineBlocks(centerLine, x1, x2, y1, z1);
		} else if (y1 != y2) {
			addYLineBlocks(centerLine, y1, y2, x1, z1);
		} else {
			addZLineBlocks(centerLine, z1, z2, x1, y1);
		}

		if (thickness <= 1) {
			return centerLine;
		}

		int radius = (thickness - 1) / 2;
		Set<BlockPos> thickened = new LinkedHashSet<>();

		boolean alongX = x1 != x2;
		boolean alongY = !alongX && y1 != y2;

		for (BlockPos center : centerLine) {
			if (alongX) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						thickened.add(center.offset(0, dy, dz));
					}
				}
			} else if (alongY) {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						thickened.add(center.offset(dx, 0, dz));
					}
				}
			} else {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dy = -radius; dy <= radius; dy++) {
						thickened.add(center.offset(dx, dy, 0));
					}
				}
			}
		}

		return new ArrayList<>(thickened);
	}

	public static void addXLineBlocks(List<BlockPos> list, int x1, int x2, int y, int z) {
		for (int x = x1; x1 < x2 ? x <= x2 : x >= x2; x += x1 < x2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	public static void addYLineBlocks(List<BlockPos> list, int y1, int y2, int x, int z) {
		for (int y = y1; y1 < y2 ? y <= y2 : y >= y2; y += y1 < y2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	public static void addZLineBlocks(List<BlockPos> list, int z1, int z2, int x, int y) {
		for (int z = z1; z1 < z2 ? z <= z2 : z >= z2; z += z1 < z2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	@Override
	protected BlockPos findSecondPos(Player player, BlockPos firstPos, boolean skipRaytrace) {
		return findLine(player, firstPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getAllBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return getLineBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	static class Criteria {
		Vec3 planeBound;
		Vec3 lineBound;
		double distToLineSq;
		double distToPlayerSq;

		Criteria(Vec3 planeBound, BlockPos firstPos, Vec3 start) {
			this.planeBound = planeBound;
			this.lineBound = toLongestLine(this.planeBound, firstPos);
			this.distToLineSq = this.lineBound.subtract(this.planeBound).lengthSqr();
			this.distToPlayerSq = this.planeBound.subtract(start).lengthSqr();
		}

		//Make it from a plane into a line
		//Select the axis that is longest
		private Vec3 toLongestLine(Vec3 boundVec, BlockPos firstPos) {
			BlockPos bound = new BlockPos(boundVec);

			BlockPos firstToSecond = bound.subtract(firstPos);
			firstToSecond = new BlockPos(Math.abs(firstToSecond.getX()), Math.abs(firstToSecond.getY()), Math.abs(firstToSecond.getZ()));
			int longest = Math.max(firstToSecond.getX(), Math.max(firstToSecond.getY(), firstToSecond.getZ()));
			if (longest == firstToSecond.getX()) {
				return new Vec3(bound.getX(), firstPos.getY(), firstPos.getZ());
			}
			if (longest == firstToSecond.getY()) {
				return new Vec3(firstPos.getX(), bound.getY(), firstPos.getZ());
			}
			if (longest == firstToSecond.getZ()) {
				return new Vec3(firstPos.getX(), firstPos.getY(), bound.getZ());
			}
			return null;
		}

		//check if its not behind the player and its not too close and not too far
		//also check if raytrace from player to block does not intersect blocks
		public boolean isValid(Vec3 start, Vec3 look, int reach, Player player, boolean skipRaytrace) {

			return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, lineBound, planeBound, distToPlayerSq);
		}

	}
}
