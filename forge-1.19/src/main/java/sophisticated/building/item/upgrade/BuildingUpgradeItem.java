package sophisticated.building.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import sophisticated.building.SophisticatedBuilding;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Building Upgrade item that can be placed in a backpack's upgrade slot (Sophisticated Core 1.19-0.4.10 API).
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
        super(SophisticatedBuilding.CREATIVE_TAB);
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.tooltip", maxBlocks)
                .withStyle(ChatFormatting.GRAY));
        if (tier == 5) { // Omega tier
            tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.omega_tooltip")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.backpack_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Only one building upgrade per backpack. Sophisticated Core 1.19 has no upgrade groups, count limits or conflict
     * definitions (1.19.2+), so the upgrade checks the backpack's installed upgrades itself; while one is installed,
     * another one is refused (also in its slot: take the old tier out first).
     */
    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean isClientSide) {
        if (!storageWrapper.getUpgradeHandler().getTypeWrappers(TYPE).isEmpty()) {
            return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.building_upgrade_conflict"),
                    Set.of(), Set.of(), Set.of());
        }
        return super.canAddUpgradeTo(storageWrapper, upgradeStack, isClientSide);
    }

    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper) {
        return new UpgradeSlotChangeResult.Success();
    }

    @Override
    public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, IStorageWrapper storageWrapper) {
        // Allow swapping building upgrades for other building upgrades (upgrading tiers)
        if (upgradeStackToPut.getItem() instanceof BuildingUpgradeItem) {
            return new UpgradeSlotChangeResult.Success();
        }
        return super.canSwapUpgradeFor(upgradeStackToPut, storageWrapper);
    }
}
