package sophisticated.building.compatibility;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import sophisticated.building.SophisticatedBuilding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper for Curios API integration.
 * All Curios-specific code is isolated here to avoid class loading issues when Curios is not installed.
 */
public class CuriosCompatHelper {

    private static Boolean curiosLoaded = null;

    /**
     * Checks if the Curios mod is loaded.
     */
    public static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            try {
                var modList = ModList.get();
                if (modList != null) {
                    curiosLoaded = modList.isLoaded("curios");
                } else {
                    curiosLoaded = net.neoforged.fml.loading.FMLLoader.getLoadingModList()
                            .getModFileById("curios") != null;
                }
            } catch (Exception e) {
                curiosLoaded = false;
            }
        }
        return curiosLoaded;
    }

    /**
     * Gets all backpack ItemStacks from Curios slots.
     * Only call this if isCuriosLoaded() returns true.
     * 
     * @param player The player to check
     * @return List of backpack ItemStacks in Curios slots
     */
    public static List<ItemStack> getBackpacksFromCurios(Player player) {
        if (!isCuriosLoaded()) {
            return List.of();
        }
        try {
            return CuriosInternalHelper.getBackpacksFromCurios(player);
        } catch (NoClassDefFoundError | Exception e) {
            SophisticatedBuilding.logger.debug("Error accessing Curios slots: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Iterates through all Curios slots and calls the consumer for each backpack found.
     * Only call this if isCuriosLoaded() returns true.
     * 
     * @param player The player to check
     * @param backpackConsumer Consumer that receives each backpack ItemStack
     */
    public static void forEachCuriosBackpack(Player player, Consumer<ItemStack> backpackConsumer) {
        if (!isCuriosLoaded()) {
            return;
        }
        try {
            CuriosInternalHelper.forEachCuriosBackpack(player, backpackConsumer);
        } catch (NoClassDefFoundError | Exception e) {
            SophisticatedBuilding.logger.debug("Error iterating Curios slots: {}", e.getMessage());
        }
    }

    /**
     * Internal helper class that actually uses Curios API.
     * Isolated to prevent class loading issues.
     */
    private static class CuriosInternalHelper {
        
        static List<ItemStack> getBackpacksFromCurios(Player player) {
            List<ItemStack> backpacks = new ArrayList<>();
            forEachCuriosBackpack(player, backpacks::add);
            return backpacks;
        }

        static void forEachCuriosBackpack(Player player, Consumer<ItemStack> consumer) {
            try {
                // Track already processed items to avoid duplicates
                java.util.Set<ItemStack> processed = new java.util.HashSet<>();
                
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(curiosHandler -> {
                    // Check all equipped curios for any backpack items
                    curiosHandler.getCurios().forEach((identifier, stacksHandler) -> {
                        var stacks = stacksHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (!stack.isEmpty() && isBackpackItem(stack) && !processed.contains(stack)) {
                                processed.add(stack);
                                consumer.accept(stack);
                            }
                        }
                    });
                });
            } catch (Exception e) {
                SophisticatedBuilding.logger.debug("Error accessing Curios inventory: {}", e.getMessage());
            }
        }

        private static boolean isBackpackItem(ItemStack stack) {
            try {
                return stack.getItem() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }
    }
}
