package sophisticated.building.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import javax.annotation.Nullable;
import java.util.List;

public class BuildingUpgradeItem extends UpgradeItemBase<BuildingUpgradeWrapper> {
    public static final UpgradeType<BuildingUpgradeWrapper> TYPE = new UpgradeType<>(BuildingUpgradeWrapper::new);
    public static final UpgradeGroup UPGRADE_GROUP = new UpgradeGroup("building_upgrades",
            TranslationHelper.INSTANCE.translUpgradeGroup("building_upgrades"));

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
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(
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
        return 1;
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
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
        if (tier == 5) {
            tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.omega_tooltip")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.sophisticatedbuilding.building_upgrade.backpack_tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
        return super.canAddUpgradeTo(storageWrapper, upgradeStack, firstLevelStorage, isClientSide);
    }

    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
        return UpgradeSlotChangeResult.success();
    }

    @Override
    public UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
        if (upgradeStackToPut.getItem() instanceof BuildingUpgradeItem) {
            return UpgradeSlotChangeResult.success();
        }
        return super.canSwapUpgradeFor(upgradeStackToPut, upgradeSlot, storageWrapper, isClientSide);
    }
}
