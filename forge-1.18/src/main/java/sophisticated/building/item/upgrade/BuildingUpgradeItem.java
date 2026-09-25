package sophisticated.building.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedbackpacks.api.UpgradeType;
import sophisticated.building.SophisticatedBuilding;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    private final int tier;
    private final int maxBlocks;
    
    /**
     * Creates a new Building Upgrade item.
     * @param tier The upgrade tier (1-4, or 5 for omega)
     * @param maxBlocks Maximum blocks that can be placed at once with this upgrade
     */
    public BuildingUpgradeItem(int tier, int maxBlocks) {
        // Sophisticated Backpacks 1.18's UpgradeItemBase takes no creative tab: it puts every upgrade into the backpacks
        // tab. fillItemCategory / getCreativeTabs below show the building upgrades in this mod's tab instead.
        super();
        this.tier = tier;
        this.maxBlocks = maxBlocks;
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (tab == SophisticatedBuilding.CREATIVE_TAB || tab == CreativeModeTab.TAB_SEARCH) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public Collection<CreativeModeTab> getCreativeTabs() {
        return Collections.singletonList(SophisticatedBuilding.CREATIVE_TAB);
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(new TranslatableComponent("item.sophisticatedbuilding.building_upgrade.tooltip", maxBlocks)
                .withStyle(ChatFormatting.GRAY));
        if (tier == 5) { // Omega tier
            tooltip.add(new TranslatableComponent("item.sophisticatedbuilding.building_upgrade.omega_tooltip")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(new TranslatableComponent("item.sophisticatedbuilding.building_upgrade.backpack_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
    
    // Sophisticated Backpacks 1.18 has no upgrade count limits or groups (they arrive with Sophisticated Core on
    // 1.18.2): only one building upgrade per backpack, refused the way SB refuses a second battery or tool swapper
    // upgrade. The slot being filled is not known here, so a tier is changed by taking the old upgrade out first.
    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IBackpackWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage) {
        Set<Integer> errorUpgradeSlots = new HashSet<>();
        storageWrapper.getUpgradeHandler().getSlotWrappers().forEach((slot, wrapper) -> {
            if (wrapper instanceof BuildingUpgradeWrapper) {
                errorUpgradeSlots.add(slot);
            }
        });
        if (!errorUpgradeSlots.isEmpty()) {
            return new UpgradeSlotChangeResult.Fail(new TranslatableComponent("sophisticatedcore.gui.error.add.building_upgrade_conflict"),
                    errorUpgradeSlots, Collections.emptySet(), Collections.emptySet());
        }
        return new UpgradeSlotChangeResult.Success();
    }
    
    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IBackpackWrapper storageWrapper) {
        return new UpgradeSlotChangeResult.Success();
    }
    
    @Override
    public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, IBackpackWrapper storageWrapper) {
        // Allow swapping building upgrades for other building upgrades (upgrading tiers)
        if (upgradeStackToPut.getItem() instanceof BuildingUpgradeItem) {
            return new UpgradeSlotChangeResult.Success();
        }
        return super.canSwapUpgradeFor(upgradeStackToPut, storageWrapper);
    }
}
