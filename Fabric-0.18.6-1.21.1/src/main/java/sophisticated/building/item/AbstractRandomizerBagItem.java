package sophisticated.building.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler.BagItemStackHandler;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.InventoryHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractRandomizerBagItem extends Item {

	// Per-position cache: maps position hash to selected slot index
	private static final Map<Long, CachedPositionSelection> positionCache = new HashMap<>();
	private static long lastCacheTime = 0;
	private static final int CACHE_DURATION_TICKS = 40; // Cache selection for 40 ticks (2 seconds)
	
	// High-quality random generator for fresh picks - better distribution than Random
	private static final SplittableRandom HIGH_QUALITY_RANDOM = new SplittableRandom();
	// Epoch counter that changes each cache cycle for better position-based randomness
	private static long randomEpoch = System.nanoTime();

	private static class CachedPositionSelection {
		int slotIndex; // Which slot was selected for this position
		long tickTime;

		CachedPositionSelection(int slot, long time) {
			this.slotIndex = slot;
			this.tickTime = time;
		}
	}

	public AbstractRandomizerBagItem() {
		super(new Item.Properties().stacksTo(1));
	}

	public abstract int getInventorySize();

	public abstract MenuProvider getContainerProvider(ItemStack item);

	/**
	 * Get the inventory of a randomizer bag backed by item components.
	 */
	@Nullable
	public IItemHandler getBagInventory(ItemStack bag) {
		if (bag.isEmpty() || bag.getItem() != this) {
			return null;
		}
		return new BagItemStackHandler(bag, getInventorySize());
	}

	/**
	 * Get all unique block types (templates) in the bag.
	 */
	public List<ItemStack> getTemplates(IItemHandler bagInventory) {
		List<ItemStack> templates = new ArrayList<>();
		if (bagInventory == null) return templates;
		
		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack stack = bagInventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				templates.add(stack.copy());
			}
		}
		return templates;
	}

	/**
	 * Pick a random template from the bag (equal weight for each slot).
	 * Returns a fresh random template; uses weighted selection for Omega bags.
	 */
	public ItemStack pickRandomTemplateFresh(IItemHandler bagInventory, @Nullable Player player) {
		if (bagInventory == null) return ItemStack.EMPTY;

		// Build list of non-empty slots that player has in inventory
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack stack = bagInventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				// Only include if player has this item (or player is null/creative)
				if (player == null || player.isCreative() || 
					InventoryHelper.findTotalItemsInInventory(player, stack.getItem()) > 0) {
					slots.add(i);
				}
			}
		}

		if (slots.isEmpty()) return ItemStack.EMPTY;

		// Use weighted selection for Omega bags
		if (this instanceof OmegaRandomizerBagItem && player != null) {
			ItemStack bagStack = player.getItemInHand(InteractionHand.MAIN_HAND);
			if (bagStack.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
				int selectedSlot = pickWeightedSlot(slots, omegaBag, bagStack);
				return bagInventory.getStackInSlot(selectedSlot).copy();
			}
		}

		// Default: equal weight random selection using high-quality random
		int randomIndex = HIGH_QUALITY_RANDOM.nextInt(slots.size());
		return bagInventory.getStackInSlot(slots.get(randomIndex)).copy();
	}

	/**
	 * Pick a random template from the bag with per-position caching for preview.
	 * Returns position-stable preview selection (cached, weighted for Omega).
	 */
	public ItemStack pickRandomTemplateForPosition(IItemHandler bagInventory, BlockPos pos, @Nullable Player player) {
		if (bagInventory == null) return ItemStack.EMPTY;

		// Build list of available slots (templates that player has in inventory)
		List<Integer> availableSlots = new ArrayList<>();
		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack stack = bagInventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				// Only include if player has this item (or player is null/creative)
				if (player == null || player.isCreative() || 
					InventoryHelper.findTotalItemsInInventory(player, stack.getItem()) > 0) {
					availableSlots.add(i);
				}
			}
		}

		if (availableSlots.isEmpty()) return ItemStack.EMPTY;

		long currentTime = System.currentTimeMillis() / 50; // Convert to tick-like time
		
		// Clear cache if time period changed and regenerate epoch for new randomness
		if (currentTime - lastCacheTime >= CACHE_DURATION_TICKS) {
			positionCache.clear();
			lastCacheTime = currentTime;
			// Use high-quality random to generate new epoch for next cycle
			randomEpoch = HIGH_QUALITY_RANDOM.nextLong();
		}

		// Use position as cache key
		long posKey = pos.asLong();
		CachedPositionSelection cached = positionCache.get(posKey);
		
		if (cached != null) {
			// Check if cached slot is still valid (available in inventory)
			if (availableSlots.contains(cached.slotIndex)) {
				return bagInventory.getStackInSlot(cached.slotIndex).copy();
			}
			// Cached slot no longer available, need to pick new one
		}

		// Pick new random slot using position-based seed with proper mixing
		// Use SplittableRandom for better distribution with position-based seeding
		int selectedSlot;
		
		// Better seed mixing: combine position with epoch using murmur-like mixing
		long seed = mixSeed(posKey, randomEpoch);
		SplittableRandom posRandom = new SplittableRandom(seed);
		
		// Use weighted selection for Omega bags
		if (this instanceof OmegaRandomizerBagItem && player != null) {
			ItemStack bagStack = player.getItemInHand(InteractionHand.MAIN_HAND);
			if (bagStack.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
				selectedSlot = pickWeightedSlotWithSplittableRandom(availableSlots, omegaBag, bagStack, posRandom);
			} else {
				// Fallback to equal weight
				int randomIndex = posRandom.nextInt(availableSlots.size());
				selectedSlot = availableSlots.get(randomIndex);
			}
		} else {
			// Default: equal weight with position seed
			int randomIndex = posRandom.nextInt(availableSlots.size());
			selectedSlot = availableSlots.get(randomIndex);
		}
		
		// Cache the selection
		positionCache.put(posKey, new CachedPositionSelection(selectedSlot, currentTime));
		
		return bagInventory.getStackInSlot(selectedSlot).copy();
	}
	
	/**
	 * Mix two long values together for better seed quality.
	 * Based on MurmurHash3 finalization mixing.
	 */
	private static long mixSeed(long a, long b) {
		long h = a ^ b;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h;
	}

	/**
	 * Pick a random template from the bag (equal weight for each slot).
	 * Templates are never consumed - they just define what blocks can be used.
	 * Only picks from templates available in player inventory.
	 */
	public ItemStack pickRandomTemplate(IItemHandler bagInventory, @Nullable Player player) {
		return pickRandomTemplateFresh(bagInventory, player);
	}

	/**
	 * Pick a random template with per-position caching for preview (exposed for CompatHelper).
	 * Each position gets a different but stable random selection.
	 */
	public ItemStack pickRandomStackCachedForPosition(IItemHandler bagInventory, BlockPos pos, @Nullable Player player) {
		return pickRandomTemplateForPosition(bagInventory, pos, player);
	}

	/**
	 * Legacy method - picks random without position context. Use pickRandomStackCachedForPosition for preview.
	 */
	public ItemStack pickRandomStackCached(IItemHandler bagInventory, ItemStack bagItem, @Nullable Player player) {
		// Without position context, just pick fresh
		return pickRandomTemplateFresh(bagInventory, player);
	}

	/**
	 * Pick a random template and consume one matching item from player's inventory.
	 * Returns the item that was consumed, or empty if not available.
	 */
	public ItemStack pickAndConsumeFromInventory(IItemHandler bagInventory, Player player) {
		if (bagInventory == null) return ItemStack.EMPTY;

		// Build list of available templates that the player has in inventory
		List<ItemStack> availableTemplates = new ArrayList<>();
		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack template = bagInventory.getStackInSlot(i);
			if (!template.isEmpty()) {
				// Check if player has this item in inventory
				if (player.isCreative() || InventoryHelper.findTotalItemsInInventory(player, template.getItem()) > 0) {
					availableTemplates.add(template);
				}
			}
		}

		if (availableTemplates.isEmpty()) {
			// No available blocks - notify player
			if (!player.level().isClientSide) {
				player.displayClientMessage(
					Component.literal("Missing blocks in inventory for randomizer bag!").withStyle(ChatFormatting.RED),
					true
				);
			}
			return ItemStack.EMPTY;
		}

		// Pick random available template
		int randomIndex = ThreadLocalRandom.current().nextInt(availableTemplates.size());
		ItemStack template = availableTemplates.get(randomIndex);

		// Consume from player inventory (unless creative)
		if (!player.isCreative()) {
			InventoryHelper.removeFromInventory(player, template.getItem(), 1);
		}

		return template.copy();
	}

	/**
	 * Get items that are in the bag but missing from player inventory.
	 */
	public Set<Item> getMissingItems(IItemHandler bagInventory, Player player) {
		Set<Item> missing = new HashSet<>();
		if (bagInventory == null || player.isCreative()) return missing;

		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack template = bagInventory.getStackInSlot(i);
			if (!template.isEmpty()) {
				if (InventoryHelper.findTotalItemsInInventory(player, template.getItem()) <= 0) {
					missing.add(template.getItem());
				}
			}
		}
		return missing;
	}

	public ItemStack findStack(IItemHandler bagInventory, Item item) {
		for (int i = 0; i < bagInventory.getSlots(); i++) {
			ItemStack stack = bagInventory.getStackInSlot(i);
			if (!stack.isEmpty() && stack.getItem() == item) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction facing = ctx.getClickedFace();
		ItemStack item = ctx.getItemInHand();
		Vec3 hitVec = ctx.getClickLocation();

		if (player == null) return InteractionResult.FAIL;

		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) { //ctx.isPlacerSneaking()
			if (world.isClientSide) return InteractionResult.SUCCESS;
			//Open inventory
			player.openMenu(getContainerProvider(item));
		} else {
			if (world.isClientSide) return InteractionResult.SUCCESS;

			//---Only place manually if in normal vanilla mode---
			if (!ServerBuildState.isLikeVanilla(player)) {
				return InteractionResult.FAIL;
			}

			//Use item
			//Get bag inventory
			ItemStack bag = ctx.getItemInHand();
			IItemHandler bagInventory = getBagInventory(bag);
			if (bagInventory == null)
				return InteractionResult.FAIL;

			// Pick random template and consume from player inventory
			ItemStack toPlace = pickAndConsumeFromInventory(bagInventory, player);
			if (toPlace.isEmpty()) return InteractionResult.FAIL;

			BlockPlaceContext blockItemUseContext = new BlockPlaceContext(new UseOnContext(player, ctx.getHand(), new BlockHitResult(hitVec, facing, pos, false)));
			if (!world.getBlockState(pos).canBeReplaced(blockItemUseContext)) {
				pos = pos.relative(facing);
			}

			BlockState blockState = Block.byItem(toPlace.getItem()).getStateForPlacement(blockItemUseContext);

			var blockEntry = new BlockEntry(pos, blockState, toPlace.getItem());
			var blockSet = new BlockSet(List.of(blockEntry), pos, pos, false);
			SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, blockSet);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack bag = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			if (world.isClientSide) return new InteractionResultHolder<>(InteractionResult.SUCCESS, bag);
			//Open inventory
			player.openMenu(getContainerProvider(bag));
		} else {
			//Use item
			//Get bag inventory
			IItemHandler bagInventory = getBagInventory(bag);
			if (bagInventory == null)
				return new InteractionResultHolder<>(InteractionResult.FAIL, bag);

			ItemStack toUse = pickRandomTemplate(bagInventory, player);
			if (toUse.isEmpty()) return new InteractionResultHolder<>(InteractionResult.FAIL, bag);

			return toUse.use(world, player, hand);
		}
		return new InteractionResultHolder<>(InteractionResult.PASS, bag);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 1;
	}
	//	@Nullable
