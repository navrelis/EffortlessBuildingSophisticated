package sophisticated.building.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class BlockPreviews {
	// Limit max concurrent animations to prevent memory issues with rapid building
	private static final int MAX_PLACED_BLOCKS_ENTRIES = 50;
	private final List<PlacedBlocksEntry> placedBlocksList = new ArrayList<>();
	private boolean miniBlockPreviewEnabled = true;
	private boolean miniBlockPreviewInitialized;
	
	// Cache for coordinates to avoid recreating HashSet every frame
	private HashSet<BlockPos> coordinatesCache;
	private int lastBlocksSize = -1;

	public void onTick() {
		var player = Minecraft.getInstance().player;

		if (!miniBlockPreviewInitialized) {
			try {
				miniBlockPreviewEnabled = ClientConfig.visuals.showMiniBlockPreview.get();
				miniBlockPreviewInitialized = true;
			} catch (IllegalStateException ignored) {
				// Config not loaded yet; keep default and retry next tick
			}
		}

		if (!CompatHelper.isCatnipLoaded()) {
			return;
		}

		drawPlacedBlocks();
		drawLookAtPreview(player);
		drawOutlineAtBreakPosition(player);
	}

	public void drawPlacedBlocks() {
		//Render placed blocks with appear animation
		if (ClientConfig.visuals.showBlockPreviews.get()) {
			for (PlacedBlocksEntry placed : placedBlocksList) {

				int totalTime = placed.breaking ? ClientConfig.visuals.breakAnimationLength.get() : ClientConfig.visuals.appearAnimationLength.get();
				if (totalTime <= 0) continue;

				float dissolve = (ClientEvents.ticksInGame - placed.time) / (float) totalTime;
				renderBlockPreviews(placed.blocks, placed.breaking, dissolve);
			}
		}

		//Expire
		placedBlocksList.removeIf(placed -> {
			int totalTime = placed.breaking ? ClientConfig.visuals.breakAnimationLength.get() : ClientConfig.visuals.appearAnimationLength.get();
			return placed.time + totalTime < ClientEvents.ticksInGame;
		});
	}

	public void drawLookAtPreview(Player player) {
		var blocks = SophisticatedBuildingClient.BUILDER_CHAIN.getBlocks();
		if (blocks.size() == 0) return;
		
		if (SophisticatedBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED &&
			blocks.size() == 1) return;
		if (SophisticatedBuildingClient.BUILDER_CHAIN.getBuildingState() == BuilderChain.BuildingState.IDLE &&
			ClientConfig.visuals.onlyShowBlockPreviewsWhenBuilding.get() &&
			blocks.size() == 1) return;

		// Performance optimization: skip getCoordinates() call if we're over the limit
		// getCoordinates() creates a new HashSet which is expensive for large block sets
		int blockCount = blocks.size();
		int maxPreviews = ClientConfig.visuals.maxBlockPreviews.get();
		
		var state = SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState();

		//Dont fade out the outline if we are still determining where to place
		//Every outline with same ID will not fade out (because it gets replaced)
		Object outlineID = "single";
		if (blockCount > 1) outlineID = blocks.firstPos;

		if (state != BuilderChain.BuildingState.BREAKING) {
			//Use fancy shader if config allows, otherwise outlines
			if (ClientConfig.visuals.showBlockPreviews.get() && blockCount < maxPreviews) {
				// Render block previews inside each ghost section to show rotation
				if (miniBlockPreviewEnabled) {
					renderBlockPreviews(blocks, false, 0f);
				}

				var coordinates = blocks.getCoordinates();
				CatnipRenderHelper.showCluster(outlineID, coordinates, "checkered", 
						1 / 32f, 1f, 1f, 1f, 1f);
			} else {
				//Thicker outline without block previews - still need coordinates for outline
				var coordinates = blocks.getCoordinates();
				CatnipRenderHelper.showCluster(outlineID, coordinates, "highlight_checkered",
						1 / 16f, 1f, 1f, 1f, 1f);
			}

		} else {
			//Breaking
			var coordinates = blocks.getCoordinates();
			CatnipRenderHelper.showCluster(outlineID, coordinates, "thin_checkered",
					1 / 16f, 0.8f, 0.1f, 0.1f, 1f);
		}

		//Display block count and dimensions in actionbar (always show dimensions)
		if (state != BuilderChain.BuildingState.IDLE) {
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
			for (BlockEntry entry : blocks) {
				BlockPos pos = entry.blockPos;
				if (pos.getX() < minX) minX = pos.getX();
				if (pos.getX() > maxX) maxX = pos.getX();
				if (pos.getY() < minY) minY = pos.getY();
				if (pos.getY() > maxY) maxY = pos.getY();
				if (pos.getZ() < minZ) minZ = pos.getZ();
				if (pos.getZ() > maxZ) maxZ = pos.getZ();
			}
			BlockPos dim = new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
			String dimensions = "(" + dim.getX() + "x" + dim.getZ() + "x" + dim.getY() + ")";
			String msg = blockCount + " blocks " + dimensions;
			SophisticatedBuilding.log(player, msg, true);
		}
	}

	public void drawOutlineAtBreakPosition(Player player) {
		if (SophisticatedBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED) return;

		BuilderChain builderChain = SophisticatedBuildingClient.BUILDER_CHAIN;
		BlockPos pos = builderChain.getStartPosForBreaking();
		if (pos == null) return;

		var abilitiesState = builderChain.getAbilitiesState();
		if (ClientConfig.visuals.onlyShowBlockPreviewsWhenBuilding.get()) {
			if (abilitiesState == BuilderChain.AbilitiesState.NONE) return;
		} else {
			if (abilitiesState != BuilderChain.AbilitiesState.CAN_BREAK) return;
		}

		//Only render if further than normal reach
		if (SophisticatedBuildingClient.BUILDER_CHAIN.getLookingAtNear() != null) return;

		AABB aabb = new AABB(pos);
		if (player.level().isLoaded(pos)) {
			var blockState = player.level().getBlockState(pos);
			if (!blockState.isAir()) {
				aabb = blockState.getShape(player.level(), pos).bounds().move(pos);
			}
		}

		CatnipRenderHelper.showAABB("break", aabb, 1 / 64f, 0x222222);
	}

	/**
	 * Render mini transparent block previews inside each ghost section.
	 */
	protected void renderMiniBlockPreviews(BlockSet blocks, boolean breaking) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		
		// Early exit if too many blocks
		int blockCount = blocks.size();
		if (cachedMaxMiniPreviews > 0 && blockCount > cachedMaxMiniPreviews) return;
		
		// Update cached config
		updateCachedConfig();
		
		Vec3 playerPos = player.position();
		
		float miniScale = 0.5f; // Half size to fit inside the outline
		float miniAlpha = 0.7f;
		
		for (BlockEntry blockEntry : blocks) {
			if (blockEntry.newBlockState == null || blockEntry.newBlockState.isAir()) continue;
			
			BlockPos pos = blockEntry.blockPos;
			
			// Skip blocks too far away for visual detail
			double distSq = playerPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			if (distSq > cachedMaxRenderDistSq) continue;
			
			// Use same slot format as effortless-building: blockPos.toShortString()
			CatnipRenderHelper.showGhostBlock(pos.toShortString(), blockEntry.newBlockState, 
					pos, miniScale, miniAlpha, blockEntry.invalid || breaking);
		}
	}

	// Cached config values to avoid repeated config lookups during rendering
	private int cachedMaxRenderDist = 64;
	private double cachedMaxRenderDistSq = 64 * 64;
	private int cachedMaxMiniPreviews = 4096; // Default to 4096 mini previews max
	private long lastConfigCheck = 0;
	private static final long CONFIG_CHECK_INTERVAL = 1000; // Check config every 1 second

	private void updateCachedConfig() {
		long now = System.currentTimeMillis();
		if (now - lastConfigCheck > CONFIG_CHECK_INTERVAL) {
			lastConfigCheck = now;
			try {
				cachedMaxRenderDist = ClientConfig.performance.previewRenderDistance.get();
				cachedMaxRenderDistSq = cachedMaxRenderDist * cachedMaxRenderDist;
				cachedMaxMiniPreviews = ClientConfig.performance.maxMiniBlockPreviews.get();
			} catch (IllegalStateException ignored) {
				// Config not loaded yet
			}
		}
	}

	protected void renderBlockPreviews(BlockSet blocks, boolean breaking, float dissolve) {
		// Get player position for distance culling
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		
		// Early exit if too many blocks (performance protection)
		int blockCount = blocks.size();
		if (cachedMaxMiniPreviews > 0 && blockCount > cachedMaxMiniPreviews) return;
		
		// Update cached config values periodically
		updateCachedConfig();
		
		Vec3 playerPos = player.position();
		
		for (BlockEntry blockEntry : blocks) {
			// Skip blocks too far away for visual detail
			double distSq = playerPos.distanceToSqr(blockEntry.blockPos.getX() + 0.5, 
					blockEntry.blockPos.getY() + 0.5, blockEntry.blockPos.getZ() + 0.5);
			if (distSq > cachedMaxRenderDistSq) continue;
			
			renderBlockPreview(blockEntry, breaking, dissolve, blocks.firstPos, blocks.lastPos);
		}
	}

	protected void renderBlockPreview(BlockEntry blockEntry, boolean breaking, float dissolve, BlockPos firstPos, BlockPos lastPos) {
		// Early null check
		if (blockEntry.newBlockState == null) return;
		
		// Skip air blocks - they don't need preview rendering
		if (blockEntry.newBlockState.isAir()) return;

		var blockPos = blockEntry.blockPos;
		var blockState = blockEntry.newBlockState;

		float baseScale = ClientConfig.visuals.previewScale.get().floatValue();
		float scale = baseScale;
		float alpha = 0.7f;
		if (dissolve > 0f) {
			float animationLength = 0.8f;

			double firstToSecond = lastPos.distSqr(firstPos);
			double place = 0;
			if (firstToSecond > 0.5) {
				double placeFromFirst = firstPos.distSqr(blockPos) / firstToSecond;
				double placeFromSecond = lastPos.distSqr(blockPos) / firstToSecond;
				place = (placeFromFirst + (1.0 - placeFromSecond)) / 2.0;
			} //else only one block

			//Scale place so we start first animation at 0 and end last animation at 1
			place *= 1f - animationLength;
			float diff = dissolve - (float) place;
			float t = diff / animationLength;
			t = Mth.clamp(t, 0, 1);
			//Now we got a usable t value for this block

//			t = (float) Mth.smoothstep(t);
			t = gain(t, 0.5f);

			if (!breaking) {
				scale = baseScale + (t * 0.3f);
				alpha = 0.7f + (t * 0.3f);
			} else {
				t = 1f - t;
				scale = baseScale + (t * 0.5f);
				alpha = 0.7f + (t * 0.3f);
			}
			alpha = Mth.clamp(alpha, 0, 1);
		}

		// Use blockPos.toShortString() as slot key like effortless-building does
		CatnipRenderHelper.showGhostBlock(blockPos.toShortString(), blockState, 
				blockPos, scale, alpha, blockEntry.invalid);
	}

	//k=1 is the identity curve, k<1 produces the classic gain() shape, and k>1 produces "s" shaped curves. The curves are symmetric (and inverse) for k=a and k=1/a.
	//https://iquilezles.org/articles/functions/
	private float gain(float x, float k)
	{
        float a = (float) (0.5 * Math.pow(2.0 * ((x < 0.5) ? x : 1.0 - x), k));
		return (x < 0.5) ? a : (1.0f - a);
	}

	public void onBlocksPlaced(BlockSet blocks) {
		if (!CompatHelper.isCatnipLoaded()) return;
		if (!ClientConfig.visuals.showBlockPreviews.get()) return;
		if (blocks.size() <= 1 || blocks.size() > ClientConfig.visuals.maxBlockPreviews.get()) return;

		// Limit number of concurrent animations to prevent memory buildup
		if (placedBlocksList.size() >= MAX_PLACED_BLOCKS_ENTRIES) {
			placedBlocksList.remove(0); // Remove oldest entry
		}
		
		placedBlocksList.add(new PlacedBlocksEntry(ClientEvents.ticksInGame, false, new BlockSet(blocks)));

		CatnipRenderHelper.keepOutline(blocks.firstPos, ClientConfig.visuals.appearAnimationLength.get());
	}

	public void onBlocksBroken(BlockSet blocks) {
		if (!CompatHelper.isCatnipLoaded()) return;
		if (!ClientConfig.visuals.showBlockPreviews.get()) return;
		if (blocks.size() <= 1 || blocks.size() > ClientConfig.visuals.maxBlockPreviews.get()) return;

		// Limit number of concurrent animations to prevent memory buildup
		if (placedBlocksList.size() >= MAX_PLACED_BLOCKS_ENTRIES) {
			placedBlocksList.remove(0); // Remove oldest entry
		}
		
		placedBlocksList.add(new PlacedBlocksEntry(ClientEvents.ticksInGame, true, new BlockSet(blocks)));

		CatnipRenderHelper.keepOutline(blocks.firstPos, ClientConfig.visuals.breakAnimationLength.get());
	}

	private void sortOnDistanceToPlayer(List<BlockPos> coordinates, Player player) {

		Collections.sort(coordinates, (lhs, rhs) -> {
			// -1 - less than, 1 - greater than, 0 - equal
			double lhsDistanceToPlayer = Vec3.atLowerCornerOf(lhs).subtract(player.getEyePosition(1f)).lengthSqr();
			double rhsDistanceToPlayer = Vec3.atLowerCornerOf(rhs).subtract(player.getEyePosition(1f)).lengthSqr();
			return (int) Math.signum(lhsDistanceToPlayer - rhsDistanceToPlayer);
		});

	}

	public static class PlacedBlocksEntry {
		float time;
		boolean breaking;
		BlockSet blocks;

		public PlacedBlocksEntry(float time, boolean breaking, BlockSet blocks) {
			this.time = time;
			this.breaking = breaking;
			this.blocks = blocks;
		}
	}

	public boolean toggleMiniBlockPreview() {
		miniBlockPreviewEnabled = !miniBlockPreviewEnabled;
		return miniBlockPreviewEnabled;
	}

	public boolean isMiniBlockPreviewEnabled() {
		return miniBlockPreviewEnabled;
	}
}

