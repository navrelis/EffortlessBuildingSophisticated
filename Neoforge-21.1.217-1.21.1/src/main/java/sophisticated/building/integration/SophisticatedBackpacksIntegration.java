package sophisticated.building.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import sophisticated.building.gui.BuildingUpgradeContainer;
import sophisticated.building.item.upgrade.BuildingUpgradeItem;
import sophisticated.building.item.upgrade.BuildingUpgradeWrapper;

import java.util.function.Supplier;

public class SophisticatedBackpacksIntegration {

    private static UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> containerType;

    /**
     * Gets or creates the building upgrade container type.
     */
    public static UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> getContainerType() {
        if (containerType == null) {
            containerType = new UpgradeContainerType<>(BuildingUpgradeContainer::new);
        }
        return containerType;
    }

    public static void registerBuildingUpgrades(
            DeferredRegister.Items items,
            Supplier<DeferredItem<Item>> upgrade1Holder,
            Supplier<DeferredItem<Item>> upgrade2Holder,
            Supplier<DeferredItem<Item>> upgrade3Holder,
            Supplier<DeferredItem<Item>> upgrade4Holder,
            Supplier<DeferredItem<Item>> upgradeOmegaHolder
    ) {
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
