package sophisticated.building.compatibility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import sophisticated.building.create.foundation.item.ItemHelper;
import sophisticated.building.item.AbstractRandomizerBagItem;

import javax.annotation.Nullable;

public class CompatHelper {

	private static Boolean sophisticatedBackpacksLoaded = null;
	private static Boolean catnipLoaded = null;
	private static Boolean createLoaded = null;

	public static void setup() {

	}

	/**
	 * Returns whether Catnip classes are present at runtime.
	 */
	public static boolean isCatnipLoaded() {
		if (catnipLoaded == null) {
			try {
				// Check if Catnip classes are available
				Class.forName("net.createmod.catnip.theme.Color");
				catnipLoaded = true;
			} catch (ClassNotFoundException e) {
				catnipLoaded = false;
			}
		}
		return catnipLoaded;
	}

	/**
	 * Checks if Create mod is loaded.
	 */
	public static boolean isCreateLoaded() {
		if (createLoaded == null) {
			try {
				var modList = ModList.get();
				if (modList != null) {
					createLoaded = modList.isLoaded("create");
				} else {
					createLoaded = net.neoforged.fml.loading.FMLLoader.getLoadingModList()
							.getModFileById("create") != null;
				}
			} catch (Exception e) {
				createLoaded = false;
			}
		}
		return createLoaded;
	}

	/**
	 * Checks if SophisticatedBackpacks mod is loaded.
	 * Result is cached for performance.
	 * Uses FMLLoader during early loading, then switches to ModList after it's available.
	 */
	public static boolean isSophisticatedBackpacksLoaded() {
		if (sophisticatedBackpacksLoaded == null) {
			try {
				// Try ModList first (preferred, available after mod discovery)
				var modList = ModList.get();
				if (modList != null) {
					sophisticatedBackpacksLoaded = modList.isLoaded("sophisticatedbackpacks");
				} else {
					// Fall back to FMLLoader during very early loading
					sophisticatedBackpacksLoaded = net.neoforged.fml.loading.FMLLoader.getLoadingModList()
							.getModFileById("sophisticatedbackpacks") != null;
				}
			} catch (Exception e) {
				// If anything fails, assume not loaded
				sophisticatedBackpacksLoaded = false;
			}
		}
		return sophisticatedBackpacksLoaded;
	}

	public static boolean isItemBlockProxy(ItemStack stack) {
		return isItemBlockProxy(stack, true);
	}

	// Returns whether this stack can provide placeable blocks.
	public static boolean isItemBlockProxy(ItemStack stack, boolean seeBlockItemsAsProxies) {
		Item item = stack.getItem();
		if (item instanceof BlockItem)
			return seeBlockItemsAsProxies;
		return item instanceof AbstractRandomizerBagItem;
	}

	// Preview selection is cached per position for short-term stability.
	public static ItemStack getItemBlockForPosition(ItemStack proxy, BlockPos pos, @Nullable Player player) {
		Item proxyItem = proxy.getItem();

		if (proxyItem instanceof BlockItem)
			return proxy;

		//Randomizer Bag
		if (proxyItem instanceof AbstractRandomizerBagItem randomizerBagItem) {
			IItemHandler bagInventory = randomizerBagItem.getBagInventory(proxy);
			if (bagInventory == null) return ItemStack.EMPTY;
			
			// Per-position cached selection - only uses templates available in player inventory
			return randomizerBagItem.pickRandomStackCachedForPosition(bagInventory, pos, player);
		}

		return ItemStack.EMPTY;
	}

	// Returns a new random placement candidate (no position cache).
	public static ItemStack getItemBlockFromStackFresh(ItemStack proxy, @Nullable Player player) {
		Item proxyItem = proxy.getItem();

		if (proxyItem instanceof BlockItem)
			return proxy;

		//Randomizer Bag
		if (proxyItem instanceof AbstractRandomizerBagItem randomizerBagItem) {
			IItemHandler bagInventory = randomizerBagItem.getBagInventory(proxy);
			if (bagInventory == null) return ItemStack.EMPTY;
			
			// Fresh random for actual placement - only templates available in inventory
			return randomizerBagItem.pickRandomTemplateFresh(bagInventory, player);
		}

		return ItemStack.EMPTY;
	}

	// Deprecated. Use getItemBlockForPosition.
	public static ItemStack getItemBlockFromStack(ItemStack proxy) {
		return getItemBlockFromStackFresh(proxy, null);
	}

	public static ItemStack getItemBlockByState(ItemStack stack, BlockState state) {
		if (state == null) return ItemStack.EMPTY;

		Item blockItem = Item.byBlock(state.getBlock());
		if (stack.getItem() instanceof BlockItem)
			return stack;
		else if (stack.getItem() instanceof AbstractRandomizerBagItem) {
			AbstractRandomizerBagItem randomizerBagItem = (AbstractRandomizerBagItem) stack.getItem();
			IItemHandler bagInventory = randomizerBagItem.getBagInventory(stack);
			return randomizerBagItem.findStack(bagInventory, blockItem);
		}

		return ItemStack.EMPTY;
	}

	public static boolean containsBlock(ItemStack stack, Block block) {
		if (stack == null || stack.isEmpty() || !isItemBlockProxy(stack)) {
			return block == null || block == Blocks.AIR;
		}

		if (stack.getItem() instanceof BlockItem) {
			return ((BlockItem) stack.getItem()).getBlock() == block;
		}

		if (stack.getItem() instanceof AbstractRandomizerBagItem randomizerBagItem) {
			IItemHandler bagInventory = randomizerBagItem.getBagInventory(stack);
			ItemStack firstMatch = ItemHelper.findFirstMatch(bagInventory, s -> s.getItem() instanceof BlockItem);
			return firstMatch != null && !firstMatch.isEmpty();
		}
		return false;
	}

}