//	@Override
//	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
//		return new ItemHandlerCapabilityProvider(getInventorySize()); TODO: Re-enable itemhandler cap on randomizer bag item!
//	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		tooltip.add(Component.literal(ChatFormatting.GRAY + "Put blocks in bag as " + ChatFormatting.YELLOW + "templates"));
		tooltip.add(Component.literal(ChatFormatting.GRAY + "Blocks are consumed from " + ChatFormatting.GREEN + "inventory"));
		tooltip.add(Component.empty());
		tooltip.add(Component.literal(ChatFormatting.BLUE + "Rightclick" + ChatFormatting.GRAY + " to place a random block"));
		tooltip.add(Component.literal(ChatFormatting.BLUE + "Sneak + rightclick" + ChatFormatting.GRAY + " to open inventory"));
		
		// Add special tooltip for Omega bag
		if (this instanceof OmegaRandomizerBagItem) {
			tooltip.add(Component.empty());
			tooltip.add(Component.literal(ChatFormatting.GOLD + "Scroll wheel" + ChatFormatting.GRAY + " on slots to adjust weight"));
		}
	}
	
	/**
	 * Pick a weighted random slot from available slots using slot weights.
	 * Uses SplittableRandom for fresh randomization with better distribution.
	 */
	private int pickWeightedSlot(List<Integer> availableSlots, OmegaRandomizerBagItem omegaBag, ItemStack bagStack) {
		return pickWeightedSlotWithSplittableRandom(availableSlots, omegaBag, bagStack, HIGH_QUALITY_RANDOM);
	}
	
	/**
	 * Pick a weighted random slot from available slots using slot weights.
	 * Uses provided Random instance for position-based consistency.
	 * @deprecated Use pickWeightedSlotWithSplittableRandom for better distribution
	 */
	@Deprecated
	private int pickWeightedSlotWithRandom(List<Integer> availableSlots, OmegaRandomizerBagItem omegaBag, ItemStack bagStack, Random random) {
		// Calculate total weight
		int totalWeight = 0;
		for (int slot : availableSlots) {
			totalWeight += omegaBag.getSlotWeight(bagStack, slot);
		}
		
		if (totalWeight <= 0) {
			// Fallback to equal weight if no weights set
			return availableSlots.get(random.nextInt(availableSlots.size()));
		}
		
		// Pick random number between 0 and totalWeight
		int randomValue = random.nextInt(totalWeight);
		
		// Find which slot this falls into
		int cumulativeWeight = 0;
		for (int slot : availableSlots) {
			cumulativeWeight += omegaBag.getSlotWeight(bagStack, slot);
			if (randomValue < cumulativeWeight) {
				return slot;
			}
		}
		
		// Fallback (should never reach here)
		return availableSlots.get(availableSlots.size() - 1);
	}
	
	/**
	 * Pick a weighted random slot from available slots using slot weights.
	 * Uses SplittableRandom for better distribution.
	 */
	private int pickWeightedSlotWithSplittableRandom(List<Integer> availableSlots, OmegaRandomizerBagItem omegaBag, ItemStack bagStack, SplittableRandom random) {
		// Calculate total weight
		int totalWeight = 0;
		for (int slot : availableSlots) {
			totalWeight += omegaBag.getSlotWeight(bagStack, slot);
		}
		
		if (totalWeight <= 0) {
			// Fallback to equal weight if no weights set
			return availableSlots.get(random.nextInt(availableSlots.size()));
		}
		
		// Pick random number between 0 and totalWeight
		int randomValue = random.nextInt(totalWeight);
		
		// Find which slot this falls into
		int cumulativeWeight = 0;
		for (int slot : availableSlots) {
			cumulativeWeight += omegaBag.getSlotWeight(bagStack, slot);
			if (randomValue < cumulativeWeight) {
				return slot;
			}
		}
		
		// Fallback (should never reach here)
		return availableSlots.get(availableSlots.size() - 1);
	}
}
