package sophisticated.building.item.upgrade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.BackpackWrapperLookup;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.integration.BackpackScanCompat;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.platform.Services;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BuildingUpgradeHelper {

    private BuildingUpgradeHelper() {
    }

    /**
     * Finds the highest-tier enabled Building Upgrade across every backpack the player carries
     * (main inventory, offhand, worn chest slot, and Trinkets/Accessories/Curios via
     * {@link PlayerInventoryProvider}). Must only be called on the logical server: on the client
     * the backpack upgrade inventory is never fully synced (see 03_ROOT_CAUSE_BUILDING_UPGRADE.md
     * RC2), so this always returns null there.
     */
    @Nullable
    public static BuildingUpgradeWrapper findBestBuildingUpgrade(Player player) {
        if (player.level.isClientSide()) {
            SophisticatedBuilding.logger.debug("findBestBuildingUpgrade called on the client; ignoring.");
            return null;
        }

        BuildingUpgradeWrapper[] best = new BuildingUpgradeWrapper[1];
        try {
            BackpackScanCompat.forEachBackpack(player, (backpack, invName, identifier, slot) -> {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(backpack);
                if (wrapper != null && wrapper.isEnabled() && (best[0] == null || wrapper.getTier() > best[0].getTier())) {
                    best[0] = wrapper;
                }
                return false;
            });
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error scanning backpacks for building upgrades: {}", e.getMessage());
        }

        return best[0];
    }

    /**
     * Finds every enabled Building Upgrade across every backpack the player carries. Server-only,
     * see {@link #findBestBuildingUpgrade(Player)}.
     */
    public static List<BuildingUpgradeWrapper> findAllBuildingUpgrades(Player player) {
        List<BuildingUpgradeWrapper> wrappers = new ArrayList<>();
        if (player.level.isClientSide()) {
            SophisticatedBuilding.logger.debug("findAllBuildingUpgrades called on the client; ignoring.");
            return wrappers;
        }

        try {
            BackpackScanCompat.forEachBackpack(player, (backpack, invName, identifier, slot) -> {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(backpack);
                if (wrapper != null && wrapper.isEnabled()) {
                    wrappers.add(wrapper);
                }
                return false;
            });
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error scanning backpacks for building upgrades: {}", e.getMessage());
        }

        return wrappers;
    }

    @Nullable
    public static BuildingUpgradeWrapper getBuildingUpgradeFromBackpack(ItemStack backpackStack) {
        if (backpackStack.isEmpty()) {
            return null;
        }

        try {
            IStorageWrapper wrapper = BackpackWrapperLookup.get(backpackStack).orElse(null);
            if (wrapper == null) {
                return null;
            }

            UpgradeHandler upgradeHandler = wrapper.getUpgradeHandler();
            var typeWrappers = upgradeHandler.getTypeWrappers(BuildingUpgradeItem.TYPE);
            if (!typeWrappers.isEmpty()) {
                return typeWrappers.get(0);
            }

            // getTypeWrappers only contains wrappers that were enabled when the type cache was last
            // built; fall back to a direct scan of every installed upgrade so a stale cache can
            // never hide an upgrade that is actually enabled (see RC3 in 03_ROOT_CAUSE...md).
            for (IUpgradeWrapper slotWrapper : upgradeHandler.getSlotWrappers().values()) {
                if (slotWrapper instanceof BuildingUpgradeWrapper buildingUpgradeWrapper && buildingUpgradeWrapper.isEnabled()) {
                    return buildingUpgradeWrapper;
                }
            }
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error checking backpack for building upgrade: {}", e.getMessage());
        }

        return null;
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
            Services.NETWORK.sendToPlayer(serverPlayer, new BackpackItemCountPacket(
                    BuiltInRegistries.ITEM.getKey(blockItem.getItem()),
                    remaining
            ));
        }

        return result;
    }

    public static int countBlockInBackpacksForDisplay(Player player, ItemStack blockItem) {
        int total = 0;
        List<BuildingUpgradeWrapper> wrappers = findAllBuildingUpgrades(player);
        for (BuildingUpgradeWrapper wrapper : wrappers) {
            total += wrapper.countItem(blockItem);
        }
        return total;
    }

    public static boolean hasBuildingUpgrade(Player player) {
        return findBestBuildingUpgrade(player) != null;
    }

    public static int getBuildingUpgradeTier(Player player) {
        BuildingUpgradeWrapper wrapper = findBestBuildingUpgrade(player);
        return wrapper != null ? wrapper.getTier() : 0;
    }
}
