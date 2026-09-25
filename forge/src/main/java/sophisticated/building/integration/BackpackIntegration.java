package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.item.upgrade.BuildingUpgradeHelper;
import sophisticated.building.platform.services.IBackpackIntegration;

/**
 * Sophisticated Backpacks integration against the official Forge build (BuildingUpgradeHelper also
 * scans worn Curios backpacks as a fallback). Only instantiated when Sophisticated Backpacks is loaded;
 * the classes it delegates to import {@code net.p3pp3rf1y.*} and are only loaded when a method is called.
 * Sophisticated Backpacks 1.16.3 has no Tool Swapper upgrade (it arrives with later 1.16 builds), so the backpack
 * tools list stays the empty default of IBackpackIntegration.
 */
public final class BackpackIntegration implements IBackpackIntegration {

    @Override
    public Item createBuildingUpgrade(int tier, int maxBlocks) {
        return SophisticatedBackpacksIntegration.createBuildingUpgrade(tier, maxBlocks);
    }

    @Override
    public void registerUpgradeContainers(Item... upgradeItems) {
        SophisticatedBackpacksIntegration.registerUpgradeContainers(upgradeItems);
    }

    @Override
    public void registerUpgradeTab() {
        SophisticatedBackpacksClientIntegration.registerUpgradeTab();
    }

    @Override
    public int getBuildingUpgradeTier(Player player) {
        return BuildingUpgradeHelper.getBuildingUpgradeTier(player);
    }

    @Override
    public int getMaxBlocksForPlayer(Player player) {
        return BuildingUpgradeHelper.getMaxBlocksForPlayer(player);
    }

    @Override
    public int getEffectiveMaxBlocksForPlayer(Player player, ItemStack itemStack) {
        return BuildingUpgradeHelper.getEffectiveMaxBlocksForPlayer(player, itemStack);
    }

    @Override
    public int countBlockInBackpacksForDisplay(Player player, ItemStack blockItem) {
        return BuildingUpgradeHelper.countBlockInBackpacksForDisplay(player, blockItem);
    }

    @Override
    public ItemStack extractBlockFromBackpack(Player player, ItemStack blockItem, int amount, boolean simulate) {
        return BuildingUpgradeHelper.extractBlockFromBackpack(player, blockItem, amount, simulate);
    }
}
