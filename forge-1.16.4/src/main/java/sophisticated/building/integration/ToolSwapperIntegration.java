package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolType;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperFilterLogic;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IUpgradeWrapper;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.BreakToolHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Enumerates tools carried inside Sophisticated Backpacks that have an enabled Tool Swapper /
 * Advanced Tool Swapper upgrade that swaps tools (Sophisticated Backpacks 1.16.4 has a "swap tools" switch where the
 * 1.16.5 builds have a tool swap mode; this is the only difference to ../forge). Imports {@code net.p3pp3rf1y.*}, so every
 * caller must be guarded by {@code CompatHelper.isSophisticatedBackpacksLoaded()} and a
 * {@code catch (Exception | LinkageError)}, exactly like {@code BuildingUpgradeHelper}.
 * {@code NoSuchMethodError}/{@code NoSuchFieldError} etc. are {@code LinkageError}s, not
 * {@code Exception}s and not {@code NoClassDefFoundError}, so the wider catch is required to
 * degrade instead of crash on any upstream binary-incompatible change, not just a missing class.
 * Server only.
 */
public class ToolSwapperIntegration {

	private ToolSwapperIntegration() {
	}

	public static List<BreakToolHelper.ToolSlot> collectBackpackTools(Player player) {
		List<BreakToolHelper.ToolSlot> tools = new ArrayList<>();

		BackpackScanCompat.forEachBackpack(player, (backpack, invName, identifier, slot) -> {
			collectFromBackpack(backpack, tools);
			return false;
		});

		return tools;
	}

	private static void collectFromBackpack(ItemStack backpackStack, List<BreakToolHelper.ToolSlot> tools) {
		if (backpackStack.isEmpty()) {
			return;
		}

		try {
			IBackpackWrapper wrapper = backpackStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).resolve().orElse(null);
			if (wrapper == null) {
				return;
			}
			ToolSwapperUpgradeWrapper toolSwapper = null;
			for (IUpgradeWrapper slotWrapper : wrapper.getUpgradeHandler().getSlotWrappers().values()) {
				if (slotWrapper instanceof ToolSwapperUpgradeWrapper
						&& ((ToolSwapperUpgradeWrapper) slotWrapper).isEnabled()
						&& ((ToolSwapperUpgradeWrapper) slotWrapper).shouldSwapTools()) {
					toolSwapper = (ToolSwapperUpgradeWrapper) slotWrapper;
					break;
				}
			}
			if (toolSwapper == null) {
				return;
			}

			BackpackInventoryHandler inventory = wrapper.getInventoryHandler();
			int slotCount = inventory.getSlots();
			ToolSwapperUpgradeWrapper finalToolSwapper = toolSwapper;
			for (int i = 0; i < slotCount; i++) {
				ItemStack stack = inventory.getStackInSlot(i);
				if (!BreakToolHelper.isTool(stack)) {
					continue;
				}
				if (!finalToolSwapper.hideSettingsTab() && !matches(finalToolSwapper, stack)) {
					continue;
				}

				int index = i;
				tools.add(new BreakToolHelper.ToolSlot() {
					@Override
					public ItemStack get() {
						return inventory.getStackInSlot(index);
					}

					@Override
					public void set(ItemStack newStack) {
						inventory.setStackInSlot(index, newStack);
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
		} catch (Exception | LinkageError e) {
			SophisticatedBuilding.logger.debug("Error scanning backpack for tool swapper tools: {}", e.getMessage());
		}
	}

	/**
	 * Sophisticated Backpacks 1.16.4's tool swapper has one tool filter per tool type (no single allow/deny list): a
	 * tool passes if the filter of one of its tool types lets it through; tools without a tool type (shears) always pass.
	 */
	private static boolean matches(ToolSwapperUpgradeWrapper toolSwapper, ItemStack stack) {
		try {
			Set<ToolType> toolTypes = stack.getToolTypes();
			if (toolTypes.isEmpty()) {
				return true;
			}
			ToolSwapperFilterLogic filter = toolSwapper.getFilterLogic();
			for (ToolType toolType : toolTypes) {
				if (filter.matchesToolFilter(stack, toolType)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return true;
		}
	}
}
