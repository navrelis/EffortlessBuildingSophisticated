package sophisticated.building.utilities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.client.ClientBackpackItemCache;

import java.util.Map;

public class InventoryHelper {

	private static int getReservedHeldCount(Player player, Item item) {
		if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
			return 0;
		}

		try {
			int maxFromUpgrade = sophisticated.building.item.upgrade.BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(
					player, new ItemStack(item));
			if (maxFromUpgrade <= 0) {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}

		int selectedSlot = player.getInventory().selected;
		ItemStack selectedStack = player.getInventory().getItem(selectedSlot);
		if (!selectedStack.isEmpty() && selectedStack.getItem() == item && selectedStack.getCount() > 0) {
			return 1;
		}

		return 0;
	}

	private static void forceResyncSelectedSlot(Player player, Item item, int selectedSlot) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		ItemStack selectedStack = player.getInventory().getItem(selectedSlot);
		if (!selectedStack.isEmpty() && selectedStack.getItem() == item) {
			player.getInventory().setItem(selectedSlot, selectedStack.copy());
		}

		player.getInventory().setChanged();
		serverPlayer.containerMenu.broadcastChanges();
		if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
			serverPlayer.inventoryMenu.broadcastChanges();
		}
	}

	@Deprecated //Use BlockHelper.findAndRemoveInInventory instead
	public static ItemStack findItemStackInInventory(Player player, Block block) {
		for (ItemStack invStack : player.getInventory().items) {
			if (!invStack.isEmpty() && invStack.getItem() instanceof BlockItem &&
				((BlockItem) invStack.getItem()).getBlock().equals(block)) {
				return invStack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static int findTotalBlocksInInventory(Player player, Block block) {
		int total = 0;
		for (ItemStack invStack : player.getInventory().items) {
			if (!invStack.isEmpty() && invStack.getItem() instanceof BlockItem &&
				((BlockItem) invStack.getItem()).getBlock().equals(block)) {
				total += invStack.getCount();
			}
		}
		return total;
	}

	/**
	 * Finds total items available in inventory plus backpacks for display.
	 * Returns total count without tier clamping for HUD display.
	 * @param player The player
	 * @param item The item to count
	 * @return Total count for display (not clamped)
	 */
	public static int findTotalItemsForDisplay(Player player, Item item) {
		int total = 0;
		for (ItemStack invStack : player.getInventory().items) {
			if (!invStack.isEmpty() && invStack.getItem().equals(item)) {
				total += invStack.getCount();
			}
		}
		// Add backpack items WITHOUT clamping for display
		total += findTotalItemsInBackpacksForDisplay(player, item);
		return total;
	}

	/**
	 * Finds total items available in inventory plus backpacks for extraction.
	 * Backpack contribution is clamped by installed building upgrade tier.
	 * @param player The player
	 * @param item The item to count
	 * @return Total extractable count (inventory + clamped backpack count)
	 */
	public static int findTotalItemsInInventory(Player player, Item item) {
		int total = 0;
		for (ItemStack invStack : player.getInventory().items) {
			if (!invStack.isEmpty() && invStack.getItem().equals(item)) {
				total += invStack.getCount();
			}
		}

		// Keep one held block available as the build anchor when a building upgrade is active.
		int reservedHeld = getReservedHeldCount(player, item);
		if (reservedHeld > 0) {
			total = Math.max(0, total - reservedHeld);
		}

		// Backpack items are clamped by effective upgrade limit so usage checks/hud stay accurate.
		if (CompatHelper.isSophisticatedBackpacksLoaded()) {
			try {
				int maxFromUpgrade = sophisticated.building.item.upgrade.BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(
						player, new ItemStack(item));
				if (maxFromUpgrade > 0) {
					int backpackCount = sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpacksForDisplay(
							player, new ItemStack(item));
					total += Math.min(backpackCount, maxFromUpgrade);
				}
			} catch (Exception e) {
				// SophisticatedBackpacks not loaded or error occurred
			}
		}
		return total;
	}

	/**
	 * Finds total items in backpacks for DISPLAY (no clamping).
	 */
	public static int findTotalItemsInBackpacksForDisplay(Player player, Item item) {
		if (player.level().isClientSide()) {
			return ClientBackpackItemCache.getCount(item);
		}

		if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
			return 0;
		}
		try {
			return sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpacksForDisplay(
					player, new ItemStack(item));
		} catch (Exception e) {
			// SophisticatedBackpacks not loaded or error occurred
			return 0;
		}
	}

	/**
	 * Finds total items available in backpacks that have building upgrades installed.
	 * This integrates with SophisticatedBackpacks when the mod is present.
	 * @deprecated Use findTotalItemsInBackpacksForDisplay for display, or rely on findTotalItemsInInventory for extraction
	 */
	@Deprecated
	public static int findTotalItemsInBackpacks(Player player, Item item) {
		if (player.level().isClientSide()) {
			return ClientBackpackItemCache.getCount(item);
		}

		if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
			return 0;
		}
		try {
			return sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpack(
					player, new ItemStack(item));
		} catch (Exception e) {
			// SophisticatedBackpacks not loaded or error occurred
			return 0;
		}
	}

	public static void removeFromInventory(Player player, Map<Item, Integer> items) {
		for (Item item : items.keySet()) {
			int count = items.get(item);
			removeFromInventory(player, item, count);
		}
	}

	public static void removeFromInventory(Player player, Item item, int amount) {
		if (player.isCreative()) return;

		int amountFound = 0;
		int preferredSlot = player.getInventory().selected;
		int reservedHeld = getReservedHeldCount(player, item);

		// Prefer backpacks first so building upgrades are consumed before player inventory
		if (CompatHelper.isSophisticatedBackpacksLoaded()) {
			amountFound += removeFromBackpacks(player, item, amount - amountFound);
		}

		// Then held Item
		if (amountFound < amount) {
			ItemStack itemstack = player.getInventory().getItem(preferredSlot);
			int count = itemstack.getCount();
			if (itemstack.getItem() == item && count > reservedHeld) {
				int availableFromHeld = count - reservedHeld;
				int taken = Math.min(availableFromHeld, amount - amountFound);
				player.getInventory().setItem(preferredSlot, new ItemStack(itemstack.getItem(), count - taken));
				amountFound += taken;
			}
		}

		// Finally the rest of the inventory
		for (int i = 0; i < player.getInventory().getContainerSize() && amountFound < amount; ++i) {
			if (i == preferredSlot) {
				continue;
			}

			ItemStack itemstack = player.getInventory().getItem(i);
			int count = itemstack.getCount();
			if (itemstack.getItem() == item && count > 0) {
				int taken = Math.min(count, amount - amountFound);
				player.getInventory().setItem(i, new ItemStack(itemstack.getItem(), count - taken));
				amountFound += taken;
			}
		}

		if (amountFound > 0) {
			player.getInventory().setChanged();
		}

		// Always resync selected slot when reserving one held block to eliminate ghost hotbar states.
		if (reservedHeld > 0) {
			forceResyncSelectedSlot(player, item, preferredSlot);
		}

		if (amountFound != amount) {
			SophisticatedBuilding.logError(player.getDisplayName().getString() + " tried to remove " + amount + " " + item + " from inventory but only removed " + amountFound);
		}
	}

	/**
	 * Removes items from backpacks that have building upgrades installed.
	 * Extraction is capped by effective building upgrade limit.
	 * @return The amount actually removed
	 */
	public static int removeFromBackpacks(Player player, Item item, int amount) {
		if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
			return 0;
		}
		try {
			int maxFromUpgrade = sophisticated.building.item.upgrade.BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(player, new ItemStack(item));
			if (maxFromUpgrade <= 0) {
				return 0;
			}

			int toExtract = Math.min(amount, maxFromUpgrade);
			ItemStack extracted = sophisticated.building.item.upgrade.BuildingUpgradeHelper.extractBlockFromBackpack(
					player, new ItemStack(item), toExtract, false);
			int removed = extracted.isEmpty() ? 0 : extracted.getCount();
			if (removed > 0 && player.level().isClientSide()) {
				return removed;
			}

			// Sync new backpack count to client for HUD accuracy
			if (removed > 0 && player instanceof ServerPlayer serverPlayer) {
				int newCount = sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpack(player, new ItemStack(item));
				var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
				if (key != null) {
					ServerPlayNetworking.send(serverPlayer,
						new sophisticated.building.network.message.BackpackItemCountPacket(key, newCount));
				}
			}
			return removed;
		} catch (Exception e) {
			// SophisticatedBackpacks not loaded or error occurred
			return 0;
		}
	}
}
