package sophisticated.building.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.api.UpgradeType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.UpgradeItemBase;
import sophisticated.building.SophisticatedBuilding;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        // Sophisticated Backpacks 1.16.3 creates every upgrade item in its own creative tab (no-arg constructor); the
        // tab is moved to this mod's in allowdedIn / getCreativeTabs below
        super();
        this.tier = tier;
        this.maxBlocks = maxBlocks;
    }
    
    public int getTier() {
        return tier;
    }
    
    public int getMaxBlocks() {
        return maxBlocks;
    }
    
    // Listed in this mod's creative tab (and the search tab) only, like the other items of the mod
    @Override
    protected boolean allowdedIn(CreativeModeTab tab) {
        return tab == CreativeModeTab.TAB_SEARCH || tab == SophisticatedBuilding.CREATIVE_TAB;
    }

    @Override
    public Collection<CreativeModeTab> getCreativeTabs() {
        return Collections.singletonList(SophisticatedBuilding.CREATIVE_TAB);
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

    // Sophisticated Backpacks 1.16.3 (1.0.0.94) has no upgrade slot change checks (canAddUpgradeTo & co. arrive with
    // later 1.16 builds), so a backpack cannot refuse a second Building Upgrade. BuildingUpgradeHelper uses the
    // highest enabled tier of each backpack, and every backpack only once.
}
