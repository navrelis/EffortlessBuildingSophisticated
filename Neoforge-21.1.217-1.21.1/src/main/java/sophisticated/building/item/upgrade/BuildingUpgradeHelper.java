package sophisticated.building.item.upgrade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.registries.BuiltInRegistries;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.compatibility.CuriosCompatHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import sophisticated.building.SophisticatedBuilding;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BuildingUpgradeHelper {

    /**
     * Finds the best building upgrade wrapper from a player's equipped backpacks.
     * Checks main inventory AND Curios slots for backpacks that contain building upgrades.
     * 
     * @param player The player to check
     * @return The BuildingUpgradeWrapper with the highest tier, or null if none found
     */
    @Nullable
    public static BuildingUpgradeWrapper findBestBuildingUpgrade(Player player) {
        BuildingUpgradeWrapper bestWrapper = null;
        int bestTier = 0;

        // Check all inventory slots for backpacks
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
            if (wrapper != null && wrapper.isEnabled() && wrapper.getTier() > bestTier) {
                bestWrapper = wrapper;
                bestTier = wrapper.getTier();
            }
        }

        // Also check Curios slots if Curios is loaded
        if (CuriosCompatHelper.isCuriosLoaded()) {
            List<ItemStack> curiosBackpacks = CuriosCompatHelper.getBackpacksFromCurios(player);
            for (ItemStack stack : curiosBackpacks) {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
                if (wrapper != null && wrapper.isEnabled() && wrapper.getTier() > bestTier) {
                    bestWrapper = wrapper;
                    bestTier = wrapper.getTier();
                }
            }
        }

        return bestWrapper;
    }
    
    /**
     * Gets all backpacks with building upgrades from both inventory and Curios slots.
     * @param player The player to check
     * @return List of all BuildingUpgradeWrappers (may be empty)
     */
    public static List<BuildingUpgradeWrapper> findAllBuildingUpgrades(Player player) {
        List<BuildingUpgradeWrapper> wrappers = new ArrayList<>();

        // Check all inventory slots for backpacks
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
            if (wrapper != null && wrapper.isEnabled()) {
                wrappers.add(wrapper);
            }
        }

        // Also check Curios slots if Curios is loaded
        if (CuriosCompatHelper.isCuriosLoaded()) {
            List<ItemStack> curiosBackpacks = CuriosCompatHelper.getBackpacksFromCurios(player);
            for (ItemStack stack : curiosBackpacks) {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
                if (wrapper != null && wrapper.isEnabled()) {
                    wrappers.add(wrapper);
                }
            }
        }

        return wrappers;
    }

    /**
     * Gets the building upgrade wrapper from a backpack ItemStack.
     * 
     * @param backpackStack The backpack item stack
     * @return The BuildingUpgradeWrapper if found and enabled, null otherwise
     */
    @Nullable
    public static BuildingUpgradeWrapper getBuildingUpgradeFromBackpack(ItemStack backpackStack) {
        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof BackpackItem)) {
            return null;
        }

        try {
            IStorageWrapper wrapper = BackpackWrapper.fromStack(backpackStack);
            if (wrapper == null) {
                return null;
            }

            UpgradeHandler upgradeHandler = wrapper.getUpgradeHandler();
            var wrappers = upgradeHandler.getTypeWrappers(BuildingUpgradeItem.TYPE);
            
            if (!wrappers.isEmpty()) {
                return wrappers.getFirst();
            }
        } catch (Exception e) {
            SophisticatedBuilding.logger.debug("Error checking backpack for building upgrade: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Gets the maximum blocks allowed for a player based on their best building upgrade.
     * 
     * @param player The player to check
     * @return The max blocks allowed, or 0 if no building upgrade found
     */
    public static int getMaxBlocksForPlayer(Player player) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        return wrapper != null ? wrapper.getMaxBlocks() : 0;
    }

    /**
     * Gets the maximum number of blocks that can be pulled from a backpack for a specific item, scaled
     * by stack upgrades. Some backpacks can hold stacks larger than the item's vanilla max stack size,
     * so we scale the upgrade limit by the ratio between the slot limit and the item's own limit.
     */
    public static int getEffectiveMaxBlocksForPlayer(Player player, ItemStack itemStack) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        if (wrapper == null) {
            return 0;
        }

        return getEffectiveMaxBlocks(wrapper);
    }

    /**
     * Building upgrade cap is authoritative; stack upgrades should not raise it. They only remove per-stack
     * extraction limits inside the backpack.
     */
    private static int getEffectiveMaxBlocks(BuildingUpgradeWrapper wrapper) {
        int baseLimit = wrapper.getMaxBlocks();
        return Math.max(0, baseLimit);
    }

    /**
     * Attempts to extract blocks from ALL player's backpacks with building upgrades.
     * Extracts from backpacks in order until the requested amount is fulfilled.
     * 
     * @param player The player
     * @param blockItem The block item to extract
     * @param amount The amount to extract
     * @param simulate If true, doesn't actually remove items
     * @return The extracted ItemStack (may be smaller than requested)
     */
    public static ItemStack extractBlockFromBackpack(Player player, ItemStack blockItem, int amount, boolean simulate) {
        List<BuildingUpgradeWrapper> wrappers = findAllBuildingUpgrades(player);
        if (wrappers.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;
        
        for (BuildingUpgradeWrapper wrapper : wrappers) {
            if (totalExtracted >= amount) break;
            
            // Check if upgrade is valid (has tier limit > 0)
            int maxFromUpgrade = getEffectiveMaxBlocks(wrapper);
            if (maxFromUpgrade <= 0) {
                continue;
            }

            // Extract as many as needed from this backpack
            int toExtract = amount - totalExtracted;
            ItemStack extracted = wrapper.extractItem(blockItem, toExtract, simulate);
            
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) {
                    result = extracted.copy();
                } else {
                    result.grow(extracted.getCount());
                }
                totalExtracted += extracted.getCount();
            }
        }
        
        // Sync remaining count to client
        if (!simulate && totalExtracted > 0 && player instanceof ServerPlayer serverPlayer) {
            int remaining = countBlockInBackpacksForDisplay(player, blockItem);
            PacketDistributor.sendToPlayer(serverPlayer, new BackpackItemCountPacket(
                    BuiltInRegistries.ITEM.getKey(blockItem.getItem()),
                    remaining
            ));
        }
        
        return result;
    }

    /**
     * Counts how many of a specific block are available in the player's backpack.
     * 
     * @param player The player
     * @param blockItem The block item to count
     * @return The total count available
     */
    public static int countBlockInBackpack(Player player, ItemStack blockItem) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        if (wrapper != null) {
            return wrapper.countItem(blockItem);
        }
        return 0;
    }
    
    /**
     * Counts how many of a specific block are available in ALL backpacks with building upgrades.
        * Returns unclamped total count for HUD display.
     * 
     * @param player The player
     * @param blockItem The block item to count
     * @return The total count available across all backpacks
     */
    public static int countBlockInBackpacksForDisplay(Player player, ItemStack blockItem) {
        int total = 0;
        List<BuildingUpgradeWrapper> wrappers = findAllBuildingUpgrades(player);
        for (BuildingUpgradeWrapper wrapper : wrappers) {
            total += wrapper.countItem(blockItem);
        }
        return total;
    }
    
    /**
     * Gets the count clamped by the best building upgrade tier limit.
     * Use this where tier limits apply.
     * 
     * @param player The player
     * @param blockItem The block item to count
     * @return The count clamped to the max blocks allowed by the upgrade tier
     */
    public static int countBlockInBackpackClamped(Player player, ItemStack blockItem) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        if (wrapper == null) {
            return 0;
        }
        int realCount = wrapper.countItem(blockItem);
        int maxBlocks = getEffectiveMaxBlocks(wrapper);
        return Math.min(realCount, maxBlocks);
    }

    /**
     * Syncs the count of a specific item in the backpack to the client.
     * 
     * @param player The player
     * @param item The item to sync
     */
    public static void syncItemCount(ServerPlayer player, net.minecraft.world.item.Item item) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        int count = 0;
        if (wrapper != null) {
            count = wrapper.countItem(new ItemStack(item));
        }
        PacketDistributor.sendToPlayer(player, new BackpackItemCountPacket(
                BuiltInRegistries.ITEM.getKey(item),
                count
        ));
    }

    /**
     * Checks if a player has any building upgrade available.
     * 
     * @param player The player to check
     * @return true if a building upgrade is available
     */
    public static boolean hasBuildingUpgrade(Player player) {
        return findBestBuildingUpgrade(player) != null;
    }

    /**
     * Gets the tier of the player's best building upgrade.
     * 
     * @param player The player to check
     * @return The upgrade tier (1-5), or 0 if no upgrade
     */
    public static int getBuildingUpgradeTier(Player player) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        return wrapper != null ? wrapper.getTier() : 0;
    }
}
