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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class BuildingUpgradeItem extends UpgradeItemBase<BuildingUpgradeWrapper> {
    public static final UpgradeType<BuildingUpgradeWrapper> TYPE = new UpgradeType<>(BuildingUpgradeWrapper::new);

    private final int tier;
    private final int maxBlocks;

    public BuildingUpgradeItem(int tier, int maxBlocks) {
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
        if (tier == 5) {
            tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.omega_tooltip")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.backpack_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Only one building upgrade per backpack. Sophisticated Core 1.19.4 (0.5.109) has no upgrade groups, count limits
     * or conflict definitions, so the upgrade checks the backpack's installed upgrades itself: while one is installed,
     * another one is refused (swapping tiers in its slot stays possible, see canSwapUpgradeFor).
     */
    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
        if (!storageWrapper.getUpgradeHandler().getTypeWrappers(TYPE).isEmpty()) {
            return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.building_upgrade_conflict"),
                    Set.of(), Set.of(), Set.of());
        }
        return super.canAddUpgradeTo(storageWrapper, upgradeStack, firstLevelStorage, isClientSide);
    }

    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
        return new UpgradeSlotChangeResult.Success();
    }

    @Override
    public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, IStorageWrapper storageWrapper, boolean isClientSide) {
        if (upgradeStackToPut.getItem() instanceof BuildingUpgradeItem) {
            return new UpgradeSlotChangeResult.Success();
        }
        return super.canSwapUpgradeFor(upgradeStackToPut, storageWrapper, isClientSide);
    }
}
