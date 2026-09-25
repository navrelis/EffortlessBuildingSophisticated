package sophisticated.building.integration;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.Position;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.UpgradeSettingsTabManager;
import sophisticated.building.client.gui.BuildingUpgradeSettingsTab;
import sophisticated.building.gui.BuildingUpgradeContainer;

public class SophisticatedBackpacksClientIntegration {

    public static void registerUpgradeTab() {
        UpgradeSettingsTabManager.register(
            SophisticatedBackpacksIntegration.getContainerType(),
            (BuildingUpgradeContainer c, Position p, BackpackScreen s) -> 
                new BuildingUpgradeSettingsTab(c, p, s,
                    new TranslatableComponent("sophisticatedbuilding.gui.building_upgrade.tab"),
                    new TextComponent(""))
        );
    }
}
