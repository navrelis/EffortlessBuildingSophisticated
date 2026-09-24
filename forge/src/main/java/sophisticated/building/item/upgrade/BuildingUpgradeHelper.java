package sophisticated.building.item.upgrade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.platform.Services;
import sophisticated.building.compatibility.CuriosCompatHelper;
import sophisticated.building.integration.BackpackScanCompat;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import sophisticated.building.SophisticatedBuilding;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BuildingUpgradeHelper {

    /**
     * Finds the highest-tier enabled Building Upgrade across every backpack the player carries.
     * {@link PlayerInventoryProvider#runOnBackpacks} covers main inventory, offhand, worn chest
     * slot and any inventory handler SophisticatedBackpacks' own compat layer registered
     * (Trinkets, and the official port's own Curios compat, see
     * net.p3pp3rf1y.sophisticatedbackpacks.compat.curios.CuriosCompat); {@link CuriosCompatHelper}
     * is still consulted afterwards as a belt-and-braces fallback in case a given backpacks build
     * does not auto-register a Curios handler. When the provider's own scan already covered
     * Curios, a stack it yielded would otherwise be visited a second time here; those duplicates
     * are skipped (see {@link #alreadyVisited}). Must only be called on the logical server: on the
     * client the backpack upgrade inventory is never fully synced (see
     * 03_ROOT_CAUSE_BUILDING_UPGRADE.md RC2), so this always returns null there.
     */
    @Nullable
    public static BuildingUpgradeWrapper findBestBuildingUpgrade(Player player) {
        if (player.level.isClientSide()) {
            SophisticatedBuilding.logger.debug("findBestBuildingUpgrade called on the client; ignoring.");
            return null;
        }

        BuildingUpgradeWrapper[] best = new BuildingUpgradeWrapper[1];
        Set<ItemStack> visitedStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<UUID> visitedContentsUuids = new HashSet<>();
        try {
            BackpackScanCompat.forEachBackpack(player, (backpack, invName, identifier, slot) -> {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(backpack);
                if (wrapper != null && wrapper.isEnabled()) {
                    visitedStacks.add(backpack);
                    backpackContentsUuid(wrapper).ifPresent(visitedContentsUuids::add);
                    if (best[0] == null || wrapper.getTier() > best[0].getTier()) {
                        best[0] = wrapper;
                    }
                }
                return false;
            });
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error scanning backpacks for building upgrades: {}", e.getMessage());
        }

        if (CuriosCompatHelper.isCuriosLoaded()) {
            for (ItemStack stack : CuriosCompatHelper.getBackpacksFromCurios(player)) {
                if (alreadyVisited(stack, visitedStacks, visitedContentsUuids)) {
                    continue;
                }
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
                if (wrapper != null && wrapper.isEnabled() && (best[0] == null || wrapper.getTier() > best[0].getTier())) {
                    best[0] = wrapper;
                }
            }
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

        Set<ItemStack> visitedStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<UUID> visitedContentsUuids = new HashSet<>();
        try {
            BackpackScanCompat.forEachBackpack(player, (backpack, invName, identifier, slot) -> {
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(backpack);
                if (wrapper != null && wrapper.isEnabled()) {
                    visitedStacks.add(backpack);
                    backpackContentsUuid(wrapper).ifPresent(visitedContentsUuids::add);
                    wrappers.add(wrapper);
                }
                return false;
            });
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error scanning backpacks for building upgrades: {}", e.getMessage());
        }

        if (CuriosCompatHelper.isCuriosLoaded()) {
            for (ItemStack stack : CuriosCompatHelper.getBackpacksFromCurios(player)) {
                if (alreadyVisited(stack, visitedStacks, visitedContentsUuids)) {
                    continue;
                }
                BuildingUpgradeWrapper wrapper = getBuildingUpgradeFromBackpack(stack);
                if (wrapper != null && wrapper.isEnabled()) {
                    wrappers.add(wrapper);
                }
            }
        }

        return wrappers;
    }

    /**
     * Whether {@code curiosStack} was already visited by {@link BackpackScanCompat#forEachBackpack}
     * (which, on backpacks builds with a Curios compat handler registered, already scans worn
     * Curios slots itself). {@link CuriosCompatHelper#getBackpacksFromCurios} is only a fallback
     * for builds that lack that handler, so without this check the same backpack could be counted
     * (and extracted from) twice: once through the provider's scan, once through this fallback.
     * <p>
     * Checked first by reference identity, which holds whenever Curios hands back the same
     * {@link ItemStack} instance the provider's scan already saw (true for the built-in
     * {@code ItemStackHandler}-backed slot storage Curios uses). As a second safety net in case a
     * given Curios/Backpacks build instead hands back a copy, it falls back to comparing the
     * backpack's persistent contents UUID ({@link IStorageWrapper#getContentsUuid()}), which is
     * stable across copies of the same backpack stack.
     */
    private static boolean alreadyVisited(ItemStack curiosStack, Set<ItemStack> visitedStacks, Set<UUID> visitedContentsUuids) {
        if (visitedStacks.contains(curiosStack)) {
            return true;
        }
        if (visitedContentsUuids.isEmpty()) {
            return false;
        }
        Optional<UUID> contentsUuid = backpackContentsUuid(curiosStack);
        return contentsUuid.isPresent() && visitedContentsUuids.contains(contentsUuid.get());
    }

    private static Optional<UUID> backpackContentsUuid(BuildingUpgradeWrapper wrapper) {
        try {
            return wrapper.getStorageWrapper().getContentsUuid();
        } catch (Exception | LinkageError e) {
            return Optional.empty();
        }
    }

    private static Optional<UUID> backpackContentsUuid(ItemStack backpackStack) {
        try {
            return backpackStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).resolve().flatMap(IStorageWrapper::getContentsUuid);
        } catch (Exception | LinkageError e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the building upgrade wrapper from a backpack ItemStack.
     *
     * @param backpackStack The backpack item stack
     * @return The BuildingUpgradeWrapper if found and enabled, null otherwise
     */
    @Nullable
    public static BuildingUpgradeWrapper getBuildingUpgradeFromBackpack(ItemStack backpackStack) {
        if (backpackStack.isEmpty()) {
            return null;
        }

        try {
            IStorageWrapper wrapper = backpackStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).resolve().orElse(null);
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

        int maxFromUpgrade = getEffectiveMaxBlocksForPlayer(player, blockItem);
        if (maxFromUpgrade <= 0) {
            return ItemStack.EMPTY;
        }

        int amountToExtract = Math.min(amount, maxFromUpgrade);

        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (BuildingUpgradeWrapper wrapper : wrappers) {
            if (totalExtracted >= amountToExtract) break;

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
        
        // Sync remaining count to client
        if (!simulate && totalExtracted > 0 && player instanceof ServerPlayer serverPlayer) {
            int remaining = countBlockInBackpacksForDisplay(player, blockItem);
            Services.NETWORK.sendToPlayer(serverPlayer, new BackpackItemCountPacket(
                    Registry.ITEM.getKey(blockItem.getItem()),
                    remaining
            ));
        }
        
        return result;
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
