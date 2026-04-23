package sophisticated.building.item.upgrade;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CuriosCompatHelper;
import sophisticated.building.network.message.BackpackItemCountPacket;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BuildingUpgradeHelper {

    private BuildingUpgradeHelper() {
    }

    @Nullable
    public static BuildingUpgradeWrapper findBestBuildingUpgrade(Player player) {
        BuildingUpgradeWrapper bestWrapper = null;
        int bestTier = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
            if (wrapper != null && wrapper.isEnabled() && wrapper.getTier() > bestTier) {
                bestWrapper = wrapper;
                bestTier = wrapper.getTier();
            }
        }

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

    public static List<BuildingUpgradeWrapper> findAllBuildingUpgrades(Player player) {
        List<BuildingUpgradeWrapper> wrappers = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
            if (wrapper != null && wrapper.isEnabled()) {
                wrappers.add(wrapper);
            }
        }

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

    @Nullable
    public static BuildingUpgradeWrapper getBuildingUpgradeFromBackpack(ItemStack backpackStack) {
        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof BackpackItem)) {
            return null;
        }

        try {
            IStorageWrapper wrapper = getStorageWrapperFromBackpack(backpackStack);
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

    @Nullable
    private static IStorageWrapper getStorageWrapperFromBackpack(ItemStack backpackStack) {
        try {
            Method fromStackMethod = BackpackWrapper.class.getMethod("fromStack", ItemStack.class);
            Object wrapperObj = fromStackMethod.invoke(null, backpackStack);
            if (wrapperObj instanceof IStorageWrapper storageWrapper) {
                return storageWrapper;
            }
        } catch (Exception ignored) {
            // Fall back to direct wrapper construction.
        }

        try {
            return new BackpackWrapper(backpackStack);
        } catch (Exception e) {
            SophisticatedBuilding.logger.debug("Error creating backpack wrapper fallback: {}", e.getMessage());
            return null;
        }
    }

    public static int getMaxBlocksForPlayer(Player player) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        return wrapper != null ? wrapper.getMaxBlocks() : 0;
    }

    public static int getEffectiveMaxBlocksForPlayer(Player player, ItemStack itemStack) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        if (wrapper == null) {
            return 0;
        }

        return getEffectiveMaxBlocks(wrapper);
    }

    private static int getEffectiveMaxBlocks(BuildingUpgradeWrapper wrapper) {
        int baseLimit = wrapper.getMaxBlocks();
        return Math.max(0, baseLimit);
    }

    public static ItemStack extractBlockFromBackpack(Player player, ItemStack blockItem, int amount, boolean simulate) {
        List<BuildingUpgradeWrapper> wrappers = findAllBuildingUpgrades(player);
        if (wrappers.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int maxFromUpgrade = getEffectiveMaxBlocksForPlayer(player, blockItem);
        if (maxFromUpgrade <= 0) {
            return ItemStack.EMPTY;
        }

        int amountToExtract = Math.min(amount, maxFromUpgrade);

        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (BuildingUpgradeWrapper wrapper : wrappers) {
            if (totalExtracted >= amountToExtract) {
                break;
            }

            int wrapperLimit = getEffectiveMaxBlocks(wrapper);
            if (wrapperLimit <= 0) {
                continue;
            }

            int toExtract = Math.min(amountToExtract - totalExtracted, wrapperLimit);
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

        if (!simulate && totalExtracted > 0 && player instanceof ServerPlayer serverPlayer) {
            int remaining = countBlockInBackpacksForDisplay(player, blockItem);
            ServerPlayNetworking.send(serverPlayer, new BackpackItemCountPacket(
                    BuiltInRegistries.ITEM.getKey(blockItem.getItem()),
                    remaining
            ));
        }

        return result;
    }

    public static int countBlockInBackpack(Player player, ItemStack blockItem) {
        return countBlockInBackpackClamped(player, blockItem);
    }

    public static int countBlockInBackpacksForDisplay(Player player, ItemStack blockItem) {
        int total = 0;
        List<BuildingUpgradeWrapper> wrappers = findAllBuildingUpgrades(player);
        for (BuildingUpgradeWrapper wrapper : wrappers) {
            total += wrapper.countItem(blockItem);
        }
        return total;
    }

    public static int countBlockInBackpackClamped(Player player, ItemStack blockItem) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        if (wrapper == null) {
            return 0;
        }
        int realCount = wrapper.countItem(blockItem);
        int maxBlocks = getEffectiveMaxBlocks(wrapper);
        return Math.min(realCount, maxBlocks);
    }

    public static void syncItemCount(ServerPlayer player, Item item) {
        int count = countBlockInBackpackClamped(player, new ItemStack(item));
        ServerPlayNetworking.send(player, new BackpackItemCountPacket(
                BuiltInRegistries.ITEM.getKey(item),
                count
        ));
    }

    public static boolean hasBuildingUpgrade(Player player) {
        return findBestBuildingUpgrade(player) != null;
    }

    public static int getBuildingUpgradeTier(Player player) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        return wrapper != null ? wrapper.getTier() : 0;
    }
}
