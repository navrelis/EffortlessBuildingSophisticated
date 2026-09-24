package sophisticated.building.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ThreeClicksBuildMode;

import java.util.ArrayList;
import java.util.List;

/**
 * TerrainMound - Creates various natural-looking terrain shapes for terraforming.
 * Supports multiple terrain types: Mound, Slope, Flat, Mountain, and Wall.
 * Direction-based shapes (Slope, Wall) face the player's looking direction.
 * 
 * Click 1: Set the center or corner point (depends on CIRCLE_START setting)
 * Click 2: Set the radius/extent
 * Click 3: Set the maximum height
 */
public class TerrainMound extends ThreeClicksBuildMode {

	/**
	 * Generates the block positions for terrain based on the selected type.
	 */
	public static List<BlockPos> getTerrainBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		List<BlockPos> list = new ArrayList<>();

		int axisLimit = AttachmentHandler.getMaxBlocksPerAxis(player, false);

		float centerX = x1;
		float centerZ = z1;
		int baseY = y1;

		// Adjust for CIRCLE_START option (same pattern as Circle/Sphere)
		if (ModeOptions.getCircleStart() == ModeOptions.ActionEnum.CIRCLE_START_CORNER) {
			// Corner mode: first click is corner, calculate center from corners
			centerX = x1 + (x2 - x1) / 2f;
			centerZ = z1 + (z2 - z1) / 2f;
		} else {
			// Center mode: first click is center, mirror the second click position
			x1 = (int) (centerX - (x2 - centerX));
			z1 = (int) (centerZ - (z2 - centerZ));
		}

		// Calculate radii from center
		float radiusX = Mth.abs(x2 - centerX);
		float radiusZ = Mth.abs(z2 - centerZ);

		// Ensure minimum radius of 1
		if (radiusX < 1) radiusX = 1;
		if (radiusZ < 1) radiusZ = 1;

		// Maximum height from base to third click
		int maxHeight = Math.abs(y3 - baseY);
		if (maxHeight < 1) maxHeight = 1;

		// Apply axis limits
		if (radiusX > axisLimit) radiusX = axisLimit;
		if (radiusZ > axisLimit) radiusZ = axisLimit;
		if (maxHeight > axisLimit) maxHeight = axisLimit;

		// Get options
		boolean filled = ModeOptions.getFill() == ModeOptions.ActionEnum.FULL;
		boolean addNoise = ModeOptions.getTerrainNoise() == ModeOptions.ActionEnum.TERRAIN_NOISE_ON;
		ModeOptions.ActionEnum terrainType = ModeOptions.getTerrainType();

		// Get player facing direction for directional shapes (Slope, Wall)
		float yaw = player.getYRot();
		float dirX = -Mth.sin(yaw * ((float) Math.PI / 180f));
		float dirZ = Mth.cos(yaw * ((float) Math.PI / 180f));

		// Deterministic seed for consistent previews
		long seed = (long) Math.round(centerX) * 31 + (long) Math.round(centerZ) * 17 + (long) baseY * 13;

		// Iterate through the rectangular area
		int startX = (int) Math.floor(centerX - radiusX);
		int endX = (int) Math.ceil(centerX + radiusX);
		int startZ = (int) Math.floor(centerZ - radiusZ);
		int endZ = (int) Math.ceil(centerZ + radiusZ);

		// Use the larger radius for normalization in directional shapes
		float maxRadius = Math.max(radiusX, radiusZ);

		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				// Calculate normalized distance from center using ellipse formula
				float dx = (x - centerX) / radiusX;
				float dz = (z - centerZ) / radiusZ;
				float normalizedDist = Mth.sqrt(dx * dx + dz * dz);

				// Calculate directional offset (how far along the player's facing direction)
				float rawDx = x - centerX;
				float rawDz = z - centerZ;
				float directionalOffset = (rawDx * dirX + rawDz * dirZ) / maxRadius;

				// Calculate perpendicular offset (how far to the side)
				float perpendicularOffset = (-rawDx * dirZ + rawDz * dirX) / maxRadius;

				// Calculate height based on terrain type
				float heightFactor = calculateHeightFactor(terrainType, normalizedDist, dx, dz, 
					directionalOffset, perpendicularOffset, seed, x, z, radiusX, radiusZ);

				// Skip if outside the shape or no height
				if (heightFactor <= 0) continue;

				int heightAtPos = Math.round(maxHeight * heightFactor);

				// Add natural noise variation
				if (addNoise && heightAtPos > 0 && maxHeight > 2) {
					int noiseValue = calculateNoise(x, z, seed, normalizedDist, terrainType, maxHeight);
					heightAtPos = Math.max(0, heightAtPos + noiseValue);
				}

