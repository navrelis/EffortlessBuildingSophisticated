package sophisticated.building.utilities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.client.ClientBackpackItemCache;

import java.util.Map;

public class InventoryHelper {

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
	 * The tier limit determines IF backpacks can be used, not HOW MANY items.
	 * Returns total count without clamping.
	 * @param player The player
	 * @param item The item to count
	 * @return Total extractable count (not clamped - tier only gates access)
	 */
	public static int findTotalItemsInInventory(Player player, Item item) {
		int total = 0;
		for (ItemStack invStack : player.getInventory().items) {
			if (!invStack.isEmpty() && invStack.getItem().equals(item)) {
				total += invStack.getCount();
			}
		}
		// Backpack items are NOT clamped - tier only determines IF upgrade works
		// If player has a valid building upgrade, they can extract ALL items from backpack
		if (CompatHelper.isSophisticatedBackpacksLoaded()) {
			try {
				// Check if player has a valid building upgrade at all
				int maxFromUpgrade = sophisticated.building.item.upgrade.BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(
						player, new ItemStack(item));
				if (maxFromUpgrade > 0) {
					// Upgrade is valid, count ALL items in backpack (no cap)
					int backpackCount = sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpacksForDisplay(
							player, new ItemStack(item));
					total += backpackCount;
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

		// Prefer backpacks first so building upgrades are consumed before player inventory
		if (CompatHelper.isSophisticatedBackpacksLoaded()) {
			amountFound += removeFromBackpacks(player, item, amount - amountFound);
		}

		// Then held Item
		if (amountFound < amount) {
			int preferredSlot = player.getInventory().selected;
			ItemStack itemstack = player.getInventory().getItem(preferredSlot);
			int count = itemstack.getCount();
			if (itemstack.getItem() == item && count > 0) {
				int taken = Math.min(count, amount - amountFound);
				player.getInventory().setItem(preferredSlot, new ItemStack(itemstack.getItem(), count - taken));
				amountFound += taken;
			}
		}

		// Finally the rest of the inventory
		for (int i = 0; i < player.getInventory().getContainerSize() && amountFound < amount; ++i) {
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

		if (amountFound != amount) {
			SophisticatedBuilding.logError(player.getDisplayName().getString() + " tried to remove " + amount + " " + item + " from inventory but only removed " + amountFound);
		}
	}

	/**
	 * Removes items from backpacks that have building upgrades installed.
	 * The tier limit is used to determine IF the backpack can be used,
	 * but does NOT limit how many items can be extracted.
	 * @return The amount actually removed
	 */
	public static int removeFromBackpacks(Player player, Item item, int amount) {
		if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
			return 0;
		}
		try {
			// Check if player has a building upgrade at all
			int maxFromUpgrade = sophisticated.building.item.upgrade.BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(player, new ItemStack(item));
			if (maxFromUpgrade <= 0) {
				return 0;
			}

			// Extract as many as needed - tier limit doesn't cap extraction,
			// it only determines if the upgrade can be used at all
			ItemStack extracted = sophisticated.building.item.upgrade.BuildingUpgradeHelper.extractBlockFromBackpack(
					player, new ItemStack(item), amount, false);
			int removed = extracted.isEmpty() ? 0 : extracted.getCount();
			if (removed > 0 && player.level().isClientSide()) {
				return removed;
			}

			// Sync new backpack count to client for HUD accuracy
			if (removed > 0 && player.level().getServer() != null) {
				int newCount = sophisticated.building.item.upgrade.BuildingUpgradeHelper.countBlockInBackpack(player, new ItemStack(item));
				var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
				if (key != null) {
					net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player,
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
