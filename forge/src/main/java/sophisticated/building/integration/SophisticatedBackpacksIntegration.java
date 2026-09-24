package sophisticated.building.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import sophisticated.building.gui.BuildingUpgradeContainer;
import sophisticated.building.item.upgrade.BuildingUpgradeItem;
import sophisticated.building.item.upgrade.BuildingUpgradeWrapper;

public class SophisticatedBackpacksIntegration {

    private static UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> containerType;

    private SophisticatedBackpacksIntegration() {
    }

    public static UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> getContainerType() {
        if (containerType == null) {
            containerType = new UpgradeContainerType<>(BuildingUpgradeContainer::new);
        }
        return containerType;
    }

    public static void registerUpgradeContainers(Item... upgradeItems) {
        UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> type = getContainerType();
        for (Item item : upgradeItems) {
            UpgradeContainerRegistry.register(BuiltInRegistries.ITEM.getKey(item), type);
        }
    }

    public static Item createBuildingUpgrade(int tier, int maxBlocks) {
        return new BuildingUpgradeItem(tier, maxBlocks);
    }
}