				// Add blocks
				if (heightAtPos > 0) {
					if (filled) {
						// Filled mode: add all blocks from base to height
						for (int y = 0; y <= heightAtPos; y++) {
							list.add(new BlockPos(x, baseY + y, z));
						}
					} else {
						// Hollow/surface mode: only add the top surface block
						list.add(new BlockPos(x, baseY + heightAtPos, z));
					}
				} else if (heightFactor > 0 && normalizedDist < 0.95f) {
					// Add base layer block for smoother edges
					list.add(new BlockPos(x, baseY, z));
				}
			}
		}

		return list;
	}

	/**
	 * Calculate height factor (0.0 to 1.0) based on terrain type.
	 * Uses directionalOffset for shapes that face the player's direction.
	 */
	private static float calculateHeightFactor(ModeOptions.ActionEnum terrainType, float normalizedDist,
	                                           float dx, float dz, float directionalOffset, float perpendicularOffset,
	                                           long seed, int x, int z,
	                                           float radiusX, float radiusZ) {
		switch (terrainType) {
			case TERRAIN_MOUND:
				return calculateMoundHeight(normalizedDist, seed, x, z);
			case TERRAIN_SLOPE:
				return calculateSlopeHeight(directionalOffset, perpendicularOffset, normalizedDist);
			case TERRAIN_FLAT:
				return calculateFlatHeight(normalizedDist, seed, x, z);
			case TERRAIN_MOUNTAIN:
				return calculateMountainHeight(normalizedDist, seed, x, z);
			case TERRAIN_WALL:
				return calculateWallHeight(directionalOffset, perpendicularOffset, normalizedDist);
			default:
				return calculateMoundHeight(normalizedDist, seed, x, z);
		}
	}

	/**
	 * MOUND: Natural Minecraft-style hill.
	 * Uses stepped terracing like vanilla terrain with gentle slopes.
	 */
	private static float calculateMoundHeight(float normalizedDist, long seed, int x, int z) {
		if (normalizedDist > 1.0f) return 0;

		// Use a power curve for more natural hill shape (steeper near edges, flatter at top)
		float baseHeight = 1.0f - (normalizedDist * normalizedDist);

		// Add subtle natural variation to break up the perfect circle
		long posHash = (x * 12345L) ^ (z * 67890L) ^ seed;
		float variation = ((posHash & 0xFF) / 255f - 0.5f) * 0.15f;

		// Apply variation more in the middle, less at edges
		float result = baseHeight + variation * (1.0f - normalizedDist);

		return Mth.clamp(result, 0, 1);
	}

	/**
	 * SLOPE: Ramp that faces the player's looking direction.
	 * High at back, low at front (where player is looking).
	 */
	private static float calculateSlopeHeight(float directionalOffset, float perpendicularOffset, float normalizedDist) {
		if (normalizedDist > 1.0f) return 0;

		// Gradient based on player facing direction
		// directionalOffset: -1 = behind player, +1 = in front of player
		// We want high at back (-1), low at front (+1)
		float gradient = (1.0f - directionalOffset) / 2.0f;
		gradient = Mth.clamp(gradient, 0, 1);

		// Taper the sides for natural look
		float sideTaper = 1.0f - Math.abs(perpendicularOffset);
		sideTaper = Mth.clamp(sideTaper, 0, 1);
		sideTaper = sideTaper * sideTaper; // Smooth the taper

		// Edge falloff
		float edgeFalloff = normalizedDist > 0.85f ? (1.0f - normalizedDist) / 0.15f : 1.0f;

		return gradient * sideTaper * edgeFalloff;
	}

	/**
	 * FLAT: Natural plateau with irregular Minecraft-style edges.
	 * Flat top with natural edge variations.
	 */
	private static float calculateFlatHeight(float normalizedDist, long seed, int x, int z) {
		if (normalizedDist > 1.0f) return 0;

		// Add variation to the edge threshold for irregular boundaries
		long edgeHash = (x * 31337L) ^ (z * 73856L) ^ seed;
		float edgeVariation = ((edgeHash & 0xFF) / 255f) * 0.25f;
		float effectiveEdge = 0.55f + edgeVariation;

		if (normalizedDist < effectiveEdge) {
			// Flat plateau center with tiny variations
			long plateauHash = (x * 98765L) ^ (z * 43210L) ^ seed;
			float plateauVariation = ((plateauHash & 0x3F) / 63f - 0.5f) * 0.05f;
			return Mth.clamp(1.0f + plateauVariation, 0.95f, 1.0f);
		} else {
			// Stepped edge transition (more Minecraft-like)
			float edgeDist = (normalizedDist - effectiveEdge) / (1.0f - effectiveEdge);

			// Create 2-3 steps instead of smooth gradient
			float stepped = 1.0f - edgeDist;
			stepped = (float) Math.floor(stepped * 3) / 3f;

			return Mth.clamp(stepped, 0, 1);
		}
	}

	/**
	 * MOUNTAIN: Peaked terrain with Minecraft-style terracing.
	 * Sharp peak, stepped sides like extreme hills biome.
	 */
	private static float calculateMountainHeight(float normalizedDist, long seed, int x, int z) {
		if (normalizedDist > 1.0f) return 0;

		// Sharp peak that drops off quickly
		float baseHeight = (float) Math.pow(1.0f - normalizedDist, 1.5);

		// Add terracing/stepping for Minecraft aesthetic
		// More steps = more natural looking
		long terraceHash = (x * 48271L) ^ (z * 22695477L) ^ seed;
		float terraceOffset = ((terraceHash & 0xFF) / 255f) * 0.1f;

		// Create subtle terraces
		float terraced = baseHeight + terraceOffset;
		int numSteps = 6;
		terraced = (float) Math.floor(terraced * numSteps + 0.5f) / numSteps;

		// Add ridge-like features
		long ridgeHash = ((x + z) * 13579L) ^ seed;
		float ridgeInfluence = ((ridgeHash & 0x1FF) / 511f - 0.5f) * 0.15f;

		// Apply ridge more strongly in middle heights
		float ridgeWeight = 4f * baseHeight * (1f - baseHeight);
		terraced += ridgeInfluence * ridgeWeight;

		// Edge smoothing
		if (normalizedDist > 0.9f) {
			terraced *= (1.0f - normalizedDist) / 0.1f;
		}

		return Mth.clamp(terraced, 0, 1);
	}

	/**
	 * WALL: Cliff face that faces the player's looking direction.
	 * Steep cliff in front, gentle back slope.
	 */
	private static float calculateWallHeight(float directionalOffset, float perpendicularOffset, float normalizedDist) {
		if (normalizedDist > 1.0f) return 0;

		// directionalOffset: -1 = behind player, +1 = in front (where player looks)
		// Wall should be steep facing the player, gentle slope behind
		float wallFactor;

		if (directionalOffset > 0) {
			// Front side (where player is looking) - steep cliff
			wallFactor = 1.0f;
		} else {
			// Back side - gentle slope going down
			float backSlope = 1.0f + directionalOffset; // 1.0 at center, 0.0 at back edge
			wallFactor = backSlope * backSlope; // Smooth curve
		}

		// Taper the sides for natural cliff ends
		float sideTaper = 1.0f - (perpendicularOffset * perpendicularOffset);
		sideTaper = Mth.clamp(sideTaper, 0, 1);

		// Edge falloff
		float edgeFalloff = normalizedDist > 0.85f ? (1.0f - normalizedDist) / 0.15f : 1.0f;

		return wallFactor * sideTaper * edgeFalloff;
	}

	/**
	 * Calculate noise value for natural variation based on terrain type.
	 * Produces Minecraft-style stepped variations.
	 */
	private static int calculateNoise(int x, int z, long seed, float normalizedDist, ModeOptions.ActionEnum terrainType, int maxHeight) {
		// Position-based deterministic noise
		long posHash = (x * 73856093L) ^ (z * 19349663L) ^ seed;
		int baseNoise = (int) ((posHash & 0xFFFF) % 3) - 1; // -1 to +1 (smaller range)

		// Scale based on terrain type and size
		float noiseScale = (1.0f - normalizedDist * 0.5f);

		// Adjust noise intensity based on terrain type
		switch (terrainType) {
			case TERRAIN_MOUNTAIN:
				// More noise for jagged peaks
				baseNoise = (int) ((posHash & 0xFFFF) % 5) - 2; // -2 to +2
				noiseScale *= 1.2f;
				break;
			case TERRAIN_FLAT:
				// Minimal noise for plateaus
				noiseScale *= 0.3f;
				break;
			case TERRAIN_WALL:
				// Moderate noise for cliff texture
				noiseScale *= 0.6f;
				break;
			case TERRAIN_MOUND:
			case TERRAIN_SLOPE:
			default:
				// Standard noise
				noiseScale *= 0.5f;
				break;
		}

		// Scale noise to be proportional to terrain height
		if (maxHeight > 10) {
			noiseScale *= (maxHeight / 10f);
		}

		return Math.round(baseNoise * noiseScale);
	}

	@Override
	protected BlockPos findSecondPos(Player player, BlockPos firstPos, boolean skipRaytrace) {
		// Second click defines the radius on the floor plane
		return Floor.findFloor(player, firstPos, skipRaytrace);
	}

	@Override
	protected BlockPos findThirdPos(Player player, BlockPos firstPos, BlockPos secondPos, boolean skipRaytrace) {
		// Third click defines the height
		return findHeight(player, secondPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getIntermediateBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
		// Show a circle preview while dragging for radius (uses same CIRCLE_START logic)
		return Circle.getCircleBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		return getTerrainBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3);
	}
}
