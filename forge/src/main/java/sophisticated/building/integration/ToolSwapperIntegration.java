package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapMode;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.toolswapper.ToolSwapperUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.BreakToolHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates tools carried inside Sophisticated Backpacks that have an enabled Tool Swapper /
 * Advanced Tool Swapper upgrade (mode != NO_SWAP). Imports {@code net.p3pp3rf1y.*}, so every
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

		BackpackScanCompat.forEachBackpack(player, (backpack, invName, slot) -> {
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
			IStorageWrapper wrapper = backpackStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).resolve().orElse(null);
			if (wrapper == null) {
				return;
			}
			ToolSwapperUpgradeWrapper toolSwapper = null;
			for (IUpgradeWrapper slotWrapper : wrapper.getUpgradeHandler().getSlotWrappers().values()) {
				if (slotWrapper instanceof ToolSwapperUpgradeWrapper candidate
						&& candidate.isEnabled()
						&& candidate.getToolSwapMode() != ToolSwapMode.NO_SWAP) {
					toolSwapper = candidate;
					break;
				}
			}
			if (toolSwapper == null) {
				return;
			}

			InventoryHandler inventory = wrapper.getInventoryHandler();
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

	private static boolean matches(ToolSwapperUpgradeWrapper toolSwapper, ItemStack stack) {
		try {
			return toolSwapper.getFilterLogic().matchesFilter(stack);
		} catch (Exception e) {
			return true;
		}
	}
}
