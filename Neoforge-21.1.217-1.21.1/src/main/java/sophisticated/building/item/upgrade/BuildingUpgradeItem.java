package sophisticated.building.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Building Upgrade item that can be placed in a backpack's upgrade slot.
 * Each tier allows placing more blocks at once when using effortless building.
 * 
 * Tier 1: 32 blocks max
 * Tier 2: 64 blocks max
 * Tier 3: 128 blocks max
 * Tier 4: 256 blocks max
 * Omega: 2048 blocks max
 */
public class BuildingUpgradeItem extends UpgradeItemBase<BuildingUpgradeWrapper> {
    

    
    public static final UpgradeType<BuildingUpgradeWrapper> TYPE = new UpgradeType<>(BuildingUpgradeWrapper::new);
    public static final UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("building_upgrades", 
            TranslationHelper.INSTANCE.translUpgradeGroup("building_upgrades"));
    
    // Simple config that allows 1 upgrade per backpack
    private static final IUpgradeCountLimitConfig LIMIT_CONFIG = new IUpgradeCountLimitConfig() {
        @Override
        public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
            return 1;
        }
        
        @Override
        public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
            return 1;
        }
    };
    
    private final int tier;
    private final int maxBlocks;
    
    /**
     * Creates a new Building Upgrade item.
     * @param tier The upgrade tier (1-4, or 5 for omega)
     * @param maxBlocks Maximum blocks that can be placed at once with this upgrade
     */
    public BuildingUpgradeItem(int tier, int maxBlocks) {
        super(LIMIT_CONFIG);
        this.tier = tier;
        this.maxBlocks = maxBlocks;
    }
    
    public int getTier() {
        return tier;
    }
    
    public int getMaxBlocks() {
        return maxBlocks;
    }
    
    @Override
    public UpgradeType<BuildingUpgradeWrapper> getType() {
        return TYPE;
    }
    
    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        // Building upgrades conflict with each other - only one can be installed
        return List.of(new UpgradeConflictDefinition(
                item -> item instanceof BuildingUpgradeItem,
                0,
                TranslationHelper.INSTANCE.translError("add.building_upgrade_conflict"),
                TranslationHelper.INSTANCE.translError("add.building_upgrade_conflict")
        ));
    }
    
    @Override
    public UpgradeGroup getUpgradeGroup() {
        return UPGRADE_GROUP;
    }
    
    @Override
    public int getUpgradesPerStorage(String storageType) {
        // Only 1 building upgrade per backpack
        return 1;
    }
    
    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        // Only 1 from the building upgrade group per backpack
        return 1;
    }
    
    @Override
    public Component getName() {
        return Component.translatable(getDescriptionId());
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.tooltip", maxBlocks)
                .withStyle(ChatFormatting.GRAY));
        if (tier == 5) { // Omega tier
            tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.omega_tooltip")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.backpack_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
    
    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
        // Use default implementation from parent
        UpgradeSlotChangeResult result = super.canAddUpgradeTo(storageWrapper, upgradeStack, firstLevelStorage, isClientSide);
        return result;
    }
    
    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
        return UpgradeSlotChangeResult.success();
    }
    
    @Override
    public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
        // Allow swapping building upgrades for other building upgrades (upgrading tiers)
        if (upgradeStackToPut.getItem() instanceof BuildingUpgradeItem) {
            return UpgradeSlotChangeResult.success();
        }
        return super.canSwapUpgradeFor(upgradeStackToPut, upgradeSlot, storageWrapper, isClientSide);
    }
}
