package sophisticated.building.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.platform.Services;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-authoritative (but client-callable for the preview) tool selection for survival block
 * breaking. See 08_SURVIVAL_BREAKING_ANALYSIS.md D1/D2/D3 for the design and
 * 09_SURVIVAL_BREAKING_TASKS.md T-S2 for the contract.
 */
public class BreakToolHelper {

	private BreakToolHelper() {
	}

	/** One slot that may hold a candidate tool, abstracting over player inventory vs backpack slots. */
	public interface ToolSlot {
		ItemStack get();

		void set(ItemStack stack);

		boolean isMainHand();

		String describe();
	}

	public static boolean isTool(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.getItem() instanceof DiggerItem || stack.getItem() instanceof ShearsItem) {
			return true;
		}
		return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
	}

	public static List<ToolSlot> collectCandidates(Player player) {
		List<ToolSlot> candidates = new ArrayList<>();

		var inventory = player.getInventory();
		int selected = inventory.selected;

		addPlayerSlot(candidates, inventory, selected, true);
		for (int i = 0; i < 9; i++) {
			if (i == selected) {
				continue;
			}
			addPlayerSlot(candidates, inventory, i, false);
		}
		for (int i = 9; i < 36; i++) {
			addPlayerSlot(candidates, inventory, i, false);
		}
		addOffhandSlot(candidates, player);

		if (!player.level().isClientSide()) {
			if (CompatHelper.isSophisticatedBackpacksLoaded()) {
				try {
					candidates.addAll(Services.backpacks().collectBackpackTools(player));
				} catch (Exception | LinkageError e) {
					SophisticatedBuilding.logger.debug("Error collecting backpack tools: {}", e.getMessage());
				}
			}
		} else {
			List<ItemStack> backpackTools = ClientBackpackToolCache.snapshot();
			for (int i = 0; i < backpackTools.size(); i++) {
				ItemStack stack = backpackTools.get(i);
				if (!isTool(stack)) {
					continue;
				}
				int index = i;
				candidates.add(new ToolSlot() {
					@Override
					public ItemStack get() {
						return backpackTools.get(index);
					}

					@Override
					public void set(ItemStack stack) {
						// read-only on the client; the server is authoritative for the real break.
					}

					@Override
					public boolean isMainHand() {
						return false;
					}

					@Override
					public String describe() {
						return "backpack:" + index;
					}
				});
			}
		}

		return candidates;
	}

	private static void addPlayerSlot(List<ToolSlot> candidates, Inventory inventory, int index, boolean mainHand) {
		ItemStack stack = inventory.getItem(index);
		if (!isTool(stack)) {
			return;
		}
		candidates.add(new ToolSlot() {
			@Override
			public ItemStack get() {
				return inventory.getItem(index);
			}

			@Override
			public void set(ItemStack stack) {
				inventory.setItem(index, stack);
			}

			@Override
			public boolean isMainHand() {
				return mainHand;
			}

			@Override
			public String describe() {
				return "inventory:" + index;
			}
		});
	}

	private static void addOffhandSlot(List<ToolSlot> candidates, Player player) {
		ItemStack stack = player.getOffhandItem();
		if (!isTool(stack)) {
			return;
		}
		var inventory = player.getInventory();
		int offhandIndex = Inventory.SLOT_OFFHAND;
		candidates.add(new ToolSlot() {
			@Override
			public ItemStack get() {
				return player.getOffhandItem();
			}

			@Override
			public void set(ItemStack stack) {
				inventory.setItem(offhandIndex, stack);
			}

			@Override
			public boolean isMainHand() {
				return false;
			}

			@Override
			public String describe() {
				return "offhand";
			}
		});
	}

	public static ToolSelector.Need needFor(Level level, BlockPos pos, BlockState state) {
		float destroySpeed = state.getDestroySpeed(level, pos);
		if (destroySpeed < 0) {
			return ToolSelector.Need.UNBREAKABLE;
		}
		if (destroySpeed == 0) {
			return ToolSelector.Need.NO_TOOL;
		}
		return ToolSelector.Need.TOOL;
	}

	private static final class StackCandidate implements ToolSelector.Candidate {
		private final ItemStack stack;
		private final BlockState state;
		private final boolean mainHand;

		StackCandidate(ItemStack stack, BlockState state, boolean mainHand) {
			this.stack = stack;
			this.state = state;
			this.mainHand = mainHand;
		}

		@Override
		public boolean isEffective() {
			return stack.getDestroySpeed(state) > 1.0F;
		}

		@Override
		public boolean isCorrect() {
			return stack.isCorrectToolForDrops(state);
		}

		@Override
		public int remainingUses() {
			return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
		}

		@Override
		public boolean isMainHand() {
			return mainHand;
		}
	}

	/** Sentinel {@link ToolSlot} meaning "no tool could be selected". */
	private static final ToolSlot NONE = new ToolSlot() {
		@Override
		public ItemStack get() {
			return ItemStack.EMPTY;
		}

		@Override
		public void set(ItemStack stack) {
		}

		@Override
		public boolean isMainHand() {
			return false;
		}

		@Override
		public String describe() {
			return "none";
		}
	};

	/**
	 * @return {@code null} for "use the empty hand", {@link #NONE} when impossible, otherwise the
	 * selected {@link ToolSlot} from {@code candidates}.
	 */
	@Nullable
	public static ToolSlot selectTool(Player player, Level level, BlockPos pos, BlockState state, List<ToolSlot> candidates) {
		ToolSelector.Need need = needFor(level, pos, state);
		boolean requiresCorrectTool = state.requiresCorrectToolForDrops();
		boolean stopBeforeToolBreaks = ServerConfig.survivalBreaking.stopBeforeToolBreaks.get();

		List<ToolSelector.Candidate> adapters = new ArrayList<>(candidates.size());
		for (ToolSlot slot : candidates) {
			adapters.add(new StackCandidate(slot.get(), state, slot.isMainHand()));
		}

		int index = ToolSelector.select(adapters, need, requiresCorrectTool, stopBeforeToolBreaks);
		if (index == -1) {
			return null;
		}
		if (index == -2) {
			return NONE;
		}
		return candidates.get(index);
	}

	public static boolean isImpossible(@Nullable ToolSlot slot) {
		return slot == NONE;
	}

	public static int estimateBreakTicks(Level level, BlockPos pos, BlockState state, ItemStack tool) {
		float hardness = state.getDestroySpeed(level, pos);
		float toolSpeed = tool.isEmpty() ? 1f : tool.getDestroySpeed(state);
		boolean correct = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
		return ToolSelector.estimateBreakTicks(hardness, toolSpeed, correct);
	}

	/** The outcome of planning a client-side breaking preview: which tools will be used how many
	 * times, and how many entries cannot be broken at all. */
	public static final class BreakPlan {
		public final Map<ToolSlot, Integer> usesPerTool = new LinkedHashMap<>();
		public int unbreakable = 0;
		public int delayTicks = 0;
	}

	/**
	 * Plans a survival break of {@code blocks} against copies of the current candidate stacks
	 * (durability is drained per assignment so the preview reflects the real per-tool budget),
	 * flags entries the plan cannot break as {@code invalid}, and returns the resulting
	 * {@link BreakPlan} for the HUD.
	 *
	 * @param skipPos when non-null, the entry at this position is neither planned, counted, nor
	 *                flagged invalid - vanilla handles that block in Disable mode, matching the
	 *                server's own {@code skipFirst} handling in {@code ServerBlockPlacer}.
	 */
	public static BreakPlan planClient(Player player, Iterable<BlockEntry> blocks, @Nullable BlockPos skipPos) {
		BreakPlan plan = new BreakPlan();
		Level level = player.level();

		List<ToolSlot> candidates = collectCandidates(player);
		// Work on copies of the candidate stacks so repeated selection within the same plan drains
		// the "remaining uses" budget without touching the real inventory. index-aligned with
		// `candidates`; wrapped so `selectTool`/StackCandidate see the draining copy, not the slot's
		// live stack.
		List<ItemStack> copies = new ArrayList<>(candidates.size());
		List<ToolSlot> liveView = new ArrayList<>(candidates.size());
		for (int i = 0; i < candidates.size(); i++) {
			ToolSlot original = candidates.get(i);
			copies.add(original.get().copy());
			int index = i;
			liveView.add(new ToolSlot() {
				@Override
				public ItemStack get() {
					return copies.get(index);
				}

				@Override
				public void set(ItemStack stack) {
					// planning only; never writes back to the real slot
				}

				@Override
				public boolean isMainHand() {
					return original.isMainHand();
				}

				@Override
				public String describe() {
					return original.describe();
				}
			});
		}

		int totalTicks = 0;
		for (BlockEntry entry : blocks) {
			if (skipPos != null && entry.blockPos.equals(skipPos)) {
				continue;
			}

			BlockState state = level.getBlockState(entry.blockPos);

			ToolSlot selected = selectTool(player, level, entry.blockPos, state, liveView);
			if (isImpossible(selected)) {
				entry.invalid = true;
				plan.unbreakable++;
				continue;
			}

			ItemStack tool = selected == null ? ItemStack.EMPTY : selected.get();
			totalTicks += estimateBreakTicks(level, entry.blockPos, state, tool);

			if (selected != null) {
				int selectedIndex = liveView.indexOf(selected);
				ItemStack copy = copies.get(selectedIndex);
				if (state.getDestroySpeed(level, entry.blockPos) > 0 && copy.isDamageableItem()) {
					copy.setDamageValue(copy.getDamageValue() + 1);
				}
				plan.usesPerTool.merge(candidates.get(selectedIndex), 1, Integer::sum);
			}
		}

		plan.delayTicks = ToolSelector.capDelay(totalTicks, ServerConfig.survivalBreaking.maxDelayTicks.get());
		return plan;
	}
}
